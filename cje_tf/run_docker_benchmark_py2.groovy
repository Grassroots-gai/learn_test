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
TENSORFLOW_BRANCH = 'v1.8.0'
if ('TENSORFLOW_BRANCH' in params) {
    echo "TENSORFLOW_BRANCH in params"
    if (params.TENSORFLOW_BRANCH != '') {
        TENSORFLOW_BRANCH = params.TENSORFLOW_BRANCH
        echo TENSORFLOW_BRANCH
    }
}
echo "TENSORFLOW_BRANCH:  $TENSORFLOW_BRANCH"

// set default value for PRIVATE_TENSORFLOW_BRANCH
PRIVATE_TENSORFLOW_BRANCH = 'master'
if ('PRIVATE_TENSORFLOW_BRANCH' in params) {
    echo "PRIVATE_TENSORFLOW_BRANCH in params"
    if (params.PRIVATE_TENSORFLOW_BRANCH != '') {
        PRIVATE_TENSORFLOW_BRANCH = params.PRIVATE_TENSORFLOW_BRANCH
        echo PRIVATE_TENSORFLOW_BRANCH
    }
}
echo "PRIVATE_TENSORFLOW_BRANCH:  $PRIVATE_TENSORFLOW_BRANCH"

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
// possible values: e.g. amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.8-py2-build
//                       amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.8-py3-build
//                       amr-registry.caas.intel.com/aipg-tf/qa:1.8.0-mkl-py2
DOCKER_IMAGE = 'amr-registry.caas.intel.com/aipg-tf/qa:1.8.0-mkl-py2'
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

// check value for RUN_BENCHMARK
if (params.RUN_BENCHMARK) 
    echo "params.RUN_BENCHMARK is true"
else
    echo "params.RUN_BENCHMARK is false"
