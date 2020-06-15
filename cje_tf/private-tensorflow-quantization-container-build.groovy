static final String node_label = params.get('NODE_LABEL', 'skx' || 'bdw')

static final String private_tf_repo_url = "https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git"
static final String private_tf_repo_branch = params.get('TF_BRANCH', 'quantization-develop')

static final String cje_tf_repo_url = "https://gitlab.devtools.intel.com/TensorFlow/QA/cje-tf.git"

static final String docker_registry_url =  'https://amr-registry.caas.intel.com'
static final String docker_registry_credentials = 'lab_tfbot'

static final String docker_image_name = params.get('IMAGE_NAMESPACE', 'amr-registry.caas.intel.com/aipg-tf/pr')
static final String docker_build_version = "private-tf-quantization-tools"
static final String docker_image = "${docker_image_name}:${docker_build_version}"

static final String jenkins_git_credentials = "lab_tfbot"
static final String jenkins_git_credentials_lab = "lab_tfbot"


node(node_label) {
    try {
        stage('CleanUp') {
            sh '''#!/bin/bash -x
                cd $WORKSPACE
                sudo rm -rf *
                sudo rm -rf .??* || true
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
                      branches                         : [[name: private_tf_repo_branch]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'private-tensorflow']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$jenkins_git_credentials_lab",
                                                           url          : "$private_tf_repo_url"]]])

        }

        stage('Build') {

            sh """#!/bin/bash -x
            pushd private-tensorflow/tensorflow/tools/docker

            # build empty docker images
            docker build -f Dockerfile.quantization-tools -t $docker_image --build-arg http_proxy=http://proxy-us.intel.com:911 --build-arg https_proxy=http://proxy-us.intel.com:912 .

            """

        }

        stage('Push') {
            sh """
            # depending on machine, regardless we want unzip
            sudo apt-get -y -qq install unzip || sudo yum install -y unzip
            http_proxy='' &&\
              curl http://certificates.intel.com/repository/certificates/IntelSHA2RootChain-Base64.zip > /tmp/IntelSHA2RootChain-Base64.zip
            yes | sudo unzip /tmp/IntelSHA2RootChain-Base64.zip -d /usr/local/share/ca-certificates/
            rm /tmp/IntelSHA2RootChain-Base64.zip
            sudo update-ca-certificates || sudo update-ca-trust
            sudo service docker restart
            """
            docker.withRegistry(docker_registry_url, docker_registry_credentials) {
                docker.image(docker_image).push()
            }
        }
    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    } finally {

    } // finally
}
