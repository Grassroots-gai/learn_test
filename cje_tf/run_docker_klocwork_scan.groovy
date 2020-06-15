CJE_TF_COMMON_DIR = "cje-tf/common"

KW_LICENSE_SERVER = 'klocwork02p.elic.intel.com:7500'

GIT_CREDENTIAL = 'aipgbot-orca'
GIT_CREDENTIAL_LAB = "lab_tfbot"

HTTP_PROXY = 'http://proxy-us.intel.com:911'
HTTPS_PROXY = 'https://proxy-us.intel.com:912'

// DO NOT RUN THIS ON AIPG-FM-SKX* NODES OR YOU WILL GET "Unable to establish SSL connection." ERROR
static final String node_label = params.get('NODE_LABEL', 'skx')
static final String repo_to_scan = params.get('REPO_TO_SCAN', 'https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git')
static final String repo_branch = params.get('REPO_BRANCH', 'master')
static final String security_scan_branch = params.get('SECURITY_SCAN_BRANCH', 'master')
// tf-common only used if tensorflow repo is being scanned
static final String tensorflow_common_branch = params.get('TENSORFLOW_COMMON_BRANCH', 'master')
static final String docker_image = params.get('DOCKER_IMAGE', 'amr-registry.caas.intel.com/aipg-tf/qa:nightly-master-avx2-devel-mkl')
static final String slack_channel = params.get('SLACK_CHANNEL', '#tensorflow-jenkins')
static final String kw_server_url = params.get('KW_SERVER_URL', 'https://klocwork-ir2.devtools.intel.com:8080')
static final String summary_title = params.get('SUMMARY_TITLE', 'Tensorflow Klocwork scan summary')
static final String run_type = params.get('RUN_TYPE', 'mkldnn')
// need to have a klockwork project created that is of the same name
static final String project_name = params.get('PROJECT_NAME', 'private-tensorflow')
static final String kw_version = params.get('KW_VERSION', '12.3')