Boolean RUN_BENCHMARK = params.RUN_BENCHMARK
Boolean FULL_VALIDATION = false
if ( RUN_BENCHMARK ) {
  FULL_VALIDATION = false
  MODELS="resnet50,inception3,vgg16,ds2,SSDvgg16,mnist,resnet32cifar10,cifar10,dcgan"
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
SUMMARY_TITLE = 'Tensorflow benchmark test summary - py2'
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

// set default value for TENSORFLOW_CIFAR10_BRANCH
TENSORFLOW_CIFAR10_BRANCH = 'master'
if ('TENSORFLOW_CIFAR10_BRANCH' in params) {
    echo "TENSORFLOW_CIFAR10_BRANCH in params"
    if (params.TENSORFLOW_CIFAR10_BRANCH != '') {
        TENSORFLOW_CIFAR10_BRANCH = params.TENSORFLOW_CIFAR10_BRANCH
        echo TENSORFLOW_CIFAR10_BRANCH
    }
}
echo "TENSORFLOW_CIFAR10_BRANCH: $TENSORFLOW_CIFAR10_BRANCH"

// set default value for TENSORFLOW_SSD_BRANCH_INFERENCE
TENSORFLOW_SSD_BRANCH_INFERENCE = 'master'
if ('TENSORFLOW_SSD_BRANCH_INFERENCE' in params) {
    echo "TENSORFLOW_SSD_BRANCH_INFERENCE in params"
    if (params.TENSORFLOW_SSD_BRANCH_INFERENCE != '') {
        TENSORFLOW_SSD_BRANCH_INFERENCE = params.TENSORFLOW_SSD_BRANCH_INFERENCE
        echo TENSORFLOW_SSD_BRANCH_INFERENCE
    }
}
echo "TENSORFLOW_SSD_BRANCH_INFERENCE: $TENSORFLOW_SSD_BRANCH_INFERENCE"

// set default value for TENSORFLOW_SSD_BRANCH_TRAINING
TENSORFLOW_SSD_BRANCH_TRAINING = 'master'
if ('TENSORFLOW_SSD_BRANCH_TRAINING' in params) {
    echo "TENSORFLOW_SSD_BRANCH_TRAINING in params"
    if (params.TENSORFLOW_SSD_BRANCH_TRAINING != '') {
        TENSORFLOW_SSD_BRANCH_TRAINING = params.TENSORFLOW_SSD_BRANCH_TRAINING
        echo TENSORFLOW_SSD_BRANCH_TRAINING
    }
}
echo "TENSORFLOW_SSD_BRANCH_TRAINING: $TENSORFLOW_SSD_BRANCH_TRAINING"

def cloneTFRepo() {

    String GIT_CREDENTIAL = "lab_tfbot"
    String GIT_CREDENTIAL_LAB = "lab_tfbot"


    echo "------- running cloneTFRepo -------"

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

        // private tensorflow
        checkout([$class: 'GitSCM', 
            branches: [[name: PRIVATE_TENSORFLOW_BRANCH]], 
            browser: [$class: 'AssemblaWeb', repoUrl: ''], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [[$class: 'RelativeTargetDirectory', 
                relativeTargetDir: 'private-tensorflow']], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB", 
                url: "https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git"]]])

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

        // deepSpeech2 - ds2
        checkout([$class: 'GitSCM', 
            branches: [[name: '*/master']], 
            browser: [$class: 'AssemblaWeb', repoUrl: ''], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [[$class: 'RelativeTargetDirectory', 
                relativeTargetDir: 'deepSpeech2']], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL", 
                url: 'https://github.com/yao-matrix/deepSpeech2.git']]])
        
        checkout([$class: 'GitSCM',
            branches: [[name: TENSORFLOW_SSD_BRANCH_INFERENCE]],
            browser: [$class: 'AssemblaWeb', repoUrl: ''],
            doGenerateSubmoduleConfigurations: false,
            extensions: [[$class: 'RelativeTargetDirectory',
                relativeTargetDir: 'tensorflow-SSD-Inference']],
            submoduleCfg: [],
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                url: 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-SSD.git']]])

        // model SSDvgg16 for training
        checkout([$class: 'GitSCM',
            branches: [[name: TENSORFLOW_SSD_BRANCH_TRAINING]],
            browser: [$class: 'AssemblaWeb', repoUrl: ''],
            doGenerateSubmoduleConfigurations: false,
            extensions: [[$class: 'RelativeTargetDirectory',
                relativeTargetDir: 'tensorflow-SSD-Training']],
            submoduleCfg: [],
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                url: 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-SSD.git']]])

        // nmt
        checkout([$class: 'GitSCM', 
            branches: [[name: '*/master']], 
            browser: [$class: 'AssemblaWeb', repoUrl: ''], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [[$class: 'RelativeTargetDirectory', 
                relativeTargetDir: 'nmt']], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL", 
                url: 'https://github.com/tensorflow/nmt.git']]])

        // models - cifar10, resnet32 w/cifar10
        checkout([$class: 'GitSCM', 
            branches: [[name: TENSORFLOW_CIFAR10_BRANCH]], 
            browser: [$class: 'AssemblaWeb', repoUrl: ''], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [[$class: 'RelativeTargetDirectory', 
                relativeTargetDir: 'models']], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL", 
                url: 'https://github.com/tensorflow/models.git']]])

        // dcgan-tf-benchmark
        checkout([$class: 'GitSCM', 
            branches: [[name: "*/master"]], 
            browser: [$class: 'AssemblaWeb', repoUrl: ''], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [[$class: 'RelativeTargetDirectory', 
                relativeTargetDir: 'dcgan-tf-benchmark']], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL", 
                url: 'https://github.com/MustafaMustafa/dcgan-tf-benchmark']]])

    } catch(e) {

        echo "===================="
        echo "ERROR: Exception caught in module which clones the tensorflow repo - cloneTFRepo()"
        echo "ERROR: ${e}"
        echo "===================="

        echo ' '
        echo "Build marked as FAILURE"
        currentBuild.result = 'FAILURE'

    }  // catch
}

def cleanup() {

    sh '''#!/bin/bash -x
    cd WORKSPACE
    sudo rm -rf *
    '''

}

def runBenchmark(String DOCKER_IMAGE, String DATASET_LICATION, Boolean RUN_BENCHMARK, String TARGET_PLATFORM, String TENSORFLOW_BRANCH, String PYTHON, Boolean SINGLE_SOCKET, String SERVERNAME) {

    WORKSPACE_VOLUME="${WORKSPACE}:/workspace"
    DATASET_VOLUME="${DATASET_LOCATION}:${DATASET_LOCATION}"

    docker.image("$DOCKER_IMAGE").inside("--env \"http_proxy=${http_proxy}\" \
                                          --env \"https_proxy=${https_proxy}\" \
                                          --volume ${WORKSPACE_VOLUME} \
                                          --volume ${DATASET_VOLUME} \
                                          --env DATASET_LOCATION=$DATASET_LOCATION \
                                          --env RUN_BENCHMARK=$RUN_BENCHMARK \
                                          --env TARGET_PLATFORM=$TARGET_PLATFORM \
                                          --env TENSORFLOW_BRANCH=$TENSORFLOW_BRANCH \
                                          --env PYTHON=$PYTHON \
                                          --env SINGLE_SOCKET=$SINGLE_SOCKET \
                                          --env SERVERNAME=$SERVERNAME \
                                          --env MODELS=$MODELS \
                                          --env MODES=$MODES \
                                          --privileged \
                                          -u root:root") {

        sh '''#!/bin/bash -x
        
        cd /workspace        
        chmod 775 ./cje-tf/scripts/run_docker_benchmark_py2.sh
        ./cje-tf/scripts/run_docker_benchmark_py2.sh
        '''
    }
 
}

