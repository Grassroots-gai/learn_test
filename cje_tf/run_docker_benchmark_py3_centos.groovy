Boolean SINGLE_SOCKET = true
TENSORFLOW_COMMON_BRANCH='master'
ERROR="0"
CJE_TF='cje-tf'
CJE_TF_COMMON_DIR = "$CJE_TF/common"
RUN_TYPE = 'mkldnn'

GIT_CREDENTIAL = "lab_tfbot"
GIT_CREDENTIAL_LAB = "lab_tfbot"

http_proxy="http://proxy-us.intel.com:911"
https_proxy="https://proxy-us.intel.com:912"

// set default value for NODE_LABEL
NODE_LABEL = 'nervana-skx17.fm.intel.com'
if ('NODE_LABEL' in params) {
    echo "NODE_LABEL in params"
    if (params.NODE_LABEL != '') {
        NODE_LABEL = params.NODE_LABEL
        echo NODE_LABEL
    }
}
echo "NODE_LABEL: $NODE_LABEL"

// set default value for GIT_URL
GIT_URL = "https://github.com/tensorflow/tensorflow.git"
if ('GIT_URL' in params) {
    echo "GIT_URL in params"
    if (params.GIT_URL != '') {
        GIT_URL = params.GIT_URL
        echo GIT_URL
    }
}
echo "GIT_URL: $GIT_URL"

// set default value for GIT_NAME
GIT_NAME = "tensorflow"
if ('GIT_NAME' in params) {
    echo "GIT_NAME in params"
    if (params.GIT_NAME != '') {
        GIT_NAME = params.GIT_NAME
        echo GIT_NAME
    }
}
echo "GIT_NAME: $GIT_NAME"

// set default value for TENSORFLOW_BRANCH
TENSORFLOW_BRANCH = 'v1.9.0'
if ('TENSORFLOW_BRANCH' in params) {
    echo "TENSORFLOW_BRANCH in params"
    if (params.TENSORFLOW_BRANCH != '') {
        TENSORFLOW_BRANCH = params.TENSORFLOW_BRANCH
        echo TENSORFLOW_BRANCH
    }
}
echo "TENSORFLOW_BRANCH:  $TENSORFLOW_BRANCH"

// set default value for PYTHON version
// possible values: e.g. 2.7
//                       3.5
PYTHON = '2.7'
if ('PYTHON' in params) {
    echo "PYTHON in params"
    if (params.PYTHON != '') {
        PYTHON = params.PYTHON
        echo PYTHON
    }
}
echo "PYTHON:  $PYTHON"

// set default DOCKER_IMAGE  value
// possible values: e.g. 
// 1.8:             
// amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.8-py2-build
// amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.8-py3-build
// amr-registry.caas.intel.com/aipg-tf/qa:1.8.0-mkl-py2
// 1.9:
// avx:
// amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.9.0-devel-mkl
// amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.9.0-devel-mkl-py3
//  avx2:
//  amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.9.0-avx2-devel-mkl
//  amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.9.0-avx2-devel-mkl-py3
DOCKER_IMAGE = 'amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.9.0-avx2-devel-mkl-py3'
if ('DOCKER_IMAGE' in params) {
    echo "DOCKER_IMAGE in params"
   if (params.DOCKER_IMAGE != '') {
        DOCKER_IMAGE = params.DOCKER_IMAGE
        echo DOCKER_IMAGE
    }
}
echo "DOCKER_IMAGE: $DOCKER_IMAGE"

// set default value for TARGET_PLATFORM
// possible values: e.g. avx
//                       avx2
//                       avx512
TARGET_PLATFORM = 'avx'
if ('TARGET_PLATFORM' in params) {
    echo "TARGET_PLATFORM in params"
    if (params.TARGET_PLATFORM != '') {
        TARGET_PLATFORM = params.TARGET_PLATFORM
        echo TARGET_PLATFORM
    }
}
echo "TARGET_PLATFORM: $TARGET_PLATFORM"

// set default value for DATASET_LOCATION
DATASET_LOCATION = '/localdisk/dataset'
if ('DATASET_LOCATION' in params) {
    echo "DATASET_LOCATION in params"
    if (params.DATASET_LOCATION != '') {
        DATASET_LOCATION = params.DATASET_LOCATION
        echo DATASET_LOCATION
    }
}
echo "DATASET_LOCATION: $DATASET_LOCATION"

