// Groovy scripts
updateGitlabCommitStatus state: 'pending'
gitLabConnection('gitlab.devtools.intel.com')
gitlab_credential = "f4bb679e-e69f-4738-9a4d-a67630e89aaf"

// setting node_label

node_label = "test_mr_8280"
if ('node_label' in params && params.node_label != '') {
    node_label = params.node_label
}
echo "Running on node ${node_label}"


// -- Start -- //
node(node_label) {
    deleteDir()
    stage("Downloads") {
        // download
        dir(WORKSPACE) {
            checkout scm
            TOOL_REPO="https://gitlab.devtools.intel.com/guomingz/perf_projection_tool.git"
            checkout changelog: true, poll: true, scm:[
                    $class: 'GitSCM',
                    branches: [[name: "origin/${gitlabSourceBranch}"]],
                    browser: [$class: 'AssemblaWeb', repoUrl: ''],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [
                            [$class: 'RelativeTargetDirectory',relativeTargetDir: "perf_projection_tool"],
                            [$class: 'CloneOption', timeout: 60],
                            [$class: 'PreBuildMerge', options: [fastForwardMode: 'FF', mergeRemote: 'origin', mergeStrategy: 'DEFAULT', mergeTarget: "${env.gitlabTargetBranch}"]]
                    ],
                    submoduleCfg: [],
                    userRemoteConfigs: [
                            [credentialsId: "${gitlab_credential}",
                             url: "${TOOL_REPO}"]
                    ]
            ]
        }
    }
    stage("Test"){
        dir(WORKSPACE) {
            withEnv(["CONDA_BIN_PATH=${CONDA_BIN_PATH}","TF_VERSION=${TF_VERSION}"]){
                sh'''#!/bin/bash -x
                    export PATH="${CONDA_BIN_PATH}:$PATH"
                    ENV_NAME="mr_test"          
                    source ${CONDA_BIN_PATH}/activate ${ENV_NAME}
                    if [ $? -ne 0 ]; then
                        attempts=0
                        until [[ "$attempts" -ge 3 ]]
                        do
                            conda create python=3.6.9 -y -n ${ENV_NAME} && break
                            attempts=$[$attempts+1]
                            sleep 5
                        done																	
                        source activate ${ENV_NAME}
                        attempts1=0
                        until [[ "$attempts1" -ge 3 ]]
                        do
                            pip install --no-cache-dir ${TF_VERSION} openpyxl pyyaml && break
                            attempts1=$[$attempts1+1]
                            sleep 5
                        done							
                    fi
                    pwd
                    ls
                    cd perf_projection_tool
                    export PYTHONPATH=$(pwd)
                    mkdir test_log
                    cd unittests
                    python runtest.py |& tee ../../all_test.log
                    cp -r ../test_log $WORKSPACE/
                '''
                TEST_STATUS=""
                FAILED_NUMBER = sh( script: 'cat $WORKSPACE/all_test.log | grep "FAILED" | wc -l', returnStdout: true ).trim()
                int FAILED_NUMBER = Integer.parseInt(FAILED_NUMBER)
                if(FAILED_NUMBER >= 1){
                    TEST_STATUS="FAILED"
                }else{
                    TEST_STATUS="SUCCESS"
                }
            }
        }
    }
    stage("Report"){
        dir("$WORKSPACE") {
            withEnv(["TEST_STATUS=${TEST_STATUS}","MR_branch=${gitlabSourceBranch}", "Target_branch=${env.gitlabTargetBranch}",
                     "MergeRequestIid=${gitlabMergeRequestIid}", "gitlabSourceRepoHomepage=${gitlabSourceRepoHomepage}",
                     "gitlabMergeRequestLastCommit=${gitlabMergeRequestLastCommit}", "gitlabUserEmail=${gitlabUserEmail}",
                     "gitlabMergeRequestTitle=${gitlabMergeRequestTitle}", "TF_VERSION=${TF_VERSION}", "TOOL_REPO=${TOOL_REPO}",
                     "BUILDNUM=${currentBuild.number}"]){
                sh'''#!/bin/bash -x
                    python perf_projection_tool_mr_test/write_html.py -w $WORKSPACE -ts $TEST_STATUS -bu $BUILD_URL \
                    -mb $MR_branch -tb $Target_branch -c $gitlabMergeRequestLastCommit -tv "$TF_VERSION" \
                    -mi $gitlabMergeRequestIid -ue $gitlabUserEmail -mt "$gitlabMergeRequestTitle" \
                    -rp $gitlabSourceRepoHomepage -tr $TOOL_REPO -bn $BUILDNUM
                '''
            }
        }
    }
    stage("Artifacts") {
        dir("$WORKSPACE") {
            recipient_list = params.recipient_list + ',' + gitlabUserEmail
            emailext subject: "Tensorflow MR Test Report",
                to: "${recipient_list}",
                replyTo: "${recipient_list}",
                body: '''${FILE,path="Tensorflow_MR_Test_report.html"}''',
                attachmentsPattern: "Tensorflow_MR_Test_report.html",
                mimeType: 'text/html'
            archiveArtifacts artifacts: 'test_log/**,all_test.log', excludes: null
            fingerprint: true
        }
        updateGitlabCommitStatus state: 'success'
    }
}
