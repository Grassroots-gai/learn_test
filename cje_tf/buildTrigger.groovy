// GIT_NAME
// TENSORFLOW_BRANCH
// RUN_TYPE
// BUILD_LABEL
// OPTIONAL_BAZEL_BUILD_OPTIONS

GIT_CREDENTIAL = 'lab_tfbot'
GIT_CREDENTIAL_LAB = "lab_tfbot"

CJE_TF='cje-tf'
CJE_TF_COMMON_DIR = "$CJE_TF/common"
SLACK_CHANNEL = '#test-jenkin-notify'
TARGET_PLATFORM=["avx","avx2"]

// setting BUILD_LABEL default to be nervana-bdw27.fm.intel.com or get input from params
BUILD_LABEL = 'nervana-bdw27.fm.intel.com'
if ('BUILD_LABEL' in params) {
    echo "BUILD_LABEL in params"
    if (params.BUILD_LABEL != '') {
        BUILD_LABEL = params.BUILD_LABEL
        echo BUILD_LABEL
    }
}
echo BUILD_LABEL

String build_tf_v2= 'no'
String tf_version_tag = ''
String config_v2_disable = ''
echo BUILD_TENSORFLOW_V2
if (BUILD_TENSORFLOW_V2 == "true") {
	build_tf_v2='yes'
    tf_version_tag = 'TF-v2'
}
else {
    build_tf_v2='no'
}


def archiveArtifactory() {

    echo "---------------------------------------------------------"
    echo " ---------------- archiveArtifactory ------------------- "
    echo "---------------------------------------------------------"

    for ( targetPlatform in TARGET_PLATFORM) {
        dir("$WORKSPACE" + "/publish" + "/$targetPlatform") {
            stashFiles = "TFWheelAndBuildLog_${targetPlatform}"
            echo "stashFiles is ${stashFiles}"
            unstash "TFWheelAndBuildLog_${targetPlatform}"

            def server = Artifactory.server 'ubit-artifactory-or'
            def uploadSpec = """{

                "files": [
                {
                  "pattern": "*",
                  "target": "aipg-local/aipg-tf/${env.JOB_NAME}/${env.BUILD_NUMBER}/${targetPlatform}/"
                }
                ]
             }"""
             def buildInfo = server.upload(uploadSpec)
             server.publishBuildInfo(buildInfo)
        }
    }

}


