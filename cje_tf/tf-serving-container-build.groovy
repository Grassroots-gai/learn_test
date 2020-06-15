static final String node_label = params.get('NODE_LABEL', 'skx' || 'bdw')

static final String tf_serving_repo_url = "https://github.com/tensorflow/serving.git"
static final String tf_serving_repo_branch = params.get('TF_SERVING_BRANCH', 'master')

static final String cje_tf_repo_url = "https://gitlab.devtools.intel.com/TensorFlow/QA/cje-tf.git"

static final String docker_registry_url =  'https://amr-registry.caas.intel.com'
static final String docker_registry_credentials = 'lab_tfbot'

String docker_image_name = params.get('IMAGE_NAMESPACE', 'amr-registry.caas.intel.com/aipg-tf/tf-serving')
if (params.get('BASE_DOCKER_IMAGE', '')) {
    docker_image_name += '-not-based-on-ubuntu'
}


static final String docker_build_version = "intel-tf-serving-${tf_serving_repo_branch}"

// TODO: add -py3 options when that is available
static final String docker_py2 = "${docker_image_name}:${docker_build_version}-py2"
static final String docker_py2_mkl = "${docker_image_name}:${docker_build_version}-py2-mkl"
static final String docker_py2_mkl_devel = "${docker_image_name}:${docker_build_version}-py2-mkl-devel"
static final ArrayList docker_images = [docker_py2, docker_py2_mkl, docker_py2_mkl_devel ]

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
                      branches                         : [[name: tf_serving_repo_branch]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'tensorflow-serving']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[url: "$tf_serving_repo_url"]]])


        }

        stage('Build') {

            sh """#!/bin/bash -x
            set -e

            pushd tensorflow-serving/tensorflow_serving/tools/docker

            # make tf serving dockerfiles from somewhere else if we want
            if [[ "$BASE_DOCKER_IMAGE" != "" ]]; then
                # it's possible to have multiple FROM statements but we want to replace the first one only
                sed -i -e "0,/^FROM ubuntu[[:graph:]]*/ s@^FROM ubuntu[[:graph:]]*@FROM $BASE_DOCKER_IMAGE@" Dockerfile.devel
                sed -i -e "0,/^FROM ubuntu[[:graph:]]*/ s@^FROM ubuntu[[:graph:]]*@FROM $BASE_DOCKER_IMAGE@" Dockerfile.devel-mkl
                sed -i -e "0,/^FROM ubuntu[[:graph:]]*/ s@^FROM ubuntu[[:graph:]]*@FROM $BASE_DOCKER_IMAGE@" Dockerfile.mkl
            fi

            # build eigen images
            docker build -f Dockerfile.devel -t $docker_py2 --build-arg http_proxy=http://proxy-us.intel.com:911 --build-arg https_proxy=http://proxy-us.intel.com:912 .

            # build mkl images
            docker build -f Dockerfile.devel-mkl -t $docker_py2_mkl_devel --build-arg http_proxy=http://proxy-us.intel.com:911 --build-arg https_proxy=http://proxy-us.intel.com:912 .
            docker build -f Dockerfile.mkl -t $docker_py2_mkl --build-arg TF_SERVING_BUILD_IMAGE=$docker_py2_mkl_devel --build-arg http_proxy=http://proxy-us.intel.com:911 --build-arg https_proxy=http://proxy-us.intel.com:912 .
            # TODO: other dockerfiles for python3 when they are available here
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

    } // finally
}
