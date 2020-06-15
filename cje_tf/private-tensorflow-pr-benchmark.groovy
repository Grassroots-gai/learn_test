PRIVATE_TENSORFLOW_REPO_BRANCH="master"
TENSORFLOW_SSD_BRANCH="NCHW"
//CJE_TF_BRANCH="karen/test-dataset"
//models=["resnet50", "inception3",  "vgg16", "ds2", "SSDvgg16", "mnist", "resnet32cifar10", "cifar10", "dcgan"]
//TFDOQA-1122 reduce the number of tests running
models=["resnet50", "inception3",  "vgg16", "ds2"]
modes=["training", "inference"]
SINGLE_SOCKET=true
FULL_VALIDATION=false
MODELS_REFSPEC = "3f78f4cfd21c786c62bf321c07830071027ebb5e"

TENSORFLOW_SSD_BRANCH_INFERENCE = "andrew/fp32-SSDvgg16"
TENSORFLOW_SSD_BRANCH_TRAINING = "NCHW"

//q2_models=[ "fastrcnn", "inceptionv3", "inception_v4",  "inception_resnet_v2", "mobilenet_v1", "resnet50", "SqueezeNet", "YoloV2", "wideDeep"]
//TFDOQA-1122 reduce the number of tests running
q2_models=[ "inceptionv3", "mobilenet_v1", "resnet50", "SqueezeNet", "YoloV2", "wideDeep"]

TF_SLIM_MODEL='tensorflow-slim-models'
TF_SLIM_MODEL_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-slim-models.git'

// fastrcnn
TF_FASTRCNN='tensorflow-models'
TF_FASTRCNN_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-models.git'
TF_FASTRCNN_BRANCH='FastRCNN-Resnet50'
COCOAPI_URL='https://github.com/cocodataset/cocoapi.git'
COCOAPI='cocoapi'

// inceptionv3, resnet50
TENSORFLOW_INFERENCE='tensorflow-inference'
TENSORFLOW_INFERENCE_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-inference-deprecated.git'
TENSORFLOW_INFERENCE_BRANCH='master'

// SqueezeNet
SQUEEZE_NET_MODEL='tensorflow-SqueezeNet'
SQEEZE_NET_MODEL_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-SqueezeNet.git'

// YoloV2
YOLO_V2_MODEL='tensorflow-YoloV2'
YOLO_V2_MODEL_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-YoloV2.git'

// wideDeep
TENSORFLOW_WIDEDEEP='wideDeep'
TENSORFLOW_WIDEDEEP_URL="https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-models.git"
TENSORFLOW_WIDEDEEP_BRANCH='wei-wide-deep'

// set default value for NODE_LABEL
NODE_LABEL = 'aipg-fm-skx-10.fm.intel.com'
if ('NODE_LABEL' in params) {
    echo "NODE_LABEL in params"
    if (params.NODE_LABEL != '') {
        NODE_LABEL = params.NODE_LABEL
        echo NODE_LABEL
    }
}
echo "NODE_LABEL: $NODE_LABEL"

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

def notifyBuild(String buildStatus = 'STARTED', String msg) {

    // build status of null means successful
    buildStatus =  buildStatus ?: 'SUCCESSFUL'
    // msg of null means no additional messages to report
    msg = msg ?: ''
    String summary = "Job <${env.BUILD_URL}|${env.JOB_NAME} #${env.BUILD_NUMBER}> on ${env.NODE_NAME} : ${buildStatus} \n ${msg}"
    Integer SLACK_RETRY_TIMES = 2
    String SLACK_CHANNEL = '#test-jenkin-notify'
    Map SLACK_COLOR_STRINGS = [
            'SUCCESS': 'good',
            'UNSTABLE': 'warning',
            'FAILURE': 'danger'
    ]

    // Send notifications
    retry(SLACK_RETRY_TIMES) {
        slackSend channel: SLACK_CHANNEL, message: summary, color: SLACK_COLOR_STRINGS[currentBuild.currentResult]
    }

}

//NODE_LABEL = "aipg-fm-skx-10.fm.intel.com"
//def labels = ['aipg-ra-skx-51.ra.intel.com']
//def labels = ['nervana-skx48.fm.intel.com']

def builders =[:]
def labels=NODE_LABEL.split(',')