RUN_Q1MODELS = 'false'
if (params.RUN_Q1MODELS)
    echo "params.RUN_Q1MODELS is true"
else
    echo "params.RUN_Q1MODELS is false"
Boolean RUN_Q1MODELS=params.RUN_Q1MODELS

RUN_Q2MODELS = 'true'
if (params.RUN_Q2MODELS)
    echo "params.RUN_Q2MODELS is true"
else
    echo "params.RUN_Q2MODELS is false"
Boolean RUN_Q2MODELS=params.RUN_Q2MODELS

// check value for RUN_BENCHMARK
if (params.RUN_BENCHMARK) 
    echo "params.RUN_BENCHMARK is true"
else
    echo "params.RUN_BENCHMARK is false"
Boolean RUN_BENCHMARK = params.RUN_BENCHMARK

Boolean FULL_VALIDATION = false
if ( RUN_BENCHMARK ) {
    FULL_VALIDATION = false
    if (RUN_Q1MODELS) {
        MODELS="resnet50,inception3,vgg16,ds2,SSDvgg16,mnist,resnet32cifar10,cifar10,dcgan"
    }
    if (RUN_Q2MODELS) {
        MODELS="inception_v4,inception_resnet_v2,mobilenet_v1,SqueezeNet"
    }

}
else {
  FULL_VALIDATION = true
  MODELS="resnet32cifar10"
}

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
SUMMARY_TITLE = "Tensorflow benchmark test summary - target platform: $TARGET_PLATFORM, Python: $PYTHON"
if ('SUMMARY_TITLE' in params) {
    echo "SUMMARY_TITLE in params"
    if (params.SUMMARY_TITLE != '') {
        SUMMARY_TITLE = params.SUMMARY_TITLE
        echo SUMMARY_TITLE
    }
}
echo "SUMMARY_TITLE: $SUMMARY_TITLE"

// set default value for MODELS to run
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

TF_SLIM_MODEL='tensorflow-slim-models'
TF_SLIM_MODEL_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-slim-models.git'

SQUEEZE_NET_MODEL='tensorflow-SqueezeNet'
SQEEZE_NET_MODEL_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-SqueezeNet.git'

YOLO_V2_MODEL='tensorflow-YoloV2'
YOLO_V2_MODEL_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-YoloV2.git'

TF_FASTRCNN='tensorflow-models'
TF_FASTRCNN_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-models.git'
TF_FASTRCNN_BRANCH='FastRCNN-Resnet50'
COCOAPI_URL='https://github.com/cocodataset/cocoapi.git'
COCOAPI='cocoapi'

TENSORFLOW_NMT='tensorflow-NMT'
TENSORFLOW_NMT_URL='https://github.com/NervanaSystems/tensorflow-NMT.git'

TENSORFLOW_DEEP_SPEECH='tensorflow-DeepSpeech'
TENSORFLOW_DEEP_SPEECH_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-DeepSpeech.git'
TENSORFLOW_DEEP_SPEECH_BRANCH='bhavanis/mkl-optimizations'

TENSORFLOW_RFCN='tensorflow-RFCN'
TENSORFLOW_RFCN_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-models.git'
TENSORFLOW_RFCN_BRANCH='rfcn-resnet101-coco'

TRANSFORMER_LANGUAGE='Nervana-Tensor2tensor'
TRANSFORMER_LANGUAGE_URL="https://github.com/NervanaSystems/Nervana-Tensor2tensor"
TRANSFORMER_LANGUAGE_BRANCH='*/master'

TRANSFORMER_SPEECH='tensorflow-TransformerSpeech'
TRANSFORMER_SPEECH_URL="https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-TransformerSpeech.git"
TRANSFORMER_SPEECH_BRANCH='*/master'

TENSORFLOW_WIDEDEEP='wideDeep'
TENSORFLOW_WIDEDEEP_URL="https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-models.git"
TENSORFLOW_WIDEDEEP_BRANCH='wei-wide-deep'