node(node_label) {
    try {

        SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                          returnStdout: true).trim()
        SUMMARYLOG = "${WORKSPACE}/summary_Klocwork_scan_${SERVERNAME}.log"
        sh (script:"touch $SUMMARYLOG", returnStdout: true).trim()
        // get owner of workspace dir so can change back later
        OLDUSER = sh (script:"cd $WORKSPACE && (ls -l . | awk 'FNR==2{print \$3}')", returnStdout: true).trim()

        echo SUMMARYLOG

        stage('cleanup') {
            sh '''#!/bin/bash -x
                cd $WORKSPACE
                sudo rm -rf *
                docker stop $(docker ps -a -q)
                echo Y | docker system prune -a
            '''
        }

        // pull the cje-tf for slack script
        dir('cje-tf') {
            checkout scm
        }

        // slack notification
        def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
        notifyBuild(slack_channel, 'STARTED', '')

        stage('Checkout') {
            // repo to scan
            checkout([$class: 'GitSCM',
                      branches: [[name: repo_branch]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: 'repo-to-scan']],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: GIT_CREDENTIAL_LAB,
                                           url: repo_to_scan]]])

            // where code to run scans lives
            checkout([$class: 'GitSCM',
                      branches: [[name: security_scan_branch]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: 'security-scans']],
                      submoduleCfg: [],
                      userRemoteConfigs: [[url: "https://gitlab.devtools.intel.com/Intel-Common/QA/code-scan-tools.git"]]])

            // only need this to set vars to build tensorflow with if we're scanning that
            SET_TENSORFLOW_COMMON_ARGS = false
            String[] TF_REPOS = [
                "https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow",
                "https://github.com/Intel-tensorflow/tensorflow",
                "https://github.com/tensorflow/tensorflow",
                "https://gitlab.devtools.intel.com/intelai/tools"
            ]

            if (TF_REPOS.contains(repo_to_scan)) {
                SET_TENSORFLOW_COMMON_ARGS = true
                // tensorflow-common
                checkout([$class: 'GitSCM',
                          branches: [[name: tensorflow_common_branch]],
                          browser: [$class: 'AssemblaWeb', repoUrl: ''],
                          doGenerateSubmoduleConfigurations: false,
                          extensions: [[$class: 'RelativeTargetDirectory',
                                        relativeTargetDir: 'tensorflow-common']],
                          submoduleCfg: [],
                          userRemoteConfigs: [[credentialsId: GIT_CREDENTIAL_LAB,
                                               url: 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-common.git']]])
            }
        } // stage Checkout

        try {
          stage('Run Klocwork Scan') {
              sh """#!/bin/bash -xe
                cp $WORKSPACE/cje-tf/kw-authorization/ltoken .
              """

              // Run docker container as root, but then switch out of root when running scan
              docker.image("$docker_image").inside("--env \"HTTP_PROXY=$HTTP_PROXY\" \
                                                --env \"HTTPS_PROXY=$HTTPS_PROXY\" \
                                                --env KW_SERVER_URL=$KW_SERVER_URL \
                                                --env KW_LICENSE_SERVER=$KW_LICENSE_SERVER \
                                                --env KW_VERSION=$kw_version \
                                                --env KW_PROJECT=$project_name \
                                                --env SET_TENSORFLOW_COMMON_ARGS=$SET_TENSORFLOW_COMMON_ARGS \
                                                -u root:root") {
                sh """#!/bin/bash -xe

                # going inside workspace this way because of note above. Docker.inside auto-mounts the workspace
                cd $WORKSPACE/security-scans/klocwork
                # cd /workspace/security-scans/klocwork
                ./install_klocwork_dependencies.sh

                # create new user that isn't root for klocwork scan
                groupadd -g 999 klocworkuser && useradd -r -u 999 -g klocworkuser klocworkuser

                # make this new user the owner of the workspace folder, will change back later
                chown -R 999 $WORKSPACE
                # set up new user's home dir, it's not done for them:
                # FATAL: mkdir('/home/klocworkuser/.cache/bazel/_bazel_klocworkuser'): (error: 13): Permission denied
                mkdir -p /home/klocworkuser
                chown 999 /home/klocworkuser

                # docker requires su to be ran this way rather than separate command
                su klocworkuser -c "./run_klocwork_scan.sh"
                """
              }
          }
        } finally {
            sh """ #!/bin/bash -xe
            # change workspace folder back to original owner for log collection
            sudo chown -R $OLDUSER $WORKSPACE
            """
        }
    } catch (e) {

        // If there was an exception thrown, the build failed
        echo 'Exeption occurs: ' + e.toString()
        currentBuild.result = "FAILED"
        throw e

    } finally {

        stage('Collect Logs') {

            echo "----- stage Collect Logs -----"

            try {

                // Prepare logs
                def prepareLog = load("${CJE_TF_COMMON_DIR}/prepareLog.groovy")
                prepareLog(repo_branch, 'repo-to-scan', run_type, SUMMARYLOG, summary_title)


                withEnv(["SUMMARYLOG=$SUMMARYLOG", "kw_server_url=$kw_server_url"]) {
                    sh """#!/bin/bash -xe

                    kwbuildproject_log="kwbuildproject-log.txt"
                    kwadmin_log="kwadmin-load-log.txt"
                    if [ -f \$kwadmin_log ] &&  [ "\$(grep 'Build successfully created' \$kwadmin_log | wc -l)" = "1" ] ; then

                        echo 'Klocwork scan successfully completed' >> ${SUMMARYLOG}
                        echo "Please check the detailed report at $kw_server_url" >> ${SUMMARYLOG}

                    else
                        echo 'Klocwork scan failed' >> ${SUMMARYLOG}
                        echo "Please check the logfiles \$kwbuildproject_log, \$kwadmin_log for detailed information " >> ${SUMMARYLOG}
                        exit 1
                    fi
                    """
                 } // withEnv

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
            notifyBuild(slack_channel, currentBuild.result, msg)

        }

        stage('Archive Artifacts ') {

                archiveArtifacts artifacts: '*.log, *.txt', excludes: null
                fingerprint: true

        }
    }
}
