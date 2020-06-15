static final String node_label = params.get('NODE_LABEL', 'skx')
static final String tf_build_repo = params.get('TENSORFLOW_REPO', 'https://github.com/Intel-tensorflow/tensorflow')
static final String tf_build_branch = params.get('TENSORFLOW_BRANCH', 'master')
static final String container_uri = params.get('CONTAINER_URI', 'amr-registry.caas.intel.com/aipg-tf/manylinux2010')
static final String bazel_version = params.get('BAZEL_VERSION', '0.29.1')
static final String python_versions = params.get('PYTHON_VERSIONS', '3.5,3.6,3.7')

// TODO: For now just check for first char in the string until we have the Character.isDigit whitelisted
// static final String docker_tag_version = (tf_build_branch.startsWith("v") && Character.isDigit(tf_build_branch.charAt(1)) ? tf_build_branch.substring(1) : tf_build_branch) + "-devel-mkl"
static final String docker_tag_version = (tf_build_branch.startsWith("v") ? tf_build_branch.substring(1) : tf_build_branch) + "-devel-mkl"

static final String cje_tf_repo_url = "https://gitlab.devtools.intel.com/TensorFlow/QA/cje-tf.git"
static final String jenkins_git_credentials = "43dd58a3-200f-439d-af6f-846457950129"
static final String jenkins_git_credentials_lab = "lab_tfbot"
static final String docker_registry_url =  'https://amr-registry.caas.intel.com'
static final String docker_registry_credentials = 'lab_tfbot'

// Check if building secure wheels is requested
String enable_secure_build = 'no'
if (ENABLE_SECURE_BUILD == "true") {
    enable_secure_build='yes'
}

// Check if building wheels is requested
String build_whls = '0'
if (BUILD_WHLS == "true") {
    build_whls = '1'
}

static ArrayList docker_images = []
ArrayList py_versions = python_versions.split(',')
for (version in py_versions) {
  docker_images += container_uri+":"+docker_tag_version+"-py"+version
}

// BUILD_V1 is used for TensorFlow older than v2.0.0
String BUILD_V1='n'
if (
    (tf_build_branch.startsWith("v1.") && tf_build_branch < "v2.0.0") ||
    (tf_build_branch.startsWith("r1.") && tf_build_branch < "r2.0")) {
       BUILD_V1='y'
}

node(node_label) {

    try {
        stage('CleanUp') {

            // TODO: put back
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
                      branches                         : [[name: tf_build_branch]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'tensorflow']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$jenkins_git_credentials",
                                                           url          : "$tf_build_repo"]]])


        }

        stage('Build') {
            // build dev containers, will contain wheels
            sh """#!/bin/bash -x
            set -e
            
            base_dir=`pwd`
            cd $WORKSPACE/tensorflow/

            BUILD_DIR="$WORKSPACE/tensorflow/tensorflow/tools/ci_build"
            pushd \$BUILD_DIR                
            #TAG=`git describe --tags --always --dirty`

            # copy Dockerfile/scripts to BUILD_DIR
            cp $WORKSPACE/cje-tf/docker/Dockerfiles/Dockerfile.rbe.ubuntu16.04-manylinux2010 .
            cp $WORKSPACE/cje-tf/docker/manylinux2010_container_build.sh .
            cp $WORKSPACE/cje-tf/docker/scripts/install_manylinux_python.sh ./install
            cp $WORKSPACE/cje-tf/docker/scripts/install_manylinux_pip.sh ./install
            cp $WORKSPACE/cje-tf/docker/scripts/install_patchelf.sh ./install
            cp $WORKSPACE/cje-tf/docker/scripts/manylinux2010_whl_build.sh .

            BUILD_WHLS=${build_whls} TENSORFLOW_REPO=${tf_build_repo} PYTHON_VERSIONS=${python_versions} TENSORFLOW_BRANCH=${tf_build_branch} BAZEL_VERSION="${bazel_version}" BUILD_V1=${BUILD_V1} CONTAINER_URI=${container_uri} CONTAINER_TAG=${docker_tag_version} ./manylinux2010_container_build.sh
            if [ "${build_whls}" = "1" ]; then
              echo "Change the name to intel-tensorflow"
              # And audited for ManyLinux201x
              popd

              # extract tf created wheels from internal images
              # puts the wheels into $WORKSPACE/publish
              OUTPUT_DIR=$WORKSPACE/publish
              mkdir -p \$OUTPUT_DIR

              # copy wheel files into output_dir
              cp $WORKSPACE/tensorflow/tensorflow/tools/ci_build/*.whl $WORKSPACE/publish

              pushd \$OUTPUT_DIR

              # convert to intel-tensorflow 

              # delete non manylinux tensorflow wheels so as to not make their new packages
              find . -name "tensorflow-*" -not -path "*manylinux2010*" -exec rm {} \\;
              
              # this wheel version needed for making new packages
              python -m pip install virtualenv
              virtualenv -p python3 venv
              . venv/bin/activate
              pip install wheel==0.32.2
              $WORKSPACE/cje-tf/scripts/make_new_package_name.py tensorflow intel_tensorflow
              deactivate

              # delete tensorflow manylinux wheels since we have intel-tensorflow ones
              find . -name "tensorflow-*" -exec rm {} \\;

              # create shasums
              for file in intel_tensorflow*; do
                  sha256sum \$file > \$file.sha256sum
              done

              popd
            fi
            """
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
            yes | sudo unzip -o /tmp/IntelSHA2RootChain-Base64.zip -d /usr/local/share/ca-certificates/
            rm /tmp/IntelSHA2RootChain-Base64.zip
            sudo update-ca-certificates || sudo update-ca-trust
            sudo service docker restart
            """
            docker.withRegistry(docker_registry_url, docker_registry_credentials) {
                for (dockerImage in docker_images) {
                    docker.image(dockerImage).push()
                }
            }
        }
    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    } finally {
        if (BUILD_WHLS == "true") {
          stage('Archive Tensorflow Wheels') {
            dir("$WORKSPACE" + "/publish") {
                archiveArtifacts artifacts: 'intel_tensorflow*.whl', excludes: null
                fingerprint: true

                def server = Artifactory.server 'ubit-artifactory-or'
                def uploadSpec = """{
              "files": [
               {
                   "pattern": "intel_tensorflow*.whl",
                   "target": "aipg-local/aipg-tf/${env.JOB_NAME}/${env.BUILD_NUMBER}/"
               }
               ]
            }"""
                def buildInfo = server.upload(uploadSpec)
                server.publishBuildInfo(buildInfo)

            }
          }
        }
    } // finally
}
