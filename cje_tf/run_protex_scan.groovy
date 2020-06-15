/*
This job uses the blackduck client that was put in artifactory manually (by Steven)
https://gkipscn01.intel.com/protex/?uifsid=1#0=dW,go,fh,gc,gL,fI is where I got it from, March 2019
If needed, the jfrog cli can be found here: curl -fL https://getcli.jfrog.io | sh

By default it will scan the master branch of https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow every weekend
*/

static final String node_label = params.get('NODE_LABEL', 'skx' || 'bdw')

// this cred has to have access to whatever repo you want to scan
//static final String jenkins_git_credential = params.get('JENKINS_GIT_CREDENTIAL', 'aipgbot-orca')
static final String jenkins_git_credential = params.get('JENKINS_GIT_CREDENTIAL', 'lab_tfbot')
static final String repo_to_scan = params.get('REPO_TO_SCAN', "https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow")
static final String repo_branch_to_scan = params.get('REPO_BRANCH_TO_SCAN', "master")
static final String security_scan_branch = params.get('SECURITY_SCAN_BRANCH', "master")

static final String protex_server = params.get('PROTEX_SERVER', "https://gkipscn01.intel.com")
// will generate random 9 char project name if none passed in
static final String protex_project_name = params.get('PROTEX_PROJECT_NAME', org.apache.commons.lang.RandomStringUtils.random(9, true, true))
// this needs to come from the "credentials" section of Jenkins, has to have access to PROTEX_SERVER
static final String protex_creds = params.get('PROTEX_CREDS', "protex_creds_for_gkipscn01")


node(node_label) {
    try {
        stage('CleanUp') {
            sh '''#!/bin/bash -x
                cd $WORKSPACE
                sudo rm -rf *
                sudo rm -rf .??* || true
            '''
        }

        dir( 'cje-tf' ) {
            checkout scm
        }

        stage('Checkout') {
            checkout([$class: 'GitSCM',
                      branches: [[name: repo_branch_to_scan]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: 'repo-to-scan']],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: jenkins_git_credential,
                                           url: repo_to_scan]]])

            // where code to run dynamic scans lives
            checkout([$class: 'GitSCM',
                      branches: [[name: security_scan_branch]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: 'security-scans']],
                      submoduleCfg: [],
                      userRemoteConfigs: [[url: "https://gitlab.devtools.intel.com/Intel-Common/QA/code-scan-tools.git"]]])
        }
    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    }
    
    stage("Run scan and parse output") {
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: protex_creds, usernameVariable: 'PROTEX_USERNAME', passwordVariable: 'PROTEX_PASSWORD']]) {
            sh """#!/bin/bash -x
                cd security-scans/protex
                set +e
                MATCH_EXISTING_PROJECT=true ./run_protex_scan.sh ../../repo-to-scan $protex_server ${env.PROTEX_USERNAME} ${env.PROTEX_PASSWORD} $protex_project_name report-result.xml | tee output.txt
                set -e
            """

            // parse scan output and return as build result
            currentBuild.result = sh(script: """#!/bin/bash
                # FAILED if project not found
                # UNSTABLE if files pending identification
                # SUCCESS otherwise

                cd security-scans/protex

                if [[ \$(cat output.txt | grep "Project $protex_project_name not found!") ]]; then
                  printf "FAILED"
                elif [[ \$(cat output.txt | awk '/Files pending identification:/ {print \$4}') != "" ]] && \
                  [[ \$(cat output.txt | awk '/Files pending identification:/ {print \$4}') != "0" ]]; then
                  printf "UNSTABLE"
                elif [[ \$(cat output.txt | grep "Files scanned successfully") ]]; then
                  printf "SUCCESS"
                else
                  printf "FAILED"
                fi
            """, returnStdout: true).trim()
        }
    }
}