// **** def cloneRepo(String name, String branch, boolean checkoutBenchmark) ****
// Clone the tensorflow repo: public tensorflow or private-tensorflow,
// also clone all the models the benchmark run needs
//
// Parameters:
//     name              - tensorflow or private-tensorflow
//     branch            - branch to checkout the source from
//     checkoutBenchmark - whether or not to clone the benchmark related repos
//
// Returns: nothing
//
// NOTE: Sets global currentBuild.result to FAILURE if build fails.
def cloneRepo(String dirName, String branch, boolean checkoutBenchmark) {

    echo "---------------------------------------------------------"
    echo "---------------    running cloneRepo     ----------------"
    echo "---------------------------------------------------------"

    try {

        if ( dirName.trim() == 'tensorflow' ) {
            gitURL = "https://github.com/tensorflow/tensorflow.git"
            checkout([$class: 'GitSCM',
                branches: [[name: branch]],
                browser: [$class: 'AssemblaWeb', repoUrl: ''],
                doGenerateSubmoduleConfigurations: false,
                extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: dirName]],
                submoduleCfg: [],
                userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL", name: dirName, url: gitURL]]])        
        }
        else {
            gitURL = "https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git"

            checkout([$class: 'GitSCM',
                branches: [[name: branch]],
                browser: [$class: 'AssemblaWeb', repoUrl: ''],
                doGenerateSubmoduleConfigurations: false,
                extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: dirName]],
                submoduleCfg: [],
                userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB", name: dirName, url: gitURL]]])        
        }

        if ( checkoutBenchmark ) {

            // deepSpeech2 - ds2
            checkout([$class: 'GitSCM',
                branches: [[name: '*/master']],
                browser: [$class: 'AssemblaWeb', repoUrl: ''],
                doGenerateSubmoduleConfigurations: false,
                extensions: [[$class: 'RelativeTargetDirectory',
                    relativeTargetDir: 'deepSpeech2']],
                submoduleCfg: [],
                userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                    url: 'https://github.com/yao-matrix/deepSpeech2.git']]])

            // tensorflow-SSD - SSDvgg16
            checkout([$class: 'GitSCM',
                branches: [[name: TENSORFLOW_SSD_BRANCH]],
                browser: [$class: 'AssemblaWeb', repoUrl: ''],
                doGenerateSubmoduleConfigurations: false,
                extensions: [[$class: 'RelativeTargetDirectory',
                    relativeTargetDir: 'tensorflow-SSD']],
                submoduleCfg: [],
                userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                    url: 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-SSD.git']]])

            // nmt
            checkout([$class: 'GitSCM',
                branches: [[name: '*/master']],
                browser: [$class: 'AssemblaWeb', repoUrl: ''],
                doGenerateSubmoduleConfigurations: false,
                extensions: [[$class: 'RelativeTargetDirectory',
                    relativeTargetDir: 'nmt']],
                submoduleCfg: [],
                userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                    url: 'https://github.com/tensorflow/nmt.git']]])

            // models - cifar10, resnet32 w/cifar10
            checkout([$class: 'GitSCM',
                branches: [[name: MODELS_REFSPEC]],
                browser: [$class: 'AssemblaWeb', repoUrl: ''],
                doGenerateSubmoduleConfigurations: false,
                extensions: [[$class: 'RelativeTargetDirectory',
                    relativeTargetDir: 'models']],
                submoduleCfg: [],
                userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                    url: 'https://github.com/tensorflow/models.git']]])

            // dcgan-tf-benchmark
            checkout([$class: 'GitSCM',
                branches: [[name: "*/master"]],
                browser: [$class: 'AssemblaWeb', repoUrl: ''],
                doGenerateSubmoduleConfigurations: false,
                extensions: [[$class: 'RelativeTargetDirectory',
                    relativeTargetDir: 'dcgan-tf-benchmark']],
                submoduleCfg: [],
                userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                    url: 'https://github.com/MustafaMustafa/dcgan-tf-benchmark']]])

            }

    } catch(e) {

        echo "================================================================================"
        echo "ERROR: Exception caught in module which clones the tensorflow repo - cloneRepo()"
        echo "ERROR: ${e}"
        echo "================================================================================"

        echo ' '
        echo "Build marked as FAILURE"
        currentBuild.result = 'FAILURE'

    }  // catch
} // def cloneRepo()


