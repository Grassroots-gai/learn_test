/*
WHAT THIS JOB DOES:
Takes a branch of 1 repo and merges it in with another repo.
Defaults to running nightly for the IntelAI/models and gitlab.devtools.intel.com/intelai/models repos.
*/

// we want this job to fail if these vals aren't passed
static final String source_repo = params.get('SOURCE_REPO')
static final String source_branch = params.get('SOURCE_BRANCH')
static final String destination_repo = params.get('DESTINATION_REPO')

static final String node_label = params.get('NODE_LABEL')

static final String jenkins_git_credentials = "aipgbot-orca"
static final String jenkins_git_credentials_lab = "lab_tfbot"

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
                      branches                         : [[name: source_branch]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'source-repo-dir']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$jenkins_git_credentials",
                                                           url          : "$source_repo"]]])
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: source_branch]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'destination-repo-dir']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$jenkins_git_credentials_lab",
                                                           url          : "$destination_repo"]]])
        }

        stage('Merge branch') {

            // allow pushing back to repos using GIT_ASKPASS
            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: jenkins_git_credentials, usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
                sh """#!/bin/bash -x
                set -e

                # get url to push to, need to format it
                DESTINATION_REPO_URL=\$(sed -e 's/https:\\/\\/github.com/https:\\/\\/${env.GIT_USERNAME}:${env.GIT_PASSWORD}@github.com/g' <<< $destination_repo)

                pushd destination-repo-dir

                # add source-repo-dir to destination-repo-dir
                git remote add source-repo-dir ${source_repo}

                # enables git fetch, since git fetch url_here doesn't seem to be working
                command -v expect > /dev/null || (sudo apt-get install -y expect || sudo yum install -y expect)
                # expect will work if private repo, otherwise do normal git fetch
                # can't figure out how to handle expect fail because of the END used, so handle error another way
                set +e
                expect <<END
set timeout -1
spawn git fetch source-repo-dir
expect "Username for 'https://github.com':"
sleep 1
send "${env.GIT_USERNAME}\r"
expect "Password for 'https://${env.GIT_USERNAME}@github.com':"
sleep 1
send "${env.GIT_PASSWORD}\r"
expect eof
END
                if [[ '\$?' == "1" ]]; then
                    git fetch source-repo-dir
                fi
                set -e

                git checkout --track origin/${source_branch}
                git merge source-repo-dir/${source_branch}
                git push \$DESTINATION_REPO_URL origin/${source_branch}
                """
            }

        }
    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    } finally {

    } // finally
}
