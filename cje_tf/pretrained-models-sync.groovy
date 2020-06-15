/*
WHAT THIS JOB DOES:
- It lists the approved models names in `/tf_dataset/pre-trained-models/`  and
the published models in GCP in two files `approved_models_names.txt` and published_models.txt respectively.

- It looks up each locally found model and update it if it was already published.

- Returns messages like " The model ${published_name} is not found locally!" if the model is GCP but cannot be found locally.
and " The model ${model} with precision ${precision} is not published yet!" if the model exists locally but not published yet.

*/

static final String node_label = params.get('NODE_LABEL')

static final String gcp_credentials = "SA-aipgbot-tfdo"

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

        stage('Sync models') {

            // add credentials to upload to GCP
            withCredentials([file(credentialsId: "${gcp_credentials}", variable: 'SA_KEY')]) {
                sh """#!/bin/bash -xe
                curl -O https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-251.0.0-linux-x86_64.tar.gz
                tar zxvf google-cloud-sdk-251.0.0-linux-x86_64.tar.gz
                ./google-cloud-sdk/install.sh
                export PATH=\$PATH:$WORKSPACE/google-cloud-sdk/bin
                source $WORKSPACE/google-cloud-sdk/path.bash.inc
                source $WORKSPACE/google-cloud-sdk/completion.bash.inc
                
                gcloud auth activate-service-account --key-file=${SA_KEY}
                
                chmod 775 ./cje-tf/scripts/sync_up_pretrained_models.sh
                ./cje-tf/scripts/sync_up_pretrained_models.sh
               """
            }
        }
    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    }
}
