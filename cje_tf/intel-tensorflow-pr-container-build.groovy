static final String node_label=params.get('NODE_LABEL','nervana-skx106.fm.intel.com')
static final String intel_tf_repo_url = "https://github.com/intel-tensorflow/tensorflow.git"
static final String pr_branch = 'latest_prs'

static final String cje_tf_repo_url = "https://gitlab.devtools.intel.com/TensorFlow/QA/cje-tf.git"
static final String jenkins_git_credentials = "lab_tfbot"
CJE_TF_BRANCH="master"

static final String docker_file = params.get('DOCKER_FILE','Dockerfile.devel-mkl-pr')
static final String intel_tf_branch  = params.get('INTEL_TF_BRANCH','latest_prs')

static final String docker_registry_url =  'https://amr-registry.caas.intel.com'
static final String docker_registry_credentials = 'lab_tfbot'

String build_type_tag = 'prs'
static final String docker_build_version = "nightly-${build_type_tag}"

String build_avx_containers= 'no'
String build_avx2_containers= 'no'

if (COMPILER_TYPE_AVX == 'true') {
    build_avx_containers='yes'
}
if (COMPILER_TYPE_AVX2 == 'true') {
    build_avx2_containers='yes'
}

static final String docker_image_name = params.get('IMAGE_NAMESPACE', 'amr-registry.caas.intel.com/aipg-tf/intel_optimized')

static final String branch_avx_py2 = "${docker_image_name}:${docker_tag}"
static final String branch_avx_py3 = "${docker_image_name}:${docker_tag}-py3"

static final String branch_avx2_py2 = "${docker_image_name}:${docker_tag}"
static final String branch_avx2_py3 = "${docker_image_name}:${docker_tag}-py3"
static final String docker_image = "${docker_image_name}:${docker_build_version}"

static final ArrayList docker_images_avx = [ "${branch_avx_py2}" , "${branch_avx_py3}" ]
static final ArrayList docker_images_avx2 = ["${branch_avx2_py2}", "${branch_avx2_py3}"]

echo node_label
echo intel_tf_repo_url
echo pr_branch
echo cje_tf_repo_url
echo jenkins_git_credentials


node(node_label) {

    try {
        stage('CleanUp') {

            sh '''#!/bin/bash -x
                cd $WORKSPACE
                # sudo rm -rf *
                docker stop $(docker ps -a -q)
                echo Y | docker system prune -a
            '''
        }
	stage("Clone repository for Building PR containers") {
		    
            checkout([$class: 'GitSCM',
                              branches: [[name: "$CJE_TF_BRANCH"]],
                              browser: [$class: 'AssemblaWeb', repoUrl: ''],
                              doGenerateSubmoduleConfigurations: false,
                              extensions: [[$class: 'RelativeTargetDirectory',
                                            relativeTargetDir: 'cje-tf']],
                              submoduleCfg: [],
                              userRemoteConfigs: [[credentialsId: "$jenkins_git_credentials",
                                                   url: 'https://gitlab.devtools.intel.com/TensorFlow/QA/cje-tf.git']]])        
        
		
	}
	 stage('Build PR containers ......') {

            sh '''#!/bin/bash -x
            cd $WORKSPACE/$DOCKER_FILE_LOCATION
            IFS=',' # space is set as delimiter
            CONTAINERS=("python nightly-prs devel pip","python3 nightly-prs-py3 devel-py3 pip3")
            for c in ${CONTAINERS[@]}; do
                echo "Running container =${c}"
                lang="$(cut -d' ' -f1 <<< "${c}")"
                repotag="$(cut -d' ' -f2 <<< "${c}")"
                contag="$(cut -d' ' -f3 <<< "${c}")"
                pip="$(cut -d' ' -f4 <<< "${c}")"
                docker build --build-arg PIP=$pip --build-arg PYTHON=$lang --build-arg ROOT_CONTAINER_TAG=$contag --build-arg TF_BUILD_VERSION=$INTEL_TF_BRANCH --build-arg https_proxy=http://proxy-us.intel.com:911 . -f Dockerfile.devel-mkl-pr -t amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:$repotag
            done
   
        	'''
          }
        stage('Push PR containers ......') {
            // Authenticate to the internal registry
            docker.withRegistry(docker_registry_url, docker_registry_credentials) {
            // AVX2
            if (COMPILER_TYPE_AVX2 == "true") {
                for (dockerImage in docker_images_avx2) {
                    docker.image(dockerImage).push()

                }
            }
            }         
                
        }
        
}
catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    } finally {

    } // finally
}


