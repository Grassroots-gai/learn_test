/*
This job:
1. Clones tensorflow/tensorflow at a given commit
2. Clones cje-tf at a given commit
3. Calls the 'tensorflow/tools/ci_build/linux/mkl/build-dev-container.sh' script to build development containers for python2.7 and python 3.5
4. Calls the 'tensorflow/tools/ci_build/linux/mkl/build-non-dev-container.sh' script to build non development containers for python2.7 and python 3.5
5. Pushes Development and Non Development Containers for pyhon2.7 and python 3.5 to amr-registry.caas.intel.com

This job accepts params:
- String Type :: CHECKOUT_BRANCH :: Branch name to clone tensorflow/tensorflow from
- String Type :: LABEL :: Node name to run the job
- String Type :: DEVEL_BRANCH :: Docker build branch name
- String Type :: BUILD_VERSION :: Docker build tag
- String Type :: ROOT_CONTAINER_TAG :: Root container tag for containers that have not been released yet or older versions
- String Type :: IMAGE_LOCATION_INTERNAL :: Docker image name which includes registry/namespace/repository where you want to push the image
- String Type :: CENTRAL_WHL_PY2 :: Location of pre-build whl for python 2.7
- String Type :: CENTRAL_WHL_PY3 :: Location of pre-build whl for python 3.5
- String type :: WHL_OUTPUT_FOLDER :: Folder to put created wheels from dev container build
- Boolean Type :: BUILD_DEVELOPMENT_CONTAINERS
- Boolean Type :: BUILD_NON_DEVELOPMENT_CONTAINERS
- Boolean Type :: PUBLISH_INTERNAL_DOCKERHUB
- Boolean Type :: CONTAINER_TYPE_AVX
- Boolean Type :: CONTAINER_TYPE_AVX2
*/

static final String buildLabel = params.get('LABEL', 'bdw||skx||clx')
static final String tf_repo_checkout = params.get('TF_REPO_CHECKOUT', 'https://github.com/tensorflow/tensorflow')
static final String branchName = params.get('CHECKOUT_BRANCH', 'master')

static final String docker_build_version = params.get('BUILD_VERSION', '1.8.0')
static final String docker_image_name = params.get('IMAGE_LOCATION_INTERNAL', 'amr-registry.caas.intel.com/aipg-tf/test-jenkins')

static final String wheel_job = params.get("WHEEL_JOB", "Tensorflow-Centos-Container-Wheels")
static final String py_vers = params.get("PY_VERS", "2.7 3.6")
echo "${py_vers}"
String whl_location_py2= ''
String whl_location_py3= ''
String whl_location_avx2_py2= ''
String whl_location_avx2_py3= ''

static final String root_container = params.get('TF_ROOT_CONTAINER', 'tensorflow/tensorflow')
static final String root_container_tag = params.get('ROOT_CONTAINER_TAG', 'latest')
static final String tf_repo_url = params.get('TF_REPO_URL', 'https://github.com/tensorflow/tensorflow')
static final String tensorflow_branch = params.get('TENSORFLOW_BRANCH', 'master')


String build_tf_v2_containers= 'no'
String tf_version_tag = ''
if (BUILD_TENSORFLOW_V2 == "true") {
	build_tf_v2_containers='yes'
    tf_version_tag = 'TF-v2'
}


//Error check if Non Development containers then whl location is required
if (BUILD_NON_DEVELOPMENT_CONTAINERS == "true") {
	if (CONTAINER_TYPE_AVX == "true") {
		if (params.WHEEL_JOB == '' ) {
			error 'Missing whl locations for AVX non development containers'
		}
	}
	// Commenting code because we do not release AVX2 containers
    /*if (CONTAINER_TYPE_AVX2 == "true") {
		if (params.CENTRAL_WHL_AVX2_PY2 == '' || params.CENTRAL_WHL_AVX2_PY3 == '' ) {
			error 'Missing whl locations for AVX2 non development containers'
		}
	}*/
}


