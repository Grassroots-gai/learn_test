//******************************************************************************************
// Unit Test Down Stream Job for MR, Nightly, or Release.
// 
// Job Link: https://aipg-jenkins-tf.intel.com/job/
// Job Names using the script: 
//                                              Nightly-Private-Tensorflow-Unittest
//                                              Nightly-Private-Tensorflow-Unittest-V2
//                                              MR-Private-Tensorflow-Unittest-v2
//
// 		Parameters					Default Value
//--------------------------------------------------------------------------------------
// 		NODE_LABEL					nervana-bdw27.fm.intel.com
// 		GIT_URL						https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git
// 		TENSORFLOW_BRANCH			master
// 		TENSORFLOW_COMMON_BRANCH 	master 
// 		PYTHON						2.7
// 		TARGET_PLATFORM				avx2
// 		DOCKER_IMAGE				amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:1.9.0-avx2-devel-mkl
// 		MR_NUM						503 or others
// 		RUN_EIGEN					not set
// 		SLACK_CHANNEL				#tensorflow-jenkins
// 		SUMMARY_TITLE				Tensorflow unit test summary
// 		DISTR						ubuntu
//
// The above default setting is for MR job, MR_NUMBER must be set either from down stream job 
// config or from upstream job. 
//
// Eigen
// 		RUN_EIGEN					set
//		
// Nightly
//		MR_NUMBER 					must not set.
//		TARGET_PLATFORM				avx2
//		RUN_EIGEN					not set
// Release, 
// 		GIT_URL 					https://github.com/tensorflow/tensorflow.git"
// 		GIT_NAME 					private-tensorflow"
// 		TENSORFLOW_BRANCH 			revision number, such as 'v1.9.0'
// 
//******************************************************************************************
	
ERROR="0"
CJE_TF='cje-tf'
CJE_TF_COMMON_DIR = "$CJE_TF/common"
RUN_TYPE="mkldnn"
TF_VERSION=""

GIT_CREDENTIAL = "lab_tfbot"
GIT_CREDENTIAL_LAB = "lab_tfbot"

//Sometimes the https://proxy-us.intel.com:912 definition causes TLS handshake errors inside containers
http_proxy="http://proxy-us.intel.com:911"
https_proxy="http://proxy-us.intel.com:911"
HTTP_PROXY = 'http://proxy-us.intel.com:911'
HTTPS_PROXY = 'http://proxy-us.intel.com:911'

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

// use this parameter to determine where the tensorflow source code is cloned
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
echo "TENSORFLOW_BRANCH: $TENSORFLOW_BRANCH"

TENSORFLOW_BRANCH_UPDATED=TENSORFLOW_BRANCH.replaceAll("/","_")
echo "TENSORFLOW_BRANCH_UPDATED: $TENSORFLOW_BRANCH_UPDATED"

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

DOCKER_IMAGE_NAMESPACE = 'amr-registry.caas.intel.com/aipg-tf/qa'
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
TARGET_PLATFORM="avx2"
if ('TARGET_PLATFORM' in params) {
    echo "TARGET_PLATFORM in params"
    if (params.TARGET_PLATFORM != '') {
        TARGET_PLATFORM = params.TARGET_PLATFORM
        echo TARGET_PLATFORM
    }
}
echo "TARGET_PLATFORM: TARGET_PLATFORM"

// setting DOCKER_IMAGE with some default value or get input from params
DOCKER_IMAGE="tensorflow/tensorflow:devel"
if ('DOCKER_IMAGE' in params) {
    echo "DOCKER_IMAGE in params"
    if (params.DOCKER_IMAGE != '') {
        DOCKER_IMAGE = params.DOCKER_IMAGE
        echo DOCKER_IMAGE
    }
}
echo "DOCKER_IMAGE=$DOCKER_IMAGE"

MR_NUMBER = ''
if ('MR_NUMBER' in params) {
    echo "MR_NUMBER in params"
    if (params.MR_NUMBER != '') {
    	MR_NUMBER = params.MR_NUMBER
    	echo MR_NUMBER
    }
}
echo "MR_NUMBER=$MR_NUMBER"

