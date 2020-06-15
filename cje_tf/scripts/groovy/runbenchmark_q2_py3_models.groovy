ERROR="0"
SINGLE_SOCKET='true'
FULL_VALIDATION=false

CJE_TF='cje-tf'
CJE_TF_COMMON_DIR='cje-tf/common'

GIT_CREDENTIAL = "lab_tfbot"
GIT_CREDENTIAL_LAB = "lab_tfbot"

http_proxy="http://proxy-us.intel.com:911"
https_proxy="https://proxy-us.intel.com:912"

// setting NODE_LABEL default to be nervana-skx10.fm.intel.com or get input from params
NODE_LABEL = 'nervana-skx10.fm.intel.com'
if ('NODE_LABEL' in params) {
    echo "NODE_LABEL in params"
    if (params.NODE_LABEL != '') {
        NODE_LABEL = params.NODE_LABEL
        echo NODE_LABEL
    }
}
echo "running on $NODE_LABEL"

// setting CJE_ALGO_BRANCH default to be master or get input from params
CJE_ALGO_BRANCH = '*/master'
if ('CJE_ALGO_BRANCH' in params) {
    echo "CJE_ALGO_BRANCH in params"
    if (params.CJE_ALGO_BRANCH != '') {
        CJE_ALGO_BRANCH = params.CJE_ALGO_BRANCH
        echo CJE_ALGO_BRANCH
    }
}
echo "CJE_ALGO_BRANCH is $CJE_ALGO_BRANCH"

// setting DATASET_LOCATION default to be /tf_dataset or get input from params
DATASET_LOCATION = '/tf_dataset'
if ('DATASET_LOCATION' in params) {
    echo "DATASET_LOCATION in params"
    if (params.DATASET_LOCATION != '') {
        DATASET_LOCATION = params.DATASET_LOCATION
        echo DATASET_LOCATION
    }
}
echo "dataset location is: $DATASET_LOCATION"

// set default DOCKER_IMAGE  value
// possible values: e.g. amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.8-py2-build
//                       amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.8-py3-build
//                       amr-registry.caas.intel.com/aipg-tf/qa:1.8.0-mkl-py2
DOCKER_IMAGE = 'amr-registry.caas.intel.com/aipg-tf/qa:nightly-master-avx2-devel-mkl-py3'
if ('DOCKER_IMAGE' in params) {
    echo "DOCKER_IMAGE in params"
   if (params.DOCKER_IMAGE != '') {
        DOCKER_IMAGE = params.DOCKER_IMAGE
        echo DOCKER_IMAGE
    }
}
echo "DOCKER_IMAGE: $DOCKER_IMAGE"

// check default value for POST_TO_DASHBOARD 
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
echo "TARGET_DASHBOARD:  $TARGET_DASHBOARD"

// set default value for MODELS to run
MODELS="DenseNet,3DUNet,MaskRCNN"
if ('MODELS' in params) {
    echo "MODELS in params"
    if (params.MODELS != '') {
        MODELS = params.MODELS
        echo MODELS
    }
}
echo "MODELS: $MODELS"

// set default value for MODES to run
MODES="inference"
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
SUMMARY_TITLE = 'private-tensorflow nightly benchmark test MKL-DNN q2 py3 summary'
if ('SUMMARY_TITLE' in params) {
    echo "SUMMARY_TITLE in params"
    if (params.SUMMARY_TITLE != '') {
        SUMMARY_TITLE = params.SUMMARY_TITLE
        echo SUMMARY_TITLE
    }
}
echo "SUMMARY_TITLE: $SUMMARY_TITLE"

// setting TENSORFLOW_BRANCH with some default value or get input from params
TENSORFLOW_BRANCH = 'master'
if ('TENSORFLOW_BRANCH' in params) {
    echo "TENSORFLOW_BRANCH in params"
    if (params.TENSORFLOW_BRANCH != '') {
        TENSORFLOW_BRANCH = params.TENSORFLOW_BRANCH
        echo TENSORFLOW_BRANCH
    }
}
echo "TENSORFLOW_BRANCH: $TENSORFLOW_BRANCH"