String build_avx_containers= 'no'
String build_avx2_containers= 'no'
if (CONTAINER_TYPE_AVX == "true") {
	build_avx_containers='yes'
}
if (CONTAINER_TYPE_AVX2 == "true") {
	build_avx2_containers='yes'
}

String build_skx_containers = 'no'
String build_clx_containers = 'no'

String enable_secure_build = 'no'
if (ENABLE_SECURE_BUILD == "true") {
	enable_secure_build='yes'
}

build_dev_script =
        "BUILD_AVX2_CONTAINERS=${build_avx2_containers} BUILD_AVX_CONTAINERS=${build_avx_containers} BUILD_SKX_CONTAINERS=${build_skx_containers} " +
        "BUILD_CLX_CONTAINERS=${build_clx_containers} ROOT_CONTAINER_TAG=${root_container_tag} TF_REPO=${tf_repo_url} TF_DOCKER_BUILD_DEVEL_BRANCH=${tensorflow_branch} " +
        "BUILD_TF_V2_CONTAINERS=${build_tf_v2_containers} TF_DOCKER_BUILD_VERSION=${docker_build_version} TF_DOCKER_BUILD_IMAGE_NAME=${docker_image_name} " +
        "ENABLE_SECURE_BUILD=${enable_secure_build} ./build-dev-container.sh"
echo build_dev_script


// For Pushing Images
static final String registryEndpoint =  'https://amr-registry.caas.intel.com'
static final String registryCredentials = 'lab_tfbot'
// static final String registryEndpointExternal = '' //hub.docker.com (default. so don't need to specify the URL')
// static final String registryCredentialsExternal = '16aa27d7-5000-49b1-9298-800803af4d1d'

// Image Names for Development AVX
static final String dockerImageTagDevelPY2 = "${docker_build_version}-devel-mkl"
static final String dockerImageTagDevelPY3 = "${docker_build_version}-devel-mkl-py3"
static final String dockerImageDevelPY2FullTag = "${docker_image_name}:${dockerImageTagDevelPY2}"
static final String dockerImageDevelPY3FullTag = "${docker_image_name}:${dockerImageTagDevelPY3}"

// Image Names for Development Jupyter AVX
static final String dockerTagDevelJupyterPY2 = "${docker_build_version}-devel-mkl-jupyter"
static final String dockerTagDevelJupyterPY3 = "${docker_build_version}-devel-mkl-py3-jupyter"
static final String dockerDevelJupyterPY2FullTag = "${docker_image_name}:${dockerTagDevelJupyterPY2}"
static final String dockerDevelJupyterPY3FullTag = "${docker_image_name}:${dockerTagDevelJupyterPY3}"

static final ArrayList dockerImagesDevel = [ "${dockerImageDevelPY2FullTag}" , "${dockerImageDevelPY3FullTag}", "${dockerDevelJupyterPY2FullTag}" , "${dockerDevelJupyterPY3FullTag}" ]


// Image Names for Development AVX2
static final String dockerImageTagDevelAVX2PY2 = "${docker_build_version}-avx2-devel-mkl"
static final String dockerImageTagDevelAVX2PY3 = "${docker_build_version}-avx2-devel-mkl-py3"
static final String dockerImageDevelAVX2PY2FullTag = "${docker_image_name}:${dockerImageTagDevelAVX2PY2}"
static final String dockerImageDevelAVX2PY3FullTag = "${docker_image_name}:${dockerImageTagDevelAVX2PY3}"
static final ArrayList dockerImagesDevelAVX2 = [ "${dockerImageDevelAVX2PY2FullTag}" , "${dockerImageDevelAVX2PY3FullTag}", ]


// Image Names for Non Development AVX
static final String dockerImageTagNonDevelPY2 = "${docker_build_version}-mkl"
static final String dockerImageTagNonDevelPY3 = "${docker_build_version}-mkl-py3"
static final String dockerImageNonDevelPY2FullTag = "${docker_image_name}:${dockerImageTagNonDevelPY2}"
static final String dockerImageNonDevelPY3FullTag = "${docker_image_name}:${dockerImageTagNonDevelPY3}"

