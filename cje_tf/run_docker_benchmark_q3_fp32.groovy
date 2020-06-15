Boolean SINGLE_SOCKET = true
CJE_TF = 'cje-tf'
CJE_TF_COMMON_DIR = "$CJE_TF/common"
GIT_CREDENTIAL = "lab_tfbot"
GIT_CREDENTIAL_LAB = "lab_tfbot"

http_proxy="http://proxy-us.intel.com:911"
https_proxy="https://proxy-us.intel.com:912"

// set default value for NODE_LABEL
NODE_LABEL = 'nervana-skx48.fm.intel.com'
if ('NODE_LABEL' in params) {
    echo "NODE_LABEL in params"
    if (params.NODE_LABEL != '') {
        NODE_LABEL = params.NODE_LABEL
    }
}
echo "NODE_LABEL: $NODE_LABEL"

// set default value for TENSORFLOW_BRANCH
TENSORFLOW_BRANCH = 'master'
if ('TENSORFLOW_BRANCH' in params) {
    echo "TENSORFLOW_BRANCH in params"
    if (params.TENSORFLOW_BRANCH != '') {
        TENSORFLOW_BRANCH = params.TENSORFLOW_BRANCH
        echo TENSORFLOW_BRANCH
    }
}
echo "TENSORFLOW_BRANCH:  $TENSORFLOW_BRANCH"

// set default DOCKER_IMAGE  value
// possible values: e.g. amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.8-py2-build
//                       amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.8-py3-build
//                       amr-registry.caas.intel.com/aipg-tf/qa:1.8.0-mkl-py2
DOCKER_IMAGE = 'amr-registry.caas.intel.com/aipg-tf/qa:nightly-master-avx2-devel-mkl'
if ('DOCKER_IMAGE' in params) {
    echo "DOCKER_IMAGE in params"
    if (params.DOCKER_IMAGE != '') {
        DOCKER_IMAGE = params.DOCKER_IMAGE
        echo DOCKER_IMAGE
    }
}
echo "DOCKER_IMAGE: $DOCKER_IMAGE"

DOCKER_IMAGE_NAMESPACE = 'amr-registry.caas.intel.com/aipg-tf/pr'
if ('DOCKER_IMAGE_NAMESPACE' in params) {
    echo "DOCKER_IMAGE_NAMESPACE in params"
   if (params.DOCKER_IMAGE_NAMESPACE != '') {
        DOCKER_IMAGE_NAMESPACE = params.DOCKER_IMAGE_NAMESPACE
        echo DOCKER_IMAGE_NAMESPACE
    }
}
echo "DOCKER_IMAGE_NAMESPACE: $DOCKER_IMAGE_NAMESPACE"

// set default value for DATASET_LOCATION
DATASET_LOCATION = '/tf_dataset'
if ('DATASET_LOCATION' in params) {
    echo "DATASET_LOCATION in params"
    if (params.DATASET_LOCATION != '') {
        DATASET_LOCATION = params.DATASET_LOCATION
        echo DATASET_LOCATION
    }
}
echo "DATASET_LOCATION: $DATASET_LOCATION"

// set default value for MODELS to run
MODELS="NCF"
if ('MODELS' in params) {
    echo "MODELS in params"
    if (params.MODELS != '') {
        MODELS = params.MODELS
        echo MODELS
    }
}
echo "MODELS: $MODELS"

// set default value for MODES to run
MODES="training,inference"
if ('MODES' in params) {
    echo "MODES in params"
    if (params.MODES != '') {
        MODES = params.MODES
        echo MODES
    }
}
echo "MODES: $MODES"

// setting SLACK_CHANNEL with some default value or get input from params
SLACK_CHANNEL = '#tensorflow-jenkins'
if ('SLACK_CHANNEL' in params) {
    echo "SLACK_CHANNEL in params"
    if (params.SLACK_CHANNEL != '') {
        SLACK_CHANNEL = params.SLACK_CHANNEL
        echo SLACK_CHANNEL
    }
}
echo "SLACK_CHANNEL: $SLACK_CHANNEL"

// setting SUMMARY_TITLE with some default value or get input from params
SUMMARY_TITLE = 'private-tensorflow nightly benchmark Q3 FP32 models'
if ('SUMMARY_TITLE' in params) {
    echo "SUMMARY_TITLE in params"
    if (params.SUMMARY_TITLE != '') {
        SUMMARY_TITLE = params.SUMMARY_TITLE
        echo SUMMARY_TITLE
    }
}
echo "SUMMARY_TITLE: $SUMMARY_TITLE"

// check default value for POST_TO_DASHBOARD
POST_TO_DASHBOARD = 'false'
if (params.POST_TO_DASHBOARD)
    echo "params.POST_TO_DASHBOARD is true"
