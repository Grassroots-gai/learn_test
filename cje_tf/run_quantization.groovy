//******************************************************************************************
// Running Quantization Tests
// Details see https://gitlab.devtools.intel.com/intelai/tools/tree/develop/tensorflow-quantization/tests
// 
//******************************************************************************************
	
ERROR="0"
CJE_TF='cje-tf'
CJE_TF_COMMON_DIR = "$CJE_TF/common"
GIT_CREDENTIAL = "lab_tfbot"
GIT_CREDENTIAL_LAB = 'lab_tfbot'
GIT_URL = "https://gitlab.devtools.intel.com/intelai/tools.git"

http_proxy="http://proxy-us.intel.com:911"
https_proxy="https://proxy-us.intel.com:912"

// set default value for NODE_LABEL 
NODE_LABEL = 'nervana-bdw27.fm.intel.com'
if ('NODE_LABEL' in params) {
    echo "NODE_LABEL in params"
    if (params.NODE_LABEL != '') {
        NODE_LABEL = params.NODE_LABEL
    }
}
echo "NODE_LABEL: $NODE_LABEL"

// set default value for GIT_NAME
GIT_NAME = "tools"
if ('GIT_NAME' in params) {
    echo "GIT_NAME in params"
    if (params.GIT_NAME != '') {
        GIT_NAME = params.GIT_NAME
    }
}
echo "GIT_NAME: $GIT_NAME"

// set default value for TOOLS_BRANCH 
TOOLS_BRANCH = 'master'
if ('TOOLS_BRANCH' in params) {
    echo "TOOLS_BRANCH in params"
    if (params.TOOLS_BRANCH != '') {
        TOOLS_BRANCH = params.TOOLS_BRANCH
    }
}
echo "TOOLS_BRANCH: $TOOLS_BRANCH"

// set default value for TEST_PATH
TEST_PATH="$GIT_NAME/tensorflow-quantization/tests"
if ('TEST_PATH' in params) {
    echo "TEST_PATH in params"
    if (params.TEST_PATH != '') {
        TEST_PATH = params.TEST_PATH
    }
}
echo "TEST_PATH: $TEST_PATH"

// set default value for TEST_SCRIPT
TEST_SCRIPT="launch_test.sh"
if ('TEST_SCRIPT' in params) {
    echo "TEST_SCRIPT in params"
    if (params.TEST_SCRIPT != '') {
        TEST_SCRIPT = params.TEST_SCRIPT
    }
}
echo "TEST_SCRIPT: $TEST_SCRIPT"

// setting SLACK_CHANNEL with some default value or get input from params
SLACK_CHANNEL = '#tensorflow-jenkins'
if ('SLACK_CHANNEL' in params) {
    echo "SLACK_CHANNEL in params"
    if (params.SLACK_CHANNEL != '') {
        SLACK_CHANNEL = params.SLACK_CHANNEL
    }
}
echo "SLACK_CHANNEL: $SLACK_CHANNEL"

// setting SUMMARY_TITLE with some default value or get input from params
SUMMARY_TITLE = 'Tensorflow Quantization Test Summary '
if ('SUMMARY_TITLE' in params) {
    echo "SUMMARY_TITLE in params"
    if (params.SUMMARY_TITLE != '') {
        SUMMARY_TITLE = params.SUMMARY_TITLE
    }
}
echo "SUMMARY_TITLE: $SUMMARY_TITLE"

