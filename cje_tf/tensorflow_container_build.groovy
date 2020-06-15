static final String node_label = params.get('NODE_LABEL', 'skx' || 'bdw')

// setting TF_CHECKOUT_REPO default to be "private-tensorflow or get input from params
TF_CHECKOUT_REPO = 'private-tensorflow'
if ('TF_CHECKOUT_REPO' in params) {
    echo "TF_CHECKOUT_REPO in params"
    if (params.TF_CHECKOUT_REPO != '') {
        TF_CHECKOUT_REPO = params.TF_CHECKOUT_REPO
        echo TF_CHECKOUT_REPO 
    }
}
echo TF_CHECKOUT_REPO

// retry number
RETRY_NUM = 3
if( 'RETRY_NUM' in params && params.RETRY_NUM != '' ) {
    RETRY_NUM = params.RETRY_NUM
}
echo "RETRY_NUM: $RETRY_NUM"

// Setting default checkout repo to private-tensorflow
String tf_checkout_repo_url = "https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git"
String jenkins_git_credentials = "lab_tfbot"
String docker_tag = 'private-tf'

if ( TF_CHECKOUT_REPO.trim() == 'tensorflow' ) {
    tf_checkout_repo_url = "https://github.com/tensorflow/tensorflow.git"
    jenkins_git_credentials = "lab_tfbot"
    docker_tag = 'tf'
}
else if ( TF_CHECKOUT_REPO.trim() == 'private-tensorflow' ) {
    tf_checkout_repo_url = "https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git"
    jenkins_git_credentials = "lab_tfbot"
    docker_tag = 'private-tf'
}
else if ( TF_CHECKOUT_REPO.trim() == 'intel-tensorflow') {
    tf_checkout_repo_url = "https://github.com/Intel-tensorflow/tensorflow.git"
    jenkins_git_credentials = "b8b085e0-f9ed-4550-b829-35f0bf5187ff"
    docker_tag = 'intel-tf'
}
else
    error 'Incorrect parameter passed in TF_CHECKOUT_REPO. Please check description for selection.'


String tf_checkout_repo_branch = params.get('TF_CHECKOUT_BRANCH', 'master')

static final String cje_tf_repo_url = "https://gitlab.devtools.intel.com/TensorFlow/QA/cje-tf.git"

static final String docker_registry_url =  'https://amr-registry.caas.intel.com'
static final String docker_registry_credentials = 'lab_tfbot'


static final String root_container = params.get('TF_ROOT_CONTAINER', 'tensorflow/tensorflow')
static final String root_container_tag = params.get('TF_ROOT_CONTAINER_TAG', 'devel')
static final String tf_repo_url = params.get('TF_REPO_URL', 'https://github.com/Intel-tensorflow/tensorflow.git')
static final String tensorflow_branch = params.get('TENSORFLOW_BRANCH', 'master')


String build_avx_containers= 'no'
String build_avx2_containers= 'no'

static final String build_type = params.get('BUILD_TYPE', 'MKLDNN')


if (COMPILER_TYPE_AVX == 'true') {
    build_avx_containers='yes'
}
if (COMPILER_TYPE_AVX2 == 'true') {
    build_avx2_containers='yes'
}

String build_type_tag = ''
if (build_type == 'MKL') {
    build_type_tag='-ml-only'
}

String build_tf_v2_containers= 'no'
String tf_version_tag = ''
if (BUILD_TENSORFLOW_V2 == "true") {
	build_tf_v2_containers='yes'
    tf_version_tag = '-TF-v2'
}

String build_tf_bfloat16_containers= 'no'
String tf_version_bfloat16_tag = ''
if (BUILD_TENSORFLOW_BFLOAT16 == "true") {
    build_tf_bfloat16_containers='yes'
    tf_version_bfloat16_tag = '-BFLOAT16'
}

static final String docker_registry_namespace = params.get('IMAGE_NAMESPACE', 'amr-registry.caas.intel.com/aipg-tf/qa')
static final String docker_build_version = params.get('BUILD_VERSION', '')
if (docker_build_version == '') {
    docker_build_version="${docker_tag}-${tensorflow_branch}${build_type_tag}${tf_version_tag}${tf_version_bfloat16_tag}"
}
else if (docker_build_version == 'nightly') {
    docker_build_version="${docker_build_version}-${tensorflow_branch}${build_type_tag}${tf_version_tag}${tf_version_bfloat16_tag}"
}