else
    echo "params.POST_TO_DASHBOARD is false"
Boolean POST_TO_DASHBOARD=params.POST_TO_DASHBOARD

// setting TARGET_DASHBOARD default to be production or get input from params
TARGET_DASHBOARD = 'production'
if ('TARGET_DASHBOARD' in params) {
    echo "TARGET_DASHBOARD in params"
    if (params.TARGET_DASHBOARD != '') {
        TARGET_DASHBOARD = params.TARGET_DASHBOARD
        echo TARGET_DASHBOARD
    }
}

// set default value for TENSORFLOW_BRANCH
CJE_TF_BRANCH = 'master'
if ('CJE_TF_BRANCH' in params) {
    echo "CJE_TF_BRANCH in params"
    if (params.CJE_TF_BRANCH != '') {
        CJE_TF_BRANCH = params.CJE_TF_BRANCH
        echo CJE_TF_BRANCH
    }
}
echo "CJE_TF_BRANCH:  $CJE_TF_BRANCH"
// setting CJE_ALGO branch
CJE_ALGO_BRANCH = '*/master'
if ('CJE_ALGO_BRANCH' in params) {
    echo "CJE_ALGO_BRANCH in params"
    if (params.CJE_ALGO_BRANCH != '') {
        CJE_ALGO_BRANCH = params.CJE_ALGO_BRANCH
        echo CJE_ALGO_BRANCH
    }
}

// check if we want to download the wheel from somewhere 
if (params.DOWNLOAD_WHEEL)
    echo "params.DOWNLOAD_WHEEL is true"
else
    echo "params.DOWNLOAD_WHEEL is false"
Boolean DOWNLOAD_WHEEL=params.DOWNLOAD_WHEEL

// use this parameter only if DOWNLOAD_WHEEL is true 
// if this parameter is set, 
//     we'll download the build from the specified jenkin job's last successful artifact
//     along with the wheel's name pattern specified in the  DOWNLOAD_WHEEL_NAME
// if this parameter is not set, 
//     we'll use DOWNLOAD_WHEEL_NAME which should specify the absolute path of the wheel 
//     including the wheel name to download the specific build we need
DOWNLOAD_WHEEL_FROM_JOB = ''
if ('DOWNLOAD_WHEEL_FROM_JOB' in params) {
    echo "DOWNLOAD_WHEEL_FROM_JOB in params"
    if (params.DOWNLOAD_WHEEL_FROM_JOB != '') {
        DOWNLOAD_WHEEL_FROM_JOB = params.DOWNLOAD_WHEEL_FROM_JOB
        echo DOWNLOAD_WHEEL_FROM_JOB
    }
}
echo "downloading build artifact from $DOWNLOAD_WHEEL_FROM_JOB jenkin job"

// use this parameter only if DOWNLOAD_WHEEL is true 
// this parameter specify which wheel to be downloaded, this can be either:
// if DOWNLOAD_WHEEL_FROM_JOB not set, the full path name of where the wheel is located, including the wheel's name itself, e.g.
// /mnt/aipg_tensorflow_shared/validation/releases/v1.14/intel_tensorflow-1.13.1-cp27-cp27mu-manylinux1_x86_64.whl
// or
// if DOWNLOAD_WHEEL_FROM_JOB is set, used in conjunction with DOWNLOAD_WHEEL_FROM_JOB above, and specify the wheel's pattern 
DOWNLOAD_WHEEL_NAME = ''
if ('DOWNLOAD_WHEEL_NAME' in params) {
    echo "DOWNLOAD_WHEEL_NAME in params"
    if (params.DOWNLOAD_WHEEL_NAME != '') {
        DOWNLOAD_WHEEL_NAME = params.DOWNLOAD_WHEEL_NAME
        echo DOWNLOAD_WHEEL_NAME
    }
}
echo "downloading wheel from $DOWNLOAD_WHEEL_NAME"

PR_NUMBER = ''
if ('PR_NUMBER' in params) {
    echo "PR_NUMBER in params"
    if (params.PR_NUMBER != '') {
        PR_NUMBER = params.PR_NUMBER
        echo PR_NUMBER
    }
}
echo "PR_NUMBER: $PR_NUMBER"

// set default value for GIT_URL
GIT_URL = "https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git"
if ('GIT_URL' in params) {
    echo "GIT_URL in params"
    if (params.GIT_URL != '') {
        GIT_URL = params.GIT_URL
        echo GIT_URL
    }
}
echo "GIT_URL: $GIT_URL"

