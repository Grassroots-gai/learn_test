Boolean SINGLE_SOCKET = true
CJE_TF = 'cje-tf'
CJE_TF_COMMON_DIR = "$CJE_TF/common"
GIT_CREDENTIAL = "lab_tfbot"
GIT_CREDENTIAL_LAB = "lab_tfbot"

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

// set default DOCKER_IMAGE  value
DOCKER_IMAGE = 'amr-registry.caas.intel.com/aipg-tf/qa:nightly-int8-master-avx2-devel-mkl'
if ('DOCKER_IMAGE' in params) {
    echo "DOCKER_IMAGE in params"
    if (params.DOCKER_IMAGE != '') {
        DOCKER_IMAGE = params.DOCKER_IMAGE
        echo DOCKER_IMAGE
    }
}
echo "DOCKER_IMAGE: $DOCKER_IMAGE"

TENSORFLOW_MODELS_BRANCH = 'master'
if ('TENSORFLOW_MODELS_BRANCH' in params) {
    echo "TENSORFLOW_MODELS_BRANCH in params"
    if (params.TENSORFLOW_MODELS_BRANCH != '') {
        TENSORFLOW_MODELS_BRANCH = params.TENSORFLOW_MODELS_BRANCH
        echo TENSORFLOW_MODELS_BRANCH
    }
}
echo "TENSORFLOW_MODELS_BRANCH:  $TENSORFLOW_MODELS_BRANCH"

GIT_NAME = 'private-tensorflow'
if ('GIT_NAME' in params) {
    echo "GIT_NAME in params"
    if (params.GIT_NAME != '') {
        GIT_NAME = params.GIT_NAME
        echo GIT_NAME
    }
}
echo "GIT_NAME: $GIT_NAME"

GIT_URL = 'https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git'
if ('GIT_URL' in params) {
    echo "GIT_URL in params"
    if (params.GIT_URL != '') {
        GIT_URL = params.GIT_URL
        echo GIT_URL
    }
}
echo "GIT_URL: $GIT_URL"

TENSORFLOW_BRANCH = 'r2.0'
if ('TENSORFLOW_BRANCH' in params){
    echo "TENSORFLOW_BRANCH in params"
    if (params.TENSORFLOW_BRANCH != '') {
        TENSORFLOW_BRANCH = params.TENSORFLOW_BRANCH
        echo TENSORFLOW_BRANCH
    }
}
echo "TENSORFLOW_BRANCH:  $TENSORFLOW_BRANCH"

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
// TODO: list the Q4 int8 models here separated by comma
MODEL_LIST="resnet50,resnet56"
if ('INPUT_MODELS' in params) {
    echo "MODELS in params"
    if (params.INPUT_MODELS != '') {
        MODEL_LIST = params.INPUT_MODELS
        echo MODEL_LIST
    }
}
echo "MODELS: $MODEL_LIST"


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
SUMMARY_TITLE = 'official models benchmark test'
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

// set default value for CJE_TF_BRANCH
CJE_TF_BRANCH = 'master'
if ('CJE_TF_BRANCH' in params) {
    echo "CJE_TF_BRANCH in params"
    if (params.CJE_TF_BRANCH != '') {
        CJE_TF_BRANCH = params.CJE_TF_BRANCH
        echo CJE_TF_BRANCH
    }
}
echo "CJE_TF_BRANCH:  $CJE_TF_BRANCH"

PYTHON_VERSION = '3'
if ('PYTHON_VERSION' in params) {
    echo "PYTHON_VERSION in params"
    if (params.PYTHON_VERSION != '') {
        PYTHON_VERSION = params.PYTHON_VERSION
        echo PYTHON_VERSION
    }
}
echo "PYTHON_VERSION: $PYTHON_VERSION"

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

MR_NUMBER = ''
if ('MR_NUMBER' in params) {
    echo "MR_NUMBER in params"
    if (params.MR_NUMBER != '') {
        MR_NUMBER = params.MR_NUMBER
        echo MR_NUMBER
    }
}
echo "MR_NUMBER: $MR_NUMBER"

MR_SOURCE_BRANCH = ''
if ('MR_SOURCE_BRANCH' in params) {
    echo "MR_SOURCE_BRANCH in params"
    if (params.MR_SOURCE_BRANCH != '') {
        MR_SOURCE_BRANCH = params.MR_SOURCE_BRANCH
        echo MR_SOURCE_BRANCH
    }
}
echo "MR_SOURCE_BRANCH: $MR_SOURCE_BRANCH"

MR_MERGE_BRANCH = ''
if ('MR_MERGE_BRANCH' in params) {
    echo "MR_MERGE_BRANCH in params"
    if (params.MR_MERGE_BRANCH != '') {
        MR_MERGE_BRANCH = params.MR_MERGE_BRANCH
        echo MR_MERGE_BRANCH
    }
}
echo "MR_MERGE_BRANCH: $MR_MERGE_BRANCH"

