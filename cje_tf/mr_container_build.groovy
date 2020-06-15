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
RETRY_NUM = 2
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
    // ??
    //jenkins_git_credentials = "b8b085e0-f9ed-4550-b829-35f0bf5187ff"
    jenkins_git_credentials = "lab_tfbot"
    docker_tag = 'intel-tf'
}
else
    error 'Incorrect parameter passed in TF_CHECKOUT_REPO. Please check description for selection.'

static final String cje_tf_repo_url = "https://gitlab.devtools.intel.com/TensorFlow/QA/cje-tf.git"

static final String docker_registry_url =  'https://amr-registry.caas.intel.com'
static final String docker_registry_credentials = 'lab_tfbot'

static final String root_container = params.get('TF_ROOT_CONTAINER', 'tensorflow/tensorflow')
static final String root_container_tag = params.get('TF_ROOT_CONTAINER_TAG', 'devel')
static final String mr_number = params.get('MR_NUMBER', '')
static final String mr_source_branch = params.get('MR_SOURCE_BRANCH', '')
static final String mr_merge_branch = params.get('MR_MERGE_BRANCH', '')

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

static final String docker_registry_namespace = params.get('IMAGE_NAMESPACE', 'amr-registry.caas.intel.com/aipg-tf/mr')
static final String docker_build_version = params.get('BUILD_VERSION', '')
if (docker_build_version == '') {
    if (docker_tag == 'private-tf') {
        docker_build_version="MR${mr_number}${build_type_tag}${tf_version_tag}" // To match old PR container name for existing Jenkins jobs
    }
    else {
        docker_build_version="${docker_tag}-MR${mr_number}${build_type_tag}${tf_version_tag}"
    }    
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

            checkout([$class                        : 'GitSCM',
                branches                            : [[name: "$mr_source_branch"]],
                doGenerateSubmoduleConfigurations   : false,
                extensions                          : [
                                                        [$class: 'PreBuildMerge', 
                                                            options: [
                                                                    fastForwardMode: 'FF', 
                                                                    mergeRemote: 'tensorflow', 
                                                                    mergeStrategy: 'DEFAULT', 
                                                                    mergeTarget: "$mr_merge_branch"
                                                            ]
                                                        ],
                                                        [$class: 'RelativeTargetDirectory',
                                                        relativeTargetDir: "tensorflow"
                                                        ]
                                                      ],
                submoduleCfg                        : [],
                userRemoteConfigs                   : [[credentialsId: "$jenkins_git_credentials",
                                                       name: 'tensorflow',
                                                       url: "$tf_checkout_repo_url"]]
                ])
        }

        stage('Check Docker') {

            // check if docker with the same #sha already exist, if so, no need to build
            dir ( "$WORKSPACE/tensorflow" ) {
                 target_sha = sh (script:'git log -1 | grep Merge: | awk -F\' \' \'{print $2}\' | cut -c -7',
                              returnStdout: true).trim()
                 source_sha = sh (script:'git log -1 | grep Merge: | awk -F\' \' \'{print $3}\' | cut -c -7',
                              returnStdout: true).trim()
                 echo target_sha
                 echo source_sha
                 sha = "$target_sha$source_sha"
                 echo "sha is $sha"
            }
            docker_build_version="MR${mr_number}-${sha}${build_type_tag}${tf_version_tag}" // To match old PR container name for existing Jenkins jobs
            echo docker_build_version

            branch_avx_py2 = "${docker_registry_namespace}:${docker_build_version}-devel-mkl"
            branch_avx_py3 = "${docker_registry_namespace}:${docker_build_version}-devel-mkl-py3"

            branch_avx2_py2 = "${docker_registry_namespace}:${docker_build_version}-avx2-devel-mkl"
            branch_avx2_py3 = "${docker_registry_namespace}:${docker_build_version}-avx2-devel-mkl-py3"

            docker_images_avx2 = [ "${branch_avx2_py2}" , "${branch_avx2_py3}" ]
            docker_images_avx = [ "${branch_avx_py2}" , "${branch_avx_py3}" ]

            build_docker = "false"
            // AVX
            if ( COMPILER_TYPE_AVX == "true" ) {
                for (dockerImage in docker_images_avx) {
                    withEnv(["docker_image=$dockerImage"]) {
                        docker_status= sh(script:"docker pull ${docker_mage} > /dev/null && echo \"success\" || echo \"failed\"", returnStdout: true).trim()
                        echo docker_status
                    }
                    if ( docker_status == "success") {
                        echo "docker image already exist"
                    }
                    else {
                        build_docker = "true"
                    }
                }
            }

            // AVX2
            if ( COMPILER_TYPE_AVX2 == "true" ) {
                for (dockerImage in docker_images_avx2) {
                    withEnv(["docker_image=$dockerImage"]) {
                        docker_status= sh(script:"docker pull ${docker_image} > /dev/null && echo \"success\" || echo \"failed\"", returnStdout: true).trim()
                        echo docker_status
                    }
                    if ( docker_status == "success") {
                        echo "docker image already exist"
                    }
                    else {
                        build_docker = "true"
                    }
                }   
            }            

            if ( build_docker == "false" ) {
                currentBuild.result = "SUCCESS"
                return
            }

            build_script =
                    "BUILD_TYPE=${build_type} BUILD_AVX2_CONTAINERS=${build_avx2_containers} BUILD_AVX_CONTAINERS=${build_avx_containers} " +
                    "TF_DOCKER_BUILD_VERSION=${docker_build_version} ROOT_CONTAINER_TAG=${root_container_tag} ROOT_CONTAINER=${root_container} " +
                    "BUILD_TF_V2_CONTAINERS=${build_tf_v2_containers} TF_DOCKER_BUILD_IMAGE_NAME=${docker_registry_namespace}  ENABLE_SECURE_BUILD=${enable_secure_build} ./build-dev-container.sh "
            echo build_script

            if ( docker_status == "failed" ) {
        
                stage ( 'Build' ) {
                    // Workaround to use the local branch with PR merged
                    sh '''#!/bin/bash -x
                    #if [ -d "/tmp/clone_tf_rep" ]; then
                    #    rm -rf /tmp/clone_tf_rep/*
                    #else
                    #    mkdir -p /tmp/clone_tf_rep
                    #fi
                    sed -i -e 's/TF_MAJOR_VERSION 1/TF_MAJOR_VERSION 2/g' ${WORKSPACE}/tensorflow/tensorflow/core/public/version.h
                    sed -i -e 's/TF_MINOR_VERSION 14/TF_MINOR_VERSION 0/g' ${WORKSPACE}/tensorflow/tensorflow/core/public/version.h
                    sed -i -e "s;_VERSION = '1.14.0';_VERSION = '2.0.0';g"  ${WORKSPACE}/tensorflow/tensorflow/tools/pip_package/setup.py
                    sed -i -e 's/1.14.0/2.0.0/g' ${WORKSPACE}/tensorflow/tensorflow/tensorflow.bzl
                    #cp -r $WORKSPACE/tensorflow/ /tmp/clone_tf_rep/
                    #cp -r /tmp/clone_tf_rep/tensorflow ${WORKSPACE}/tensorflow/tensorflow/tools/ci_build/linux/mkl/
                    sed -i -e 's/^checkout_tensorflow "${TF_REPO}" "${TF_BUILD_VERSION}" "${TF_BUILD_VERSION_IS_PR}"//g' ${WORKSPACE}/tensorflow/tensorflow/tools/ci_build/linux/mkl/build-dev-container.sh
                    sed -i -e 's/--disable-v2//' ${WORKSPACE}/tensorflow/tensorflow/tools/ci_build/linux/mkl/Dockerfile.devel-mkl
                    sed -i "s;pywrap_tensorflow;_pywrap_util_port;g" ${WORKSPACE}/tensorflow/tensorflow/tools/ci_build/linux/mkl/build-dev-container.sh
                    cp -r ${WORKSPACE}/tensorflow/tensorflow/tools/ci_build/linux/mkl/* ${WORKSPACE}                    
                    '''
                    //dir("${WORKSPACE}/tensorflow/tensorflow/tools/ci_build/linux/mkl") {
                    dir("${WORKSPACE}/") {
                        sh build_script
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
            }
        }
    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    } finally {

    } // finally
}
