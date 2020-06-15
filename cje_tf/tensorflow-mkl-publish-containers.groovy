/*
This job:
Cleans the workspace and all docker images on the machine
Clones tensorflow/tensorflow at a given commit
Pulls Development and Non Development Containers for pyhon2.7 and python 3.5 from amr-registry.caas.intel.com
Pushes the images to the external docker hub

This job accepts params:
- String Type :: CHECKOUT_BRANCH :: Branch name to clone tensorflow/tensorflow from
- String Type :: LABEL :: Node name to run the job
- String Type :: BUILD_VERSION :: Docker build tag
- String Type :: ROOT_CONTAINER_TAG :: Root container tag for containers that have not been released yet or older versions
- String Type :: IMAGE_LOCATION_INTERNAL :: Docker image name which includes registry/namespace/repository where you want to push the image
// - String Type :: IMAGE_LOCATION_EXTERNAL :: Docker image name which includes registry/namespace/repository where you want to push the image

- Boolean Type :: PUBLISH_DEVELOPMENT_CONTAINERS
- Boolean Type :: PUBLISH_NON_DEVELOPMENT_CONTAINERS
- Boolean Type :: PUBLISH_INTERNAL_DOCKERHUB
- Boolean Type :: PUBLISH_EXTERNAL_DOCKERHUB
- Boolean Type :: CONTAINER_TYPE_AVX
- Boolean Type :: CONTAINER_TYPE_AVX2
- Boolean Type :: PUBLISH_PY2
- Boolean Type :: PUBLISH_PY3
- Boolean Type :: PUBLISH_JUPYTER
*/

static final String buildLabel = params.get('LABEL', 'nervana-bdw27.fm.intel.com')
static final String tfBranchName = params.get('CHECKOUT_BRANCH', 'master')
static final String cjeBranchName = params.get('JENKINS_BRANCH', 'master')
static final String docker_build_version = params.get('BUILD_VERSION', '1.8.0')
static final String docker_image_name = params.get('IMAGE_LOCATION_INTERNAL', 'amr-registry.caas.intel.com/aipg-tf/test-jenkins')
// static final String docker_image_name_external = params.get('IMAGE_LOCATION_EXTERNAL', 'docker.io/intelaipg/intel-optimized-tensorflow')


static final String root_container_tag = params.get('ROOT_CONTAINER_TAG', 'latest')


String build_avx_containers= 'no'
String build_avx2_containers= 'no'
if (CONTAINER_TYPE_AVX == "true") {
	build_avx_containers='yes'
}
if (CONTAINER_TYPE_AVX2 == "true") {
	build_avx2_containers='yes'
}

static final String run_publish_script = "cje-tf/docker/scripts/publish-docker-containers.py"

// For Pushing Images
static final String docker_registry_url = 'https://amr-registry.caas.intel.com'
static final String docker_registry_credentials = 'lab_tfbot'



// Image Names for Development AVX
static final String dockerImageTagDevelPY2 = "${docker_build_version}-devel-mkl"
static final String dockerImageTagDevelPY3 = "${docker_build_version}-devel-mkl-py3"
static final String dockerImageTagDevelPY36 = "${docker_build_version}-devel-mkl-py3.6"
static final String dockerImageDevelPY2FullTag = "${docker_image_name}:${dockerImageTagDevelPY2}"
static final String dockerImageDevelPY3FullTag = "${docker_image_name}:${dockerImageTagDevelPY3}"

// Image Names for Development Jupyter AVX
static final String dockerTagDevelJupyterPY2 = "${docker_build_version}-devel-mkl-jupyter"
static final String dockerTagDevelJupyterPY3 = "${docker_build_version}-devel-mkl-py3-jupyter"
static final String dockerDevelJupyterPY2FullTag = "${docker_image_name}:${dockerTagDevelJupyterPY2}"
static final String dockerDevelJupyterPY3FullTag = "${docker_image_name}:${dockerTagDevelJupyterPY3}"