DOCKER_IMAGE_NAMESPACE = 'amr-registry.caas.intel.com/aipg-tf/mr'
if ('DOCKER_IMAGE_NAMESPACE' in params) {
    echo "DOCKER_IMAGE_NAMESPACE in params"
    if (params.DOCKER_IMAGE_NAMESPACE != '') {
        DOCKER_IMAGE_NAMESPACE = params.DOCKER_IMAGE_NAMESPACE
        echo DOCKER_IMAGE_NAMESPACE
    }
}
echo "DOCKER_IMAGE_NAMESPACE: $DOCKER_IMAGE_NAMESPACE"

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

    }

}

def runBenchmark(String docker_image, String dataset_location, String servername, String models) {

    workspace_volumn="${WORKSPACE}:/workspace"
    dataset_volume="${dataset_location}:${dataset_location}"

    docker.image("$docker_image").inside("--env \"http_proxy=${http_proxy}\" \
                                          --env \"https_proxy=${https_proxy}\" \
                                          --volume ${workspace_volumn} \
                                          --volume ${dataset_volume} \
                                          --env DATASET_LOCATION=$dataset_location \
                                          --env SERVERNAME=$servername \
                                          --env MODELS=$models \
                                          --privileged \
                                          -u root:root") {

        sh '''#!/bin/bash -x
            chmod 775 /workspace/cje-tf/scripts/run_benchmark_official_v2.sh
            /workspace/cje-tf/scripts/run_benchmark_official_v2.sh
            echo "llsu--->exit failure"
            echo $?
            '''
    }
}

