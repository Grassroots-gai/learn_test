/*
Rebases `quantization-develop` branch of https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow with https://github.com/tensorflow/tensorflow.
Job runs nightly when the TF update repository updates complete.
Failure notification is sent to @karthik on slack in #tensorflow-oob.
This is needed for SDL.
*/

static final String node_label = params.get('NODE_LABEL')
static final String tf_version = params.get('TF_VERSION', 'master')
static final String slack_channel = params.get('SLACK_CHANNEL', '#tensorflow-oob')
static final String private_tensorflow_branch = params.get('PRIVATE_TENSORFLOW_BRANCH', 'quantization-develop')

static final String jenkins_git_credentials = "aipgbot-orca"
static final String jenkins_git_credentials_lab = "lab_tfbot"
static final String private_tensorflow_url = "https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow"

node(node_label) {

    try {
        stage('CleanUp') {

            sh '''#!/bin/bash -x
                cd $WORKSPACE
                sudo rm -rf *
            '''
        }

        // pull the cje-tf
        dir( 'cje-tf' ) {
            checkout scm
        }

        stage('Checkout') {

            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: "$private_tensorflow_branch"]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'private-tensorflow']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$jenkins_git_credentials_lab",
                                                           url          : "$private_tensorflow_url"]]])
        }

        stage('Rebase') {
            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: jenkins_git_credentials, usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
                sh """#!/bin/bash -x
                set -e

                PRIVATE_TENSORFLOW_REPO_URL=\$(sed -e 's/https:\\/\\/gitlab.devtools.intel.com/https:\\/\\/${env.GIT_USERNAME}:${env.GIT_PASSWORD}@gitlab.devtools.intel.com/g' <<< $private_tensorflow_url)

                pushd private-tensorflow
                git checkout ${private_tensorflow_branch}

                # google removed support for quantization so we need to get our folder back
                echo '* merge=ours' > tensorflow/tools/quantization/.gitattributes
                echo '* merge=ours' > tensorflow/tools/graph_transforms/.gitattributes
                git config merge.ours.driver true

                # we don't care about keeping commit history
                # git rebase origin/${tf_version}
                git pull \$PRIVATE_TENSORFLOW_REPO_URL ${tf_version}

                git add tensorflow/tools/quantization/.gitattributes tensorflow/tools/graph_transforms/.gitattributes
                git push \$PRIVATE_TENSORFLOW_REPO_URL ${private_tensorflow_branch}
                popd
                """
            }
        }

    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILURE"
        throw e
    } finally {
        def slackNotify = load("cje-tf/common/slackNotification.groovy")
        if ( currentBuild.result == "FAILURE" )
            msg = ":noooooooo: @karthik :sad:"
        else
            msg = ":woo: @karthik :yay:"
        slackNotify(slack_channel, currentBuild.result, msg)
    } // finally
}