// **** buildTF(name, runType, summaryLog, buildLog, targetPlatform ) ****
// Build a TF wheel
//
// Parameters:
//     name           - tensorflow or private-tensorflow
//     runType        - mklml or mkldnn
//     summaryLog     - the summary log to report
//     buildLog       - the build log
//     targetPlatform - the compiler flags that we read from various configuartion files, avx/avx2/avx512
//     build_v2     - disable tf_v2 compiler flag. Default is enable
//
// Returns: nothing
//
// NOTE: Sets global currentBuild.result to FAILURE if build fails.
def buildTF(String name, String runType, String summaryLog, String buildLog, String targetPlatform, String build_v2) {

    echo "---------------------------------------------------------"
    echo "---------------     running buildTF      ----------------"
    echo "---------------------------------------------------------"

    try {
        dir("${name}") {
            withEnv(["runType=${runType}", "summaryLog=${summaryLog}", "buildLog=${buildLog}", "targetPlatform=${targetPlatform}", "build_v2=${build_v2}" ]) {
                sh '''#!/bin/bash -x
                export PATH="/opt/tensorflow/java/jdk1.8.0_131/bin:/opt/tensorflow/bazel/bin:/opt/tensorflow/gcc/gcc6.3/bin:$PATH"
                export LD_LIBRARY_PATH="/opt/tensorflow/gcc/gcc6.3/lib64"
                export JAVA_HOME="/opt/tensorflow/java/jdk1.8.0_131"
                PYTHON_BIN_PATH="$WORKSPACE/venv/bin/python"
                PYTHON_LIB_PATH="$WORKSPACE/venv/lib/python2.7/site-packages"

                export PYTHON_BIN_PATH=${PYTHON_BIN_PATH} \\
                    PYTHON_LIB_PATH=${PYTHON_LIB_PATH}

                source $WORKSPACE/venv/bin/activate
                ./configure <<EOF

















                EOF
                '''

                sh '''#!/bin/bash -x
                ERROR="0"
                export PATH="/opt/tensorflow/java/jdk1.8.0_131/bin:/opt/tensorflow/bazel/bin:/opt/tensorflow/gcc/gcc6.3/bin:$PATH"
                export LD_LIBRARY_PATH="/opt/tensorflow/gcc/gcc6.3/lib64"
                export JAVA_HOME="/opt/tensorflow/java/jdk1.8.0_131"
                PYTHON_BIN_PATH="$WORKSPACE/venv/bin/python"
                PYTHON_LIB_PATH="$WORKSPACE/venv/lib/python2.7/site-packages"

                PLATFORM="haswell"
                if [ "${targetPlatform}" = "avx2" ]; then
                    PLATFORM="haswell"
                fi
                if [ "${targetPlatform}" = "avx" ]; then
                    PLATFORM="sandybridge"
                fi
                source $WORKSPACE/venv/bin/activate

                pushd tensorflow/tools/ci_build/linux/mkl
                if [ "${build_v2}" = "no" ]; then
                    python set-build-env.py -p ${PLATFORM} -f $WORKSPACE/mkl_build_${targetPlatform}.bazelrc --disable-v2
                else
                    python set-build-env.py -p ${PLATFORM} -f $WORKSPACE/mkl_build_${targetPlatform}.bazelrc
                fi

                if [ "${runType}" = "mklml" ]; then
                    bazel --output_user_root=$WORKSPACE/build build_${targetPlatform} --bazelrc=$WORKSPACE/mkl_build_${targetPlatform}.bazelrc build ${OPTIONAL_BAZEL_BUILD_OPTIONS} --copt="-DINTEL_MKL_ML" --copt=-L/opt/tensorflow/gcc6.3/lib64 -s -c opt //tensorflow/tools/pip_package:build_pip_package >& ${buildLog}
                else
                    bazel --output_user_root=$WORKSPACE/build_${targetPlatform} --bazelrc=$WORKSPACE/mkl_build_${targetPlatform}.bazelrc build ${OPTIONAL_BAZEL_BUILD_OPTIONS} --copt=-L/opt/tensorflow/gcc6.3/lib64 -s -c opt //tensorflow/tools/pip_package:build_pip_package >& ${buildLog}
                fi

                if [ "$(grep 'ERROR: ' ${buildLog} | wc -l)" = "0" ] ; then
                    RESULT="SUCCESS"
                else
                    RESULT="FAILURE"
                    ERROR="1"
                fi
                grep "ERROR: " ${buildLog}
                tail -5 ${buildLog}
                echo "RESULT: ${RESULT}"

                # posting last few lines of build log to the summary log
                echo "building with ${targetPlatform}......" >> ${summaryLog}
                if [ $ERROR = "1" ]; then
                    grep "ERROR: " ${buildLog}  >> ${summaryLog}
                    tail -5 ${buildLog}  >>  ${summaryLog}
                    echo ' ' >> ${summaryLog}
                    exit 1
                else
                    tail -4 ${buildLog}  >>  ${summaryLog}
                    echo ' ' >> ${summaryLog}
                fi

                # building pip
                popd
                bazel-bin/tensorflow/tools/pip_package/build_pip_package $WORKSPACE/build_${targetPlatform}

                '''
            } // withEnv

        } // dir

    } catch(e) {

        echo "=================================================================================="
        echo "ERROR: Exception caught in module which builds the tensorflow repo - buildTF()"
        echo "ERROR: ${e}"
        echo "=================================================================================="

        echo ' '
        echo "Build marked as FAILURE"
        currentBuild.result = 'FAILURE'

    }  // catch

} // def buildTF()