static final ArrayList dockerImagesDevel = [ "${dockerImageDevelPY2FullTag}" , "${dockerImageDevelPY3FullTag}", "${dockerDevelJupyterPY2FullTag}", "${dockerDevelJupyterPY3FullTag}" ]

// Image Names for Development AVX2
static final String dockerImageTagDevelAVX2PY2 = "${docker_build_version}-avx2-devel-mkl"
static final String dockerImageTagDevelAVX2PY3 = "${docker_build_version}-avx2-devel-mkl-py3"
static final String dockerImageTagDevelAVX2PY36 = "${docker_build_version}-avx2-devel-mkl-py3.6"
static final String dockerImageDevelAVX2PY2FullTag = "${docker_image_name}:${dockerImageTagDevelAVX2PY2}"
static final String dockerImageDevelAVX2PY3FullTag = "${docker_image_name}:${dockerImageTagDevelAVX2PY3}"

// Image Names for Development Jupyter AVX2
static final String dockerTagDevelAVX2JupyterPY2 = "${docker_build_version}-avx2-devel-mkl-jupyter"
static final String dockerTagDevelAVX2JupyterPY3 = "${docker_build_version}-avx2-devel-mkl-py3-jupyter"
static final String dockerDevelAVX2JupyterPY2FullTag = "${docker_image_name}:${dockerTagDevelAVX2JupyterPY2}"
static final String dockerDevelAVX2JupyterPY3FullTag = "${docker_image_name}:${dockerTagDevelAVX2JupyterPY3}"

static final ArrayList dockerImagesDevelAVX2 = [ "${dockerImageDevelAVX2PY2FullTag}" , "${dockerImageDevelAVX2PY3FullTag}", "${dockerDevelAVX2JupyterPY2FullTag}", "${dockerDevelAVX2JupyterPY3FullTag}" ]


// Image Names for Non Development AVX
static final String dockerImageTagNonDevelPY2 = "${docker_build_version}-mkl"
static final String dockerImageTagNonDevelPY3 = "${docker_build_version}-mkl-py3"
static final String dockerImageTagNonDevelPY36 = "${docker_build_version}-mkl-py3.6"
static final String dockerImageNonDevelPY2FullTag = "${docker_image_name}:${dockerImageTagNonDevelPY2}"
static final String dockerImageNonDevelPY3FullTag = "${docker_image_name}:${dockerImageTagNonDevelPY3}"

// Image Names for Non Development Jupyter AVX
static final String dockerTagNonDevelJupyterPY2 = "${docker_build_version}-mkl-jupyter"
static final String dockerTagNonDevelJupyterPY3 = "${docker_build_version}-mkl-py3-jupyter"
static final String dockerNonDevelPY2Jupyter = "${docker_image_name}:${dockerTagNonDevelJupyterPY2}"
static final String dockerNonDevelPY3Jupyter = "${docker_image_name}:${dockerTagNonDevelJupyterPY3}"

static final ArrayList dockerImagesNonDevel = [ "${dockerImageNonDevelPY3FullTag}", "${dockerNonDevelPY3Jupyter}" ]


// Image Names for Non Development AVX2
static final String dockerImageTagNonDevelAVX2PY2 = "${docker_build_version}-avx2-mkl"
static final String dockerImageTagNonDevelAVX2PY3 = "${docker_build_version}-avx2-mkl-py3"
static final String dockerImageTagNonDevelAVX2PY36 = "${docker_build_version}-avx2-mkl-py3.6"
static final String dockerImageNonDevelAVX2PY2FullTag = "${docker_image_name}:${dockerImageTagNonDevelAVX2PY2}"
static final String dockerImageNonDevelAVX2PY3FullTag = "${docker_image_name}:${dockerImageTagNonDevelAVX2PY3}"