node( NODE_LABEL ) {
    try {

        stage('CleanUp') {

            sh '''#!/bin/bash -x
                cd $WORKSPACE
                sudo rm -rf *
                docker stop $(docker ps -a -q)
                echo Y | docker system prune -a
            '''
        }

        deleteDir()
 
        // pull the cje-tf
        dir(CJE_TF) {
            checkout scm
        }

        SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                returnStdout: true).trim()
        SUMMARYLOG = "${WORKSPACE}/summary_${SERVERNAME}.log"
        SUMMARYTXT = "${WORKSPACE}/summary_nightly.log"
        writeFile file: SUMMARYTXT, text: "Model,Mode,Server,Data_Type,Use_Case,Batch_Size,Result\n"

        stage('Checkout') {

            // DenseNet
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: "*/master"]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'tensorflow-DenseNet']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                           url          : 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-DenseNet.git']]])
            // shanghai oob models
            // 3DUNet
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: "*/master"]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'tensorflow-3DUNet']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                           url          : 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-3DUNet-deprecated.git']]])
            
            // MaskRCNN
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: "*/master"]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'tensorflow-MaskRCNN']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                           url          : 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-MaskRCNN-deprecated.git']]])
            
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

            workspace_volumn="${WORKSPACE}:/workspace"
            dataset_volume="${DATASET_LOCATION}:${DATASET_LOCATION}"

            docker.image("${DOCKER_IMAGE}").inside("--env \"http_proxy=${http_proxy}\" " +
                    "--env \"https_proxy=${https_proxy}\" " +
                    "--env DATASET_LOCATION=$DATASET_LOCATION " + 
                    "--env WORKSPACE=/workspace " + 
                    "--env TENSORFLOW_BRANCH=$TENSORFLOW_BRANCH " + 
                    "-v ${workspace_volumn} " + 
                    "-v ${dataset_volume} " +
                    " --privileged -u root:root") {

                withEnv([ "branch=$TENSORFLOW_BRANCH"]) {
                    sh '''
                        # common dependencies
                        if [ "${TENSORFLOW_BRANCH}" == 'master' ]; then
                            pip3 install --upgrade tf-estimator-nightly
                        fi
                        apt-get clean; apt-get update -y;
                        apt-get install numactl -y
                        # 3DUnet
                        pip3 install nibabel
                        pip3 install tables
                        pip3 install nilearn
                        pip3 install keras 
                        # MaskRCNN
                        pip3 install cython
                        pip3 install pycocotools
                        pip3 install scikit-image
                        pip3 install pillow
                        pip3 install scipy==1.1.0
                    '''
                }

                models=MODELS.split(',')
                modes=MODES.split(',')
    
                sh '''#!/bin/bash -x
                if [ -d "/private-tensorflow" ]; then
                    python ${WORKSPACE}/cje-tf/scripts/get_tf_sourceinfo.py --tensorflow_dir=/private-tensorflow --workspace_dir=${WORKSPACE}
                elif [ -d "/tensorflow" ]; then
                    python ${WORKSPACE}/cje-tf/scripts/get_tf_sourceinfo.py --tensorflow_dir=/tensorflow --workspace_dir=${WORKSPACE}
                fi
                '''

                models.each { model ->
                    echo "model is ${model}"
                    modes.each { mode ->
                        withEnv([ "model=$model", "mode=$mode", "single_socket=$SINGLE_SOCKET"]) {
                            sh '''
                                $WORKSPACE/cje-tf/scripts/run_benchmark_q2_models_py3.sh --model=${model} --mode=${mode} --single_socket=${single_socket}
                                if [ $? -eq 0 ] ; then
                                    echo "running model ${model} success"
                                    RESULT="SUCCESS"
                                else
                                    echo "running model ${model} fail"
                                    RESULT="FAILURE"
                                    ERROR="1"
                                fi
                            '''
                        }
                    }
                }
            }
        }

        stage('Collect Logs') {

            // Prepare logs
            def prepareLog = load("${CJE_TF_COMMON_DIR}/prepareLog.groovy")
            prepareLog("", "", "", SUMMARYLOG, SUMMARY_TITLE)
    
            echo "models: $models"

            SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                             returnStdout: true).trim()

            models=MODELS.split(',')
            modes=MODES.split(',')

            echo "in Collect Logs stage: summarylog is $SUMMARYLOG"
            models.each { model ->
                echo "model is ${model}"
                modes.each { mode ->
                    withEnv([ "model=$model", "mode=$mode", "single_socket=$SINGLE_SOCKET", "fullvalidation=${FULL_VALIDATION}", "summaryLog=$SUMMARYLOG"]) {
                        sh '''
                            echo "before calling collect_logs_q2_models_py3.sh  summaryLog is: $summaryLog"
                            $WORKSPACE/cje-tf/scripts/collect_logs_q2_models_py3.sh --model=${model} --mode=${mode}  --fullvalidation=${fullvalidation}  --single_socket=${single_socket}
                                
                            if [ $? -eq 0 ] ; then
                                echo "running model ${model} ${mode} success"
                                RESULT="SUCCESS"
                            else
                                echo "running model ${model} ${mode} fail"
                                RESULT="FAILURE"
                                ERROR="1"
                            fi
                        '''
                    }
                }
            }
        }
    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    } finally {
        // Success or failure, always send notifications

        echo "finally SUMMARYLOG is $SUMMARYLOG"
        SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                returnStdout: true).trim()
        SUMMARYLOG = "${WORKSPACE}/summary_${SERVERNAME}.log"

        stage('Archive Artifacts ') {
            dir("$WORKSPACE") {

                archiveArtifacts artifacts: '*.log', excludes: null
                fingerprint: true

                echo "in Archive Artifacts SUMMARYLOG is $SUMMARYLOG"
                //error = sh(script: 'grep CHECK_LOG_FOR_ERROR $SUMMARYLOG', returnStdout: true).trim() 
                //def error = readFile SUMMARYLOG
                //if ( error =~ /CHECK_LOG_FOR_ERROR/ ) {
                //    echo "setting build status to fail"
                //    currentBuild.result = "FAILED"
                //}
            }
        }

        
        // only post to dashboard if POST_TO_DASHBOARD is set to true
        if (POST_TO_DASHBOARD) {

            def postAll2AIBTdashboard = load("${CJE_TF_COMMON_DIR}/postAll2AIBTdashboard.groovy")

            LOGS_DIR = "${WORKSPACE}"
            LOGS_TYPE = [ "latency", "throughput"]
            FRAMEWORK = 'tensorflow'
            FRONTEND = 'tensorflow'
            RUNTYPE = 'tfdo-inference'
            DATATYPE = 'None'
            MODELS_LIST = ["DenseNet", "3DUNet", "MaskRCNN"]
            postAll2AIBTdashboard(RUNTYPE, FRAMEWORK, FRONTEND, TARGET_DASHBOARD, LOGS_DIR, LOGS_TYPE, MODELS_LIST, DATATYPE)

        }
    } // finally
}