for (x in labels) {
    def label = x // Need to bind the label variable before the closure - can't do 'for (label in labels)'

    // Create a map to pass in to the 'parallel' step so we can fire all the builds at once
    builders[label] = {
        node(label) {

            if (label == "nervana-bdw01.fm.intel.com") {
                RUN_TYPE="mklml"
                echo RUN_TYPE
            } else {
                RUN_TYPE="mkldnn"
                echo RUN_TYPE
            }

            // build steps that should happen on all nodes go here
            try {
                echo RUN_TYPE

                deleteDir()

                //cje-tf
                dir('cje-tf') {
                    checkout scm
                }
                notifyBuild('STARTED','')
                SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                                      returnStdout: true).trim()
                SHORTNAME = sh (script:"echo $SERVERNAME | cut -f2 -d-",
                                      returnStdout: true).trim()
                SUMMARYLOG = "${WORKSPACE}/summary_${SERVERNAME}.log"

                stage("Clone repository $label") {
                    String GIT_CREDENTIAL = "lab_tfbot"
                    String GIT_CREDENTIAL_LAB = "lab_tfbot"
                    String GIT_URL = "https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow"
                    String GIT_NAME = "private-tensorflow"
                    String GIT_BRANCH = "origin-pull/pull/${ghprbPullId}/merge"
                    String GIT_REFSPEC = "+refs/pull/${ghprbPullId}/merge:refs/remotes/origin-pull/pull/${ghprbPullId}/merge"

                    checkout([$class: 'GitSCM',
                              branches: [[name: '*/master']],
                              browser: [$class: 'AssemblaWeb', repoUrl: ''],
                              doGenerateSubmoduleConfigurations: false,
                              extensions: [[$class: 'RelativeTargetDirectory',
                                            relativeTargetDir: 'private-tensorflow-benchmarks']],
                              submoduleCfg: [],
                              userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                   url: 'https://gitlab.devtools.intel.com/TensorFlow/Shared/private-tensorflow-benchmarks.git']]])
                    checkout([
                            $class: 'GitSCM', branches: [[name: "$GIT_BRANCH"]],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [[
                                                 $class: 'RelativeTargetDirectory',
                                                 relativeTargetDir: 'private-tensorflow' ]],
                            submoduleCfg: [],
                            userRemoteConfigs: [ [credentialsId: "$GIT_CREDENTIAL_LAB", name: "$GIT_NAME",
                                                  refspec: "$GIT_REFSPEC", url: "$GIT_URL"]]])

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

                    checkout([$class: 'GitSCM',
                              branches: [[name: "*/master"]],
                              browser: [$class: 'AssemblaWeb', repoUrl: ''],
                              doGenerateSubmoduleConfigurations: false,
                              extensions: [[$class: 'RelativeTargetDirectory',
                                            relativeTargetDir: 'dcgan-tf-benchmark']],
                              submoduleCfg: [],
                              userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                                   url: 'https://github.com/MustafaMustafa/dcgan-tf-benchmark']]])

                    //Q2 models
                    // inception_v3, resnet50
                    checkout([$class: 'GitSCM',
                              branches: [[name: "${TENSORFLOW_INFERENCE_BRANCH}"]],
                              browser: [$class: 'AssemblaWeb', repoUrl: ''],
                              doGenerateSubmoduleConfigurations: false,
                              extensions: [[$class: 'RelativeTargetDirectory',
                                            relativeTargetDir: "${TENSORFLOW_INFERENCE}"]],
                              submoduleCfg: [],
                              userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                                   url: "${TENSORFLOW_INFERENCE_URL}"]]])

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

                    //Slim model
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

                    // wideDeep
                    checkout([$class: 'GitSCM',
                              branches: [[name: TENSORFLOW_WIDEDEEP_BRANCH]],
                              browser: [$class: 'AssemblaWeb', repoUrl: ''],
                              doGenerateSubmoduleConfigurations: false,
                              extensions: [[$class: 'RelativeTargetDirectory',
                                            relativeTargetDir: "${TENSORFLOW_WIDEDEEP}"]],
                              submoduleCfg: [],
                              userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                   url: "${TENSORFLOW_WIDEDEEP_URL}"]]])

                }

                stage("Install dependencies $label") {
                    sh '''
                    #!/bin/bash -x
                    sudo apt-get install -y python-subprocess32 || sudo yum install -y python2-subprocess32
                    sudo sh -c 'sync; echo 1 > /proc/sys/vm/compact_memory; echo 1 > /proc/sys/vm/drop_caches' || true
                    export PATH="/nfs/site/disks/aipg_tensorflow_tools/gcc6.3/bin/:/nfs/site/disks/aipg_tensorflow_tools/bazel/bin:$PATH"
                    virtualenv -p /usr/bin/python $WORKSPACE/venv
                    source $WORKSPACE/venv/bin/activate
                    sudo touch /usr/include/stropts.h
                    pip install --upgrade autograd backports.weakref bleach==1.5.0 enum enum34 funcsigs future futures grpc gevent grpcio html5lib Markdown mock msgpack-python numpy pbr pip portpicker protobuf scikit-learn==0.18.0 scipy setuptools six tensorflow-tensorboard Werkzeug wheel h5py matplotlib opencv-python keras_applications keras_preprocessing Cython pillow lxml jupyter librosa==0.6.1 sympy requests gym google-api-python-client oauth2client tf-estimator-nightly
                    '''
                }

                stage("Configure $label") {
                    dir('private-tensorflow') {
                        sh '''#!/bin/bash -x
                        export PATH="/nfs/site/disks/aipg_tensorflow_tools/gcc6.3/bin/:/nfs/site/disks/aipg_tensorflow_tools/bazel/bin:$PATH"
                        source $WORKSPACE/venv/bin/activate
                        yes "" | python configure.py
                        '''
                    }
                }

                stage("Build on $label") {
                    dir('private-tensorflow') {
                        gitCommit = sh (
                                script: 'git log --pretty=format:"%H" -n 1',
                                returnStdout: true
                        ).trim()
						echo gitCommit
                    }
                    date = sh (
                            script: 'date +"%Y-%m-%d %H:%M"',
                            returnStdout: true
                    ).trim()
                    model_name = sh (script: 'lscpu | grep "Model name:"', returnStdout: true).trim()
                    os_version = sh (script: "cat /etc/os-release | grep PRETTY_NAME | sed 's/PRETTY_NAME=/OS Version:      /'", returnStdout: true).trim()

                    if (RUN_TYPE == "mklml") {
                        echo "mklml"
                        summaryTitle="Tensorflow MKL-ML mini convergene test summary"
                    }
                    else {
                        echo "mkldnn"
                        summaryTitle="Tensorflow MKL-DNN mini convergence test summary"
                    }
                    echo "NODE_NAME = ${env.NODE_NAME}"
                    //echo "Github Pull ID = ${ghprbPullId}"

                    echo "****Current Shell ****= ${SHELL}"
                    withEnv(["date=$date","summaryLog=$SUMMARYLOG","summaryTitle=$summaryTitle","gitCommit=$gitCommit", "model_name=$model_name","os_version=$os_version", "nodeName=${env.NODE_NAME}", "repoBranch=${PRIVATE_TENSORFLOW_REPO_BRANCH}", "label=${label}", "runType=${RUN_TYPE}"]) {
                        sh '''#!/bin/bash -x
                        myhost=`echo $label | awk -F'.' '{print $1}'`
                        //LOGFILE="./summary.log"
                        echo "summaryLog = ${summaryLog}"
			echo "*************************************************************************" > ${summaryLog}
                        echo "${summaryTitle} ${date}" >> ${summaryLog}
                        echo "Repository: private-tensorflow" >> ${summaryLog}
                        echo "Branch: ${repoBranch}" >> ${summaryLog}
                        echo "Running on: ${nodeName}" >> ${summaryLog}
                        echo "Git Revision: ${gitCommit}" >> ${summaryLog}
                        //echo "Git Pull ID: ${gitPullID}" >> ${summaryLog}
                        echo "${model_name}" >> ${summaryLog}
                        echo "${os_version}" >> ${summaryLog}
                        echo "*********************************************************************\n" >> ${summaryLog}
                        echo "\n" >> ${summaryLog}
                        '''
                    }

                    dir('private-tensorflow') {
                        withEnv(["label=$label", "runType=${RUN_TYPE}" ]) {
                            sh '''#!/bin/bash -x
                            myhost=`echo $label | awk -F'.' '{print $1}'`
                            LOGFILE="../bazel_build_${myhost}.log"
                            echo $LOGFILE
                            export PATH="/nfs/site/disks/aipg_tensorflow_tools/gcc6.3/bin/:/nfs/site/disks/aipg_tensorflow_tools/bazel/bin:$PATH"
                            export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:/nfs/site/disks/aipg_tensorflow_tools/gcc6.3/lib64:/usr/include/x86_64-linux-gnu"
                            if [ ${runType} = "mklml" ]; then
                                bazel --output_user_root=$WORKSPACE/build build --config=mkl --copt="-DINTEL_MKL_ML" --copt="-mavx2" --copt="-mfma" --copt="-march=broadwell" --copt="-O3" --copt=-L/nfs/site/disks/aipg_tensorflow_tools/gcc6.3/lib64/ -s -c opt //tensorflow/tools/pip_package:build_pip_package >& ${LOGFILE}
                            else
                                bazel --output_user_root=$WORKSPACE/build build --config=mkl --copt="-mavx2" --copt="-mfma" --copt="-march=broadwell" --copt="-O3" --copt=-L/opt/tensorflow/gcc6.3/lib64 -s -c opt //tensorflow/tools/pip_package:build_pip_package >& ${LOGFILE}
                            fi

                            if [ "$(grep 'ERROR: ' ${LOGFILE} | wc -l)" = "0" ] ; then
                                RESULT="SUCCESS"
                                ERROR="0"
                            else
                                RESULT="FAILURE"
                                ERROR="1"
                            fi
                            grep "ERROR: " ${LOGFILE}
                            tail -5 ${LOGFILE}
                            echo "RESULT: build is ${RESULT}"
                            if [ $ERROR = "1" ]; then
                                grep "ERROR: " ${LOGFILE}  >> ../summary.log
                                tail -5 ${LOGFILE}  >> ../summary.log
                                exit 1
                            fi
                            bazel-bin/tensorflow/tools/pip_package/build_pip_package $WORKSPACE/build
                            '''
                        }
                    }
                }

                stage("Wheel install $label") {
                    dir('private-tensorflow') {
                        withEnv(["label=$label"]) {
                            sh '''#!/bin/bash -x
                        export PATH="/nfs/site/disks/aipg_tensorflow_tools/gcc6.3/bin/:/nfs/site/disks/aipg_tensorflow_tools/bazel/bin:$PATH"
                        export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:/nfs/site/disks/aipg_tensorflow_tools/gcc6.3/lib64:/usr/include/x86_64-linux-gnu"
                        source $WORKSPACE/venv/bin/activate
                        pip install --upgrade $WORKSPACE/build/*.whl
                        '''
                        }
                    }
                }

                stage("Run benchmark test $label") {
                    sh '''#!/bin/bash -x
                    $WORKSPACE/cje-tf/scripts/fixup_ds2.sh
                    $WORKSPACE/cje-tf/scripts/fixup_tensorflowSSD.sh
                    #$WORKSPACE/cje-tf/scripts/fixup_mnist.sh
	            $WORKSPACE/cje-tf/scripts/fixup_cifar10.sh
	            $WORKSPACE/cje-tf/scripts/fixup_resnet32_cifar10.sh
                    $WORKSPACE/cje-tf/scripts/fixup_fastrcnn.sh
                    '''
                    notifyBuild('IN PROGRESS', '...Building COMPLETE and SUCCESSFUL, running mini convergence tests ...')

                    for (model in models) {
                        for (mode in modes) {
                            withEnv(["label=$label", "runType=$RUN_TYPE", "model=${model}", "mode=${mode}", "single_socket=${SINGLE_SOCKET}"]) {
                                sh returnStatus: true, script: '''#!/bin/bash -x
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

                    for (q2_model in q2_models) {
                        for (mode in modes) {
                            withEnv(["mdl=$q2_model","mode=$mode", "single_socket=${SINGLE_SOCKET}"]) {
                                sh '''#!/bin/bash -x
                                source $WORKSPACE/venv/bin/activate
                                $WORKSPACE/cje-tf/scripts/run_benchmark_q2_models.sh --model=${mdl} --mode=${mode} --single_socket=${single_socket}
                                if [ $? -eq 0 ] ; then
                                    echo "running model ${mdl} success"
                                    RESULT="SUCCESS"
                                else
                                    echo "running model ${mdl} fail"
                                    RESULT="FAILURE"
                                fi
                                '''
                            }
                        }
                    }
                }

                stage('Collect logs') {
                    for (model in models) {
                        for (mode in modes) {
                            withEnv(["model=$model","mode=$mode", "runType=${RUN_TYPE}", "fullvalidation=${FULL_VALIDATION}","single_socket=${SINGLE_SOCKET}"]) {
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

                    for (q2_model in q2_models) {
                        for (mode in modes) {
                            withEnv(["md=$q2_model","mode=$mode", "single_socket=${SINGLE_SOCKET}", "fullvalidation=${FULL_VALIDATION}"]) {
                                sh '''#!/bin/bash -x
                                $WORKSPACE/cje-tf/scripts/collect_logs_q2_models.sh --model=${md} --mode=${mode} --fullvalidation=${fullvalidation} --single_socket=${single_socket}
                                if [ $? -eq 0 ] ; then
                                    echo "running model ${md} ${mode} success"
                                    RESULT="SUCCESS"
                                else
                                    echo "running model ${md} ${mode} fail"
                                    RESULT="FAILURE"
                                fi
                                '''
                            }
                        }
                    }
                    echo "done collecting logs"
                }

            } catch (e) {
                // If there was an exception thrown, the build failed
                currentBuild.result = "FAILED"
                throw e
            } finally {
                // Success or failure, always send notifications
                withEnv(["runType=$RUN_TYPE", "label=$label", "summaryLog=$SUMMARYLOG"]) {

                    def msg = readFile summaryLog
                    notifyBuild(currentBuild.result, msg)
                }

                // Success or failure, always do artifacts
                stage("Archive Artifacts / Test Results $label") {
                    archiveArtifacts artifacts: 'summary*.log,bazel_build*.log,*.log', excludes: null

                }
            }
        }

    }
}

parallel builders
