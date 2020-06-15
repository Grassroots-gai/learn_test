Boolean SINGLE_SOCKET = true
CJE_TF = 'cje-tf'
CJE_TF_COMMON_DIR = "$CJE_TF/common"
GIT_CREDENTIAL = "lab_tfbot"
GIT_CREDENTIAL_LAB = 'lab_tfbot'

http_proxy="http://proxy-us.intel.com:911"
https_proxy="https://proxy-us.intel.com:912"

// set default value for NODE_LABEL
NODE_LABEL = 'nervana-skx101.fm.intel.com'
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
DOCKER_IMAGE = 'amr-registry.caas.intel.com/aipg-tf/qa:nightly-master-avx2-devel-mkl'
if ('DOCKER_IMAGE' in params) {
    echo "DOCKER_IMAGE in params"
    if (params.DOCKER_IMAGE != '') {
        DOCKER_IMAGE = params.DOCKER_IMAGE
        echo DOCKER_IMAGE
    }
}
echo "DOCKER_IMAGE: $DOCKER_IMAGE"

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
// TODO: list the Q1 2019 fp32 models here separated by comma
MODELS="inceptionv3,resnet50"
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
SUMMARY_TITLE = 'private-tensorflow nightly benchmark Q1 2019 FP32 models'
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

// set default value for CJT-TF BRANCH
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

def runBenchmark(String docker_image, String dataset_location, Boolean single_socket, String servername, String models, String modes, String git_name) {

    workspace_volumn="${WORKSPACE}:/workspace"
    dataset_volume="${dataset_location}:${dataset_location}"
    chmod 775 $WORKSPACE/cje-tf/scripts/run_benchmark_q1_2019_fp32_models.sh
    benchmark_script="DATASET_LOCATION=${dataset_location} SINGLE_SOCKET=${single_socket} SERVERNAME=${servername} MODELS=${models} MODES=${modes} DOCKER_IMAGE=${docker_image} $WORKSPACE/cje-tf/scripts/run_benchmark_q1_2019_fp32_models.sh"
    
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
                                          --privileged \
                                          -u root:root") {
            sh '''#!/bin/bash -x 
            python /workspace/cje-tf/scripts/get_tf_sourceinfo.py --tensorflow_dir=${TENSORFLOW_DIR} --workspace_dir=/workspace
            '''
    }
    
    sh benchmark_script
    
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
                
                if [ -f "$WORKSPACE/benchmark_${current_model}_${current_mode}_${servername}_latency.log" -o -f "$WORKSPACE/benchmark_${current_model}_${current_mode}_${servername}_throughput.log" -o -f "$WORKSPACE/benchmark_${current_model}_${current_mode}_${servername}_accuracy.log" ]; then
                    echo "collecting logs for model: ${current_model} mode: ${current_mode}"
                    chmod 775 $WORKSPACE/cje-tf/scripts/collect_logs_q1_2019_fp32_models.sh
                    $WORKSPACE/cje-tf/scripts/collect_logs_q1_2019_fp32_models.sh --model=${current_model} --mode=${current_mode} --single_socket=${single_socket}
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

def TEST_MAP = [:]
labels = NODE_LABEL.split(',')

for (x in labels) {
    def label = x // Need to bind the label variable before the closure - can't do 'for (label in labels)'

    // Create a map to pass in to the 'parallel' step so we can fire all the builds at once
    TEST_MAP[label] = {

        node ( label ) {
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

                // slack notification
                def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
                notifyBuild(SLACK_CHANNEL, 'STARTED', '')

                stage("Checkout $label") {

                    // Intel Models
                    checkout([$class                           : 'GitSCM',
                              branches                         : [[name: '*/master']],
                              browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                              doGenerateSubmoduleConfigurations: false,
                              extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                                   relativeTargetDir: 'tensorflow-models']],
                              submoduleCfg                     : [],
                              userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                                   url          : 'https://gitlab.devtools.intel.com/intelai/models']]])

                    // cje-algo
                    // this is needed for dashboard posting
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

                stage("Benchmark Test $label") {

                    runBenchmark(DOCKER_IMAGE, DATASET_LOCATION, SINGLE_SOCKET, SERVERNAME, MODELS, MODES, GIT_NAME)

                }

                stage("Collect Logs $label") {

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

                stage("Archive Artifacts $label") {

                    dir("$WORKSPACE") {
                        archiveArtifacts artifacts: '*.log', excludes: null
                        fingerprint: true
                    }

                }


                if (POST_TO_DASHBOARD) {

                    stage("Post to AVUS dashboard $label") {

                        def postAll2AIBTdashboard = load("${CJE_TF_COMMON_DIR}/postAll2AIBTdashboard.groovy")

                        LOGS_DIR = "${WORKSPACE}"
                        FRAMEWORK = 'tensorflow'
                        FRONTEND = 'tensorflow'                
                        DATATYPE = 'float32'                           
                
                        // posting latency and throughput
                        RUNTYPE = 'tfdo-inference'
                        LOGS_TYPE = [ "latency", "throughput"]
                        POSTING_MODELS = []
                        postAll2AIBTdashboard(RUNTYPE, FRAMEWORK, FRONTEND, TARGET_DASHBOARD, LOGS_DIR, LOGS_TYPE, POSTING_MODELS, DATATYPE)

                         // posting accuracy 
                        RUNTYPE = 'tfdo-accuracy'
                        LOGS_TYPE = [ "accuracy"]
                        POSTING_MODELS = [ "inceptionv3", "resnet50" ]
                        postAll2AIBTdashboard(RUNTYPE, FRAMEWORK, FRONTEND, TARGET_DASHBOARD, LOGS_DIR, LOGS_TYPE, POSTING_MODELS, DATATYPE)

                    } // stage

                } // if POST_TO_DASHBOARD

            } // finally

        } // node

    } // TEST_MAP[label]

} // for

parallel TEST_MAP 