// Image Names for Non Development Jupyter AVX2
static final String dockerTagNonDevelAVX2JupyterPY2 = "${docker_build_version}-avx2-mkl-jupyter"
static final String dockerTagNonDevelAVX2JupyterPY3 = "${docker_build_version}-avx2-mkl-py3-jupyter"
static final String dockerNonDevelAVX2PY2Jupyter = "${docker_image_name}:${dockerTagNonDevelAVX2JupyterPY2}"
static final String dockerNonDevelAVX2PY3Jupyter = "${docker_image_name}:${dockerTagNonDevelAVX2JupyterPY3}"

static final ArrayList dockerImagesNonDevelAVX2 = [ "${dockerImageNonDevelAVX2PY2FullTag}" , "${dockerImageNonDevelAVX2PY3FullTag}", "${dockerNonDevelAVX2PY2Jupyter}", "${dockerNonDevelAVX2PY3Jupyter}" ]


// static final Map docker_registry_map_devel = ["${dockerImageDevelPY2FullTag}": "${dockerImageDevelPY2FullExt}",  "${dockerImageDevelPY3FullTag}": "${dockerImageDevelPY3FullExt}", "${dockerImageDevelAVX2PY2FullTag}": "${dockerImageDevelAVX2PY2FullExt}", "${dockerImageDevelAVX2PY3FullTag}": "${dockerImageDevelAVX2PY3FullExt}"]


// static final Map docker_registry_map_non_devel = ["${dockerImageNonDevelPY2FullTag}": "${dockerImageNonDevelPY2FullExt}", "${dockerImageNonDevelPY3FullTag}": "${dockerImageNonDevelPY3FullExt}", "${dockerImageNonDevelAVX2PY2FullTag}": "${dockerImageNonDevelAVX2PY2FullExt}", "${dockerImageNonDevelAVX2PY3FullTag}": "${dockerImageNonDevelAVX2PY3FullExt}"]

//Container IDs for pushing to external docker hub
// AVX for now
// Add more ids for AVX2
String latest_devel_container_id = ''
String latest_devel_py3_container_id = ''
String latest_container_id = ''
String latest_py3_container_id = ''
String latest_jupyter_container_id = '' //AVX Non Development jupyter for now
String latest_jupyter_py3_container_id = '' //AVX Non Development jupyter for now