// Image Names for Non Development Jupyter AVX
static final String dockerTagNonDevelJupyterPY2 = "${docker_build_version}-mkl-jupyter"
static final String dockerTagNonDevelJupyterPY3 = "${docker_build_version}-mkl-py3-jupyter"
static final String dockerNonDevelPY2Jupyter = "${docker_image_name}:${dockerTagNonDevelJupyterPY2}"
static final String dockerNonDevelPY3Jupyter = "${docker_image_name}:${dockerTagNonDevelJupyterPY3}"

static final ArrayList dockerImagesNonDevel = [ "${dockerImageNonDevelPY3FullTag}" , "${dockerNonDevelPY3Jupyter}" ]


// Image Names for Non Development AVX2
static final String dockerImageTagNonDevelAVX2PY2 = "${docker_build_version}-avx2-mkl"
static final String dockerImageTagNonDevelAVX2PY3 = "${docker_build_version}-avx2-mkl-py3"
static final String dockerImageNonDevelAVX2PY2FullTag = "${docker_image_name}:${dockerImageTagNonDevelAVX2PY2}"
static final String dockerImageNonDevelAVX2PY3FullTag = "${docker_image_name}:${dockerImageTagNonDevelAVX2PY3}"
static final ArrayList dockerImagesNonDevelAVX2 = [ "${dockerImageNonDevelAVX2PY3FullTag}" ]


static final String http_proxy = "http://proxy-us.intel.com:911"
static final String https_proxy ="https://proxy-us.intel.com:912"

DOWNLOAD_WHEEL_FROM_JOB = ''
if ('DOWNLOAD_WHEEL_FROM_JOB' in params) {
    echo "DOWNLOAD_WHEEL_FROM_JOB in params"
    if (params.DOWNLOAD_WHEEL_FROM_JOB != '') {
        DOWNLOAD_WHEEL_FROM_JOB = params.DOWNLOAD_WHEEL_FROM_JOB
        echo DOWNLOAD_WHEEL_FROM_JOB
    }
}
echo "downloading build artifact from $DOWNLOAD_WHEEL_FROM_JOB jenkin job"

// specify DOWNLOAD_WHEEL_FROM_JOB_NUMBER, if not set, default is lastSuccessfulBuild
DOWNLOAD_WHEEL_FROM_JOB_NUMBER = 'lastSuccessfulBuild'
if( 'DOWNLOAD_WHEEL_FROM_JOB_NUMBER' in params ) {
    echo "DOWNLOAD_WHEEL_FROM_JOB_NUMBER in params"
    if ( DOWNLOAD_WHEEL_FROM_JOB_NUMBER != '' ) {
        DOWNLOAD_WHEEL_FROM_JOB_NUMBER = params.DOWNLOAD_WHEEL_FROM_JOB_NUMBER
        echo DOWNLOAD_WHEEL_FROM_JOB_NUMBER
    }
}
echo " DOWNLOAD_WHEEL_FROM_JOB_NUMBER: $DOWNLOAD_WHEEL_FROM_JOB_NUMBER"

// this parameter specify which wheel to be downloaded
// use this parameter only if DOWNLOAD_WHEEL is true 
// use in conjunction with DOWNLOAD_WHEEL_FROM_JOB above, and specify the wheel's pattern 
DOWNLOAD_WHEEL_PATTERN = 'manylinux2010_x86_64.whl'
if ('DOWNLOAD_WHEEL_PATTERN' in params) {
    echo "DOWNLOAD_WHEEL_PATTERN in params"
    if (params.DOWNLOAD_WHEEL_PATTERN != '') {
        DOWNLOAD_WHEEL_PATTERN = params.DOWNLOAD_WHEEL_PATTERN
        echo DOWNLOAD_WHEEL_PATTERN
    }
}
echo "downloading wheel from $DOWNLOAD_WHEEL_PATTERN"


