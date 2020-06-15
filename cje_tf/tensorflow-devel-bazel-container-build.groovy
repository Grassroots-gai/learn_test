static final String ROOT_IMAGE = params.get("ROOT_IMAGE", "tensorflow/tensorflow")
static final String ROOT_IMAGE_TAG = params.get("ROOT_IMAGE_TAG", "devel")
static final String BAZEL_VERSION = params.get("BAZEL_VERSION", "0.26.1")
static final String docker_registry_namespace = params.get('IMAGE_NAMESPACE', 'amr-registry.caas.intel.com/aipg-tf/tensorflow')
static final String DOCKER_IMAGE_NAME = "${docker_registry_namespace}:${ROOT_IMAGE_TAG}-${BAZEL_VERSION}"
static final String DOCKER_IMAGE_NAME_PY3 = "${docker_registry_namespace}:${ROOT_IMAGE_TAG}-${BAZEL_VERSION}-py3"
static final String docker_registry_url =  'https://amr-registry.caas.intel.com'
static final String docker_registry_credentials = 'lab_tfbot'

// set default value for NODE_LABEL
NODE_LABEL = 'clx||skx||bdw'
if ('NODE_LABEL' in params) {
    echo "NODE_LABEL in params"
    if (params.NODE_LABEL != '') {
        NODE_LABEL = params.NODE_LABEL
    }
}
echo "NODE_LABEL: $NODE_LABEL"


node( NODE_LABEL ) {
    try {
        stage('CleanUp') {
            // TODO: put back in cleanup
            sh '''#!/bin/bash -x
                cd $WORKSPACE
                sudo rm -rf *
                # docker stop $(docker ps -a -q)
                # echo Y | docker system prune -a
            '''
        }

        // pull the cje-tf
        dir( 'cje-tf' ) {
            checkout scm
        }

        stage('Build') {
            sh """#!/bin/bash -x
            set -e
            BUILD_DIR="$WORKSPACE/cje-tf/"
            pushd \$BUILD_DIR
            

            docker build \
            --build-arg HTTP_PROXY=${HTTP_PROXY} \
            --build-arg HTTPS_PROXY=${HTTPS_PROXY} \
            --build-arg http_proxy=${http_proxy} \
            --build-arg https_proxy=${https_proxy} \
            --build-arg ROOT_IMAGE=${ROOT_IMAGE} \
            --build-arg ROOT_IMAGE_TAG=${ROOT_IMAGE_TAG} \
            --build-arg BAZEL_VERSION=${BAZEL_VERSION} \
            -f $WORKSPACE/cje-tf/docker/Dockerfiles/Dockerfile.devel-bazel -t ${DOCKER_IMAGE_NAME} .

            docker build \
            --build-arg HTTP_PROXY=${HTTP_PROXY} \
            --build-arg HTTPS_PROXY=${HTTPS_PROXY} \
            --build-arg http_proxy=${http_proxy} \
            --build-arg https_proxy=${https_proxy} \
            --build-arg ROOT_IMAGE=${ROOT_IMAGE} \
            --build-arg ROOT_IMAGE_TAG=${ROOT_IMAGE_TAG}-py3 \
            --build-arg BAZEL_VERSION=${BAZEL_VERSION} \
            -f $WORKSPACE/cje-tf/docker/Dockerfiles/Dockerfile.devel-bazel -t ${DOCKER_IMAGE_NAME_PY3} .

            popd
            """

        }

        stage('Push') {
            // -- docker push amr-registry.caas.intel.com/aipg-tf/dev:centos7
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
             docker.withRegistry(docker_registry_url, docker_registry_credentials) {
                 docker.image(DOCKER_IMAGE_NAME).push()
                 docker.image(DOCKER_IMAGE_NAME_PY3).push()
             }
         }
    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    } finally {
    } // finally
}
