PRIVATE_TENSORFLOW_REPO_BRANCH="master"
models=["resnet32cifar10"]
modes=["training"]
SINGLE_SOCKET=false
FULL_VALIDATION=false
MODELS_REFSPEC = "3f78f4cfd21c786c62bf321c07830071027ebb5e"


def notifyBuild(String buildStatus = 'STARTED', String msg) {

  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'
  // msg of null means no additional messages to report
  msg = msg ?: ''
  String summary = "Job <${env.BUILD_URL}|${env.JOB_NAME} #${env.BUILD_NUMBER}> on ${env.NODE_NAME} : ${buildStatus} \n ${msg}"
  Integer SLACK_RETRY_TIMES = 2
  String SLACK_CHANNEL = '#tensorflow-jenkins'
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

//def labels = ['bdw', 'skx']
//def labels = ['nervana-bdw01.fm.intel.com', 'nervana-skx10.fm.intel.com']
//def labels = ['nervana-skx22.fm.intel.com']
def labels = ['aipg-ra-skx-52.ra.intel.com']

def builders =[:]

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
                notifyBuild('STARTED','')
                deleteDir()

                stage("Clone repository $label") {
                    String GIT_CREDENTIAL = "lab_tfbot"
                    String GIT_CREDENTIAL_LAB = "lab_tfbot"
                    String GIT_URL = "https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow"
                    String GIT_NAME = "private-tensorflow"
                    String GIT_BRANCH = "origin-pull/pull/${ghprbPullId}/merge"
                    String GIT_REFSPEC = "+refs/pull/${ghprbPullId}/merge:refs/remotes/origin-pull/pull/${ghprbPullId}/merge"

                    //checkout([$class: 'GitSCM', branches: [[name: "*/$PRIVATE_TENSORFLOW_REPO_BRANCH"]], doGenerateSubmoduleConfigurations: false, extensions: [[ $class: 'RelativeTargetDirectory', relativeTargetDir: 'private-tensorflow' ]], submoduleCfg: [], userRemoteConfigs: [ [credentialsId: "$GIT_CREDENTIAL", url: "$GIT_URL"]]])
                    //checkout([$class: 'GitSCM', branches: [[name: '*/master']], browser: [$class: 'AssemblaWeb', repoUrl: ''], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory',
                    checkout([$class: 'GitSCM', branches: [[name: "$MODELS_REFSPEC"]], browser: [$class: 'AssemblaWeb', repoUrl: ''], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory',
        relativeTargetDir: 'models']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'lab_tfbot', url: 'https://github.com/tensorflow/models.git']]])

                    checkout([
                        $class: 'GitSCM', branches: [[name: "$GIT_BRANCH"]],
                        doGenerateSubmoduleConfigurations: false, extensions: [[
                            $class: 'RelativeTargetDirectory',
                            relativeTargetDir: 'private-tensorflow'
                        ]],
                        submoduleCfg: [], userRemoteConfigs: [
                            [credentialsId: "$GIT_CREDENTIAL", name: "$GIT_NAME",
                             refspec: "$GIT_REFSPEC", url: "$GIT_URL"]]])

                    checkout([$class: 'GitSCM', 
            branches: [[name: '*/master']], 
            browser: [$class: 'AssemblaWeb', repoUrl: ''], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [[$class: 'RelativeTargetDirectory', 
                relativeTargetDir: 'cje-tf']], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL", 
                url: 'https://gitlab.devtools.intel.com/TensorFlow/QA/cje-tf.git']]])


                }
    
                stage("Install dependencies $label") {
                    sh '''
                    #!/bin/bash -x
                    sudo sh -c 'sync; echo 1 > /proc/sys/vm/compact_memory; echo 1 > /proc/sys/vm/drop_caches' || true
                    export PATH="/nfs/site/disks/aipg_tensorflow_tools/gcc6.3/bin/:/nfs/site/disks/aipg_tensorflow_tools/bazel/bin:$PATH"
                    virtualenv -p /usr/bin/python $WORKSPACE/venv
                    source $WORKSPACE/venv/bin/activate
                    sudo touch /usr/include/stropts.h
                    pip install --upgrade autograd backports.weakref bleach enum enum34 funcsigs future futures grpc gevent grpcio html5lib Markdown mock msgpack-python numpy pbr pip portpicker protobuf scikit-learn scipy setuptools six tensorflow-tensorboard Werkzeug wheel keras_applications keras_preprocessing tf-estimator-nightly
                    '''
                }
 
                stage("Configure $label") {
                    dir('private-tensorflow') {
                        sh '''#!/bin/bash -x
                        export PATH="/nfs/site/disks/aipg_tensorflow_tools/gcc6.3/bin/:/nfs/site/disks/aipg_tensorflow_tools/bazel/bin:$PATH"
                        source $WORKSPACE/venv/bin/activate
                        ./configure <<EOF

















		        EOF
                        ''' 
                    }
                }
   
                stage("Build on $label") {
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
        
                    if (RUN_TYPE == "mklml") {
                        echo "mklml"
                        summaryTitle="Tensorflow MKL-ML mini convergene test summary"
                    }
                    else {
                        echo "mkldnn"
                        summaryTitle="Tensorflow MKL-DNN mini convergence test summary"
                    }
                    echo "NODE_NAME = ${env.NODE_NAME}"
                    echo "Github Pull ID = ${ghprbPullId}"

                    SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                              returnStdout: true).trim()
                    echo SERVERNAME
                    SHORTNAME = sh (script:"echo $SERVERNAME | cut -f2 -d-",
                                          returnStdout: true).trim()
                    echo SHORTNAME
                    SUMMARYLOG = "${WORKSPACE}/summary_${SHORTNAME}.log"
        
                    withEnv(["date=$date","summaryTitle=$summaryTitle","gitCommit=$gitCommit", "model_name=$model_name","os_version=$os_version", "nodeName=${env.NODE_NAME}", "repoBranch=${PRIVATE_TENSORFLOW_REPO_BRANCH}", "label=${label}", "runType=${RUN_TYPE}","gitPullID=${ghprbPullId}","summarylog=${SUMMARYLOG}"]) {
                        sh '''#!/bin/bash -x
                        myhost=`echo $label | awk -F'.' '{print $1}'`
                        #LOGFILE="./summary.log"
                        LOGFILE=$summarylog
                        echo "log file = ${LOGFILE}"
                        echo "*************************************************************************" > ${LOGFILE}
                        echo "${summaryTitle} ${date}" >> ${LOGFILE}
                        echo "Repository: private-tensorflow" >> ${LOGFILE}
                        echo "Branch: ${repoBranch}" >> ${LOGFILE}
                        echo "Running on: ${nodeName}" >> ${LOGFILE}
                        echo "Git Revision: ${gitCommit}" >> ${LOGFILE}
                        echo "Git Pull ID: ${gitPullID}" >> ${LOGFILE}
                        echo "${model_name}" >> ${LOGFILE}
                        echo "${os_version}" >> ${LOGFILE}
                        echo "*********************************************************************\n" >> ${LOGFILE}
                        echo "\n" >> ${LOGFILE}
                        '''
                    }
        
                    dir('private-tensorflow') {
                        withEnv(["label=$label", "runType=${RUN_TYPE}" ]) {
                            sh '''#!/bin/bash -x
                            myhost=`echo $label | awk -F'.' '{print $1}'`
                            LOGFILE="../bazel_build_${myhost}.log"
                            echo $LOGFILE
                            export PATH="/nfs/site/disks/aipg_tensorflow_tools/gcc6.3/bin/:/nfs/site/disks/aipg_tensorflow_tools/bazel/bin:$PATH"
                            export LD_LIBRARY_PATH="/nfs/site/disks/aipg_tensorflow_tools/gcc6.3/lib64:$LD_LIBRARY_PATH"
                            if [ ${runType} = "mklml"]; then 
                                #export TF_MKL_ROOT="/nfs/site/home/karenwu/mklml_lnx"
                                /nfs/site/disks/aipg_tensorflow_tools/bazel/bin/bazel --output_user_root=$WORKSPACE/build build --config=mkl --copt="-DINTEL_MKL_ML" --copt="-mavx2" --copt="-mfma" --copt="-march=broadwell" --copt="-O3" --copt=-L/nfs/site/disks/aipg_tensorflow_tools/gcc6.3/lib64/ -s -c opt //tensorflow/tools/pip_package:build_pip_package >& ${LOGFILE}
                            else 
                                /nfs/site/disks/aipg_tensorflow_tools/bazel/bin/bazel --output_user_root=$WORKSPACE/build build --config=mkl --copt="-mavx2" --copt="-mfma" --copt="-march=broadwell" --copt="-O3" --copt=-L/opt/tensorflow/gcc6.3/lib64 -s -c opt //tensorflow/tools/pip_package:build_pip_package >& ${LOGFILE}
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
                                grep "ERROR: " ${LOGFILE}  >> ${SUMMARYLOG} 
                                tail -5 ${LOGFILE}  >> ${SUMMARYLOG} 
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
                        export LD_LIBRARY_PATH="/nfs/site/disks/aipg_tensorflow_tools/gcc6.3/lib64:$LD_LIBRARY_PATH"
                        source $WORKSPACE/venv/bin/activate
                        pip install --upgrade $WORKSPACE/build/*.whl
                        '''
                        }
                    }
                }


                stage("Run convergence mini test $label") {
                    sh '''#!/bin/bash -x
                    #cp /nfs/site/home/karenwu/script/fixup_resnet32_cifar10.sh .
                    #./fixup_resnet32_cifar10.sh
                    $WORKSPACE/cje-tf/scripts/fixup_resnet32_cifar10.sh

                    #cp /nfs/site/home/karenwu/script/run_benchmark.sh .
                    #cp /nfs/site/home/karenwu/script/collect_logs.sh .
                    '''
                    notifyBuild('IN PROGRESS', '...Building COMPLETE and SUCCESSFUL, running mini convergence tests ...')

                    for (model in models) {
                        for (mode in modes) {
                            withEnv(["label=$label", "runType=$RUN_TYPE", "model=${model}", "mode=${mode}", "single_socket=${SINGLE_SOCKET}"]) {
                                sh returnStatus: true, script: '''#!/bin/bash -x
                                #./run_benchmark.sh --model=${model} --mode=${mode}
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
                    for (model in models) {
                        for (mode in modes) {
                            withEnv(["model=$model","mode=$mode", "runType=${RUN_TYPE}", "fullvalidation=${FULL_VALIDATION}","single_socket=${SINGLE_SOCKET}"]) {
                                sh '''#!/bin/bash -x
                                #./collect_logs.sh --model=${model} --mode=${mode} --fullvalidation=false
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
                    echo "done collecting logs"
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
                SHORTNAME = sh (script:"echo $SERVERNAME | cut -f2 -d-",
                                          returnStdout: true).trim()
                echo SHORTNAME
                SUMMARYLOG = "${WORKSPACE}/summary_${SHORTNAME}.log"
                withEnv(["runType=$RUN_TYPE", "label=$label", "summarylog=$SUMMARYLOG"]) {
                    echo summarylog
                    def msg = readFile summarylog
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