def collectBenchmarkLog(String models) {

    echo "---------------------------------------------------------"
    echo "------------  running collectBenchnmarkLog  -------------"
    echo "---------------------------------------------------------"

    echo "models: $models"
    SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
            returnStdout: true).trim()

    models=models.split(',')

    models.each { model ->
        echo "model is ${model}"
        withEnv(["current_model=$model", "servername=${SERVERNAME}"]) {

            sh '''#!/bin/bash -x
                chmod 775 ${WORKSPACE}/cje-tf/scripts/collect_logs_benchmark_official_v2.sh
                ${WORKSPACE}/cje-tf/scripts/collect_logs_benchmark_official_v2.sh --model=${current_model}
                '''
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
        SUMMARYLOG = "${WORKSPACE}/summary_${SERVERNAME}.log"
        sh (script:"touch $SUMMARYLOG", returnStdout: true).trim()
        SUMMARYTXT = "${WORKSPACE}/summary_nightly.log"
        writeFile file: SUMMARYTXT, text: "Model,Mode,Server,Use_Case,Batch_Size,Result\n"

        // slack notification
        def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
        notifyBuild(SLACK_CHANNEL, 'STARTED', '')

        stage('Checkout') {

            // tensorflow models
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: "$TENSORFLOW_MODELS_BRANCH"]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class: 'RelativeTargetDirectory',
                                                           relativeTargetDir: "official-models"]],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL",
                                                           url: "https://github.com/tensorflow/models.git"]]])

            // tensorflow branch
            if (MR_NUMBER == '') {
                checkout([$class                           : 'GitSCM',
                          branches                         : [[name: "$TENSORFLOW_BRANCH"]],
                          browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                          doGenerateSubmoduleConfigurations: false,
                          extensions                       : [[$class: 'RelativeTargetDirectory',
                                                               relativeTargetDir: "$GIT_NAME"]],
                          submoduleCfg                     : [],
                          userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                               url: "$GIT_URL"]]])
            }
            else {

                checkout([$class                           : 'GitSCM',
                          branches                         : [[name: "$MR_SOURCE_BRANCH"]],
                          doGenerateSubmoduleConfigurations: false,
                          extensions                       : [
                                                               [$class: 'PreBuildMerge', 
                                                                   options: [
                                                                           fastForwardMode: 'FF', 
                                                                           mergeRemote: "$GIT_NAME", 
                                                                           mergeStrategy: 'DEFAULT', 
                                                                           mergeTarget: "$MR_MERGE_BRANCH"
                                                                   ]
                                                               ],
                                                               [$class: 'RelativeTargetDirectory',
                                                               relativeTargetDir: "$GIT_NAME"
                                                               ]
                                                             ],
                          submoduleCfg                     : [],
                          userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                              name: GIT_NAME,
                                                              url: "$GIT_URL"]]
                          ])
            }
        }

        stage('Benchmark Test') {

            if (DOWNLOAD_WHEEL) {

                //def downloadWheel = load("${WORKSPACE}/cje-tf/common/downloadWheel.groovy")
                //downloadWheel(DOWNLOAD_WHEEL_FROM_JOB, DOWNLOAD_WHEEL_NAME)

                withEnv(["dockerImage=$DOCKER_IMAGE"]) {
                    py_version = sh (script:'echo $dockerImage | awk -F\'-\' \'{print $NF}\' | sed \'s/[^0-9]*//g\'', returnStdout: true).trim()
                    echo "py_verion: $py_version"
                }
                echo "py_version is: $py_version"
                //download_wheel_name="*$py_version*.whl"
                //download_wheel_name="*${py_version}m-linux_x86_64.whl"
                DOWNLOAD_WHEEL_NAME="*${py_version}m-${DOWNLOAD_WHEEL_PATTERN}"
                catchError {
                    copyArtifacts(
                        projectName: DOWNLOAD_WHEEL_FROM_JOB,
                        selector: specific("$DOWNLOAD_WHEEL_FROM_JOB_NUMBER"),
                        filter: DOWNLOAD_WHEEL_NAME,
                        fingerprintArtifacts: true,
                        target: "build/")

                    archiveArtifacts artifacts: "build/**"
                }

                withEnv([ "dockerImage=$DOCKER_IMAGE" ]) {
                    sh '''#!/bin/bash -x
                        docker_namespace=$(echo ${dockerImage} | cut -f1 -d:)
                        echo $docker_namespace
                        docker_tag=$(echo ${dockerImage} | cut -f2 -d:)
                        echo $docker_tag
                        docker run --init --name=test-${docker_tag} -d -u root:root -v ${WORKSPACE}:/workspace -e "WORKSPACE=/workspace" -e http_proxy=http://proxy-us.intel.com:911 -e https_proxy=https://proxy-us.intel.com:912 ${dockerImage} tail -f /dev/null
                        docker exec test-${docker_tag} /workspace/cje-tf/scripts/install_tf_wheel.sh
                        docker commit test-${docker_tag} local:qa-${docker_tag}
                        docker push local:qa-${docker_tag}
                        docker stop test-${docker_tag}
                    '''
                }
                docker_tag = sh (script:"echo ${DOCKER_IMAGE} | cut -f2 -d:",
                          returnStdout: true).trim()
                DOCKER_IMAGE="local:qa-${docker_tag}"
            }

            if ( MR_NUMBER != '' ) {
                // MR testing, find the corresponding docker container's tag based on MR_NUMBER, sha and PYTHON_VERSION
                dir ( "$WORKSPACE/$GIT_NAME" ) {
                    target_sha = sh (script:'git log -1 | grep Merge: | awk -F\' \' \'{print $2}\' | cut -c -7',
                              returnStdout: true).trim()
                    source_sha = sh (script:'git log -1 | grep Merge: | awk -F\' \' \'{print $3}\' | cut -c -7',
                              returnStdout: true).trim()
                    echo target_sha
                    echo source_sha
                    sha = "$target_sha$source_sha"
                    echo "sha is $sha"
                }
                docker_build_version="MR${MR_NUMBER}-${sha}" 
                echo docker_build_version
                if ( "${PYTHON_VERSION}" == "2" ) {
                    DOCKER_IMAGE = "${DOCKER_IMAGE_NAMESPACE}:${docker_build_version}-TF-v2-avx2-devel-mkl"
                }
                else {
                    DOCKER_IMAGE = "${DOCKER_IMAGE_NAMESPACE}:${docker_build_version}-TF-v2-avx2-devel-mkl-py3"
                }
            }

            runBenchmark(DOCKER_IMAGE, DATASET_LOCATION, SERVERNAME, MODEL_LIST)
            echo "llsu -----> finish Benchmark Test"

        }

        stage('Collect Logs') {

            echo "llsu ------> begin Collect Logs"

            // Prepare logs
            def prepareLog = load("${CJE_TF_COMMON_DIR}/prepareLog.groovy")
            prepareLog(TENSORFLOW_BRANCH, GIT_NAME, "", SUMMARYLOG, SUMMARY_TITLE)

            collectBenchmarkLog(MODEL_LIST)

        }

    } catch (e) {
        // If there was an exception thrown, the build failed
        echo "llsu ----> catch failure"

        currentBuild.result = "FAILED"
        throw e

    } finally {
        // Success or failure, always send notifications

        withEnv(["SUMMARYLOG=$SUMMARYLOG"]) {
            def msg = readFile SUMMARYLOG

            def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
            notifyBuild(SLACK_CHANNEL, currentBuild.result, msg)

        }

        stage('Archive Artifacts ') {
            dir("$WORKSPACE") {
                archiveArtifacts artifacts: '*.log',  excludes: null
                fingerprint: true

            }
        }

        //withEnv(["SUMMARYLOG=$SUMMARYLOG", "MR_NUMBER=$MR_NUMBER"]) {

        //    sh '''#!/bin/bash -x
        //        if [ $(grep 'CHECK_LOG_FOR_ERROR' $SUMMARYLOG | wc -l) != 0 ] && [ $MR_NUMBER != '' ]; then
        //            echo "There is some benchmark got Failed! Please check it in the report."
        //            exit 1
        //        fi
        //    '''
        //}

    } // finally
}


