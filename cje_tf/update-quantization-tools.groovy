/*
WHAT THIS JOB DOES:
After the tensorflow-update-repositories job runs to update private-tensorflow with the upstream, this job
looks at the list of recent commits made to the private-tensorflow repo. The length of time that it looks
back in the repo's commit history is determined by the DIFF_SINCE build parameter. For each commit in the
history, the job looks at the list of files changed in the commit. If any graph transforms files were changed
in the private-tensorflow commit, then it compares the files with the graph transform files in the quantization
tools repo `develop` branch. If any of files in the change list also exist in the quantization tools branch, then
it patches the quantization tools repo with the updates from private-tensorflow. If the merge is successful,
then commit those changes to the `develop` branch to the quantization tools repo. If git is unable to apply
the patch, then the job will fail.
*/

// we want this job to fail if these vals aren't passed
static final String private_tf_repo = params.get('PRIVATE_TENSORFLOW_REPO')
static final String private_tf_branch = params.get('PRIVATE_TENSORFLOW_BRANCH')
static final String tools_repo = params.get('TOOLS_REPO')
static final String tools_branch = params.get('TOOLS_BRANCH')
static final String diff_since = params.get('DIFF_SINCE')

static final String node_label = params.get('NODE_LABEL')

static final String jenkins_git_credentials = "aipgbot-orca"

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
                      branches                         : [[name: private_tf_branch]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'private-tf-dir']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$jenkins_git_credentials",
                                                           url          : "$private_tf_repo"]]])
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: tools_branch]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'tools-dir']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$jenkins_git_credentials",
                                                           url          : "$tools_repo"]]])
        }

        stage('Update files') {

            // allow pushing back to repos using GIT_ASKPASS
            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: jenkins_git_credentials, usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
                sh """#!/bin/bash -x
                set -e

                # get url to push to, need to format it
                TOOLS_REPO_URL=\$(sed -e 's/https:\\/\\/github.com/https:\\/\\/${env.GIT_USERNAME}:${env.GIT_PASSWORD}@github.com/g' <<< $tools_repo)

                pushd private-tf-dir

                # Get the list of commit hashes made in the last day, in reverse so that we get the oldest commit first
                GIT_SHA_LIST=\$(git log --since=${diff_since} --format='%H' --reverse --author gardener@tensorflow.org)
                export IFS="\n"

                # Track files merged and files skipped
                FILES_MERGED=()
                FILES_SKIPPED=()
                SHA_SKIPPED=()

                for git_sha in \$GIT_SHA_LIST; do
                    # Get the specified branch of the tools repo and setup the user info in the config and check the git log
                    pushd \${WORKSPACE}/tools-dir
                    git config --global user.email "intelai@intel.com"
                    git config --global user.name "IntelAI"
                    git fetch \$TOOLS_REPO_URL
                    git checkout ${tools_branch}

                    # Do we already have this commit patched in the tools repo?
                    TOOLS_COMMIT_FOUND=\$(git log --format="%s" --since=${diff_since} | grep \$git_sha | wc -l)
                    echo "Found \${TOOLS_COMMIT_FOUND} commit(s) found in the tools repo for git sha \$git_sha"
                    popd

                    if [ \${TOOLS_COMMIT_FOUND} -gt 0 ]; then
                        echo "Skipping merge of \$git_sha for \$file_changed because it's already been applied"
                        SHA_SKIPPED+=(\$git_sha)
                        continue
                    fi

                    echo "Checking files changed in private-tensorflow commit \$git_sha"

                    # Get the list of file names that were changed in this commit to private-tensorflow
                    export GIT_DIFF=\$(git diff --name-only \$git_sha~1 \$git_sha)
                    export IFS="\n"

                    # Loop through the changed files
                    for file_changed in \$GIT_DIFF; do
                        # We only care about changes to files in the graph_transforms directory
                        if  [[ \$file_changed == tensorflow/tools/graph_transforms/* ]] ;
                        then
                            echo "Found graph transforms file changed: \$file_changed"

                            FILE_BASENAME=\$(basename \$file_changed)
                            TOOLS_FILE_PATH=${WORKSPACE}/tools-dir/tensorflow_quantization/graph_transforms/\$FILE_BASENAME

                            if [ -f "\$TOOLS_FILE_PATH" ]; then
                                # Create patch based on the changed file

                                export PATCH_FILE=\$(git format-patch -1 \$git_sha \$file_changed)
                                export PATCH_FILE_PATH=\$(pwd)/\$PATCH_FILE

                                # Parse out the git SHA from the patch so that we can note it in the commit message to the tools repo
                                IFS=" " SHA_LINE_ARRAY=(\$(head -n 1 \$PATCH_FILE_PATH))

                                # Replace the '/tensorflow/tools/graph_transforms' path in the patch with '/tensorflow_quantization/graph_transforms' in the patch file
                                sed -i -e 's:/tensorflow/tools/graph_transforms:/tensorflow_quantization/graph_transforms:g' \$PATCH_FILE

                                # Apply the patch to the tools repo
                                pushd \${WORKSPACE}/tools-dir

                                git apply \$PATCH_FILE_PATH
                                git add .
                                git status

                                # Commit and push the changes to the tools repo
                                git commit -m "Update \$FILE_BASENAME from private-tensorflow \$git_sha"
                                git push \$TOOLS_REPO_URL ${tools_branch}
                                FILES_MERGED+=(\$FILE_BASENAME)

                                popd
                            else
                                echo "No matching file found in the quantization tools at: \$TOOLS_FILE_PATH"
                                echo "Skipping merge of \$FILE_BASENAME"
                                FILES_SKIPPED+=(\$FILE_BASENAME)
                            fi
                        fi
                    done
                done

                echo "----------------------------------------------------------------------------"
                echo "SUMMARY:"
                echo "File changes merged to the tools repo for: \${FILES_MERGED[@]}"
                echo "Files skipped because they don't exist in the tools repo: \${FILES_SKIPPED[@]}"
                echo "Skipped the following commits since they have already been merged to the tools repo: \${SHA_SKIPPED[@]}"
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