// check default value for RUN_EIGEN 
Boolean RUN_EIGEN=false
if ('RUN_EIGEN' in params) {
    echo "RUN_EIGEN in params"
	if (params.RUN_EIGEN) 
    	echo "params.RUN_EIGEN is true"
	else
    	echo "params.RUN_EIGEN is false"
	RUN_EIGEN=params.RUN_EIGEN
}
echo "RUN_EIGEN: $RUN_EIGEN"

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

// setting TEST_TO_SKIP with some default value or get input from params
// We need to convert it to a bash ; separated array that can be passed
// as an env var
TEST_TO_SKIP = ''
if ('TEST_TO_SKIP' in params) {
    echo "TEST_TO_SKIP in params"
    echo params.TEST_TO_SKIP
    if (params.TEST_TO_SKIP != '') {
        TEST_TO_SKIP_LIST = params.TEST_TO_SKIP.split()
        echo "Skipping tests: "
        for (i=0; i < TEST_TO_SKIP_LIST.size(); i++)
        {
          echo TEST_TO_SKIP_LIST[i]
          if (TEST_TO_SKIP != '')
          {
            TEST_TO_SKIP+=';'
          }
          TEST_TO_SKIP+=TEST_TO_SKIP_LIST[i]
        }
    }
}
echo "TEST_TO_SKIP: $TEST_TO_SKIP"

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

DISTR = 'ubuntu'
if ('DISTR' in params) {
    echo "DISTR in params"
    if (params.DISTR != '') {
        DISTR = params.DISTR
        echo DISTR
    }
}
echo "DISTR: $DISTR"

BAZEL_VERSION = ''
if ('BAZEL_VERSION' in params) {
    echo "BAZEL_VERSION in params"
    if (params.BAZEL_VERSION != '') {
        BAZEL_VERSION = params.BAZEL_VERSION
        echo BAZEL_VERSION
    }
}
echo "BAZEL_VERSION: $BAZEL_VERSION"

// Setting OPTIONAL_BAZEL_TEST_OPTIONS if entered by the user. Default is null
OPTIONAL_BAZEL_TEST_OPTIONS = ''
if ('OPTIONAL_BAZEL_TEST_OPTIONS' in params) {
    echo "OPTIONAL_BAZEL_TEST_OPTIONS in params"
    if (params.OPTIONAL_BAZEL_TEST_OPTIONS != '') {
        OPTIONAL_BAZEL_TEST_OPTIONS = params.OPTIONAL_BAZEL_TEST_OPTIONS
    }
}
echo "OPTIONAL_BAZEL_TEST_OPTIONS: $OPTIONAL_BAZEL_TEST_OPTIONS"

// setting DOWNLOAD_BASELINE default to the job Nightly-Private-Tensorflow-Eigen unless otherwise specified in params 
DOWNLOAD_BASELINE = 'Nightly-Private-Tensorflow-Eigen'
if ('DOWNLOAD_BASELINE' in params) {
    echo "DOWNLOAD_BASELINE in params"
    if (params.DOWNLOAD_BASELINE != '') {
        DOWNLOAD_BASELINE = params.DOWNLOAD_BASELINE
        echo DOWNLOAD_BASELINE
    }
}
echo "downloading baseline from $DOWNLOAD_BASELINE jenkin job"

// tf target branch merged into
TF_MERGE_BRANCH = 'master'
if ('TF_MERGE_BRANCH' in params) {
    echo "TF_MERGE_BRANCH in params"
    if (params.TF_MERGE_BRANCH != '') {
        TF_MERGE_BRANCH = params.TF_MERGE_BRANCH
        echo TF_MERGE_BRANCH
    }
}
echo "TF_MERGE_BRANCH: $TF_MERGE_BRANCH"

def uploadArtifactory() {

    echo "---------------------------------------------------------"
    echo " ---------------- uploadArtifactory ------------------- "
    echo "---------------------------------------------------------"

    dir("$WORKSPACE/publish") {
        def server = Artifactory.server 'ubit-artifactory-or'
        unstash "uploadfile"
        def uploadSpec = """{
            "files": [
            {
               "pattern": "*.failures",
               "target": "aipg-local/aipg-tf/${env.JOB_NAME}/${env.BUILD_NUMBER}/baseline/"
            }
            ]
            }"""
        def buildInfo = server.upload(uploadSpec)
        server.publishBuildInfo(buildInfo)
    }
}

