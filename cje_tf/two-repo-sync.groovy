/*
WHAT THIS JOB DOES:
'SRC_BRANCH == 'source branch' -- branch of SOURCE_REPO to make release from
* Take code from 'SRC_BRANCH', merge to master branch on SOURCE_REPO
* Tag previous merge with 'Release Tag' in SOURCE_REPO master
* Take code from 'SRC_BRANCH' and merge to develop branch
* Take code master branch and merge with DESTINATION_REPO and tag with 'Release Tag'
*/

// we want this job to fail if these vals aren't passed
static final String source_repo = params.get('SOURCE_REPO')
static final String source_branch = params.get('SOURCE_BRANCH')
static final String destination_repo = params.get('DESTINATION_REPO')
static final String release_tag = params.get('RELEASE_TAG')
static final String release_notes = params.get('RELEASE_NOTES')

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
                      branches                         : [[name: source_branch]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'source-repo-dir']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$jenkins_git_credentials",
                                                           url          : "$source_repo"]]])
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: "master"]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'destination-repo-dir']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$jenkins_git_credentials",
                                                           url          : "$destination_repo"]]])
        }

        stage('Update git') {
            // we need to have git > 2.0 in order to use new --allow-unrelated-histories flag
            // technically that functionality used to exist so we could try and run with and without
            // the flag but it'd be better to have the same git version everywhere instead
            sh """#/bin/bash -x
            set -e

            # check if we have a good git version
            if [[ "`git --version`" != "git version 2."* ]]; then
                # yum doesn't have the latest git version, so we'll go with dnf
                sudo apt install -y git-all || \
                    (sudo yum clean all && sudo yum update -y &&
                     sudo yum remove -y git &&
                     sudo yum install -y https://centos7.iuscommunity.org/ius-release.rpm &&
                     sudo yum install -y git2u)
            fi
            """
        }

        stage('Merge code') {

            // allow pushing back to repos using GIT_ASKPASS
            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: jenkins_git_credentials, usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
                sh """#!/bin/bash -x
                set -e

                # get url to push to, need to format it
                SOURCE_REPO_URL=\$(sed -e 's/https:\\/\\/github.com/https:\\/\\/${env.GIT_USERNAME}:${env.GIT_PASSWORD}@github.com/g' <<< $source_repo)
                DESTINATION_REPO_URL=\$(sed -e 's/https:\\/\\/github.com/https:\\/\\/${env.GIT_USERNAME}:${env.GIT_PASSWORD}@github.com/g' <<< $destination_repo)
                # also get name of source and target repos and owner for use with github api (tagging with markdown support)
                # getting this from value passed in makes it easier to develop using dummy repos
                IFS='/.' read -r -a SOURCE_REPO_VALS <<< "$source_repo"
                IFS='/.' read -r -a DESTINATION_REPO_VALS <<< "$destination_repo"
                SOURCE_REPO_OWNER=\${SOURCE_REPO_VALS[4]}
                SOURCE_REPO_NAME=\${SOURCE_REPO_VALS[5]}
                DESTINATION_REPO_OWNER=\${DESTINATION_REPO_VALS[4]}
                DESTINATION_REPO_NAME=\${DESTINATION_REPO_VALS[5]}

                # generate git tag post data
                # note that json doesn't support multiline, so I replace all newlines with their escape sequence 
                # TODO: Allow entering newlines. Right now it breaks everything so we expect a string with \\n in it
                # THEORY: do the line below in groovy itself, and _then_ pass it to this shell block. Then it won't be newlines possibly
                # release_notes_formatted="'""\$(sed ':a;N;\$!ba;s/\\n/\\\\n/g' <<< "${release_notes}")""'"
                # echo '\$'"'"'{"tag_name": "'${release_tag}'", "target_commitish": "'${source_branch}'", "name": "'${release_tag}'", "body": "'\$release_notes_formatted'", "draft": false, "prerelease": false}'"'" > /tmp/tag_data.txt
                tag_data='{\"tag_name\": \"${release_tag}\", \"target_commitish\": \"${source_branch}\", \"name\": \"${release_tag}\", \"body\": \"${release_notes}\", \"draft\": false, \"prerelease\": false}'

                pushd source-repo-dir

                ### * Take code from 'SRC_BRANCH', merge to master branch
                git checkout master
                git pull \$SOURCE_REPO_URL master
                
                # if this fails with conflicts we need to manually resolve.
                # it means we have changes in `master` that aren't in `develop` in the same git history
                git checkout master
                git merge origin/${source_branch}
                git push \$SOURCE_REPO_URL master

                ### * Tag previous merge with 'Release Tag' in source-repo-dir master

                ### * Take code from 'SRC_BRANCH' and merge to develop branch in case hotfixes were done there too
                git checkout develop
                git pull \$SOURCE_REPO_URL develop

                source_branch_into_develop=""
                # merge 'SRC_BRANCH' into it -- if branch is master this won't do anything which is ok
                # NOTE: this could fail, but that's ok. We'll make a note of it for user at end
                (git checkout develop &&
                    git merge ${source_branch} &&
                    git push \$SOURCE_REPO_URL develop) || \
                (git reset --hard origin/develop &&
                    source_branch_into_develop="Unable to merge ${source_branch} into develop due to conflicts. Please resolve these manually.")


                # tag 'SRC_BRANCH' with release_tag and push
                git checkout ${source_branch}
                
                # TODO: don't delete tag
                # delete old tag just in case it exists
                git push --delete \$SOURCE_REPO_URL ${release_tag} || true
                git tag --delete ${release_tag} || true

                # need to use github api to make tag to support markdown, see https://github.community/t5/How-to-use-Git-and-GitHub/Markdown-in-quot-git-tag-quot-message-not-rendered-in-Release/td-p/13478
                curl -u ${env.GIT_USERNAME}:${env.GIT_PASSWORD} --header "Content-Type: application/json" -X POST \
                    --data "\$tag_data" https://api.github.com/repos/\$SOURCE_REPO_OWNER/\$SOURCE_REPO_NAME/releases > /dev/null

                # git tag ${release_tag} -m "$release_notes"
                # git push \$SOURCE_REPO_URL --tags


                # done with source-repo-dir for now
                popd

                ### * Take code master branch and merge with https://IntelAI/destination-repo-dir and tag with 'Release Tag'
                pushd destination-repo-dir

                # configure destination-repo-dir info
                git config --global user.email "intelai@intel.com"
                git config --global user.name "IntelAI"

                # enables git fetch, since git fetch url_here doesn't seem to be working
                command -v expect > /dev/null || (sudo apt-get install -y expect || sudo yum install -y expect)

                # add source-repo-dir to destination-repo-dir
                git remote add source-repo-dir ${source_repo}
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

                # check out branch with same name as 'SRC_BRANCH' on destination-repo-dir
                git checkout -b ${source_branch} --track source-repo-dir/${source_branch}
                # make sure that release branch here is up to date
                git pull \$DESTINATION_REPO_URL ${source_branch} || true
                git merge -X theirs --allow-unrelated-histories --squash -m "$release_notes" source-repo-dir/master
                # since we are allowing unrelated histories we need to force update
                git push \$DESTINATION_REPO_URL ${source_branch} --force

                # tag new release branch with release_tag and push, same as with prior repo
                
                # TODO: don't delete tag
                # delete old tag just in case it exists
                git push --delete \$DESTINATION_REPO_URL ${release_tag} || true
                git tag --delete ${release_tag} || true
                

                # need to use github api to make tag to support markdown, see https://github.community/t5/How-to-use-Git-and-GitHub/Markdown-in-quot-git-tag-quot-message-not-rendered-in-Release/td-p/13478
                curl -u ${env.GIT_USERNAME}:${env.GIT_PASSWORD} --header "Content-Type: application/json" -X POST \
                    --data "\$tag_data" https://api.github.com/repos/\$DESTINATION_REPO_OWNER/\$DESTINATION_REPO_NAME/releases > /dev/null

                # git tag ${release_tag} -m "$release_notes"
                # git push \$DESTINATION_REPO_URL --tags

                # update master of destination-repo-dir with new release branch
                set +e
                expect <<END
set timeout -1
spawn git fetch origin
expect "Username for 'https://github.com':"
sleep 1
send "${env.GIT_USERNAME}\r"
expect "Password for 'https://${env.GIT_USERNAME}@github.com':"
sleep 1
send "${env.GIT_PASSWORD}\r"
expect eof
END
                if [[ '\$?' == "1" ]]; then
                    git fetch origin
                fi
                set -e

                # || true in case we're using the master branch
                git checkout --track origin/master || true
                git pull \$DESTINATION_REPO_URL master
                git merge ${source_branch}
                git push \$DESTINATION_REPO_URL master

                # finally, echo message if there was a problem
                # TODO: maybe send a slack message instead?
                echo \$source_branch_into_develop
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