def collectBenchmarkLog(String models, String modes, Boolean single_socket, Boolean full_validation) {

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
            withEnv(["current_model=$model","current_mode=$mode", "single_socket=${single_socket}", "full_validation=${full_validation}", "servername=${SERVERNAME}"]) {
                
                sh '''#!/bin/bash -x
                # Q1 models
                Q1_MODELS="resnet50 inception3 vgg16 ds2 SSDvgg16 mnist resnet32cifar10 cifar10 dcgan"
                # Q2 models
                Q2MODELS="inception_v4 inception_resnet_v2 SqueezeNet YoloV2 fastrcnn gnmt rfcn transformerLanguage transformerSpeech WaveNet wideDeep WaveNet_Magenta deepSpeech mobilenet_v1"
                # Checking the model we want to run is a Q1 model or Q2 model
                echo $Q1_MODELS | grep -w -F -q $current_model
                if [ $? -eq 0 ]; then
                    echo "$current_model is a Q1 model"
                    if [ -f "$WORKSPACE/benchmark_${current_model}_${current_mode}_${servername}.log" ]; then
                        chmod 775 $WORKSPACE/cje-tf/scripts/collect_logs.sh
                        $WORKSPACE/cje-tf/scripts/collect_logs.sh --model=${current_model} --mode=${current_mode} --fullvalidation=${full_validation} --single_socket=${single_socket}
                    fi
                else
                    echo "$current_model is a Q2 model"
                    chmod 775 $WORKSPACE/cje-tf/scripts/collect_logs_q2_models.sh
                    $WORKSPACE/cje-tf/scripts/collect_logs_q2_models.sh --model=${current_model} --mode=${current_mode} --fullvalidation=${full_validation} --single_socket=${single_socket}
                  
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

        // slack notification
        def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
        notifyBuild(SLACK_CHANNEL, 'STARTED', '')

        stage('Checkout') {

            cloneTFRepo()

        }

        stage('Benchmark Test') {

            WORKSPACE_VOLUME="${WORKSPACE}:/workspace"
            DATASET_VOLUME="${DATASET_LOCATION}:${DATASET_LOCATION}"

            docker.image("$DOCKER_IMAGE").inside("--env \"http_proxy=${http_proxy}\" \
                                              --env \"https_proxy=${https_proxy}\" \
                                              --volume ${WORKSPACE_VOLUME} \
                                              --volume ${DATASET_VOLUME} \
                                              --env DATASET_LOCATION=$DATASET_LOCATION \
                                              --env RUN_BENCHMARK=$RUN_BENCHMARK \
                                              --env TARGET_PLATFORM=$TARGET_PLATFORM \
                                              --env TENSORFLOW_BRANCH=$TENSORFLOW_BRANCH \
                                              --env PYTHON=$PYTHON \
                                              --env SINGLE_SOCKET=$SINGLE_SOCKET \
                                              --env SERVERNAME=${SERVERNAME} \
                                              --env MODELS=${MODELS} \
                                              --env MODES=${MODES} \
                                              --privileged \
                                              -u root:root") {

                sh '''#!/bin/bash -x
                python --version
                pip list

                # below are the dependencies that are needed when running with different models,
                # these are now done when the docker is created
                ### install common dependencies
                export DEBIAN_FRONTEND=noninteractive # This is to disable tzdata prompt
                apt-get clean; apt-get update -y;
                apt-get install libsm6 libxext6 numactl python-tk -y
                pip install requests

                #pip install matplotlib
                #pip install opencv-python
                #pip install --upgrade /localdisk/dataset/*.whl
                chmod 775 /workspace/cje-tf/scripts/run_docker_benchmark_py2.sh
                /workspace/cje-tf/scripts/run_docker_benchmark_py2.sh
                '''

            }
        }

        stage('Collect Logs') {

            // Prepare logs
            def prepareLog = load("${CJE_TF_COMMON_DIR}/prepareLog.groovy")
            prepareLog(TENSORFLOW_BRANCH, GIT_NAME, RUN_TYPE, SUMMARYLOG, SUMMARY_TITLE)

            collectBenchmarkLog(MODELS, MODES, SINGLE_SOCKET, FULL_VALIDATION)

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



