//models=["resnet50", "inception3",  "vgg16", "ds2", "SSDvgg16", "mnist", "resnet32cifar10", "cifar10", "dcgan"]
//modes=["training", "inference"]
ERROR="0"
SINGLE_SOCKET=true
FULL_VALIDATION=false
CJE_TF='cje-tf'
CJE_TF_COMMON_DIR = "$CJE_TF/common"

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

// setting SLACK_CHANNEL with some default value or get input from params
SLACK_CHANNEL = '#test-jenkin-notify'
if ('SLACK_CHANNEL' in params) {
    echo "SLACK_CHANNEL in params"
    if (params.SLACK_CHANNEL != '') {
        SLACK_CHANNEL = params.SLACK_CHANNEL
        echo SLACK_CHANNEL
    }
}
echo "Slack channel is $SLACK_CHANNEL"

// setting CJE_ALGO branch
CJE_ALGO_BRANCH = '*/master'
if ('CJE_ALGO_BRANCH' in params) {
    echo "CJE_ALGO_BRANCH in params"
    if (params.CJE_ALGO_BRANCH != '') {
        CJE_ALGO_BRANCH = params.CJE_ALGO_BRANCH
        echo CJE_ALGO_BRANCH
    }
}

// setting NODE_LABEL default 
NODE_LABEL = 'aipg-ra-skx51.ra.intel.com'
if ('NODE_LABEL' in params) {
    echo "NODE_LABEL in params"
    if (params.NODE_LABEL != '') {
        NODE_LABEL = params.NODE_LABEL
        echo NODE_LABEL
    }
}