PYTHON_VERSION = '3'
if ('PYTHON_VERSION' in params) {
    echo "PYTHON_VERSION in params"
    if (params.PYTHON_VERSION != '') {
        PYTHON_VERSION = params.PYTHON_VERSION
        echo PYTHON_VERSION
    }
}
echo "PYTHON_VERSION: $PYTHON_VERSION"

static final String GIT_NAME = params.get('GIT_NAME', 'private-tensorflow')

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

def runBenchmark(String docker_image, String dataset_location, Boolean single_socket, String servername, String models, String modes, String git_name, Boolean download_wheel) {

    workspace_volumn="${WORKSPACE}:/workspace"
    dataset_volume="${dataset_location}:${dataset_location}"

    docker.image("$docker_image").inside("--env \"http_proxy=${http_proxy}\" \
                                          --env \"https_proxy=${https_proxy}\" \
                                          --volume ${workspace_volumn} \
                                          --volume ${dataset_volume} \
                                          --env DATASET_LOCATION=$dataset_location \
                                          --env SINGLE_SOCKET=$single_socket \
                                          --env SERVERNAME=$servername \
                                          --env MODELS=$models \
                                          --env MODES=$modes \
                                          --env TENSORFLOW_DIR=/$git_name \
                                          --env DOWNOLAD_WHEEL=$download_wheel \
                                          --privileged \
                                          -u root:root") {
            sh '''#!/bin/bash -x 
            python /workspace/cje-tf/scripts/get_tf_sourceinfo.py --tensorflow_dir=${TENSORFLOW_DIR} --workspace_dir=/workspace
            chmod 775 /workspace/cje-tf/scripts/run_benchmark_q3_fp32_models.sh
            /workspace/cje-tf/scripts/run_benchmark_q3_fp32_models.sh
            '''
    }
}

def collectBenchmarkLog(String models, String modes, Boolean single_socket) {

    echo "---------------------------------------------------------"
    echo "------------  running collectBenchnmarkLog  -------------"
    echo "---------------------------------------------------------"

    echo "models: $models"
    echo "modes: $modes"
    SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
            returnStdout: true).trim()

    models=models.split(',')
    modes=modes.split(',')

    models.each { model ->
        echo "model is ${model}"
        modes.each { mode ->
            echo "mode is ${mode}"
            withEnv(["current_model=$model","current_mode=$mode", "single_socket=${single_socket}", "servername=${SERVERNAME}"]) {

                sh '''#!/bin/bash -x
                
                if [ -f "$WORKSPACE/benchmark_${current_model}_${current_mode}_${servername}_latency.log" -o -f "$WORKSPACE/benchmark_${current_model}_${current_mode}_${servername}_throughput.log" ]; then
                    echo "collecting logs for model: ${current_model} mode: ${current_mode}"
                    chmod 775 $WORKSPACE/cje-tf/scripts/collect_logs_q3_fp32_models.sh
                    $WORKSPACE/cje-tf/scripts/collect_logs_q3_fp32_models.sh --model=${current_model} --mode=${current_mode} --single_socket=${single_socket}
                fi

                if [ $? -eq 0 ] ; then
                    echo "running model ${current_model} ${current_mode} success"
                    RESULT="SUCCESS"
                else
                    echo "running model ${current_model} ${current_mode} fail"
                    RESULT="FAILURE"
                fi
                '''
            }
        }
    }
    echo "done running collectBenchmarkLog ......."
    stash allowEmpty: true, includes: "*.log", name: "logfile"
}