static final String branch_avx_py2 = "${docker_registry_namespace}:${docker_build_version}-devel-mkl"
static final String branch_avx_py3 = "${docker_registry_namespace}:${docker_build_version}-devel-mkl-py3"

static final String branch_avx2_py2 = "${docker_registry_namespace}:${docker_build_version}-avx2-devel-mkl"
static final String branch_avx2_py3 = "${docker_registry_namespace}:${docker_build_version}-avx2-devel-mkl-py3"

static final ArrayList docker_images_avx2 = [ "${branch_avx2_py2}" , "${branch_avx2_py3}" ]
static final ArrayList docker_images_avx = [ "${branch_avx_py2}" , "${branch_avx_py3}" ]

String enable_secure_build = 'no'
if (ENABLE_SECURE_BUILD == "true") {
	enable_secure_build='yes'    
} 

build_script =
        "BUILD_TYPE=${build_type} BUILD_AVX2_CONTAINERS=${build_avx2_containers} BUILD_AVX_CONTAINERS=${build_avx_containers} " +
        "TF_DOCKER_BUILD_DEVEL_BRANCH=${tensorflow_branch} TF_DOCKER_BUILD_VERSION=${docker_build_version} ROOT_CONTAINER_TAG=${root_container_tag} ROOT_CONTAINER=${root_container} " +
        "BUILD_TF_V2_CONTAINERS=${build_tf_v2_containers} BUILD_TF_BFLOAT16_CONTAINERS=${build_tf_bfloat16_containers} TF_DOCKER_BUILD_IMAGE_NAME=${docker_registry_namespace}  ENABLE_SECURE_BUILD=${enable_secure_build} ./build-dev-container.sh "
 echo build_script

