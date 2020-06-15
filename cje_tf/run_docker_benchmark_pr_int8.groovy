static final String docker_image_namespace = params.get('DOCKER_IMAGE_NAMESPACE', 'amr-registry.caas.intel.com/aipg-tf/pr')
static final String DATASET_LOCATION = params.get('DATASET_LOCATION', '/tf_dataset')
static final String SLACK_CHANNEL = params.get('SLACK_CHANNEL', '#tensorflow-jenkins')
static final String docker_build_version = "PR${PR_NUMBER}"
static final String DOCKER_IMAGE = "${docker_image_namespace}:${docker_build_version}-avx2-devel-mkl"
static final String GIT_NAME = params.get('GIT_NAME', 'private-tensorflow')
static final String PYTHON_VERSION = params.get('PYTHON_VERSION', '3')

MODEL_LIST="ResNet50,SSDMobilenet,InceptionV3,RFCN,InceptionResNetV2"
modes=["inference"]
SINGLE_SOCKET=true
SUMMARY_TITLE = 'private-tensorflow pr int8 summary'

def cleanup() {

    try {
        sh '''#!/bin/bash -x
        cd ${WORKSPACE}
        sudo rm -rf *
        docker stop $(docker ps -aq)
        docker rm -vf $(docker ps -aq)
        docker rmi $(docker images --format {{.Repository}}:{{.Tag}})
                
        '''
    } catch(e) {

        echo "==============================================="
        echo "ERROR: Exception caught in cleanup()           "
        echo "ERROR: ${e}"
        echo "==============================================="

        echo ' '
        echo "Error while doing cleanup"

    }  // catch

}

def collectBenchmarkLog(String models, Boolean single_socket) {

    echo "---------------------------------------------------------"
    echo "------------  running collectBenchnmarkLog  -------------"
    echo "---------------------------------------------------------"

    echo "models: $models"
    SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
            returnStdout: true).trim()
    SUMMARYLOG = "${WORKSPACE}/summary_${SERVERNAME}.log"

    models=models.split(',')

    models.each { model ->
        echo "model is ${model}"
        withEnv(["current_model=$model", "single_socket=${single_socket}", "servername=${SERVERNAME}"]) {

            sh '''#!/bin/bash -x
                if [ -f "$WORKSPACE/benchmark_${current_model}_inference_${servername}_throughput.log" -o -f "$WORKSPACE/benchmark_${current_model}_inference_${servername}_latency.log" -o -f "$WORKSPACE/benchmark_${current_model}_inference_${servername}_accuracy.log" ]; then
                    echo "collecting logs for model: ${current_model}"
                    chmod 775 $WORKSPACE/cje-tf/scripts/collect_logs_q3_int8_models.sh
                    $WORKSPACE/cje-tf/scripts/collect_logs_q3_int8_models.sh --model=${current_model} --single_socket=${single_socket}                                                
                fi
                if [ $? -eq 0 ] ; then
                    echo "running model ${current_model} success"
                    RESULT="SUCCESS"
                else
                    echo "running model ${current_model} fail"
                    RESULT="FAILURE"
                fi
                '''
        }
    }
    echo "done running collectBenchmarkLog ......."
    stash allowEmpty: true, includes: "*.log", name: "logfile"
}

def runBenchmark(String docker_image, String dataset_location, Boolean single_socket, String servername, String models, String git_name) {

    workspace_volumn="${WORKSPACE}:/workspace"
    dataset_volume="${dataset_location}:${dataset_location}"

    docker.image("$docker_image").inside("--env \"http_proxy=${http_proxy}\" \
            --env \"https_proxy=${https_proxy}\" \
            --volume ${workspace_volumn} \
            --volume ${dataset_volume} \
            --env DATASET_LOCATION=$dataset_location \
            --env SINGLE_SOCKET=$single_socket \
            --env SERVERNAME=$servername \
            --env MODELS=$models \
            --env TENSORFLOW_DIR=/$git_name \
            --privileged \
            -u root:root") {

            sh '''#!/bin/bash -x
            python /workspace/cje-tf/scripts/get_tf_sourceinfo.py --tensorflow_dir=${TENSORFLOW_DIR} --workspace_dir=/workspace
            chmod 775 /workspace/cje-tf/scripts/run_benchmark_q3_int8_models.sh
            /workspace/cje-tf/scripts/run_benchmark_q3_int8_models.sh
            '''
    }
}