node(BUILD_LABEL) {

    try {

        // first clean the workspace
        deleteDir()

        // pull the cje-tf
        dir(CJE_TF) {   
            checkout scm
        }
   
        // slack notify
        def slackNotify = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
        slackNotify(SLACK_CHANNEL, 'started', '')

        // setting GIT_NAME default to be "private-tensorflow or get input from params
        GIT_NAME = 'private-tensorflow'
        if ('GIT_NAME' in params) {
            echo "GIT_NAME in params"
            if (params.GIT_NAME != '') {
                GIT_NAME = params.GIT_NAME
                echo GIT_NAME 
            }
        }
        echo GIT_NAME 
 
        // setting TENSORFLOW_BRANCH default to be master or get input from params
        TENSORFLOW_BRANCH = 'master'
        if ('TENSORFLOW_BRANCH' in params) {
            echo "TENSORFLOW_BRANCH in params"
            if (params.TENSORFLOW_BRANCH != '') {
                TENSORFLOW_BRANCH = params.TENSORFLOW_BRANCH
                echo TENSORFLOW_BRANCH
            }
        }
        echo TENSORFLOW_BRANCH

        // setting RUN_TYPE default to be mkldnn or get input from params
        RUN_TYPE = 'mkldnn'
        if ('RUN_TYPE' in params) {
            echo "RUN_TYPE in params"
            if (params.RUN_TYPE != '') {
                RUN_TYPE = params.RUN_TYPE
                echo RUN_TYPE
            }
        }
        echo RUN_TYPE

        // setting TARGET_PLATFORM default to be avx2 or get input from params
        /*
        TARGET_PLATFORM = 'avx2'
        if ('TARGET_PLATFORM' in params) {
            echo "TARGET_PLATFORM in params"
            if (params.TARGET_PLATFORM != '') {
                TARGET_PLATFORM = params.TARGET_PLATFORM
                echo TARGET_PLATFORM
            }
        }
        echo TARGET_PLATFORM
        */

        // Setting OPTIONAL_BAZEL_BUILD_OPTIONS if entered by the user. Default is null
        OPTIONAL_BAZEL_BUILD_OPTIONS = ''
        if ('OPTIONAL_BAZEL_BUILD_OPTIONS' in params) {
            echo "OPTIONAL_BAZEL_BUILD_OPTIONS in params"
            if (params.OPTIONAL_BAZEL_BUILD_OPTIONS != '') {
                OPTIONAL_BAZEL_BUILD_OPTIONS = params.OPTIONAL_BAZEL_BUILD_OPTIONS
                echo OPTIONAL_BAZEL_BUILD_OPTIONS
            }
        }
        echo BUILD_LABEL

        // setting SUMMARY_LOG name
        SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                         returnStdout: true).trim()
        SHORTNAME = sh (script:"echo $SERVERNAME | cut -f2 -d-",
                        returnStdout: true).trim()
        SUMMARY_LOG = "${WORKSPACE}/summary_${SERVERNAME}.log"

        stage('Clone') {
            echo "Clone TF from ${GIT_NAME} refspec ${TENSORFLOW_BRANCH}"

            // Clone the TF repo
            boolean checkoutBenchmark = false
            cloneRepo(GIT_NAME, TENSORFLOW_BRANCH, checkoutBenchmark)

            // Prepare logs
            def prepareLog = load("${CJE_TF_COMMON_DIR}/prepareLog.groovy")
            prepareLog(TENSORFLOW_BRANCH, GIT_NAME, RUN_TYPE, SUMMARY_LOG, "Tensorflow BUILD summary")

            // Prepare virtual environment
            def prepareEnv = load("${CJE_TF_COMMON_DIR}/prepareEnvironment.groovy")
            prepareEnv()
        }

        stage('Build') {
            
            for ( targetPlatform in TARGET_PLATFORM) {          
                BUILD_LOG = "${WORKSPACE}/bazel_build_${targetPlatform}_${SERVERNAME}.log"
                echo BUILD_LOG
                stashFiles = "TFWheelAndBuildLog_${targetPlatform}"
                echo stashFiles
                buildTF(GIT_NAME, RUN_TYPE, SUMMARY_LOG, BUILD_LOG, targetPlatform, build_tf_v2)
                stash name: "${stashFiles}", includes: "build_${targetPlatform}/*.whl,bazel_build_${targetPlatform}_${SERVERNAME}.log", useDefaultExcludes: false, allowEmpty: true

            }
        }
      
    } catch (e) {

        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e

    } finally {

        // Success or failure, always send notifications
        withEnv(["SUMMARY_LOG=$SUMMARY_LOG"]) {
            echo SUMMARY_LOG
            def msg = readFile SUMMARY_LOG

            def slackNotify = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
            slackNotify(SLACK_CHANNEL, currentBuild.result, msg)
        }

        stage('Publish / Archive Artifacts') {
            for ( targetPlatform in TARGET_PLATFORM) {
                dir("$WORKSPACE" + "/publish" + "/$targetPlatform") {
                    stashFiles = "TFWheelAndBuildLog_${targetPlatform}"
                    unstash "TFWheelAndBuildLog_${targetPlatform}"

                
                    archiveArtifacts artifacts: "*.log,build_${targetPlatform}/*.whl", excludes: null
                        fingerprint: true

                }
            }
            archiveArtifactory()

        } // stage 'Publish / Archive Artifacts'

    } // finally

} // node 