node(buildLabel) {
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
        checkout([$class: 'GitSCM', branches: [[name: tfBranchName]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'tensorflow']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '5d094940-c7a0-42a8-8417-1ecc9a7bd947', url: 'https://github.com/tensorflow/tensorflow']]])
		//private cje-tf
        checkout([$class: 'GitSCM', branches: [[name: cjeBranchName]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'cje-tf']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '5d094940-c7a0-42a8-8417-1ecc9a7bd947', url: 'https://gitlab.devtools.intel.com/TensorFlow/QA/cje-tf']]])

	}
	stage('Pull'){

		docker.withRegistry(docker_registry_url, docker_registry_credentials) {
			//Pull Development Images
			if (PUBLISH_DEVELOPMENT_CONTAINERS == "true") {
				// AVX
				if (CONTAINER_TYPE_AVX == "true") {
					/*for (dockerImage in dockerImagesDevel) {
						docker.image(dockerImage).pull()
					}*/
                    if (PUBLISH_PY2 == "true") {
                        dockerImage="${dockerImageDevelPY2FullTag}"
                        docker.image(dockerImage).pull()
                    }
                    if (PUBLISH_PY3 == "true") {
                        dockerImage="${dockerImageDevelPY3FullTag}"
                        docker.image(dockerImage).pull()
                    }
                    
				}

				// AVX2
				if (CONTAINER_TYPE_AVX2 == "true") {
					/*for (dockerImage in dockerImagesDevelAVX2) {
						docker.image(dockerImage).pull()
					}*/
                    if (PUBLISH_PY2 == "true") {
                        dockerImage="${dockerImageDevelAVX2PY2FullTag}"
                        docker.image(dockerImage).pull()
                    }
                    if (PUBLISH_PY3 == "true") {
                        dockerImage="${dockerImageDevelAVX2PY3FullTag}"
                        docker.image(dockerImage).pull()
                    }
				}
			}

			// Pull Non development Images
			if (PUBLISH_NON_DEVELOPMENT_CONTAINERS == "true") {
				// AVX
				if (CONTAINER_TYPE_AVX == "true") {
					/*for (dockerImage in dockerImagesNonDevel) {
						docker.image(dockerImage).pull()
					}*/
                    if (PUBLISH_PY2 == "true") {
                        dockerImage="${dockerImageNonDevelPY2FullTag}"
                        docker.image(dockerImage).pull()

                        if (PUBLISH_JUPYTER == "true") {
                            dockerImageJupyter="${dockerNonDevelPY2Jupyter}"
                            docker.image(dockerImageJupyter).pull()
                        }
                    }
                    if (PUBLISH_PY3 == "true") {
                        dockerImage="${dockerImageNonDevelPY3FullTag}"
                        docker.image(dockerImage).pull()

                        if (PUBLISH_JUPYTER == "true") {
                            dockerImageJupyter="${dockerNonDevelPY3Jupyter}"
                            docker.image(dockerImageJupyter).pull()
                        }
                    }
				}

				// AVX2
				if (CONTAINER_TYPE_AVX2 == "true") {
					/*for (dockerImage in dockerImagesNonDevelAVX2) {
						docker.image(dockerImage).pull()
					}*/
                    if (PUBLISH_PY2 == "true") {
                        dockerImage="${dockerImageNonDevelAVX2PY2FullTag}"
                        docker.image(dockerImage).pull()
                    }
                    if (PUBLISH_PY3 == "true") {
                        dockerImage="${dockerImageNonDevelAVX2PY3FullTag}"
                        docker.image(dockerImage).pull()
                    }
				}
			}
		}

	}

	stage('Test'){
		//Development Images
		if (PUBLISH_DEVELOPMENT_CONTAINERS == "true") {
			// AVX
			if (CONTAINER_TYPE_AVX == "true") {

                if (PUBLISH_PY2 == "true") {
                    withEnv(["dockerImage=$dockerImageDevelPY2FullTag"]) {
					sh '''#!/bin/bash -x
					latest_devel_container_id=$(docker images --format "{{.ID}}" ${dockerImage})
					echo "${latest_devel_container_id}" > latest_devel_container_id.txt
					'''
				    }
				    latest_devel_container_id=readFile('latest_devel_container_id.txt').trim()
                    echo "latest_devel_container_id=$latest_devel_container_id";
                }
                if (PUBLISH_PY3 == "true") {
                    withEnv(["dockerImage=$dockerImageDevelPY3FullTag"]) {
					sh '''#!/bin/bash -x
					latest_devel_py3_container_id=$(docker images --format "{{.ID}}" ${dockerImage})
					echo "${latest_devel_py3_container_id}" > latest_devel_py3_container_id.txt
					'''
				    }
				    latest_devel_py3_container_id=readFile('latest_devel_py3_container_id.txt').trim()
                    echo "latest_devel_py3_container_id=$latest_devel_py3_container_id";
                }				
			}

			// AVX2
			if (CONTAINER_TYPE_AVX2 == "true") {
                if (PUBLISH_PY2 == "true") {
                    withEnv(["dockerImage=$dockerImageDevelAVX2PY2FullTag"]) {
					sh '''#!/bin/bash -x
					latest_devel_container_id=$(docker images --format "{{.ID}}" ${dockerImage})
					echo "${latest_devel_container_id}" > latest_devel_container_id.txt
					'''
				    }
                    latest_devel_container_id=readFile('latest_devel_container_id.txt').trim()
                    echo "latest_devel_container_id=$latest_devel_container_id";
                }
                if (PUBLISH_PY3 == "true") {
                    withEnv(["dockerImage=$dockerImageDevelAVX2PY3FullTag"]) {
					sh '''#!/bin/bash -x
					latest_devel_py3_container_id=$(docker images --format "{{.ID}}" ${dockerImage})
					echo "${latest_devel_py3_container_id}" > latest_devel_py3_container_id.txt
					'''
				    }
                    latest_devel_py3_container_id=readFile('latest_devel_py3_container_id.txt').trim()
                    echo "latest_devel_py3_container_id=$latest_devel_py3_container_id";
                }				
			}
		}

		//Non Development Images
		if (PUBLISH_NON_DEVELOPMENT_CONTAINERS == "true") {
			// AVX
			if (CONTAINER_TYPE_AVX == "true") {
                if (PUBLISH_PY2 == "true") {
                    withEnv(["dockerImage=$dockerImageNonDevelPY2FullTag"]) {
					sh '''#!/bin/bash -x
					latest_container_id=$(docker images --format "{{.ID}}" ${dockerImage})
					echo "${latest_container_id}" > latest_container_id.txt
					'''
				    }
				    latest_container_id=readFile('latest_container_id.txt').trim()
                    echo "latest_container_id=$latest_container_id";

                    if (PUBLISH_JUPYTER == "true") {
                        withEnv(["dockerImage=$dockerNonDevelPY2Jupyter"]) {
					    sh '''#!/bin/bash -x
					    latest_container_jupyter_id=$(docker images --format "{{.ID}}" ${dockerImage})
					    echo "${latest_jupyter_container_id}" > latest_jupyter_container_id.txt
					    '''
				        }
				        latest_jupyter_container_id=readFile('latest_jupyter_container_id.txt').trim()
                        echo "latest_jupyter_container_id=$latest_jupyter_container_id";                       
                    }
                }
                if (PUBLISH_PY3 == "true") {
                    withEnv(["dockerImage=$dockerImageNonDevelPY3FullTag"]) {
					sh '''#!/bin/bash -x
					latest_py3_container_id=$(docker images --format "{{.ID}}" ${dockerImage})
					echo "${latest_py3_container_id}" > latest_py3_container_id.txt
					'''
				    }
				    latest_py3_container_id=readFile('latest_py3_container_id.txt').trim()
                    echo "latest_py3_container_id=$latest_py3_container_id";

                    if (PUBLISH_JUPYTER == "true") {
                        withEnv(["dockerImage=$dockerNonDevelPY3Jupyter"]) {
					    sh '''#!/bin/bash -x
					    latest_jupyter_py3_container_id=$(docker images --format "{{.ID}}" ${dockerImage})
					    echo "${latest_jupyter_py3_container_id}" > latest_jupyter_py3_container_id.txt
					    '''
				        }
				        latest_jupyter_py3_container_id=readFile('latest_jupyter_py3_container_id.txt').trim()
                        echo "latest_jupyter_py3_container_id=$latest_jupyter_py3_container_id";                       
                    }
                }				
			}

			// AVX2
			if (CONTAINER_TYPE_AVX2 == "true") {
                if (PUBLISH_PY2 == "true") {
                    withEnv(["dockerImage=$dockerImageNonDevelAVX2PY2FullTag"]) {
					sh '''#!/bin/bash -x
					latest_container_id=$(docker images --format "{{.ID}}" ${dockerImage})
					echo "${latest_container_id}" > latest_container_id.txt
					'''
				    }
                    latest_container_id=readFile('latest_container_id.txt').trim()
                    echo "latest_container_id=$latest_container_id";
                }
                if (PUBLISH_PY3 == "true") {
                    withEnv(["dockerImage=$dockerImageNonDevelAVX2PY3FullTag"]) {
					sh '''#!/bin/bash -x
					latest_py3_container_id=$(docker images --format "{{.ID}}" ${dockerImage})
					echo "${latest_py3_container_id}" > latest_py3_container_id.txt
					'''
				    }
                    latest_py3_container_id=readFile('latest_py3_container_id.txt').trim()
                    echo "latest_py3_container_id=$latest_py3_container_id";
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
			docker.withRegistry(docker_registry_url, docker_registry_credentials) {
				//Push Development Images
				if (PUBLISH_DEVELOPMENT_CONTAINERS == "true") {
					// AVX
					if (CONTAINER_TYPE_AVX == "true") {
						/*for (dockerImage in dockerImagesDevel) {
							docker.image(dockerImage).push()
						}*/
                        if (PUBLISH_PY2 == "true") {
                            dockerImage="${dockerImageDevelPY2FullTag}"
                            docker.image(dockerImage).push()
                        }
                        if (PUBLISH_PY3 == "true") {
                            dockerImage="${dockerImageDevelPY3FullTag}"
                            docker.image(dockerImage).push()
                        }
					}

					// AVX2
					if (CONTAINER_TYPE_AVX2 == "true") {
						/*for (dockerImage in dockerImagesDevelAVX2) {
							docker.image(dockerImage).push()
						}*/
                        if (PUBLISH_PY2 == "true") {
                            dockerImage="${dockerImageDevelAVX2PY2FullTag}"
                            docker.image(dockerImage).push()
                        }
                        if (PUBLISH_PY3 == "true") {
                            dockerImage="${dockerImageDevelAVX2PY3FullTag}"
                            docker.image(dockerImage).push()
                        }
					}
				}

				// Push Non development Images
				if (PUBLISH_NON_DEVELOPMENT_CONTAINERS == "true") {
					// AVX
					if (CONTAINER_TYPE_AVX == "true") {
						/*for (dockerImage in dockerImagesNonDevel) {
							docker.image(dockerImage).push()
						}*/
                        if (PUBLISH_PY2 == "true") {
                            dockerImage="${dockerImageNonDevelPY2FullTag}"
                            docker.image(dockerImage).push()

                            if (PUBLISH_JUPYTER == "true") {
                                dockerImageJupyter="${dockerNonDevelPY2Jupyter}"
                                docker.image(dockerImageJupyter).push()
                            }
                        }
                        if (PUBLISH_PY3 == "true") {
                            dockerImage="${dockerImageNonDevelPY3FullTag}"
                            docker.image(dockerImage).push()

                            if (PUBLISH_JUPYTER == "true") {
                                dockerImageJupyter="${dockerNonDevelPY3Jupyter}"
                                docker.image(dockerImageJupyter).push()
                            }
                        }
					}

					// AVX2
					if (CONTAINER_TYPE_AVX2 == "true") {
						/*for (dockerImage in dockerImagesNonDevelAVX2) {
							docker.image(dockerImage).push()
						}*/
                        if (PUBLISH_PY2 == "true") {
                            dockerImage="${dockerImageNonDevelAVX2PY2FullTag}"
                            docker.image(dockerImage).push()
                        }
                        if (PUBLISH_PY3 == "true") {
                            dockerImage="${dockerImageNonDevelAVX2PY3FullTag}"
                            docker.image(dockerImage).push()
                        }
					}
				}
			}
		}


		// Authenticate to the external registry
		if (PUBLISH_EXTERNAL_DOCKERHUB == "true") {

			withEnv(["run_publish_script=$run_publish_script", "latest_devel_container_id=$latest_devel_container_id", "latest_devel_py3_container_id=$latest_devel_py3_container_id", "latest_container_id=$latest_container_id", "latest_py3_container_id=$latest_py3_container_id", "latest_jupyter_container_id=$latest_jupyter_container_id", "latest_jupyter_py3_container_id=$latest_jupyter_py3_container_id", "tensorflow_version=$docker_build_version"]) {
				withCredentials([string(credentialsId: '3792147f-dc59-4fb6-9512-31ca9c361e5d', variable: 'PUBLIC_DOCKER_CONTENT_TRUST_ROOT_PASSPHRASE'), string(credentialsId: 'ae2f9545-e8b7-40ea-8d02-332112545879', variable: 'PUBLIC_DOCKER_PROD_CONTENT_TRUST_REPOSITORY_PASSPHRASE'), string(credentialsId: '2d24f3d7-c4e4-4a8a-93db-0f88ce4d237a', variable: 'PUBLIC_DOCKER_STAGING_CONTENT_TRUST_REPOSITORY_PASSPHRASE')]) {

						//def latest_devel_container_id=readFile('latest_devel_container_id.txt').trim()
                        echo "latest_devel_container_id=$latest_devel_container_id";

                        //def latest_devel_py3_container_id=readFile('latest_devel_py3_container_id.txt').trim()
                        echo "latest_devel_py3_container_id=$latest_devel_py3_container_id";

                        //def latest_container_id=readFile('latest_container_id.txt').trim()
                        echo "latest_container_id=$latest_container_id";

                        //def latest_py3_container_id=readFile('latest_py3_container_id.txt').trim()
                        echo "latest_py3_container_id=$latest_py3_container_id";

                        echo "latest_jupyter_container_id=$latest_jupyter_container_id";
                        echo "latest_jupyter_py3_container_id=$latest_jupyter_py3_container_id";

                        sh '''#!/bin/bash -x
                        echo "latest_devel_container_id_shell=$latest_devel_container_id";
                        echo "latest_devel_py3_container_id_shell=$latest_devel_py3_container_id";
                        echo "latest_container_id_shell=$latest_container_id";
                        echo "latest_py3_container_id_shell=$latest_py3_container_id";
                        echo "latest_jupyter_container_id_shell=$latest_jupyter_container_id";
                        echo "latest_jupyter_py3_container_id_shell=$latest_jupyter_py3_container_id";
                        

                        //copy key
                        mkdir -p ~/.docker/trust/private
                        mkdir -p ~/ .docker/private
                        cp /mnt/aipg_tensorflow_shared/validation/cje-tf/docker/credentials/27a685c301064008451574d8030c0d205e9a1055fe415275cbf57692943d718f.key ~/.docker/trust/private
                        cp /mnt/aipg_tensorflow_shared/validation/cje-tf/docker/credentials/27a685c301064008451574d8030c0d205e9a1055fe415275cbf57692943d718f.key ~/ .docker/private

                        publish_script_args="-v ${tensorflow_version} "
                        if [ -z "${latest_container_id}" ]; then
                            echo "latest_container_id is not set"
                        else
                            publish_script_args+="-l ${latest_container_id} "
                        fi
                        if [ -z "${latest_py3_container_id}" ]; then
                            echo "latest_py3_container_id is not set"
                        else
                            publish_script_args+="-p ${latest_py3_container_id} "
                        fi
                        if [ -z "${latest_devel_container_id}" ]; then
                            echo "${latest_devel_container_id} is not set"
                        else
                            publish_script_args+="-d ${latest_devel_container_id} "
                        fi
                        if [ -z "${latest_devel_py3_container_id}" ]; then
                            echo "latest_devel_py3_container_id is not set"
                        else
                            publish_script_args+="-y ${latest_devel_py3_container_id} "
                        fi
                        if [ -z "${latest_jupyter_container_id}" ]; then
                            echo "latest_jupyter_container_id is not set"
                        else
                            publish_script_args+="-j ${latest_jupyter_container_id} "
                        fi
                        if [ -z "${latest_jupyter_py3_container_id}" ]; then
                            echo "latest_jupyter_py3_container_id is not set"
                        else
                            publish_script_args+="-k ${latest_jupyter_py3_container_id} "
                        fi

                        python ${run_publish_script} ${publish_script_args}
                        '''
				}
			}
		}
	}
}