// setting DATASET_LOCATION default
DATASET_LOCATION = '/tf_dataset'
if ('DATASET_LOCATION' in params) {
    echo "DATASET_LOCATION in params"
    if (params.DATASET_LOCATION != '') {
        DATASET_LOCATION = params.DATASET_LOCATION
        echo DATASET_LOCATION
    }
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

node(NODE_LABEL) {

try {
        
    deleteDir()
 
    // pull the cje-tf
    dir(CJE_TF) {
        checkout scm
    }

    SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                              returnStdout: true).trim()
    SUMMARYLOG = "${WORKSPACE}/summary_${SERVERNAME}.log"

    // slack notification
    def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
    notifyBuild(SLACK_CHANNEL, 'STARTED', '')

    stage('checkout') {
        String GIT_CREDENTIAL = "lab_tfbot"
        String GIT_CREDENTIAL_LAB = "lab_tfbot"

        String GIT_URL = "https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git"
        String GIT_NAME = "private-tensorflow"

        checkout([$class: 'GitSCM', 
            branches: [[name: PRIVATE_TENSORFLOW_BRANCH]], 
            browser: [$class: 'AssemblaWeb', repoUrl: ''], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [[$class: 'RelativeTargetDirectory', 
                relativeTargetDir: 'private-tensorflow']], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB", 
                url: "$GIT_URL"]]])

        checkout([$class: 'GitSCM', 
            branches: [[name: PRIVATE_TENSORFLOW_BENCHMARKS_BRANCH]], 
            browser: [$class: 'AssemblaWeb', repoUrl: ''], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [[$class: 'RelativeTargetDirectory', 
                relativeTargetDir: 'private-tensorflow-benchmarks']], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                url: 'https://gitlab.devtools.intel.com/TensorFlow/Shared/private-tensorflow-benchmarks.git']]])

        // model ds2
        checkout([$class: 'GitSCM', 
            branches: [[name: '*/master']], 
            browser: [$class: 'AssemblaWeb', repoUrl: ''], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [[$class: 'RelativeTargetDirectory', 
                relativeTargetDir: 'deepSpeech2']], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL", 
                url: 'https://github.com/yao-matrix/deepSpeech2.git']]])
        
        // model SSDvgg16 for inference
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

        // model nmt
        checkout([$class: 'GitSCM', 
            branches: [[name: '*/master']], 
            browser: [$class: 'AssemblaWeb', repoUrl: ''], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [[$class: 'RelativeTargetDirectory', 
                relativeTargetDir: 'nmt']], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL", 
                url: 'https://github.com/tensorflow/nmt.git']]])

        // model nmt & gnmt
        checkout([$class: 'GitSCM', 
            branches: [[name: '*/master']], 
            browser: [$class: 'AssemblaWeb', repoUrl: ''], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [[$class: 'RelativeTargetDirectory', 
                relativeTargetDir: 'tensorflow-NMT']], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL", 
                url: 'https://github.com/NervanaSystems/tensorflow-NMT.git']]])

        // models
        checkout([$class: 'GitSCM', 
            branches: [[name: MODELS_COMMIT]], 
            browser: [$class: 'AssemblaWeb', repoUrl: ''], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [[$class: 'RelativeTargetDirectory', 
                relativeTargetDir: 'models']], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL", 
                url: 'https://github.com/tensorflow/models.git']]])

        checkout([$class: 'GitSCM', 
            branches: [[name: "*/master"]], 
            browser: [$class: 'AssemblaWeb', repoUrl: ''], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [[$class: 'RelativeTargetDirectory', 
                relativeTargetDir: 'dcgan-tf-benchmark']], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL", 
                url: 'https://github.com/MustafaMustafa/dcgan-tf-benchmark']]])

        // cje-algo
        checkout([$class: 'GitSCM', 
            branches: [[name: CJE_ALGO_BRANCH]], 
            browser: [$class: 'AssemblaWeb', repoUrl: ''], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [[$class: 'RelativeTargetDirectory', 
                relativeTargetDir: 'cje-algo']], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL", 
                url: 'https://github.intel.com/AIPG/cje-algo.git']]])

    }

    stage('Install dependencies') {
        sh '''#!/bin/bash -x
        sudo sh -c 'sync; echo 1 > /proc/sys/vm/compact_memory; echo 1 > /proc/sys/vm/drop_caches' || true
        export PATH="/nfs/fm/disks/aipg_tensorflow_tools/gcc6.3/bin/:/nfs/fm/disks/aipg_tensorflow_tools/bazel/bin:$PATH"
        virtualenv-3 -p /usr/bin/python $WORKSPACE/venv
        source $WORKSPACE/venv/bin/activate
        sudo touch /usr/include/stropts.h
        sudo yum install tkinter
        pip install --upgrade autograd backports.weakref bleach enum enum34 funcsigs future futures grpc gevent grpcio html5lib Markdown mock msgpack-python numpy pbr pip portpicker protobuf scikit-learn scipy setuptools six tensorflow-tensorboard Werkzeug wheel h5py matplotlib opencv-python keras_applications keras_preprocessing
        '''
    }

    stage('Configure') {
        dir('private-tensorflow') {
            sh '''#!/bin/bash -x
            export PATH="/nfs/fm/disks/aipg_tensorflow_tools/gcc6.3/bin/:/nfs/fm/disks/aipg_tensorflow_tools/bazel/bin:$PATH"
            source $WORKSPACE/venv/bin/activate
            ./configure <<EOF
    
















		    EOF
            ''' 
        }
    }

    stage('Build') {
        dir('private-tensorflow') {
            SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                              returnStdout: true).trim()
            SUMMARYLOG = "${WORKSPACE}/summary_${SERVERNAME}.log"
            withEnv(["SUMMARYLOG=$SUMMARYLOG"]) {
            sh '''#!/bin/bash -x
            ERROR="0"
            LOGFILE='../bazel_build.log'
            export PATH="/nfs/fm/disks/aipg_tensorflow_tools/gcc6.3/bin/:/nfs/fm/disks/aipg_tensorflow_tools/bazel/bin:$PATH"
            export LD_LIBRARY_PATH="/nfs/fm/disks/aipg_tensorflow_tools/gcc6.3/lib64:$LD_LIBRARY_PATH"
            source $WORKSPACE/venv/bin/activate
            /nfs/fm/disks/aipg_tensorflow_tools/bazel/bin/bazel --output_user_root=$WORKSPACE/build build --config=mkl --copt="-mavx2" --copt="-mfma" --copt="-march=broadwell" --copt="-O3" -s -c opt //tensorflow/tools/pip_package:build_pip_package >& ${LOGFILE}

            if [ "$(grep 'ERROR: ' ${LOGFILE} | wc -l)" = "0" ] ; then
                RESULT="SUCCESS"
            else
                RESULT="FAILURE"
                ERROR="1"
            fi
            grep "ERROR: " ${LOGFILE}
            tail -5 ${LOGFILE}
            echo "RESULT: ${RESULT}"
            if [ $ERROR = "1" ]; then 
              grep "ERROR: " ${LOGFILE}  >> ${SUMMARYLOG}
              tail -5 ${LOGFILE}  >> ${SUMMARYLOG}
              exit 1
            fi

            '''
            }
        }
    }
    
    
    stage('Build pip package') {
        dir('private-tensorflow') {
            sh '''#!/bin/bash -x
            export PATH="/nfs/fm/disks/aipg_tensorflow_tools/gcc6.3/bin/:/nfs/fm/disks/aipg_tensorflow_tools/bazel/bin:$PATH"
            export LD_LIBRARY_PATH="/nfs/fm/disks/aipg_tensorflow_tools/gcc6.3/lib64:$LD_LIBRARY_PATH"
            source $WORKSPACE/venv/bin/activate
            bazel-bin/tensorflow/tools/pip_package/build_pip_package $WORKSPACE/build
            '''
       }
       stash allowEmpty: true, includes: "build/*.whl", name: "wheelfile"
    }
    
    stage('Wheel install') {
        dir('private-tensorflow') {
            sh '''#!/bin/bash -x
            export PATH="/nfs/fm/disks/aipg_tensorflow_tools/gcc6.3/bin/:/nfs/fm/disks/aipg_tensorflow_tools/bazel/bin:$PATH"
            export LD_LIBRARY_PATH="/nfs/fm/disks/aipg_tensorflow_tools/gcc6.3/lib64:$LD_LIBRARY_PATH"
            source $WORKSPACE/venv/bin/activate
            pip install --upgrade $WORKSPACE/build/*.whl
            '''
        }
    }
    
    stage('Run benchmark') {
        notifyBuild(SLACK_CHANNEL, 'IN PROGRESS', '... Building COMPLETE and SUCCESSFUL, running benchmark tests ...')

        sh '''#!/bin/bash -x
        source $WORKSPACE/venv/bin/activate
        export DATASET_LOCATION=${DATASET_LOCATION}
        $WORKSPACE/cje-tf/scripts/fixup_ds2.sh
        # 02/20/2018 use a different repo tensorflow-SSD
        $WORKSPACE/cje-tf/scripts/fixup_tensorflowSSD.sh
        #$WORKSPACE/cje-tf/scripts/fixup_mnist.sh
	$WORKSPACE/cje-tf/scripts/fixup_cifar10.sh
	$WORKSPACE/cje-tf/scripts/fixup_resnet32_cifar10.sh
	$WORKSPACE/cje-tf/scripts/fixup_gnmt.sh

	'''
          
        models=MODELS.split(',')
        modes=MODES.split(',')

        models.each { model ->
            echo "model is ${model}"
            modes.each { mode ->
                withEnv(["model=$model","mode=$mode","ERROR=$ERROR", "single_socket=${SINGLE_SOCKET}"]) {
                    sh '''#!/bin/bash -x
                    source $WORKSPACE/venv/bin/activate
                    $WORKSPACE/cje-tf/scripts/run_benchmark.sh --model=${model} --mode=${mode} --single_socket=${single_socket}
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
    
    stage('Collect logs') {
        dir('private-tensorflow') {
            gitCommit = sh (
              script: 'git log --pretty=format:"%H" -n 1',
              returnStdout: true
            ).trim()
        }
        date = sh (
            script: 'date +"%Y-%m-%d %H:%M"',
            returnStdout: true
        ).trim()
        model_name = sh (script: 'lscpu | grep "Model name:"', returnStdout: true).trim()
        os_version = sh (script: "cat /etc/os-release | grep PRETTY_NAME | sed 's/PRETTY_NAME=/OS Version:      /'", returnStdout: true).trim()
        SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                              returnStdout: true).trim()
        echo SERVERNAME
        SUMMARYLOG = "${WORKSPACE}/summary_${SERVERNAME}.log"
        
        echo "NODE_NAME = ${env.NODE_NAME}"
        withEnv(["date=$date","gitCommit=$gitCommit", "model_name=$model_name","os_version=$os_version", "nodeName=${env.NODE_NAME}", "repoBranch=$PRIVATE_TENSORFLOW_BRANCH", "summaryLog=${SUMMARYLOG}"]) {
            sh '''#!/bin/bash -x
            echo "*************************************************************************" > ${summaryLog}
            echo "Tensorflow nightly benchmark test MKL-DNN summary ${date}" >> ${summaryLog}
            echo "Repository: private-tensorflow" >> ${summaryLog}
            echo "Branch: ${repoBranch}" >> ${summaryLog}
            echo "Running on: ${nodeName}" >> ${summaryLog}
            echo "Git Revision: ${gitCommit}" >> ${summaryLog}
            echo "${model_name}" >> ${summaryLog}
            echo "${os_version}" >> ${summaryLog}
            echo "*********************************************************************\n" >> ${summaryLog}
            echo "\n" >> ${summaryLog}
            '''
        }
        
        models=MODELS.split(',')
        modes=MODES.split(',')

        models.each { model ->
            echo "model is ${model}"
            modes.each { mode ->
                withEnv(["model=$model","mode=$mode","ERROR=$ERROR", "single_socket=${SINGLE_SOCKET}", "fullvalidation=${FULL_VALIDATION}"]) {
                    sh '''#!/bin/bash -x
                    $WORKSPACE/cje-tf/scripts/collect_logs.sh --model=${model} --mode=${mode}  --fullvalidation=${fullvalidation}  --single_socket=${single_socket}
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
        echo "done running benchmark ${ERROR} ...end"
        if ( ERROR.equals("1") )
             currentBuild.result = "UNSTABLE"
        stash allowEmpty: true, includes: "*.log", name: "logfile"
     }
    
} catch (e) {
    // If there was an exception thrown, the build failed
    currentBuild.result = "FAILED"
    throw e
} finally {
    // Success or failure, always send notifications

    SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                          returnStdout: true).trim()
    echo SERVERNAME
    SUMMARYLOG = "${WORKSPACE}/summary_${SERVERNAME}.log"
    withEnv(["SUMMARYLOG=$SUMMARYLOG"]) {
        echo SUMMARYLOG
        def msg = readFile SUMMARYLOG
        def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
        notifyBuild(SLACK_CHANNEL, currentBuild.result, msg)
    }

    // Success or failure, always do artifacts
    stage('Archive Artifacts / Test Results') {
        dir("$WORKSPACE" + "/publish") {
            unstash "logfile"
            unstash "wheelfile"

            archiveArtifacts artifacts: '*.log,build/*.whl', excludes: null
                                fingerprint: true
                                
            def server = Artifactory.server 'ubit-artifactory-or'
            def uploadSpec = """{
              "files": [
               {
                   "pattern": "*",
                   "target": "aipg-local/aipg-tf/${env.JOB_NAME}/${env.BUILD_NUMBER}/"
               }
               ]
            }"""
            def buildInfo = server.upload(uploadSpec)
            server.publishBuildInfo(buildInfo)
                               
            SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                          returnStdout: true).trim()
            echo SERVERNAME
            SUMMARYLOG = "${WORKSPACE}/summary_${SERVERNAME}.log"

        }
    }

    if (POST_TO_DASHBOARD) {

            stage('Post to AVUS dashboard') {

                def postAll2AIBTdashboard = load("${CJE_TF_COMMON_DIR}/postAll2AIBTdashboard.groovy")

                LOGS_DIR = "${WORKSPACE}"
                FRAMEWORK = 'tensorflow'
                FRONTEND = 'tensorflow'
                DATATYPE = 'None'
                
                // only posting vgg16 & SSDvgg16 data for throughput and latency
                RUNTYPE = 'tfdo-inference'
                LOGS_TYPE = [ "throughput", "latency" ]
                POSTING_MODELS = [ "vgg16", "SSDvgg16"]
                postAll2AIBTdashboard(RUNTYPE, FRAMEWORK, FRONTEND, TARGET_DASHBOARD, LOGS_DIR, LOGS_TYPE, POSTING_MODELS, DATATYPE)


                // only posting SSDvgg16 data for accuracy
                RUNTYPE = 'tfdo-accuracy'
                LOGS_TYPE = [ "accuracy" ]
                POSTING_MODELS = [ "SSDvgg16" ]
                postAll2AIBTdashboard(RUNTYPE, FRAMEWORK, FRONTEND, TARGET_DASHBOARD, LOGS_DIR, LOGS_TYPE, POSTING_MODELS, DATATYPE)

            } // stage
    } // if POST_TO_DASHBOARD

  } // finally

} // node

