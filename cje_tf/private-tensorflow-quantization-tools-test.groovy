static final String node_label = params.get('NODE_LABEL', 'skx' || 'bdw')

static final String private_tf_repo_url = "https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git"
static final String private_tf_repo_branch = params.get('TF_BRANCH', 'quantization-develop')

static final String docker_registry_url =  'https://amr-registry.caas.intel.com'
static final String docker_registry_credentials = 'ai_bot'

static final String docker_image_name = params.get('IMAGE_NAMESPACE', 'amr-registry.caas.intel.com/aipg-tf/pr')
static final String docker_build_version = "private-tf-quantization-tools"
static final String docker_image = "${docker_image_name}:${docker_build_version}"

static final String jenkins_git_credentials = "lab_tfbot"
static final String jenkins_git_credentials_lab = "lab_tfbot"


http_proxy="http://proxy-chain.intel.com:911"
https_proxy="https://proxy-chain.intel.com:912"

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


        stage('Test') {
            WORKSPACE_VOLUME="${WORKSPACE}:/workspace"
            TENSORFLOW_DIR="${WORKSPACE}:/workspace/private-tensorflow"
            echo WORKSPACE_VOLUME
            echo TENSORFLOW_DIR

            docker.image(docker_image).inside("--env \"http_proxy=${http_proxy}\" \
                                                      --env \"https_proxy=${https_proxy}\" \
                                                      --volume ${WORKSPACE_VOLUME} \
                                                      -u root:root" ) { 

                    sh '''#!/bin/bash -x
                    export JAVA_HOME="/usr/lib/jvm/java-8-openjdk-amd64"
                    cd /workspace
                    cd private-tensorflow
                    bazel clean
                    bazel build tensorflow/tools/graph_transforms:transform_graph
                    bazel build tensorflow/tools/graph_transforms:summarize_graph
                    bazel test tensorflow/tools/graph_transforms:all
                    '''
            }//docker
        }//stage
    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    } finally {

    } // finally

}