node(params.NODE_LABEL) {
    try {

        cleanup()
        deleteDir()
        
        dir('cje-tf') {
            checkout scm
        }

        // slack notification
        def notifyBuild = load("cje-tf/common/slackNotification.groovy")
        notifyBuild(SLACK_CHANNEL, 'STARTED', '')

        SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
            returnStdout: true).trim()
        SUMMARYLOG = "${WORKSPACE}/summary_${SERVERNAME}.log"
        
        stage("Checkout repository") {

            String GIT_CREDENTIAL = "lab_tfbot"
            String GIT_CREDENTIAL_LAB = "lab_tfbot"
                                                  
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: "origin-pull/pull/$PR_NUMBER/merge"]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'tensorflow']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                           refspec: "+refs/pull/$PR_NUMBER/merge:refs/remotes/origin-pull/pull/$PR_NUMBER/merge",
                                                           url          : "https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git"]]])

            // ResNet50, InceptionV3, InceptionResNetV2, RFCN
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: 'master']],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'tensorflow-inference']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                           url          : 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-inference-deprecated.git']]])                                                                
            // SSD-Mobilenet
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: 'hfei3/int8-ssdmobilenet']],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'tensorflow-models-ssdmobilenet']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",

                                                           url          : 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-models.git']]])

            // need to checkout cocoapi for SSD-Mobilenet
            // cocoapi
            checkout([$class: 'GitSCM',
                      branches: [[name: '*/master']],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "cocoapi"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                           url: "https://github.com/cocodataset/cocoapi.git"]]])
            
        }  //stage
        
        stage('Benchmark Test') {

            dir ( "$WORKSPACE/tensorflow" ) {
                sha = sh (script:"git log -1 --pretty=format:%H | cut -c -7",
                          returnStdout: true).trim()
            }
            docker_build_version="PR${PR_NUMBER}-${sha}" 
            echo docker_build_version
            // PR testing, find the corresponding docker container based on PR_NUMBER and PYTHON_VERSION
            if ( "${PYTHON_VERSION}" == "2" ) 
                DOCKER_IMAGE = "${docker_image_namespace}:${docker_build_version}-avx2-devel-mkl"
            else
                DOCKER_IMAGE = "${docker_image_namespace}:${docker_build_version}-avx2-devel-mkl-py3"
            echo "docker image is ${DOCKER_IMAGE}"
            echo "dataset location is ${DATASET_LOCATION}"
            echo "servername is ${SERVERNAME}"
            echo "mode list is ${MODEL_LIST}"
            runBenchmark(DOCKER_IMAGE, DATASET_LOCATION, SINGLE_SOCKET, SERVERNAME, MODEL_LIST, GIT_NAME)

        }
        
        stage('Collect Logs') {

            // Prepare logs
            def prepareLog = load("cje-tf/common/prepareLog.groovy")
            prepareLog("", "", "", SUMMARYLOG, SUMMARY_TITLE)

            collectBenchmarkLog(MODEL_LIST, SINGLE_SOCKET)

        }

    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    } finally {
         withEnv(["SUMMARYLOG=$SUMMARYLOG"]) {
            def msg = readFile SUMMARYLOG

            def notifyBuild = load("cje-tf/common/slackNotification.groovy")
            notifyBuild(SLACK_CHANNEL, currentBuild.result, msg)
        }

        stage('Archive Artifacts ') {
            dir("$WORKSPACE") {
                archiveArtifacts artifacts: '*.log', excludes: null
                fingerprint: true

            }
        }
    }  // finally
}
