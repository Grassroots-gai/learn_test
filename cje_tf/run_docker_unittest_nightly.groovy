CJE_TF='cje-tf'
CJE_TF_COMMON_DIR = "$CJE_TF/common"
RUN_TYPE="mkldnn"

GIT_CREDENTIAL = "lab_tfbot"
GIT_CREDENTIAL_LAB = "lab_tfbot"

http_proxy="http://proxy-us.intel.com:911"
https_proxy="https://proxy-us.intel.com:912"

// set default value for NODE_LABEL 
NODE_LABEL = 'nervana-bdw27.fm.intel.com'
if ('NODE_LABEL' in params) {
    echo "NODE_LABEL in params"
    if (params.NODE_LABEL != '') {
        NODE_LABEL = params.NODE_LABEL
        echo NODE_LABEL
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

// set default value for DOCKER_GIT_NAME
DOCKER_GIT_NAME = "/private-tensorflow"
if ('DOCKER_GIT_NAME' in params) {
    echo "DOCKER_GIT_NAME in params"
    if (params.DOCKER_GIT_NAME != '') {
        DOCKER_GIT_NAME = params.DOCKER_GIT_NAME
        echo DOCKER_GIT_NAME
    }
}
echo "DOCKER_GIT_NAME: $DOCKER_GIT_NAME"

// set default value for TENSORFLOW_BRANCH 
TENSORFLOW_BRANCH = 'master'
if ('TENSORFLOW_BRANCH' in params) {
    echo "TENSORFLOW_BRANCH in params"
    if (params.TENSORFLOW_BRANCH != '') {
        TENSORFLOW_BRANCH = params.TENSORFLOW_BRANCH
        echo TENSORFLOW_BRANCH
    }
}
echo "TENSORFLOW_BRANCH: $TENSORFLOW_BRANCH"

// set default value for TENSORFLOW_COMMON_BRANCH 
TENSORFLOW_COMMON_BRANCH='master'
if ('TENSORFLOW_COMMON_BRANCH' in params) {
    echo "TENSORFLOW_COMMON_BRANCH in params"
    if (params.TENSORFLOW_COMMON_BRANCH != '') {
        TENSORFLOW_COMMON_BRANCH = params.TENSORFLOW_COMMON_BRANCH
        echo TENSORFLOW_COMMON_BRANCH
    }
}
echo "TENSORFLOW_COMMON_BRANCH: $TENSORFLOW_COMMON_BRANCH"

// set default value for PYTHON version
// possible values: e.g. 2.7
//                       3.5 
//                       3.6
PYTHON = '2.7'
if ('PYTHON' in params) {
    echo "PYTHON in params"
    if (params.PYTHON != '') {
        PYTHON = params.PYTHON
        echo PYTHON
    }
}
echo "PYTHON: $PYTHON"

// set default DOCKER_IMAGE  value
// possible values: e.g. 
// 1.8: 
// amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.8-py2-build
// amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.8-py3-build
// 1.9:
// avx:
// amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.9.0-devel-mkl
// amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.9.0-devel-mkl-py3
// avx2:
// amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.9.0-avx2-devel-mkl
// amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.9.0-avx2-devel-mkl-py3
DOCKER_IMAGE="amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.9.0-avx2-devel-mkl"
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
TARGET_PLATFORM="avx2"
if ('TARGET_PLATFORM' in params) {
    echo "TARGET_PLATFORM in params"
    if (params.TARGET_PLATFORM != '') {
        TARGET_PLATFORM = params.TARGET_PLATFORM
        echo TARGET_PLATFORM
    }
}
echo "TARGET_PLATFORM: $TARGET_PLATFORM"

// check default value for RUN_EIGEN 
if (params.RUN_EIGEN) 
    echo "params.RUN_EIGEN is true"
else
    echo "params.RUN_EIGEN is false"
Boolean RUN_EIGEN=params.RUN_EIGEN

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
SUMMARY_TITLE = 'Tensorflow unit test summary '
if ('SUMMARY_TITLE' in params) {
    echo "SUMMARY_TITLE in params"
    if (params.SUMMARY_TITLE != '') {
        SUMMARY_TITLE = params.SUMMARY_TITLE
        echo SUMMARY_TITLE
    }
}
echo "SUMMARY_TITLE: $SUMMARY_TITLE"

def cloneTFRepo() {

    String GIT_CREDENTIAL = "lab_tfbot"
    String GIT_CREDENTIAL_LAB = "lab_tfbot"

    echo "------- running cloneTFRepo -------"

    try { 

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

def collectUnitTestLog(String runType, String summaryLog, String unitTestLog) {

    echo "---------------------------------------------------------"
    echo "---------     running collectUnitTestLog       ----------"
    echo "---------------------------------------------------------"

    try {

        String error = ''
        withEnv(["runType=${runType}", "summaryLog=${summaryLog}", "unitTestLog=${unitTestLog}" ]) {
            resp = sh (
            
                returnStatus: true,
                script: '''
                ERROR="0"
                echo $summaryLog
                echo $unitTestLog
                tail -2 ${unitTestLog} >> ${summaryLog} 
                if [ "$(grep 'ERROR: ' ${unitTestLog} | wc -l)" = "0" ] ; then
                    RESULT="SUCCESS"
                    echo SUCCESS
                else
                    RESULT="FAILURE"
                    ERROR="1"
                    echo FAILURE
                fi
                exit $ERROR
                '''
            )
            
            stash allowEmpty: true, includes: "*.log", name: "logfile"

        } // withEnv
        print("DEBUG: Response: ${resp}")
      
        return resp

    } catch(e) {

            echo "==============================================================================================="
            echo "ERROR: Exception caught in module which collect the unit test log - collectUnitTetLog()        "
            echo "ERROR: ${e}"
            echo "==============================================================================================="

            echo ' '
            echo "Build marked as FAILURE"
            currentBuild.result = 'FAILURE'

    }  // catch
}

def generateEigenReport(String name, String tensorflow_branch, String target_platform, String python, String summary_log) {

    // nightly runs on the master branch
    if ( TENSORFLOW_BRANCH == 'master' ) {
        withEnv(["TENSORFLOW_BRANCH=${tensorflow_branch}", "TARGET_PLATFORM=${target_platform}", "PYTHON=${python}", "SUMMARY_LOG=${summary_log}"]) {

            sh '''#!/bin/bash -x
                eigen_log="${WORKSPACE}/eigen_build_${TENSORFLOW_BRANCH}_${TARGET_PLATFORM}_Python_${PYTHON}.log"
                eigen_failures="${WORKSPACE}/eigen.failures"

                fgrep Executed ${eigen_log}
                if [ $? -ne 0 ]
                then
                    echo "Test failed to execute" >> ${SUMMARY_LOG}
                    exit 1
                fi
                fgrep "were skipped" ${eigen_log}
                if [ $? -eq 0 ]
                then
                    echo "Some tests skipped, unsure of results, leaving eigen.failures as is" >> ${SUMMARY_LOG} 
                    exit 1
                fi

                #tail -2 ${eigen_log} >> ${SUMMARY_LOG}

                get_failure_script=$WORKSPACE/cje-tf/scripts/get_my_failures.sh
                if [ ! -f ${get_failure_script} ]
                then
                echo "ERROR!!! Unable to find the patch file: ${get_failure_script} "
                exit 1
                fi
               
                mkdir -p $WORKSPACE/eigen_logs
                log_location=$WORKSPACE/eigen_logs 

                cp ${get_failure_script} .
                ./get_my_failures.sh ${eigen_log} all
                cp logs.tar.gz  ${log_location}/eigen.logs.tar.gz
                cp ${eigen_log} ${log_location}
                cp ${eigen_failures} ${log_location}
                
                #eigen_build_results_shared_dir="/mnt/aipg_tensorflow_shared/validation/logs"
                eigen_build_results_shared_dir=/mnt/aipg_tensorflow_shared/validation/tf_master_validation/
                if [ ! -d ${eigen_build_results_shared_dir} ]
                then
                mkdir -p ${eigen_build_results_shared_dir}
                fi
                
                date=`date "+%Y_%m_%d"`
                eigen_failure_file=${eigen_build_results_shared_dir}/eigen.failures
                eigen_logs_zip=${eigen_build_results_shared_dir}/eigen.logs.tar.gz
                
                if [ -f ${eigen_failure_file} ]
                then
                    if [-f ${eigen_failure_file}_${date} ]
                    then
                    rm -rf ${eigen_failure_file}_${date}
                    fi
                cp ${eigen_failure_file} ${eigen_failure_file}_${date}
                fi
                
                if [ -f ${eigen_logs_zip} ]
                then
                    if [-f ${eigen_logs_zip}_${date} ]
                    then
                    rm -rf ${eigen_logs_zip}_${date}
                    fi
                cp ${eigen_logs_zip} ${eigen_logs_zip}_${date}
                fi
                
                cp ${log_location}/eigen.failures ${eigen_build_results_shared_dir}/eigen.failures
                cp ${log_location}/eigen.logs.tar.gz ${eigen_build_results_shared_dir}/eigen.logs.tar.gz
            '''

        }
                
    }

    else {

        withEnv(["TENSORFLOW_BRANCH=${tensorflow_branch}", "TARGET_PLATFORM=${target_platform}", "PYTHON=${python}"]) {
            sh '''#!/bin/bash -x
            eigen_shared_dir="/mnt/aipg_tensorflow_shared/validation/tf_${TENSORFLOW_BRANCH}_validation/${TENSORFLOW_BRANCH}_${TARGET_PLATFORM}_Python_${PYTHON}"
            if [ ! -d ${eigen_shared_dir} ]; then
                mkdir -p ${eigen_shared_dir}
            fi
            shared_eigen_failures="${eigen_shared_dir}/eigen.failures"
            date=`date "+%Y_%m_%d"`
                
            if [ -f ${shared_eigen_failures} ]; then
                if [ -f ${shared_eigen_failures}_${date} ]; then
                    rm -rf ${shared_eigen_failures}_${date}
                fi
                cp ${shared_eigen_failures} ${shared_eigen_failures}_${date}
            fi

            eigen_failures="${WORKSPACE}/eigen.failures"

            cp ${eigen_failures} ${eigen_shared_dir}/.
            cp "${WORKSPACE}/eigen_build_${TENSORFLOW_BRANCH}_${TARGET_PLATFORM}_Python_${PYTHON}.log" ${eigen_shared_dir}
            '''
        }
    }

    junit allowEmptyResults: true, testResults: "${name}/bazel-testlogs/tensorflow/**/test.xml"
}


def generateUnitTestReport(String name, String runType, String testLog) {

    echo "---------------------------------------------------------"
    echo "-----------   running generateUnitTestReport  -----------"
    echo "---------------------------------------------------------"

    try {

        dir(WORKSPACE) {
            withEnv(["runType=${runType}", "testLog=${testLog}"]) {
                sh '''#!/bin/bash -x

                    python $WORKSPACE/cje-tf/scripts/tests_check.py ${testLog}
                    $WORKSPACE/cje-tf/scripts/updateUnitTestFailures.sh --runLog=${testLog} --runType=${runType}

                '''
            }
        }

        dir("${name}/bazel-testlogs") {
            withEnv(["runType=${runType}"]) {
                sh '''#!/bin/bash -x

                    cp $WORKSPACE/cje-tf/scripts/remove_eigen_failures.py .
                    python remove_eigen_failures.py "/mnt/aipg_tensorflow_shared/validation/tf_${TENSORFLOW_BRANCH}_validation/${TENSORFLOW_BRANCH}_${TARGET_PLATFORM}_Python_${PYTHON}/eigen.failures"


                '''
            }
        }
        junit allowEmptyResults: true, testResults: "${name}/bazel-testlogs/tensorflow/**/test.xml"

    } catch(e) {

        echo "==============================================================================================="
        echo "ERROR: Exception caught in module which generate the unit test report - generateUnitTetReport()"
        echo "ERROR: ${e}"
        echo "==============================================================================================="

        echo ' '
        echo "Build marked as FAILURE"
        currentBuild.result = 'FAILURE'

    }  // catch

}

def cleanup() {

    try {

        sh '''#!/bin/bash -x
        cd WORKSPACE
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

        SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                          returnStdout: true).trim()
        SHORTNAME = sh (script:"echo $SERVERNAME | cut -f2 -d-",
                              returnStdout: true).trim()
        SUMMARYLOG = "${WORKSPACE}/summary_${TENSORFLOW_BRANCH}_${TARGET_PLATFORM}_Python_${PYTHON}_${SERVERNAME}.log"
        if ( RUN_EIGEN ) 
            TESTLOG = "${WORKSPACE}/eigen_build_${TENSORFLOW_BRANCH}_${TARGET_PLATFORM}_Python_${PYTHON}.log"
        else
            TESTLOG = "${WORKSPACE}/unit_test_${TENSORFLOW_BRANCH}_${TARGET_PLATFORM}_Python_${PYTHON}.log"
        echo SUMMARYLOG
        echo TESTLOG
    
        cleanup()    
        deleteDir()

        sh (script:"touch $SUMMARYLOG",
                   returnStdout: true).trim()
                   
        // pull the cje-tf
        dir(CJE_TF) {
            checkout scm
        }

        // slack notification
        def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
        notifyBuild(SLACK_CHANNEL, 'STARTED', '')

        stage('Checkout') {

            cloneTFRepo()

        }

        stage('Run Unit Test') {

            echo "----- stage Run Unit Test -----"
 
            try {

                echo TESTLOG
                WORKSPACE_VOLUME="${WORKSPACE}:/workspace"

                echo WORKSPACE
                echo DOCKER_GIT_NAME
                echo TARGET_PLATFORM
                echo TENSORFLOW_BRANCH
                echo PYTHON

                docker.image("$DOCKER_IMAGE").inside("--env \"http_proxy=${http_proxy}\" \
                                                      --env \"https_proxy=${https_proxy}\" \
                                                      --volume ${WORKSPACE_VOLUME} \
                                                      --env RUN_EIGEN=$RUN_EIGEN \
                                                      --env TARGET_PLATFORM=$TARGET_PLATFORM \
                                                      --env TENSORFLOW_BRANCH=$TENSORFLOW_BRANCH \
                                                      --env PYTHON=$PYTHON \
                                                      --env TENSORFLOW_DIR=$DOCKER_GIT_NAME \
                                                      -u root:root" ) { 
            

                    sh '''#!/bin/bash -x
                    cd /workspace
                    chmod 775 ./cje-tf/scripts/run_docker_unittest.sh
                    ./cje-tf/scripts/run_docker_unittest.sh
                    '''
                }

            } catch(e) {

                echo "==============================================="
                echo "ERROR: Exception caught in stage Run Unit Test "
                echo "ERROR: ${e}"
                echo "==============================================="

                echo ' '
                echo "Build marked as FAILURE"
                currentBuild.result = 'FAILURE'

            }  // catch

        } // stage Run Unit Test

    } catch (e) {

        // If there was an exception thrown, the build failed
        echo 'Exeption occurs: ' + e.toString()
        currentBuild.result = "FAILED"
        throw e

    } finally {

        stage('Collect Logs') {

            echo "----- stage Collect Logs -----"

            try {
               
                withEnv(["run_eigen=${RUN_EIGEN}"]) { 
                    sh '''#!/bin/bash -x
                        if [ -d "${WORKSPACE}/eigen_build" ]; then
                            sudo chown jenkins:jenkins -R "${WORKSPACE}/eigen_build"
                        fi
                        if [ -d "${WORKSPACE}/test" ]; then
                            sudo chown jenkins:jenkins -R "${WORKSPACE}/test"
                        fi
                        if [ -d "${WORKSPACE}/${GIT_NAME}" ]; then
                            sudo chown jenkins:jenkins -R "${WORKSPACE}/${GIT_NAME}"
                        fi
                    '''
                }

                // Prepare logs
                def prepareLog = load("${CJE_TF_COMMON_DIR}/prepareLog.groovy")
                prepareLog(TENSORFLOW_BRANCH, GIT_NAME, RUN_TYPE, SUMMARYLOG, SUMMARY_TITLE)

                unitTestResult = collectUnitTestLog(RUN_TYPE, SUMMARYLOG, TESTLOG) 
           
                if ( unitTestResult ) {

                    echo "collectUnitTestLog returns 1"
                    echo "setting currentBuild.result = FAILED"
                    exit 1

                }
                else {
                    
                    echo "collectUnitTestLog returns 0"
                    if (!RUN_EIGEN) {

                        generateUnitTestReport(GIT_NAME, RUN_TYPE, TESTLOG)

                    } else {

                        generateEigenReport(GIT_NAME, TENSORFLOW_BRANCH, TARGET_PLATFORM, PYTHON, SUMMARYLOG) 

                    }
                }

            } catch(e) {

                echo "==============================================="
                echo "ERROR: Exception caught in stage Collect Logs  "
                echo "ERROR: ${e}"
                echo "==============================================="

                echo ' '
                echo "Build marked as FAILURE"
                currentBuild.result = 'FAILURE'

            }  // catch

        } // stage Collect Logs

        // Success or failure, always send notifications
        withEnv(["SUMMARYLOG=$SUMMARYLOG"]) {
            echo SUMMARYLOG
            def msg = readFile SUMMARYLOG
            
            def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
            notifyBuild(SLACK_CHANNEL, currentBuild.result, msg)

        }

        stage('Archive Artifacts ') {

            dir("$WORKSPACE") {

                if (RUN_EIGEN) {

                    archiveArtifacts artifacts: '*.log, eigen_logs/eigen.logs.tar.gz, eigen_logs/eigen.failures', excludes: null

                }
                else {

                    archiveArtifacts artifacts: '*.log', excludes: null

                }
                fingerprint: true

            }
            
        }

    }
}

