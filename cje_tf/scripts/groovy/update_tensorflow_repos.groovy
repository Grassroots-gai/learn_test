private_tf_credential="lab_tfbot"
private_tf_credential_lab="lab_tfbot"
public_intel_tf_credential="b8b085e0-f9ed-4550-b829-35f0bf5187ff"

private_tensorflow_dir="private-tensorflow"
public_intel_tensorflow_dir="tensorflow"


def updateRepo(update_private_repo) {

    def repo_dir = public_intel_tensorflow_dir
    def credential=public_intel_tf_credential

    if (update_private_repo) {
        repo_dir = private_tensorflow_dir
        credential=private_tf_credential_lab
    }


    withCredentials([[$class: 'UsernamePasswordMultiBinding',
                      credentialsId: "$credential",
                      usernameVariable: 'UserName', passwordVariable: 'Password']]) {

        if (update_private_repo) {
            sh '''#!/bin/bash -x
                echo '------>Clone private Tensorflow Repo<-------'
                git clone https://$UserName:$Password@gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git
            '''
        } else {
            sh '''#!/bin/bash -x
                echo '------>Clone Intel public Tensorflow Repo<-------'
                git clone https://$UserName:$Password@github.com/Intel-tensorflow/tensorflow.git
            '''
        }
    }

    dir(repo_dir) {

        sh '''#!/bin/bash -x
            echo "------>Display current git status<-------"
            git status
            git branch

            git remote add upstream "https://github.com/tensorflow/tensorflow.git"
            if [ $? -ne 0 ]
            then
            echo "could not set remote public repo"
            exit 1
            fi

            git fetch upstream
            if [ $? -ne 0 ]
            then
            echo "could not fetch upstream repo"
            exit 1
            fi

            git checkout master
            if [ $? -ne 0 ]
            then
            echo "checkout master branch failed"
            exit 1
            fi

            git rebase upstream/master
            if [ $? -ne 0 ]
            then
            echo "rebase upstream failed"
            exit 1
            fi

            git push -f origin master
            if [ $? -ne 0 ]
            then
            echo "push to master failed"
            exit 1
            fi
        '''
    }

}

node('nervana-bdw27.fm.intel.com') {

    try {
        //clean up directory for rebase
        deleteDir()

        stage('Prepare for updating repositories') {

            sh '''#!/bin/bash -x
                validation_shared_dir="/mnt/aipg_tensorflow_shared/validation"

                if [ ! -d ${validation_shared_dir} ]
                then
                echo "Validation shared directory doesn't exist: ${validation_shared_dir}"
                exit 1
                fi

                #if this file exists, won't update the master
                if [ -f ${validation_shared_dir}/_DONOTRUN_UPDATE_REPO ]
                then
                    echo "Did not update repos today, quitting"
                    echo "Remove the file ${validation_shared_dir}/_DONOTRUN_UPDATE_REPO to run the update script"
                exit 1
                fi
            '''
        }

        stage('Update private Tensorflow repository') {
            updateRepo(true)
        }

        stage('Update public Intel Tensorflow repository') {
            updateRepo(false)
        }

    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    } finally {

    }

}
