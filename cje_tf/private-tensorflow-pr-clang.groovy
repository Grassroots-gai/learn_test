CJE_TF='cje-tf'
CJE_TF_COMMON_DIR = "$CJE_TF/common"
RUN_TYPE="mkldnn"

GIT_CREDENTIAL = "lab_tfbot"

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


PR_NUMBER = ''
if ('PR_NUMBER' in params) {
    echo "PR_NUMBER in params"
    if (params.PR_NUMBER != '') {
        PR_NUMBER = params.PR_NUMBER
        echo PR_NUMBER
    }
}
echo "PR_NUMBER: $PR_NUMBER"

GIT_URL = ''
if ('GIT_URL' in params) {
    echo "GIT_URL in params"
    if (params.GIT_URL != '') {
        GIT_URL = params.GIT_URL
        echo GIT_URL
    }
}
echo "GIT_URL: $GIT_URL"

def splitURL = GIT_URL.split('/')
String REPO_NAME = splitURL[splitURL.size() - 1]
echo "RepoDirectory:$REPO_NAME"

def cleanup() {

    try {

        sh '''#!/bin/bash -x
        cd $WORKSPACE
        sudo rm -rf *
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

        cleanup()
        deleteDir()

        SUMMARYLOG = "${WORKSPACE}/summary.log"
        sh (script:"touch $SUMMARYLOG", returnStdout: true).trim()
        withEnv(["pullID=$PR_NUMBER","RepoName=$REPO_NAME"]) {
            sh '''#!/bin/bash -x
                echo "*********************************************************************" > ./summary.log
                echo "PR test clangformat and Pylintformat summary" >> ./summary.log
                echo "Repository:$RepoName " >> ./summary.log
                echo "Git Pull ID: ${pullID}" >> ./summary.log
                echo "*********************************************************************\n" >> ./summary.log
                echo "PR scanning filelist:" >> ./summary.log
            '''
        }

        // pull the cje-tf
        dir(CJE_TF) {
            checkout scm
        }

        // slack notification
        def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
        notifyBuild(SLACK_CHANNEL, 'STARTED', '')


        stage('checkout') {
            String GIT_CREDENTIAL = "lab_tfbot"
            String GIT_BRANCH = "origin-pull/pull/$PR_NUMBER/merge"
            String GIT_NAME = REPO_NAME
            String GIT_REFSPEC = "+refs/pull/$PR_NUMBER/merge:refs/remotes/origin-pull/pull/$PR_NUMBER/merge"
            echo "checking out :$GIT_NAME"

            checkout([
                    $class: 'GitSCM', branches: [[name: "$GIT_BRANCH"]],
                    doGenerateSubmoduleConfigurations: false, extensions: [[
                    $class: 'RelativeTargetDirectory',
                    relativeTargetDir: REPO_NAME
                    ]],
                    submoduleCfg: [], userRemoteConfigs: [
                    [credentialsId: "$GIT_CREDENTIAL", name: "$GIT_NAME",
                    refspec: "$GIT_REFSPEC", url: "$GIT_URL"]]])
        }//stage

        stage('clang format checking') {
            dir("$WORKSPACE") {
                sh '''#!/bin/bash -x
                    mkdir diff_dir
                '''
            }
            dir("${REPO_NAME}"){
                withEnv(["CJE_TF=$CJE_TF","RepoName=$REPO_NAME", "SUMMARYLOG=$SUMMARYLOG"]) {
                sh '''#!/bin/bash -x

                    #MASTER_HEAD=$(git show-ref  -s refs/remotes/$RepoName/master)
                    MASTER_HEAD=$(git merge-base FETCH_HEAD remotes/$RepoName/master)
                    echo $MASTER_HEAD
                    files_changed=`git --no-pager diff --name-only FETCH_HEAD $MASTER_HEAD`
                    echo $files_changed 
                    echo "$files_changed:\n" >> $SUMMARYLOG
                    echo "Clang format check....." >> $SUMMARYLOG
                    cp -pv --parents `git --no-pager diff --name-only FETCH_HEAD $(git merge-base FETCH_HEAD remotes/$RepoName/master)` ../diff_dir
                    cd $WORKSPACE
                    $WORKSPACE/$CJE_TF/scripts/run_clang_format.sh $WORKSPACE/diff_dir
                    if [ -s $WORKSPACE/clang_failed.txt ]
                    then
                        echo "Some files are not in clang format"
                        echo "CLANG FORMAT TEST FAILED! Some files are not in clang format:" >> $SUMMARYLOG
                        cat $WORKSPACE/clang_failed.txt >> $SUMMARYLOG
                        echo "\n" >> $SUMMARYLOG
                        find $WORKSPACE/diff_dir -name '*.clang.txt' -exec cp {} $WORKSPACE \\;
                    else
                        echo "CLANG FORMAT TEST PASSED!\n" >> $SUMMARYLOG
                    fi
                '''
                }
            }//dir
        }//stage


        stage('pylint format checking') {
            dir("$WORKSPACE") {
                sh '''#!/bin/bash -x
                    mkdir diff_dir2
                '''
            }
            dir("${REPO_NAME}"){
            withEnv(["CJE_TF=$CJE_TF","RepoName=$REPO_NAME","SUMMARYLOG=$SUMMARYLOG"]) {
                sh '''#!/bin/bash -x
                    #MASTER_HEAD=$(git show-ref -s refs/remotes/$RepoName/master)
                    MASTER_HEAD=$(git merge-base FETCH_HEAD remotes/$RepoName/master)
                    echo $MASTER_HEAD
                    git --no-pager diff --name-only FETCH_HEAD $MASTER_HEAD
                    cp -pv --parents `git --no-pager diff --name-only FETCH_HEAD $(git merge-base FETCH_HEAD remotes/$RepoName/master)` ../diff_dir2
                    cd $WORKSPACE
                    echo "pylint format check....." >> $SUMMARYLOG
                    chmod 777 $WORKSPACE/$CJE_TF/scripts/run_pylint_format.sh
                    $WORKSPACE/$CJE_TF/scripts/run_pylint_format.sh $WORKSPACE/diff_dir2
                    if [ -s $WORKSPACE/pylint_failed.txt ]
                    then
                        echo "Some files are not in python format"
                        echo "\n" >> $SUMMARYLOG
                        echo "PYLINT FORMAT TEST FAILED! Some files are not in Python format:" >> $SUMMARYLOG
                        cat $WORKSPACE/pylint_failed.txt >> $SUMMARYLOG
                        find $WORKSPACE/diff_dir2 -name '*.pylint.txt' -exec cp {} $WORKSPACE \\;
                    else
                        echo "PYLINT FORMAT TEST PASSED!" >> $SUMMARYLOG
                    fi
                '''
                }
            }//dir
        }//stage

    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        // Success or failure, always send notifications
        withEnv(["SUMMARYLOG=$SUMMARYLOG"]) {
            echo SUMMARYLOG
            def msg = readFile SUMMARYLOG

            def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
            notifyBuild(SLACK_CHANNEL, currentBuild.result, msg)

        }

        stage('Archive Artifacts / Test Results') {

            archiveArtifacts artifacts: 'summary.log, *.txt', excludes: null
                                fingerprint: true
        }

        // finally we can set the currentBuild status
        withEnv(["SUMMARYLOG=${SUMMARYLOG}" ]) {
            resp = sh (
                    returnStatus: true,
                    script: '''
                    if [ $(grep 'FAILED' ${SUMMARYLOG} | wc -l) != 0 ]; then
                        exit 1
                    else
                        exit 0
                    fi
                    '''
            )
            echo "resp is $resp"
            if ( resp == 1 ) {
                 currentBuild.result = "FAILED"
            }
            else {
                 currentBuild.result = "SUCCESS"
            }
        }
    }
}