def downloadArtifactory = { jobName, fileSpec ->

    echo "---------------------------------------------------------"
    echo " --------------- downloadArtifactory ------------------- "
    echo "---------------------------------------------------------"

    def server = Artifactory.server 'ubit-artifactory-or'
        def downloadSpec = """{
            "files": [
                {
                    "pattern": "aipg-local/aipg-tf/${jobName}/(*)/${fileSpec}",
                    "target": "baseline/",
                    "build": "${jobName}/LATEST",
                    "flat": "true"
                }
            ]
        }"""
        def buildInfo = server.download(downloadSpec)
}

def cloneTFRepo() {

    String GIT_CREDENTIAL = "lab_tfbot"
    String GIT_CREDENTIAL_LAB = "lab_tfbot"


    echo "------- running cloneTFRepo -------"

    try {

        // always check out TF source from different branch with different refspec depending on: 
        //     - if MR       : merge MR source branch
        //     - if nightly  : master branch
        //     - if release  : release branch
        if (MR_NUMBER == '') {
            echo "unit test on $TENSORFLOW_BRANCH branch"
            branch = "$TENSORFLOW_BRANCH"
            refspec = "+refs/heads/*:refs/remotes/$GIT_NAME/*"

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
        }
        else {
            echo "unit test on MR $TENSORFLOW_BRANCH branch"
            branch = "$TENSORFLOW_BRANCH"

            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: "$branch"]],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [
                                                          [$class: 'PreBuildMerge', 
                                                            options: [
                                                                    fastForwardMode: 'FF', 
                                                                    mergeRemote: "$GIT_NAME", 
                                                                    mergeStrategy: 'DEFAULT', 
                                                                    mergeTarget: "$TF_MERGE_BRANCH"
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
        
        // tensorflow-common
        checkout([$class                           : 'GitSCM', 
                  branches                         : [[name: TENSORFLOW_COMMON_BRANCH]], 
                  browser                          : [$class: 'AssemblaWeb', repoUrl: ''], 
                  doGenerateSubmoduleConfigurations: false, 
                  extensions                       : [[$class: 'RelativeTargetDirectory', 
                                                       relativeTargetDir: 'tensorflow-common']], 
                  submoduleCfg                     : [], 
                  userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",
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
                echo "unitTestLog=$unitTestLog\nERROR count = $(grep 'ERROR: ' ${unitTestLog} | wc -l)"
                echo "ERRORs: $(grep 'ERROR: ' ${unitTestLog})"
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
            stash allowEmpty: true, includes: "*.failures", name: "uploadfile"

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

def generateEigenReport(String name, String tensorflow_branch, String target_platform, String python, String summary_log, String tf_version) {

    echo "---------------------------------------------------------"
    echo "-----------     running generateEigenReport   -----------"
    echo "---------------------------------------------------------"

        withEnv(["TENSORFLOW_BRANCH=${tensorflow_branch}", "TARGET_PLATFORM=${target_platform}", "PYTHON=${python}", "SUMMARY_LOG=${summary_log}", "TF_VERSION=${tf_version}"]) {

            sh '''#!/bin/bash -x
                eigen_log="${WORKSPACE}/eigen_build_${TENSORFLOW_BRANCH}_${TARGET_PLATFORM}_Python_${PYTHON}.log"
                eigen_failures="${WORKSPACE}/eigen.failures"
                fgrep Executed ${eigen_log}
                if [ $? -ne 0 ]; then
                    echo "Test failed to execute" >> ${SUMMARY_LOG}
                    exit 1
                fi
                fgrep "were skipped" ${eigen_log}
                if [ $? -eq 0 ]; then
                    echo "Some tests skipped, unsure of results, leaving eigen.failures as is" >> ${SUMMARY_LOG} 
                    exit 1
                fi
                get_failure_script=$WORKSPACE/cje-tf/scripts/get_my_failures.sh
                if [ ! -f ${get_failure_script} ]; then
                    echo "ERROR!!! Unable to find the patch file: ${get_failure_script} "
                    exit 1
                fi
               
                mkdir -p $WORKSPACE/logs
                log_location=$WORKSPACE/logs 
                cp ${get_failure_script} .
                ./get_my_failures.sh ${eigen_log} all
                cp logs.tar.gz  ${log_location}/eigen.logs.tar.gz
                cp ${eigen_log} ${log_location}
                cp ${eigen_failures} ${log_location}
                
            '''

        }
                
    junit allowEmptyResults: true, testResults: "${name}/bazel-testlogs/tensorflow/**/test.xml"

}

def generateUnitTestReport(String name, String tensorflow_branch, String target_platform, String python, String tf_version, String distr, String mr_number, String testlog) {

    echo "---------------------------------------------------------"
    echo "-----------   running generateUnitTestReport  -----------"
    echo "---------------------------------------------------------"

    // for release, compare ut.failures with the corresponding release's eigen.failures
    // for nightly, compare ut.failures with the nightly eigen.failures, also update the corresponding dnn.failures/dnn_v2.failures with ut.failures
    // for MR, compare ut.failures with the nightly dnn.failures/dnn_v2.failures

    try {
        
        withEnv(["TENSORFLOW_BRANCH=${tensorflow_branch}", "TARGET_PLATFORM=${target_platform}", "PYTHON=${python}", "TF_VERSION=${tf_version}", "DISTR=${distr}", "MR_NUMBER=${mr_number}", "TESTLOG=${testlog}"]) {
            sh '''#!/bin/bash -x
                echo $TENSORFLOW_BRANCH
                echo $TF_VERSION
                echo $MR_NUMBER
                 
                cp $WORKSPACE/cje-tf/scripts/remove_eigen_failures.py .                 
                eigen_failure="${WORKSPACE}/baseline/eigen.failures"
                dnn_failure="${WORKSPACE}/baseline/ut.failures"
	        python $WORKSPACE/cje-tf/scripts/tests_check.py ${TESTLOG}
                if [ "${MR_NUMBER}" == '' ]; then 
	            python remove_eigen_failures.py --baseFailureFile=${eigen_failure} --utFailureFile="$WORKSPACE/ut.failures" --newFailureFile="$WORKSPACE/newFailure.txt"
                else
                    python remove_eigen_failures.py --baseFailureFile=${dnn_failure} --utFailureFile="$WORKSPACE/ut.failures" --newFailureFile="$WORKSPACE/newFailure.txt"
                fi
            '''
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

node( NODE_LABEL ) {

    try {

        SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                          returnStdout: true).trim()
        SUMMARYLOG = "${WORKSPACE}/summary_${TENSORFLOW_BRANCH_UPDATED}_${TARGET_PLATFORM}_Python_${PYTHON}_${SERVERNAME}.log"
        if ( RUN_EIGEN ) 
            TESTLOG = "${WORKSPACE}/eigen_build_${TENSORFLOW_BRANCH_UPDATED}_${TARGET_PLATFORM}_Python_${PYTHON}.log"
        else
            TESTLOG = "${WORKSPACE}/unit_test_${TENSORFLOW_BRANCH_UPDATED}_${TARGET_PLATFORM}_Python_${PYTHON}.log"
        echo SUMMARYLOG
        echo TESTLOG

        if ( "${OPTIONAL_BAZEL_TEST_OPTIONS}".contains("--config=v2") ) {    
            TF_VERSION="v2"
        }

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

                withEnv(["http_proxy=$http_proxy", \
                         "https_proxy=$https_proxy", \
                         "HTTP_PROXY=$HTTP_PROXY", \
                         "HTTPS_PROXY=$HTTPS_PROXY"  ]) {

                    echo WORKSPACE
                    echo GIT_NAME
                    echo TARGET_PLATFORM
                    echo TENSORFLOW_BRANCH
                    echo TENSORFLOW_BRANCH_UPDATED
                    echo PYTHON
                    echo "Skipping tests: $TEST_TO_SKIP"
                    docker.image("$DOCKER_IMAGE").inside("--env \"http_proxy=${http_proxy}\" \
                                                          --env \"https_proxy=${https_proxy}\" \
                                                          --env \"HTTP_PROXY=${HTTP_PROXY}\" \
                                                          --env \"HTTPS_PROXY=${HTTPS_PROXY}\" \
                                                          --volume ${WORKSPACE_VOLUME} \
                                                          --env RUN_EIGEN=$RUN_EIGEN \
                                                          --env TARGET_PLATFORM=$TARGET_PLATFORM \
                                                          --env TENSORFLOW_BRANCH=${TENSORFLOW_BRANCH_UPDATED} \
                                                          --env PYTHON=$PYTHON \
                                                          --env TENSORFLOW_DIR=$GIT_NAME \
                                                          --env OPTIONAL_BAZEL_TEST_OPTIONS=$OPTIONAL_BAZEL_TEST_OPTIONS \
                                                          --env TEST_TO_SKIP=$TEST_TO_SKIP \
                                                          --env BAZEL_VERSION=$BAZEL_VERSION \
                                                          --env MR_NUMBER=$MR_NUMBER \
                                                          -u root:root" ) { 
            
                        sh '''#!/bin/bash -x
                        cd /workspace
                        python /workspace/cje-tf/scripts/get_tf_sourceinfo.py --tensorflow_dir=$TENSORFLOW_DIR --workspace_dir=/workspace
                        chmod 775 ./cje-tf/scripts/run_docker_unittest.sh
                        ./cje-tf/scripts/run_docker_unittest.sh
                        '''
                    }
                }  // withEnv

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
                        output=`stat -c "%U %G" .`
                        user=`echo $output | awk -F' ' '{print $1}'`
                        echo $user
                        group=`echo $output | awk -F' ' '{print $2}'`
                        echo $group
                        if [ -d "${WORKSPACE}/eigen_build" ]; then
                            sudo chown $user:$group -R "${WORKSPACE}/eigen_build"
                        fi
                        if [ -d "${WORKSPACE}/test" ]; then
                            sudo chown $user:$group -R "${WORKSPACE}/test"
                        fi
                        if [ -d "${WORKSPACE}/${GIT_NAME}" ]; then
                            sudo chown $user:$group -R "${WORKSPACE}/${GIT_NAME}"
                        fi
                        mkdir -p $WORKSPACE/baseline
                    '''
                }

                // Prepare logs
                def prepareLog = load("${CJE_TF_COMMON_DIR}/prepareLog.groovy")
                prepareLog("", "", RUN_TYPE, SUMMARYLOG, SUMMARY_TITLE)

                unitTestResult = collectUnitTestLog(RUN_TYPE, SUMMARYLOG, TESTLOG) 
           
                if ( unitTestResult ) {

                    echo "collectUnitTestLog returns 1"
                    echo "setting currentBuild.result = FAILED"
                    currentBuild.result = 'FAILURE'

                }
                else {
                    
                    echo "collectUnitTestLog returns 0"
                    if (!RUN_EIGEN) {
                        if (MR_NUMBER == '') {
                            // download the eigen.failures from corresponding job
                            downloadSpec = downloadArtifactory(DOWNLOAD_BASELINE, "baseline/eigen.failures")
                        }
                        else {
                            // download the ut.failures from corresponding job
                            downloadSpec = downloadArtifactory(DOWNLOAD_BASELINE, "baseline/ut.failures")
                        }

                        generateUnitTestReport(GIT_NAME, TENSORFLOW_BRANCH_UPDATED, TARGET_PLATFORM, PYTHON, TF_VERSION, DISTR, MR_NUMBER, TESTLOG)
                    } 
                    else {
                        generateEigenReport(GIT_NAME, TENSORFLOW_BRANCH_UPDATED, TARGET_PLATFORM, PYTHON, SUMMARYLOG, TF_VERSION)
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

                    archiveArtifacts artifacts: '*.log, logs/eigen.logs.tar.gz, logs/eigen.failures', excludes: null

                }
                else {

                    archiveArtifacts artifacts: '*.log, ut.failures, *.txt', excludes: null

                }
                fingerprint: true
            }

            // only upload eigen.failure or ut.failure if not a MR
            if (MR_NUMBER == '') 
                uploadArtifactory()

        }

    }
}

