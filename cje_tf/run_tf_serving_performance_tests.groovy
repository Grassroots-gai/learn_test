//Assumption: jenkins job will set BASELINE_CONFIG_FILE parameter.

GIT_CREDENTIAL = "lab_tfbot"
GIT_CREDENTIAL_LAB = 'lab_tfbot'
INTEL_MODELS_URL = 'https://gitlab.devtools.intel.com/intelai/models.git'
CJE_TF_URL = 'https://gitlab.devtools.intel.com/TensorFlow/QA/cje-tf.git'
echo params.BASELINE_CONFIG_FILE
BASELINE_SCRIPT_PATH = '${WORKSPACE}/cje-tf/scripts/${params.BASELINE_CONFIG_FILE}'

node ( params.NODE_LABEL ) {
   deleteDir()
   SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",returnStdout: true).trim()
   SUMMARYLOG = "${WORKSPACE}/summary_${SERVERNAME}.log"
   BASELINE_SCRIPT_PATH = "${WORKSPACE}/cje-tf/scripts/${params.BASELINE_CONFIG_FILE}"
   stage('Checkout'){
        // cje-tf
        checkout([$class: 'GitSCM',
                  branches: [[name: params.CJE_TF_BRANCH]],
                  browser: [$class: 'AssemblaWeb', repoUrl: ''],
                  doGenerateSubmoduleConfigurations: false,
                  extensions: [[$class: 'RelativeTargetDirectory',
                                relativeTargetDir: 'cje-tf']],
                  submoduleCfg: [],
                  userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                       url: "$CJE_TF_URL"]]])

        // intel-models
        checkout([$class: 'GitSCM',
                  branches: [[name: params.INTEL_MODELS_BRANCH]],
                  browser: [$class: 'AssemblaWeb', repoUrl: ''],
                  doGenerateSubmoduleConfigurations: false,
                  extensions: [[$class: 'RelativeTargetDirectory',
                                relativeTargetDir: 'intel-models']],
                  submoduleCfg: [],
                  userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                       url: "$INTEL_MODELS_URL"]]])
   }
   stage('Run'){
        sh """#!/bin/bash -xe
        cd $WORKSPACE/cje-tf
        env | grep -i proxy
        python ./scripts/run_tf_serving_perf_tests.py --intel_trained_models_dir /tf_dataset/pre-trained-models \
         --intel_models_repo_dir  $WORKSPACE/intel-models \
         --output_dir $WORKSPACE --baseline_file $BASELINE_SCRIPT_PATH
        """
   }
   stage('Archive Artifacts ') {
            dir("$WORKSPACE") {
                archiveArtifacts artifacts: '**/*.log, *.csv', excludes: null
                fingerprint: true
            }
    }
}
