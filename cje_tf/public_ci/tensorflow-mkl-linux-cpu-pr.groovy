/*
This job:
1. Clones tensorflow/tensorflow at a given commit
2. Calls the 'tensorflow/tools/ci_build/linux/mkl/basic-mkl-test.sh' script

This job accepts params:
- String Type :: CHECKOUT_BRANCH :: A string branch name to clone tensorflow/tensorflow from
*/

static final ArrayList buildLabels = [ 'bdw' /*, 'skx'*/ ]
static final String buildScript = 'tensorflow/tools/ci_build/linux/mkl/basic-mkl-test.sh'
static final String branchName = params.get('CHECKOUT_BRANCH', 'master')

 parallelBuild = [:]
buildLabels.each { l ->
    parallelBuild += [
        "${l}": {
            node(l) {

                try {

                    stage('Checkout') {
                        deleteDir()
                        checkout([$class: 'GitSCM', branches: [[name: branchName]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '5d094940-c7a0-42a8-8417-1ecc9a7bd947', url: 'https://github.com/tensorflow/tensorflow', refspec: '+refs/pull/*/head:refs/remotes/origin/pull/*']]])
                    }

                    stage('Build') {
                        sh buildScript
                    }

                } catch (e) {

                    // If there was an exception thrown, the build failed
                    currentBuild.result = "FAILED"
                    throw e

                } finally {
                    
                    echo 

                    /*stage('Notify') {
                        //slackSend ...
                        String slack_channel = '#ci-fixes-dungeon'
                        buildStatus =  currentBuild.result ?: 'SUCCESSFUL'
                        String summary = "Job &lt;${env.BUILD_URL}|${env.JOB_NAME} #${env.BUILD_NUMBER}> on ${env.NODE_NAME} : ${buildStatus}"

                        Integer SLACK_RETRY_TIMES = 2
                        Map SLACK_COLOR_STRINGS = [
                            'SUCCESS': 'good',
                            'UNSTABLE': 'warning',
                            'FAILURE': 'danger'
                        ]

                         // Send notifications
                         retry(SLACK_RETRY_TIMES) {
                           slackSend channel: slack_channel, message: summary, color: SLACK_COLOR_STRINGS[currentBuild.currentResult]
                         }

                        //ns// AR Nick to add in slack configuration, #ci-fixes-dungeon
                    }*/

                }
           }
        }
    ]
}
parallel parallelBuild

