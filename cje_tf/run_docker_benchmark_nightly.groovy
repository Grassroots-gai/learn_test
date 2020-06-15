Boolean SINGLE_SOCKET = true
TENSORFLOW_COMMON_BRANCH='master'
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
    }
}
echo "NODE_LABEL: $NODE_LABEL"

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

// set default value for GIT_NAME
GIT_NAME = "private-tensorflow"
if ('GIT_NAME' in params) {
    echo "GIT_NAME in params"
    if (params.GIT_NAME != '') {
        GIT_NAME = params.GIT_NAME
        echo GIT_NAME
    }
}
echo "GIT_NAME: $GIT_NAME"

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

// set default value for TARGET_PLATFORM
// possible values: e.g. avx
//                       avx2
//                       avx512
TARGET_PLATFORM = 'avx2'
if ('TARGET_PLATFORM' in params) {
    echo "TARGET_PLATFORM in params"
    if (params.TARGET_PLATFORM != '') {
        TARGET_PLATFORM = params.TARGET_PLATFORM
        echo TARGET_PLATFORM
    }
}
echo "TARGET_PLATFORM: $TARGET_PLATFORM"

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

RUN_Q1MODELS = 'true'
if (params.RUN_Q1MODELS)
    echo "params.RUN_Q1MODELS is true"
else
    echo "params.RUN_Q1MODELS is false"
Boolean RUN_Q1MODELS=params.RUN_Q1MODELS

RUN_Q2MODELS = 'false'
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
        MODELS="inception_v4,inception_resnet_v2,SqueezeNet,YoloV2,fastrcnn,gnmt,rfcn,transformerLanguage,transformerSpeech,WaveNet,wideDeep,WaveNet_Magenta,deepSpeech,mobilenet_v1,UNet,DRAW,A3C,DCGAN,3DGAN,inceptionv3,resnet50"
    }

}
else {
  FULL_VALIDATION = true
  MODELS="resnet32cifar10"
}

// set default value for MODELS to run
if ('MODELS' in params) {
    echo "MODELS in params"
    if (params.MODELS != '') {
        MODELS = params.MODELS
        echo MODELS
    }
}
echo "MODELS: $MODELS"

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
SUMMARY_TITLE = 'private-tensorflow nightly benchmark test MKL-DNN summary'
if ('SUMMARY_TITLE' in params) {
    echo "SUMMARY_TITLE in params"
    if (params.SUMMARY_TITLE != '') {
        SUMMARY_TITLE = params.SUMMARY_TITLE
        echo SUMMARY_TITLE
    }
}
echo "SUMMARY_TITLE: $SUMMARY_TITLE"

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

// check default value for POST_TO_DASHBOARD
POST_TO_DASHBOARD = 'true'
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

// setting CJE_ALGO branch 
CJE_ALGO_BRANCH = '*/master'
if ('CJE_ALGO_BRANCH' in params) {
    echo "CJE_ALGO_BRANCH in params"
    if (params.CJE_ALGO_BRANCH != '') {
        CJE_ALGO_BRANCH = params.CJE_ALGO_BRANCH
        echo CJE_ALGO_BRANCH
    }
}

PR_NUMBER = ''
if ('PR_NUMBER' in params) {
    echo "PR_NUMBER in params"
    if (params.PR_NUMBER != '') {
        PR_NUMBER = params.PR_NUMBER
        echo PR_NUMBER
    }
}
echo "PR_NUMBER: $PR_NUMBER"

PYTHON_VERSION = '3'
if ('PYTHON_VERSION' in params) {
    echo "PYTHON_VERSION in params"
    if (params.PYTHON_VERSION != '') {
        PYTHON_VERSION = params.PYTHON_VERSION
        echo PYTHON_VERSION
    }
}
echo "PYTHON_VERSION: $PYTHON_VERSION"

