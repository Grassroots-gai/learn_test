//
// pulls the nightly built containers, typically from amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow namespace,
// install the additional packages needed for the nightly Jenkin runs
// pushs the containers(with same tag) back under the amr-registry.caas.intel.com/aipg-tf/qa namespace
//
//
static final String docker_registry_url =  'https://amr-registry.caas.intel.com'
static final String docker_registry_credentials = 'lab_tfbot'
static final String docker_registry_namespace = params.get('IMAGE_NAMESPACE', 'amr-registry.caas.intel.com/aipg-tf/qa')
static final String docker_images_to_rebuild = params.get('DOCKER_IMAGE', 'amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:nightly-master-avx2-devel-mkl,amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:nightly-master-avx2-devel-mkl-py3,amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:nightly-master-devel-mkl,amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:nightly-master-devel-mkl-py3')

// retry number
RETRY_NUM = 3
if( 'RETRY_NUM' in params && params.RETRY_NUM != '' ) {
    RETRY_NUM = params.RETRY_NUM
}
echo "RETRY_NUM: $RETRY_NUM"

def cleanup() {

    try {
        sh '''#!/bin/bash -x
        cd $WORKSPACE
        sudo rm -rf *
        docker stop $(docker ps -aq)
        docker rm -vf $(docker ps -aq)
        docker rmi $(docker images --format {{.Repository}}:{{.Tag}})
                
        '''
    } catch(e) {

        echo "==============================================="
        echo "ERROR: Exception caught in cleanup()           "
        echo "ERROR: ${e}"
        echo "==============================================="

        echo ' '
        echo "Error while doing cleanup"

    }  // catch

}

node(params.NODE_LABEL) {

    try {

        // first clean the workspace
        cleanup()
        deleteDir()

        // pull the cje-tf
        dir('cje-tf') {
            checkout scm
        }

        docker_images = docker_images_to_rebuild.split(',')
        sh '''#!/bin/bash -x
            cp $WORKSPACE/cje-tf/scripts/install_dependency.sh $WORKSPACE
            chmod 775 $WORKSPACE/install_dependency.sh
        '''

        stage('Install dependency') {
            for (docker_image in docker_images) {
                docker_tag = sh (script:"echo ${docker_image} | cut -f2 -d:", returnStdout: true).trim()
                echo docker_tag
                withEnv(["dockerImage=$docker_image","dockerRegistry=$docker_registry_namespace", "dockerTag=$docker_tag"]) {
                    sh '''#!/bin/bash -x
                        docker pull ${dockerImage}
                        name=`echo $RANDOM`
                        docker run --init --name=test-${name} -d -u root:root -v ${WORKSPACE}:/workspace -e "WORKSPACE=/workspace" -e http_proxy=http://proxy-us.intel.com:911 -e https_proxy=https://proxy-us.intel.com:912 ${dockerImage} tail -f /dev/null
                        docker exec test-${name} /workspace/install_dependency.sh
                        docker commit test-${name} ${dockerRegistry}:${dockerTag}
                    '''
                }
            }
        }
   
        stage('Push') {

            for (docker_image in docker_images) {
                sh '''#!/bin/bash -x
                    # depending on machine, regardless we want unzip
                    sudo apt-get -y -qq install unzip || sudo yum install -y unzip
                    http_proxy='' &&\
                          curl http://certificates.intel.com/repository/certificates/IntelSHA2RootChain-Base64.zip > /tmp/IntelSHA2RootChain-Base64.zip
                    sudo unzip -o /tmp/IntelSHA2RootChain-Base64.zip -d /usr/local/share/ca-certificates/
                    rm /tmp/IntelSHA2RootChain-Base64.zip
                    sudo update-ca-certificates || sudo update-ca-trust
                    sudo service docker restart
                '''

                docker_tag = sh (script:"echo ${docker_image} | cut -f2 -d:", returnStdout: true).trim()
                echo docker_tag
                dockerImage = "${docker_registry_namespace}:${docker_tag}"
                retry(RETRY_NUM) {
                    docker.withRegistry(docker_registry_url, docker_registry_credentials) {
                        docker.image(dockerImage).push()
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