def cloneTFRepo() {
    echo "------- running cloneTFRepo() -------"
	echo TOOLS_BRANCH
	echo GIT_NAME
	echo GIT_CREDENTIAL
	echo GIT_URL
          	
    try {	
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: TOOLS_BRANCH]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: GIT_NAME]],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                      name							   : GIT_NAME,
                      url          					   : GIT_URL]]])                                              
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
    echo "------- running cleanup() -------"
    try {
        sh '''#!/bin/bash -x
        echo $WORKSPACE
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
        SHORTNAME = sh (script:"echo $SERVERNAME | cut -f2 -d-",
                              returnStdout: true).trim()
        echo SERVERNAME
        echo SHORTNAME
        TESTLOG = "${WORKSPACE}/Test_Quantilization_${GIT_NAME}_${TOOLS_BRANCH}.log"
        SUMMARYLOG = "${WORKSPACE}/Summary_Quantilization_${GIT_NAME}_${TOOLS_BRANCH}_${SERVERNAME}.log"
  		echo TESTLOG
  		echo "SUMMARYLOG=$SUMMARYLOG"
  		
        stage('Clean Up') {
	        cleanup()    
        	deleteDir()
        }

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

        stage('Quantization Test') {

            echo "----- stage Run Quantization Test -----"
 
            try {
                echo "tests are under $WORKSPACE/$TEST_PATH"
				withEnv(["TEST_PATH=$TEST_PATH", "TESTLOG=$TESTLOG", "TEST_SCRIPT=$TEST_SCRIPT"]) {
	                sh '''#!/bin/bash -x
	                echo $WORKSPACE
	                echo $TEST_PATH
	                //TIME=`date "+%Y_%h_%d_%H:%M:%S"`
	                //TESTLOGFILE=${TESTLOG}_${TIME}.log
	                //echo ${TESTLOGFILE}
	                chmod 755 $WORKSPACE/$TEST_PATH/$TEST_SCRIPT
					cd $WORKSPACE/$TEST_PATH
					./$TEST_SCRIPT>$TESTLOG
                	'''
                }
            } catch(e) {

                echo "======================================================="
                echo "ERROR: Exception caught in stage Run Quantization Test "
                echo "ERROR: ${e}"
                echo "======================================================="

                echo ' '
                echo "Build marked as FAILURE"
                currentBuild.result = 'FAILURE'

            }  // catch

        } // stage Run Quantization Test

    } catch (e) {
        // If there was an exception thrown, the build failed
        echo 'Exeption occurs: ' + e.toString()
        currentBuild.result = "FAILED"
        throw e
    } finally {
        stage('Output Log Summary') {
    		model_name = sh (script: 'lscpu | grep "Model name:"', returnStdout: true).trim()
    		os_version = sh (script: "cat /etc/os-release | grep PRETTY_NAME | sed 's/PRETTY_NAME=/OS Version:      /'", returnStdout: true).trim()
		    withEnv(["branch=$TOOLS_BRANCH", "name=$GIT_NAME", "summaryLog=$SUMMARYLOG", "summaryTitle=$SUMMARY_TITLE", "nodeName=${env.NODE_NAME}", "model_name=$model_name","os_version=$os_version", "unitTestLog=$TESTLOG" ]) {
		        sh '''#!/bin/bash -x
		        TIME=`date "+%Y_%h_%d_%H:%M:%S"`
		        echo "*************************************************************************" > ${summaryLog}
		        echo "${summary}" >> ${summaryLog}
		        if [[ "$name" != "" ]]; then
		            echo "Repository: ${name}" >> ${summaryLog}
		        fi
		        if [[ "$branch" != "" ]]; then
		            echo "Branch: ${branch}" >> ${summaryLog}
		        fi
		        if [[ "$gitCommit" != "" ]]; then
		            echo "Git Revision: ${gitCommit}" >> ${summaryLog}
		        fi
		        echo "Running on: ${nodeName}" >> ${summaryLog}
		        echo "${model_name}" >> ${summaryLog}
		        echo "${os_version}" >> ${summaryLog}
		        echo "*************************************************************************\n" >> ${summaryLog}
		        echo "\n" >> ${summaryLog}

                tail -5 ${unitTestLog} >> ${summaryLog} 		        
		        '''  
		    }
        }
        
        // Success or failure, always send notifications
        withEnv(["SUMMARYLOG=$SUMMARYLOG"]) {
            def msg = readFile SUMMARYLOG
            def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
            notifyBuild(SLACK_CHANNEL, currentBuild.result, msg)
        }
        
        stage('Archive Artifacts ') {
	        dir("$WORKSPACE") {
	            archiveArtifacts artifacts: '*.log', excludes: null
	            fingerprint: true
	        }
	    }
	}
}