def cleanup() {

    try {
        sh '''#!/bin/bash -x
        cd ${WORKSPACE}
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


            def cloneTFBenchmark = load("${CJE_TF_COMMON_DIR}/cloneTFBenchmark.groovy")
            cloneTFBenchmark(RUN_Q1MODELS, RUN_Q2MODELS)

        }

        stage('Benchmark Test') {
 
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
                echo "dataset location is ${DATASET_LOCATION}"
                echo "servername is ${SERVERNAME}"
                echo "models is ${MODELS}"
                echo "modes is ${MODES}"
            }

            def runBenchmarkInDocker = load("${CJE_TF_COMMON_DIR}/runBenchmarkInDocker.groovy")
            runBenchmarkInDocker(DOCKER_IMAGE, DATASET_LOCATION, RUN_BENCHMARK, TARGET_PLATFORM, TENSORFLOW_BRANCH, PYTHON_VERSION, SINGLE_SOCKET, SERVERNAME, MODELS, MODES, RUN_Q1MODELS, RUN_Q2MODELS)

        }

        stage('Collect Logs') {

            // Prepare logs
            def prepareLog = load("${CJE_TF_COMMON_DIR}/prepareLog.groovy")
            prepareLog(TENSORFLOW_BRANCH, GIT_NAME, RUN_TYPE, SUMMARYLOG, SUMMARY_TITLE)

            def collectBenchmarkLogs = load("${CJE_TF_COMMON_DIR}/collectBenchmarkLogs.groovy")
            collectBenchmarkLogs(MODELS, MODES, SINGLE_SOCKET, FULL_VALIDATION, RUN_Q1MODELS, RUN_Q2MODELS)      

            stash allowEmpty: true, includes: "*.log", name: "logfile"
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

        if (POST_TO_DASHBOARD) {

            stage('Post to AVUS dashboard') {

                def postAll2AIBTdashboard = load("${CJE_TF_COMMON_DIR}/postAll2AIBTdashboard.groovy")

                LOGS_DIR = "${WORKSPACE}"
                FRAMEWORK = 'tensorflow'
                FRONTEND = 'tensorflow'
                RUNTYPE = 'tfdo-inference'
                DATATYPE = 'float32'

                // for Q1 models only posting vgg16 & SSDvgg16 
                if (RUN_Q1MODELS) {
                    LOGS_TYPE = [ "latency", "throughput" ]
                    POSTING_MODELS = [ "vgg16", "SSDvgg16"]
                    postAll2AIBTdashboard(RUNTYPE, FRAMEWORK, FRONTEND, TARGET_DASHBOARD, LOGS_DIR, LOGS_TYPE, POSTING_MODELS, DATATYPE)

                    LOGS_TYPE = [ "accuracy" ]
                    POSTING_MODELS = [ "SSDvgg16" ]
                    RUNTYPE = 'tfdo-accuracy'
                    postAll2AIBTdashboard(RUNTYPE, FRAMEWORK, FRONTEND, TARGET_DASHBOARD, LOGS_DIR, LOGS_TYPE, POSTING_MODELS, DATATYPE)
                }
                // for Q2 models posting both latency and throughput data
                if (RUN_Q2MODELS) {
                    RUNTYPE = 'tfdo-inference'
                    LOGS_TYPE = [ "latency", "throughput"]
                    POSTING_MODELS = ["inception_v4",  "inception_resnet_v2", "SqueezeNet", "YoloV2", "fastrcnn", "gnmt", "rfcn", "transformerLanguage", "transformerSpeech", "WaveNet", "wideDeep", "WaveNet_Magenta", "deepSpeech", "mobilenet_v1", "UNet",  "DRAW", "A3C", "DCGAN", "3DGAN", "inceptionv3", "resnet50"]
                    postAll2AIBTdashboard(RUNTYPE, FRAMEWORK, FRONTEND, TARGET_DASHBOARD, LOGS_DIR, LOGS_TYPE, POSTING_MODELS, DATATYPE)
                }

            } // stage

        } // if POST_TO_DASHBOARD

    } // finally
}