node(node_label) {    

    try {
        stage('CleanUp') {

            sh '''#!/bin/bash -x
                cd $WORKSPACE
                sudo rm -rf *
                docker stop $(docker ps -a -q)
                echo Y | docker system prune -a
            '''
        }

        // pull the cje-tf
        dir( 'cje-tf' ) {
            checkout scm
        }

        stage('Checkout') {

            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: tf_checkout_repo_branch]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'tensorflow']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$jenkins_git_credentials",
                                                           url          : "$tf_checkout_repo_url"]]])


        }

        stage('Build') {           

            withCredentials([usernamePassword(credentialsId: "$jenkins_git_credentials" , passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                dir("${WORKSPACE}/tensorflow/tensorflow/tools/ci_build/linux/mkl") {
                    withEnv(["TF_REPO=https://$GIT_USERNAME:$GIT_PASSWORD@$tf_repo_url"]) {
                        sh '''#!/bin/bash -x
                        sed -e 's/BUILD_TF_V2_CONTAINERS=\${BUILD_TF_V2_CONTAINERS:-yes}/BUILD_TF_V2_CONTAINERS=\${BUILD_TF_V2_CONTAINERS:-yes}\\nBUILD_TF_BFLOAT16_CONTAINERS=\${BUILD_TF_BFLOAT16_CONTAINERS:-no}\\n/' \
                            -e 's/debug \"BUILD_TF_V2_CONTAINERS=\${BUILD_TF_V2_CONTAINERS}\"/debug \"BUILD_TF_V2_CONTAINERS=\${BUILD_TF_V2_CONTAINERS}\"\\ndebug \"BUILD_TF_BFLOAT16_CONTAINERS=\${BUILD_TF_BFLOAT16_CONTAINERS}\"/' \
                            -e 's/  #Add build arg for Secure Build/  #Add build arg for bfloat16 build\\n  if [[ \${BUILD_TF_BFLOAT16_CONTAINERS} == \"no\" ]]; then\\n    TF_DOCKER_BUILD_ARGS+=(\"--build-arg CONFIG_BFLOAT16_BUILD=--disable-bfloat16\")\\n  fi\\n\\n  #Add build arg for Secure Build/' ./build-dev-container.sh > ./build-dev-container.sh.tmp
                        cp ./build-dev-container.sh.tmp ./build-dev-container.sh

                        sed -e 's/ARG ENABLE_SECURE_BUILD/ARG ENABLE_SECURE_BUILD\\nARG CONFIG_BFLOAT16_BUILD=\"\"/' -e 's/\${ENABLE_SECURE_BUILD}/\${ENABLE_SECURE_BUILD} \${CONFIG_BFLOAT16_BUILD}/' ./Dockerfile.devel-mkl > ./Dockerfile.devel-mkl.tmp
                        cp ./Dockerfile.devel-mkl.tmp ./Dockerfile.devel-mkl

                        sed -e 's/      self.bazel_flags_ += \"--config=v2 \"/      self.bazel_flags_ += \"--config=v2 \"\\n    if not self.args.disable_bfloat16:\\n      self.bazel_flags_ += "--copt=-DENABLE_INTEL_MKL_BFLOAT16 \"/' \
                            -e 's/    self.args = arg_parser.parse_args()/    arg_parser.add_argument(\\n        \"--disable-bfloat16\",\\n        dest=\"disable_bfloat16\",\\n        help=\"Disable bfloat16 build by default.\",\\n        action=\"store_true\")\\n\\n    self.args = arg_parser.parse_args()/' ./set-build-env.py > ./set-build-env.py.tmp
                        cp ./set-build-env.py.tmp ./set-build-env.py
                        '''
                        sh 'env'
                        sh build_script
                    }
                }
            }

        }

        stage('Test') {
            docker_test_dir="${WORKSPACE}/cje-tf/docker/tests"
            workspace_scripts="${WORKSPACE}/cje-tf/scripts"
            test_script = "test_docker_image.sh"
            test_command = "DOCKER_TEST_DIR=${docker_test_dir} TEST_SCRIPT=${test_script} ${workspace_scripts}/run_docker_image_tests.sh"

            // AVX
            if (COMPILER_TYPE_AVX == "true") {
                for (docker_image in docker_images_avx) {
                    sh "DOCKER_IMAGE=${docker_image} ${test_command}"
                }
            }

            // AVX2
            if (COMPILER_TYPE_AVX2 == "true") {
                for (docker_image in docker_images_avx2) {
                    sh "DOCKER_IMAGE=${docker_image} ${test_command}"
                }
            }
        }

        stage('Push') {
            // push to public internal registry, getting latest CA chain from Intel's PKI
            // to fix this issue, we do below. It works already on some nodes but not all, see https://soco.intel.com/groups/caas-evaluation-workgroup/blog/2018/11/13/how-to-using-container-registry-and-its-various-features
            // -- docker push amr-registry.caas.intel.com/aipg-tf/dev:centos7
            //      The push refers to repository [amr-registry.caas.intel.com/aipg-tf/dev]
            //      Get https://amr-registry.caas.intel.com/v2/: x509: certificate signed by unknown authority
            sh """
            # depending on machine, regardless we want unzip
            sudo apt-get -y -qq install unzip || sudo yum install -y unzip
            http_proxy='' &&\
              curl http://certificates.intel.com/repository/certificates/IntelSHA2RootChain-Base64.zip > /tmp/IntelSHA2RootChain-Base64.zip
            sudo unzip -o /tmp/IntelSHA2RootChain-Base64.zip -d /usr/local/share/ca-certificates/
            rm /tmp/IntelSHA2RootChain-Base64.zip
            sudo update-ca-certificates || sudo update-ca-trust
            sudo service docker restart
            """
            retry(RETRY_NUM) {
                docker.withRegistry(docker_registry_url, docker_registry_credentials) {
                    if (COMPILER_TYPE_AVX == "true") {
                        for (dockerImage in docker_images_avx) {
                            docker.image(dockerImage).push()
                        }
                    }

                    // AVX2
                    if (COMPILER_TYPE_AVX2 == "true") {
                        for (dockerImage in docker_images_avx2) {
                            docker.image(dockerImage).push()
                        }
                    }
                }
            }
        }
    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    } finally {

    } // finally
}