node(buildLabel) {
        
	try {

    stage('CleanUp') {
            sh '''#!/bin/bash -x
                cd $WORKSPACE
                sudo rm -rf *
                docker stop $(docker ps -a -q)
                echo Y | docker system prune -a
            '''
    }
    stage('Checkout') {
	        deleteDir()
		    //public tensorflow
	        checkout([$class: 'GitSCM', branches: [[name: branchName]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'tensorflow']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '5d094940-c7a0-42a8-8417-1ecc9a7bd947', url: "$tf_repo_checkout"]]])

		    //cje-tf
		    dir('cje-tf') {
		        checkout scm
		    }

    }
    stage('Build') {
        // Build dev containers
        if (BUILD_DEVELOPMENT_CONTAINERS == "true") {
	        dir("${WORKSPACE}/tensorflow/tensorflow/tools/ci_build/linux/mkl") {
                sh build_dev_script
            } 
        }

        // Build non dev containers

        
        
        if (BUILD_NON_DEVELOPMENT_CONTAINERS == "true") {

            //Download wheels from job
            DOWNLOAD_WHEEL_NAME="*${DOWNLOAD_WHEEL_PATTERN}*"
            copyArtifacts(
                projectName: DOWNLOAD_WHEEL_FROM_JOB,
                selector: specific("$DOWNLOAD_WHEEL_FROM_JOB_NUMBER"),
                filter: DOWNLOAD_WHEEL_NAME,
                fingerprintArtifacts: true,
                target: "build/")

            //archiveArtifacts artifacts: "build/**"        

            sh """#!/bin/bash -x
            set -e
            BUILD_DIR="$WORKSPACE/cje-tf/docker"
            pushd \$BUILD_DIR                
            TAG=`git describe --tags --always --dirty`

            # copy Dockerfiles to BUILD_DIR
            cp $WORKSPACE/cje-tf/docker/Dockerfiles/Dockerfile.mkl .
            cp $WORKSPACE/cje-tf/docker/Dockerfiles/Dockerfile.mkl-jupyter .
                    
            # copy wheels dir to BUILD_DIR
            TF_WHLS_DIR=$WORKSPACE/build
            cp -R \$TF_WHLS_DIR/* .
            echo "copy successful"                 
            
            # For each python version pass the whl to the shell script for building non dev containers
            for PY_VER in ${py_vers}; do \
                ORIG_PY_VER=\$PY_VER
                # make version without '.' and then check if we have a wheel that matches that
                PY_VER=\${PY_VER//[.]/}
                for wheel in `find . -name "*\$PY_VER*.whl" -and -name "*manylinux2010*"`; do
                    whl_name=\$(basename \$wheel)
                    if [[ \$PY_VER == "27" ]]; then
                        whl_location_py2="\$whl_name"
                    elif [[ \$PY_VER == "36" ]]; then
                        whl_location_py3="\$whl_name"                          
                    fi                                                        
                done
            done

            BUILD_AVX2_CONTAINERS=${build_avx2_containers} BUILD_AVX_CONTAINERS=${build_avx_containers} \
            ROOT_CONTAINER_TAG=${root_container_tag} TF_REPO=${tf_repo_url} \
            TF_DOCKER_BUILD_IMAGE_NAME=${docker_image_name} TF_DOCKER_BUILD_VERSION=${docker_build_version} \
            TF_DOCKER_BUILD_WHL_AVX_PY2=\$whl_location_py2 TF_DOCKER_BUILD_WHL_AVX_PY3=\$whl_location_py3 \
            TF_DOCKER_BUILD_WHL_AVX2_PY2=${whl_location_avx2_py2} TF_DOCKER_BUILD_WHL_AVX2_PY3=${whl_location_avx2_py3} \
            ./build-non-dev-container.sh

            # Build jupyter containers
            BUILD_AVX2_CONTAINERS=${build_avx2_containers} BUILD_AVX_CONTAINERS=${build_avx_containers} \
            ROOT_CONTAINER_TAG=${root_container_tag} TF_REPO=${tf_repo_url} BUILD_JUPYTER_CONTAINERS=yes \
            TF_DOCKER_BUILD_IMAGE_NAME=${docker_image_name} TF_DOCKER_BUILD_VERSION=${docker_build_version} \
            TF_DOCKER_BUILD_WHL_AVX_PY2=\$whl_location_py2 TF_DOCKER_BUILD_WHL_AVX_PY3=\$whl_location_py3 \
            TF_DOCKER_BUILD_WHL_AVX2_PY2=${whl_location_avx2_py2} TF_DOCKER_BUILD_WHL_AVX2_PY3=${whl_location_avx2_py3} \
            ./build-non-dev-container.sh

            popd                   
            """                    
        }
    }
    stage('Test'){
        docker_test_dir="${WORKSPACE}/cje-tf/docker/tests"
        workspace_scripts="${WORKSPACE}/cje-tf/scripts"
        test_script = "test_docker_image.sh"
        test_command = "DOCKER_TEST_DIR=${docker_test_dir} TEST_SCRIPT=${test_script} ${workspace_scripts}/run_docker_image_tests.sh"
	    //Development Images
	    if (BUILD_DEVELOPMENT_CONTAINERS == "true") {
	        // AVX
		    if (CONTAINER_TYPE_AVX == "true") {
		        for (dockerImage in dockerImagesDevel) {
		            withEnv(["dockerImage=$dockerImage"]) {
			            sh '''#!/bin/bash -x
			            docker images ${dockerImage}
			            '''
                        sh "DOCKER_IMAGE=${dockerImage} ${test_command}"
			        }
		        }
		    }

		    // AVX2
		    if (CONTAINER_TYPE_AVX2 == "true") {
		        for (dockerImage in dockerImagesDevelAVX2) {
		            withEnv(["dockerImage=$dockerImage"]) {
			            sh '''#!/bin/bash -x
			            docker images ${dockerImage}
			            '''
                        sh "DOCKER_IMAGE=${dockerImage} ${test_command}"
			        }
		        }
		    }
	    }

	    //Non Development Images
	    if (BUILD_NON_DEVELOPMENT_CONTAINERS == "true") {
	        // AVX
	        if (CONTAINER_TYPE_AVX == "true") {
	            for (dockerImage in dockerImagesNonDevel) {
		            withEnv(["dockerImage=$dockerImage"]) {
		                sh '''#!/bin/bash -x
			            docker images ${dockerImage}
			            '''
                        sh "DOCKER_IMAGE=${dockerImage} ${test_command}"
		            }
		        }
	        }

		    // AVX2
		    if (CONTAINER_TYPE_AVX2 == "true") {
		        for (dockerImage in dockerImagesNonDevelAVX2) {
		            withEnv(["dockerImage=$dockerImage"]) {
			            sh '''#!/bin/bash -x
			            docker images ${dockerImage}
			            '''
                        sh "DOCKER_IMAGE=${dockerImage} ${test_command}"
			        }
		        }
		    }

	    }
	}
	stage('Push') {
        // Authenticate to the internal registry
        if (PUBLISH_INTERNAL_DOCKERHUB == "true") {
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
		    docker.withRegistry(registryEndpoint, registryCredentials) {
		        //Push Development Images
		        if (BUILD_DEVELOPMENT_CONTAINERS == "true") {
			        // AVX
			        if (CONTAINER_TYPE_AVX == "true") {
			            for (dockerImage in dockerImagesDevel) {
	        		            docker.image(dockerImage).push()
			            }  
			        }

			        // AVX2
			        if (CONTAINER_TYPE_AVX2 == "true") {
			            for (dockerImage in dockerImagesDevelAVX2) {
				            docker.image(dockerImage).push()
			            }
			        }
		        }

			    // Push Non development Images
			    if (BUILD_NON_DEVELOPMENT_CONTAINERS == "true") {
			        // AVX
			        if (CONTAINER_TYPE_AVX == "true") {
				        for (dockerImage in dockerImagesNonDevel) {
			                    docker.image(dockerImage).push()
				        }
			        }

			        // AVX2
			        if (CONTAINER_TYPE_AVX2 == "true") {
				        for (dockerImage in dockerImagesNonDevelAVX2) {
			                    docker.image(dockerImage).push()
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
    }
}