node( NODE_LABEL ) {
    try {

        // first clean the workspace
        cleanup()
        deleteDir()

        // pull the cje-tf
        dir(CJE_TF) {
            checkout scm
        }

        SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                returnStdout: true).trim()
        SHORTNAME = sh (script:"echo $SERVERNAME | cut -f2 -d-",
                returnStdout: true).trim()
        SUMMARYLOG = "${WORKSPACE}/summary_${SERVERNAME}.log"
        
        SUMMARYTXT = "${WORKSPACE}/summary_nightly.log"
        writeFile file: SUMMARYTXT, text: "Model,Mode,Server,Data_Type,Use_Case,Batch_Size,Result\n"

        // slack notification
        def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
        notifyBuild(SLACK_CHANNEL, 'STARTED', '')

        stage('Checkout') {
            
            // check out TF source from different branch with different refspec depending on: 
            //     - if PR       : merge PR branch
            //     - if nightly  : master branch
            //     - if release  : release branch
            if (PR_NUMBER == '') {
                branch = "$TENSORFLOW_BRANCH"
                refspec = "+refs/heads/*:refs/remotes/tensorflow/*"
            }
            else {
                branch = "origin-pull/pull/$PR_NUMBER/merge"
                refspec = "+refs/pull/$PR_NUMBER/merge:refs/remotes/origin-pull/pull/$PR_NUMBER/merge"
            }

            // tensorflow
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: "$branch"]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class: 'RelativeTargetDirectory',
                                                           relativeTargetDir: "$GIT_NAME"]],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                           refspec: "${refspec}",
                                                           name: GIT_NAME,
                                                           url: "$GIT_URL"]]])

            // ncf
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: 'bhavanis/ncf']],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'tensorflow-models']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                           url          : 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-models.git']]])
                                                           
            // wide-deep-census
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: 'wide_deep_training_census']],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'tensorflow-models-wide-deep-census']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                           url          : 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-models.git']]])

            // wide-deep-criteo
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: 'wide_deep_criteo_training_inference']],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'tensorflow-models-wide-deep-criteo']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                           url          : 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-models.git']]])

            // Inception_ResNet_v2
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: 'inception_resnet_v2']],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                            relativeTargetDir: 'tensorflow-Inception_ResNet_v2']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                           url          : 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-slim-models.git']]])
            // MobileNet_v1
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: 'mobilenet_v1']],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                            relativeTargetDir: 'tensorflow-MobileNet_v1']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                           url          : 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-slim-models.git']]])
            

            // cje-algo
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: CJE_ALGO_BRANCH]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'cje-algo']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL",
                                                           url          : 'https://github.intel.com/AIPG/cje-algo.git']]])

        }

        stage('Benchmark Test') {

            if (DOWNLOAD_WHEEL) {

                def downloadWheel = load("${CJE_TF_COMMON_DIR}/downloadWheel.groovy")
                downloadWheel(DOWNLOAD_WHEEL_FROM_JOB, DOWNLOAD_WHEEL_NAME)
    
            }

            if ( PR_NUMBER != '' ) {
                // PR testing, find the corresponding docker container based on PR_NUMBER and PYTHON_VERSION
                dir ( "$WORKSPACE/$GIT_NAME" ) {
                    sha = sh (script:"git log -1 --pretty=format:%H | cut -c -7",
                              returnStdout: true).trim()
                }
                docker_build_version="PR${PR_NUMBER}-${sha}" 
                echo docker_build_version
                if ( "${PYTHON_VERSION}" == "2" ) 
                    DOCKER_IMAGE = "${DOCKER_IMAGE_NAMESPACE}:${docker_build_version}-avx2-devel-mkl"
                else
                    DOCKER_IMAGE = "${DOCKER_IMAGE_NAMESPACE}:${docker_build_version}-avx2-devel-mkl-py3"
                echo "docker image is ${DOCKER_IMAGE}"
            }

            runBenchmark(DOCKER_IMAGE, DATASET_LOCATION, SINGLE_SOCKET, SERVERNAME, MODELS, MODES, GIT_NAME, DOWNLOAD_WHEEL)

        }

        stage('Collect Logs') {

            // Prepare logs
            def prepareLog = load("${CJE_TF_COMMON_DIR}/prepareLog.groovy")
            prepareLog(TENSORFLOW_BRANCH, "", "", SUMMARYLOG, SUMMARY_TITLE)
            collectBenchmarkLog(MODELS, MODES, SINGLE_SOCKET)

        }

    } catch (e) {
        currentBuild.result = "FAILED"
        throw e

    } finally {
        // Success or failure, always send notifications

        withEnv(["SUMMARYLOG=$SUMMARYLOG"]) {
            echo "$SUMMARYLOG"
            def msg = readFile SUMMARYLOG

            def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
            notifyBuild(SLACK_CHANNEL, currentBuild.result, msg)
        }

        stage('Archive Artifacts ') {
            dir("$WORKSPACE") {
                archiveArtifacts artifacts: '*.log', excludes: null
                fingerprint: true
            }
        }


        if (POST_TO_DASHBOARD) {

            stage('Post to AVUS dashboard') {

                def postAll2AIBTdashboard = load("${CJE_TF_COMMON_DIR}/postAll2AIBTdashboard.groovy")

                LOGS_DIR = "${WORKSPACE}"
                FRAMEWORK = 'tensorflow'
                FRONTEND = 'tensorflow'
                RUNTYPE = 'tfdo-inference'
                DATATYPE = 'float32'

                LOGS_TYPE = [ "latency", "throughput", "accuracy"]
                POSTING_MODELS = [ "NCF" ]
                postAll2AIBTdashboard(RUNTYPE, FRAMEWORK, FRONTEND, TARGET_DASHBOARD, LOGS_DIR, LOGS_TYPE, POSTING_MODELS, DATATYPE)

            } // stage

        } // if POST_TO_DASHBOARD

    } // finally
}

