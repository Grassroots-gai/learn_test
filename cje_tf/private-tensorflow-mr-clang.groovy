CJE_TF='cje-tf'
CJE_TF_COMMON_DIR = "$CJE_TF/common"

GIT_CREDENTIAL = "lab_tfbot"
GIT_CREDENTIAL_LAB = "lab_tfbot"

http_proxy="http://proxy-us.intel.com:911"
https_proxy="https://proxy-us.intel.com:912"

// set default value for NODE_LABEL
NODE_LABEL = 'aipg-ra-bdw-03.ra.intel.com'
if ('NODE_LABEL' in params && params.NODE_LABEL != '') {
    NODE_LABEL = params.NODE_LABEL
}
echo "NODE_LABEL: $NODE_LABEL"

MR_NUMBER = ''
if ('MR_NUMBER' in params && params.MR_NUMBER != '') {
    MR_NUMBER = params.MR_NUMBER
}
echo "MR_NUMBER: $MR_NUMBER"

MR_ID = ''
if ('MR_ID' in params && params.ID != '') {
    MR_ID = params.MR_ID
}
echo "MR_ID: $MR_ID"

GIT_URL = ''
if ('GIT_URL' in params && params.GIT_URL != '') {
    GIT_URL = params.GIT_URL
}
echo "GIT_URL: $GIT_URL"

MR_SOURCE_BRANCH = ''
if ('MR_SOURCE_BRANCH' in params && params.MR_SOURCE_BRANCH != '') {
    MR_SOURCE_BRANCH = params.MR_SOURCE_BRANCH
}
echo "MR_SOURCE_BRANCH: $MR_SOURCE_BRANCH"

MR_MERGE_BRANCH = ''
if ('MR_MERGE_BRANCH' in params && params.MR_MERGE_BRANCH != '') {
    MR_MERGE_BRANCH = params.MR_MERGE_BRANCH
}
echo "MR_MERGE_BRANCH: $MR_MERGE_BRANCH"

GIT_NAME = ''
if ('GIT_NAME' in params && params.GIT_NAME != '') {
    GIT_NAME = params.GIT_NAME
}
echo "GIT_NAME: $GIT_NAME"

//def splitURL = GIT_URL.split('/')
//String REPO_NAME = splitURL[splitURL.size() - 1]
//echo "RepoDirectory:$REPO_NAME"

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
        withEnv(["Number=${MR_NUMBER}","RepoName=${GIT_NAME}","Id=${MR_ID}"]) {
            sh '''#!/bin/bash -x
                echo "*********************************************************************" > ./summary.log
                echo "gitlab Merge Request test clangformat and Pylintformat summary" >> ./summary.log
                echo "Repository: $RepoName " >> ./summary.log
                echo "Git Merge Request ID: ${Id}" >> ./summary.log
                echo "Git Merge Request NUMBER: ${Number}" >> ./summary.log
                echo "*********************************************************************\n" >> ./summary.log
                echo "MR scanning filelist:" >> ./summary.log
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

            checkout([$class                        : 'GitSCM',
                branches                            : [[name: "$MR_SOURCE_BRANCH"]],
                doGenerateSubmoduleConfigurations   : false,
                extensions                          : [
                                                        [$class: 'PreBuildMerge', 
                                                            options: [
                                                                    fastForwardMode: 'FF', 
                                                                    mergeRemote: "$GIT_NAME", 
                                                                    mergeStrategy: 'DEFAULT', 
                                                                    mergeTarget: "$MR_MERGE_BRANCH"
                                                            ]
                                                        ],
                                                        [$class: 'RelativeTargetDirectory',
                                                        relativeTargetDir: "$GIT_NAME"
                                                        ]
                                                      ],
                submoduleCfg                        : [],
                userRemoteConfigs                   : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                       name: GIT_NAME,
                                                       url: "$GIT_URL"]]
                ])
        }  //stage

        stage('clang format checking') {
            dir("$WORKSPACE") {
                sh '''#!/bin/bash -x
                    mkdir diff_dir
                '''
            }
            dir("${GIT_NAME}"){
                withEnv(["CJE_TF=$CJE_TF","RepoName=$GIT_NAME", "SUMMARYLOG=$SUMMARYLOG"]) {
                sh '''#!/bin/bash -x

                    MASTER_HEAD=$(git show-ref -s refs/remotes/$RepoName/master)
                    echo $MASTER_HEAD
                    files_changed=`git --no-pager diff --name-only $MASTER_HEAD`
                    echo $files_changed 
                    echo "$files_changed:\n" >> $SUMMARYLOG
                    echo "Clang format check....." >> $SUMMARYLOG
                    cp -pv --parents `git --no-pager diff --name-only $(git show-ref -s remotes/$RepoName/master)` ../diff_dir
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
            } //dir
        } //stage


        stage('pylint format checking') {
            dir("$WORKSPACE") {
                sh '''#!/bin/bash -x
                    mkdir diff_dir2
                '''
            }
            dir("${GIT_NAME}"){
            withEnv(["CJE_TF=$CJE_TF","RepoName=$GIT_NAME","SUMMARYLOG=$SUMMARYLOG"]) {
                sh '''#!/bin/bash -x
                    MASTER_HEAD=$(git show-ref -s refs/remotes/$RepoName/master)
                    echo $MASTER_HEAD
                    git --no-pager diff --name-only $MASTER_HEAD
                    cp -pv --parents `git --no-pager diff --name-only $(git show-ref remotes/$RepoName/master)` ../diff_dir2
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
        } //stage

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