WAVENET_URL="https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-regular-wavenet-deprecated.git"
WAVENET_DIR='tensorflow-regular-wavenet'

WAVENET_MAGENTA_URL="https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-WaveNet.git"
WAVENET_MAGENTA_DIR="tensorflow-WaveNet"
WAVENET_MAGENTA_BRANCH="ashraf/tf-Wavenet"


def cloneTFRepo() {

    String GIT_CREDENTIAL = "lab_tfbot"
    String GIT_CREDENTIAL_LAB = "lab_tfbot"


    echo "---------------------------------------------------------"
    echo "-------------     running cloneTFRepo     ---------------"
    echo "---------------------------------------------------------"

    try { 
        checkout([$class: 'GitSCM', 
            branches: [[name: TENSORFLOW_BRANCH]], 
            browser: [$class: 'AssemblaWeb', repoUrl: ''], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [[$class: 'RelativeTargetDirectory', 
                relativeTargetDir: GIT_NAME]], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL", 
                name: GIT_NAME, 
                url: GIT_URL]]])

        // private-tensorflow-benchmarks
        checkout([$class: 'GitSCM', 
            branches: [[name: TENSORFLOW_BENCHMARKS_BRANCH]], 
            browser: [$class: 'AssemblaWeb', repoUrl: ''], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [[$class: 'RelativeTargetDirectory', 
                relativeTargetDir: 'private-tensorflow-benchmarks']], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                url: 'https://gitlab.devtools.intel.com/TensorFlow/Shared/private-tensorflow-benchmarks.git']]])

        // tensorflow-common
        checkout([$class: 'GitSCM', 
            branches: [[name: TENSORFLOW_COMMON_BRANCH]], 
            browser: [$class: 'AssemblaWeb', repoUrl: ''], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [[$class: 'RelativeTargetDirectory', 
                relativeTargetDir: 'tensorflow-common']], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                url: 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-common.git']]])

        // DenseNet
        checkout([$class                     : 'GitSCM',
            branches                         : [[name: "*/master"]],
            browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
            doGenerateSubmoduleConfigurations: false,
            extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                relativeTargetDir: 'tensorflow-DenseNet']],
            submoduleCfg                     : [],
            userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                url          : 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-DenseNet.git']]])


        // slim-model
        checkout([$class: 'GitSCM',
                      branches: [[name: '*/master']],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "$TF_SLIM_MODEL"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "$TF_SLIM_MODEL_URL"]]])


        // squeezenet
        checkout([$class: 'GitSCM',
                      branches: [[name: '*/master']],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "$SQUEEZE_NET_MODEL"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "$SQEEZE_NET_MODEL_URL"]]])

        // YoloV2
        checkout([$class: 'GitSCM',
                      branches: [[name: '*/master']],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "$YOLO_V2_MODEL"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "$YOLO_V2_MODEL_URL"]]])

        // deepSpeech
        checkout([$class: 'GitSCM',
                      branches: [[name: TENSORFLOW_DEEP_SPEECH_BRANCH]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${TENSORFLOW_DEEP_SPEECH}"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "${TENSORFLOW_DEEP_SPEECH_URL}"]]])


        // fastRCNN
        checkout([$class: 'GitSCM',
                      branches: [[name: TF_FASTRCNN_BRANCH]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${TF_FASTRCNN}"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "${TF_FASTRCNN_URL}"]]])

        // cocoapi
        checkout([$class: 'GitSCM',
                      branches: [[name: '*/master']],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${COCOAPI}"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                           url: "${COCOAPI_URL}"]]])

        // model nmt
        checkout([$class: 'GitSCM',
                      branches: [[name: '*/master']],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                     relativeTargetDir: "${TENSORFLOW_NMT}"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                           url: "${TENSORFLOW_NMT_URL}"]]])

        // RFCN
        checkout([$class: 'GitSCM',
                      branches: [[name: TENSORFLOW_RFCN_BRANCH]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${TENSORFLOW_RFCN}"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "${TENSORFLOW_RFCN_URL}"]]])

        // transformerLanguage
        checkout([$class: 'GitSCM',
                      branches: [[name: TRANSFORMER_LANGUAGE_BRANCH]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${TRANSFORMER_LANGUAGE}"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                           url: "${TRANSFORMER_LANGUAGE_URL}"]]])

        // transformerSpeech
        checkout([$class: 'GitSCM',
                      branches: [[name: TRANSFORMER_SPEECH_BRANCH]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${TRANSFORMER_SPEECH}"],
                                   [$class: 'SubmoduleOption',
                                    disableSubmodules: false,
                                    parentCredentials: true,
                                    recursiveSubmodules: true,
                                    reference: '',
                                    trackingSubmodules: false]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "${TRANSFORMER_SPEECH_URL}"]]])

        // wavenet
        checkout([$class: 'GitSCM',
                      branches: [[name: '*/master']],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "$WAVENET_DIR"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                           url: "$WAVENET_URL"]]])

        // wavenet-magenta
        checkout([$class: 'GitSCM',
                      branches: [[name: "$WAVENET_MAGENTA_BRANCH"]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "$WAVENET_MAGENTA_DIR"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "$WAVENET_MAGENTA_URL"]]])


    } catch(e) {

        echo "=================================================================================="
        echo "ERROR: Exception caught in module which clones the tensorflow repo - cloneTFRepo()"
        echo "ERROR: ${e}"
        echo "=================================================================================="

        echo ' '
        echo "Build marked as FAILURE"
        currentBuild.result = 'FAILURE'

    }  // catch
}

def cleanup() {

    sh '''#!/bin/bash -x
    cd ${WORKSPACE}
    sudo rm -rf *
    '''

}

def collectBenchmarkLog(String models, String modes, Boolean single_socket, Boolean full_validation) {

    echo "---------------------------------------------------------"
    echo "------------  running collectBenchnmarkLog  -------------"
    echo "---------------------------------------------------------"

    try {
        echo "models: $models"
        echo "modes: $modes"
        ERROR="0"
        SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                              returnStdout: true).trim()

        models=models.split(',')
        modes=modes.split(',')

        models.each { model ->
            modes.each { mode ->
                withEnv(["current_model=$model","current_mode=$mode", "single_socket=${single_socket}", "full_validation=${full_validation}", "error=${ERROR}", "servername=${SERVERNAME}"]) {
                    sh '''#!/bin/bash -x
                    # Q1 models
                    Q1_MODELS="resnet50 inception3 vgg16 ds2 SSDvgg16 mnist resnet32cifar10 cifar10 dcgan"
                    # Q2 models
                    Q2MODELS="inception_v4 inception_resnet_v2 SqueezeNet YoloV2 fastrcnn gnmt rfcn transformerLanguage transformerSpeech WaveNet wideDeep WaveNet_Magenta deepSpeech mobilenet_v1"
                    # Checking the model we want to run is a Q1 model or Q2 model
                    echo $Q1_MODELS | grep -w -F -q $current_model
                    if [ $? -eq 0 ]; then
                        echo "$current_model is a Q1 model"
                        chmod 775 $WORKSPACE/cje-tf/scripts/collect_logs.sh
                        $WORKSPACE/cje-tf/scripts/collect_logs.sh --model=${current_model} --mode=${current_mode} --fullvalidation=${full_validation} --single_socket=${single_socket}
                    else
                        echo "$model is a Q2 model"
                        chmod 775 $WORKSPACE/cje-tf/scripts/collect_logs_q2_models.sh
                        $WORKSPACE/cje-tf/scripts/collect_logs_q2_models.sh --model=${current_model} --mode=${current_mode} --fullvalidation=${full_validation} --single_socket=${single_socket}
                  
                    fi

                    if [ $? -eq 0 ] ; then
                        echo "running model ${current_model} ${current_mode} success"
                        RESULT="SUCCESS"
                    else
                        echo "running model ${current_model} ${current_mode} fail"
                        RESULT="FAILURE"
                        error="1"
                    fi
                    '''
                }
            }
        }
        echo "done running benchmark ${ERROR} ...end"
        if ( ERROR.equals("1") )
             currentBuild.result = "UNSTABLE"
        stash allowEmpty: true, includes: "*.log", name: "logfile"
  
    } catch(e) {

        echo "========================================================================================="
        echo "ERROR: Exception caught in module which collect the benchmark log - collectBenchmarkLog()"
        echo "ERROR: ${e}"
        echo "========================================================================================="

        echo ' '
        echo "Build marked as FAILURE"
        currentBuild.result = 'FAILURE'

    }  // catch

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

        // slack notification
        def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
        notifyBuild(SLACK_CHANNEL, 'STARTED', '')

        stage('Checkout') {

            cloneTFRepo()

        }

        stage('Get TF wheel') {

            if (DOWNLOAD_WHEEL) {

                def downloadWheel = load("${CJE_TF_COMMON_DIR}/downloadWheel.groovy")
                downloadWheel(DOWNLOAD_WHEEL_FROM_JOB, DOWNLOAD_WHEEL_NAME)
    
            }

        }

        stage('Benchmark Test') {

            try {

                sh '''
                    cp ${WORKSPACE}/tensorflow-common/benchmark-interface/platform_util.py ${WORKSPACE}/tensorflow-slim-models
                    cp ${WORKSPACE}/tensorflow-common/benchmark-interface/platform_util.py ${WORKSPACE}/tensorflow-SqueezeNet
                '''

                WORKSPACE_VOLUME="${WORKSPACE}:/workspace"
                DATASET_VOLUME="${DATASET_LOCATION}:${DATASET_LOCATION}"

                docker.image("$DOCKER_IMAGE").pull()
                docker.image("$DOCKER_IMAGE").inside("--env \"http_proxy=${http_proxy}\" \
                                                  --env \"https_proxy=${https_proxy}\" \
                                                  --volume ${WORKSPACE_VOLUME} \
                                                  --volume ${DATASET_VOLUME} \
                                                  --env GIT_NAME=$GIT_NAME \
                                                  --env DATASET_LOCATION=$DATASET_LOCATION \
                                                  --env RUN_BENCHMARK=$RUN_BENCHMARK \
                                                  --env TARGET_PLATFORM=$TARGET_PLATFORM \
                                                  --env TENSORFLOW_BRANCH=$TENSORFLOW_BRANCH \
                                                  --env PYTHON=$PYTHON \
                                                  --env SINGLE_SOCKET=$SINGLE_SOCKET \
                                                  --env SERVERNAME=${SERVERNAME} \
                                                  --env MODELS=${MODELS} \
                                                  --env MODES=${MODES} \
                                                  --env RUN_Q1MODELS=$RUN_Q1MODELS \
                                                  --env RUN_Q2MODELS=$RUN_Q2MODELS \
                                                  --privileged \
                                                  -u root:root") {

                    sh '''#!/bin/bash -x
                        chmod 775 /workspace/cje-tf/scripts/run_docker_benchmark_py2_centos.sh                
                        /workspace/cje-tf/scripts/run_docker_benchmark_py2_centos.sh
                    '''
 
                }

            } catch(e) {

                echo "==============================================================="
                echo "ERROR: Exception caught in stage which runs the benchmark test "
                echo "ERROR: ${e}"
                echo "==============================================================="

                echo ' '
                echo "Build marked as FAILURE"
                currentBuild.result = 'FAILURE'

            }  // catch
        }

        stage('Collect Logs') {

            try {

                // Prepare logs
                def prepareLog = load("${CJE_TF_COMMON_DIR}/prepareLog.groovy")
                prepareLog(TENSORFLOW_BRANCH, GIT_NAME, RUN_TYPE, SUMMARYLOG, SUMMARY_TITLE)

                collectBenchmarkLog(MODELS, MODES, SINGLE_SOCKET, FULL_VALIDATION)

            } catch(e) {

                echo "=============================================================="
                echo "ERROR: Exception caught  which runs in the stage Collect Logs "
                echo "ERROR: ${e}"
                echo "=============================================================="

                echo ' '
                echo "Build marked as FAILURE"
                currentBuild.result = 'FAILURE'

            }  // catch

        }
 
    } catch (e) {
        // If there was an exception thrown, the build failed

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

            dir("$WORKSPACE" + "/publish") {
                unstash "logfile"

                archiveArtifacts artifacts: '*.log', excludes: null
                fingerprint: true

            }

        }

    } // finally
}

