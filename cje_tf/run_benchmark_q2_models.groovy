
models=["inception_v4",  "inception_resnet_v2", "SqueezeNet", "YoloV2", "fastrcnn", "gnmt", "rfcn", "transformerLanguage", "transformerSpeech", "WaveNet", "wideDeep", "WaveNet_Magenta", "deepSpeech", "mobilenet_v1", "UNet",  "DRAW", "A3C", "DCGAN", "3DGAN", "inceptionv3", "resnet50"]

modes=["training", "inference"]

ERROR="0"
SLACK_CHANNEL = '#test-jenkin-notify'
SINGLE_SOCKET=true
FULL_VALIDATION=false
CJE_TF='cje-tf'
CJE_TF_COMMON_DIR = "$CJE_TF/common"

GIT_CREDENTIAL = "lab_tfbot"
GIT_CREDENTIAL_LAB = "lab_tfbot"

TF_SLIM_MODEL='tensorflow-slim-models'
TF_SLIM_MODEL_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-slim-models.git'

SQUEEZE_NET_MODEL='tensorflow-SqueezeNet'
SQEEZE_NET_MODEL_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-SqueezeNet.git'

YOLO_V2_MODEL='tensorflow-YoloV2'
YOLO_V2_MODEL_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-YoloV2.git'

TF_FASTRCNN='tensorflow-models'
TF_FASTRCNN_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-models.git'
TF_FASTRCNN_BRANCH='FastRCNN-Resnet50'
COCOAPI_URL='https://github.com/cocodataset/cocoapi.git'
COCOAPI='cocoapi'

TENSORFLOW_NMT='tensorflow-NMT'
TENSORFLOW_NMT_URL='https://github.com/NervanaSystems/tensorflow-NMT.git'

TENSORFLOW_DEEP_SPEECH='tensorflow-DeepSpeech'
TENSORFLOW_DEEP_SPEECH_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-DeepSpeech.git'
TENSORFLOW_DEEP_SPEECH_BRANCH='bhavanis/mkl-optimizations'

TENSORFLOW_RFCN='tensorflow-RFCN'
TENSORFLOW_RFCN_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-models.git'
TENSORFLOW_RFCN_BRANCH='rfcn-resnet101-coco'

TRANSFORMER_LANGUAGE='tensorflow-TransformerLanguage'
TRANSFORMER_LANGUAGE_URL="https://github.com/NervanaSystems/tensorflow-TransformerLanguage"
//TRANSFORMER_LANGUAGE_BRANCH='*/master'
// a recent commit breaks the run for this model, https://jira.devtools.intel.com/browse/TFDO-2492
// use earlier commit to run the model for now
TRANSFORMER_LANGUAGE_BRANCH='abfe42741e64a2fe3e8dd055d01998ce120f9070'

TRANSFORMER_SPEECH='tensorflow-TransformerSpeech'
TRANSFORMER_SPEECH_URL="https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-TransformerSpeech.git"
TRANSFORMER_SPEECH_BRANCH='*/master'

TENSORFLOW_WIDEDEEP='wideDeep'
TENSORFLOW_WIDEDEEP_URL="https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-models.git"
TENSORFLOW_WIDEDEEP_BRANCH='wei-wide-deep'

WAVENET_URL="https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-regular-wavenet-deprecated.git"
WAVENET_DIR='tensorflow-regular-wavenet'
WAVENET_BRANCH='ashraf/optimize_intel'

WAVENET_MAGENTA_URL="https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-WaveNet.git"
WAVENET_MAGENTA_DIR="tensorflow-WaveNet"
WAVENET_MAGENTA_BRANCH="ashraf/tf-Wavenet"

// Shanghai OOB models
TENSORFLOW_UNET='tensorflow-UNet'
TENSORFLOW_UNET_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-UNet.git'
TENSORFLOW_UNET_BRANCH='master'

TENSORFLOW_DRAW='tensorflow-DRAW'
TENSORFLOW_DRAW_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-DRAW-deprecated.git'
TENSORFLOW_DRAW_BRANCH='master'

// A3C + ALE
TENSORFLOW_A3C='tensorflow-A3C'
TENSORFLOW_A3C_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-A3C.git'
TENSORFLOW_A3C_BRANCH='master'
TENSORFLOW_A3C_ALE='Arcade-Learning-Environment'
TENSORFLOW_A3C_ALE_URL='https://github.com/miyosuda/Arcade-Learning-Environment.git'
TENSORFLOW_A3C_ALE_BRANCH='master'

TENSORFLOW_DCGAN='tensorflow-DCGAN'
TENSORFLOW_DCGAN_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-DCGAN.git'
TENSORFLOW_DCGAN_BRANCH='master'

TENSORFLOW_3DGAN='tensorflow-3DGAN'
TENSORFLOW_3DGAN_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-3DGAN.git'
TENSORFLOW_3DGAN_BRANCH='master'

TENSORFLOW_INFERENCE='tensorflow-inference'
TENSORFLOW_INFERENCE_URL='https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-inference-deprecated.git'
TENSORFLOW_INFERENCE_BRANCH='master'


// setting DOWNLOAD_BUILD default to the job private-tensorflow-build-trigger unless otherwise specified in params 
DOWNLOAD_BUILD = 'private-tensorflow-build-trigger'
if ('DOWNLOAD_BUILD' in params) {
    echo "DOWNLOAD_BUILD in params"
    if (params.DOWNLOAD_BUILD != '') {
        DOWNLOAD_BUILD = params.DOWNLOAD_BUILD
        echo DOWNLOAD_BUILD
    }
}
echo "downloading build from $DOWNLOAD_BUILD jenkin job"

// setting TARGET_PLATFORM default to avx2 unless otherwise specified in params 
TARGET_PLATFORM = 'avx2'
if ('TARGET_PLATFORM' in params) {
    echo "TARGET_PLATFORM in params"
    if (params.TARGET_PLATFORM != '') {
        TARGET_PLATFORM = params.TARGET_PLATFORM
        echo TARGET_PLATFORM
    }
}
echo "downloading $TARGET_PLATFORM build from $DOWNLOAD_BUILD jenkin job"

// setting NODE_LABEL default to be nervana-skx10.fm.intel.com or get input from params
NODE_LABEL = 'nervana-skx10.fm.intel.com'
if ('NODE_LABEL' in params) {
    echo "NODE_LABEL in params"
    if (params.NODE_LABEL != '') {
        NODE_LABEL = params.NODE_LABEL
        echo NODE_LABEL
    }
}
echo "running on $NODE_LABEL"

// setting GIT_NAME default to be private-tensorflow or get input from params
GIT_NAME = 'private-tensorflow'
if ('GIT_NAME' in params) {
    echo "GIT_NAME in params"
    if (params.GIT_NAME != '') {
        GIT_NAME = params.GIT_NAME
        echo GIT_NAME
    }
}
echo "GIT_NAME is $GIT_NAME"

// setting GIT_URL default to be https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow or get input from params
GIT_URL = 'https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git'
if ('GIT_URL' in params) {
    echo "GIT_URL in params"
    if (params.GIT_URL != '') {
        GIT_URL = params.GIT_URL
        echo GIT_URL
    }
}
echo "GIT_URL is $GIT_URL"

// setting PRIVATE_TENSORFLOW_BRANCH default to be master or get input from params
PRIVATE_TENSORFLOW_BRANCH = '*/master'
if ('PRIVATE_TENSORFLOW_BRANCH' in params) {
    echo "PRIVATE_TENSORFLOW_BRANCH in params"
    if (params.PRIVATE_TENSORFLOW_BRANCH != '') {
        PRIVATE_TENSORFLOW_BRANCH = params.PRIVATE_TENSORFLOW_BRANCH
        echo PRIVATE_TENSORFLOW_BRANCH
    }
}
echo "PRIVATE_TENSORFLOW_BRANCH is $PRIVATE_TENSORFLOW_BRANCH"

// setting RUN_TYPE default to be mkldnn or get input from params
RUN_TYPE = 'mkldnn'
if ('RUN_TYPE' in params) {
    echo "RUN_TYPE in params"
    if (params.RUN_TYPE != '') {
        RUN_TYPE = params.RUN_TYPE
        echo RUN_TYPE
    }
}
echo "Running with $RUN_TYPE"

// setting SUMMARY_TITLE with some default value or get input from params
SUMMARY_TITLE = "Tensorflow Q2 models nightly benchmark test MKL-DNN summary"
if ('SUMMARY_TITLE' in params) {
    echo "SUMMARY_TITLE in params"
    if (params.SUMMARY_TITLE != '') {
        SUMMARY_TITLE = params.SUMMARY_TITLE
        echo SUMMARY_TITLE
    }
}
echo "Summary title is $SUMMARY_TITLE"

// setting SLACK_CHANNEL with some default value or get input from params
SLACK_CHANNEL = '#tensorflow-jenkins'
//SLACK_CHANNEL = '#test-jenkin-notify'
if ('SLACK_CHANNEL' in params) {
    echo "SLACK_CHANNEL in params"
    if (params.SLACK_CHANNEL != '') {
        SLACK_CHANNEL = params.SLACK_CHANNEL
        echo SLACK_CHANNEL
    }
}
echo "Slack channel is $SLACK_CHANNEL"

// check default value for POST_TO_DASHBOARD 
if (params.POST_TO_DASHBOARD) 
    echo "params.POST_TO_DASHBOARD is true"
else
    echo "params.POST_TO_DASHBOARD is false"
Boolean POST_TO_DASHBOARD=params.POST_TO_DASHBOARD

// setting TARGET_DASHBOARD default to be production or get input from params
TARGET_DASHBOARD = 'production'
if ('TARGET_DASHBOARD' in params) {
    echo "TARGET_DASHBOARD in params"
    if (params.TARGET_DASHBOARD != '') {
        TARGET_DASHBOARD = params.TARGET_DASHBOARD
        echo TARGET_DASHBOARD
    }
}

// setting CJE_ALGO branch 
CJE_ALGO_BRANCH = '*/master'
if ('CJE_ALGO_BRANCH' in params) {
    echo "CJE_ALGO_BRANCH in params"
    if (params.CJE_ALGO_BRANCH != '') {
        CJE_ALGO_BRANCH = params.CJE_ALGO_BRANCH
        echo CJE_ALGO_BRANCH
    }
}

// setting default DATASET_LOCATION 
DATASET_LOCATION = '/tf_dataset'
if ('DATASET_LOCATION' in params) {
    echo "DATASET_LOCATION in params"
    if (params.DATASET_LOCATION != '') {
        DATASET_LOCATION = params.DATASET_LOCATION
        echo DATASET_LOCATION
    }
}

def downloadLatestArtifacts = { jobName, fileSpec ->
    def server = Artifactory.server 'ubit-artifactory-or'
    def downloadSpec = """{
        "files": [
            {
                "pattern": "aipg-local/aipg-tf/${jobName}/(*)/${fileSpec}",
                "target": "build/",
                "build": "${jobName}/LATEST",
                "flat": "true"
            }
        ]
    }"""
    def buildInfo = server.download(downloadSpec)
}

node( NODE_LABEL) {

    try {

         // first clean the workspace
        deleteDir()

        // pull the cje-tf
        dir(CJE_TF) {
            checkout scm
        }

        SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                              returnStdout: true).trim()
        SHORTNAME = sh (script:"echo $SERVERNAME | cut -f2 -d-",
                              returnStdout: true).trim()
        SUMMARYLOG = "${WORKSPACE}/summary_${SERVERNAME}.log"
        SUMMARYTXT = "${WORKSPACE}/summary_nightly.log"
        writeFile file: SUMMARYTXT, text: "Model,Mode,Server,Data_Type,Use_Case,Batch_Size,Result\n"

        // slack notification
        def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
        notifyBuild(SLACK_CHANNEL, 'STARTED', '')

        stage('checkout') {

            checkout([$class: 'GitSCM',
                      branches: [[name: PRIVATE_TENSORFLOW_BRANCH]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: 'private-tensorflow']],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "$GIT_URL"]]])

            // slim-model
            checkout([$class: 'GitSCM',
                      branches: [[name: '*/master']],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "$TF_SLIM_MODEL"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "$TF_SLIM_MODEL_URL"]]])

            // squeezenet
            checkout([$class: 'GitSCM',
                      branches: [[name: '*/master']],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "$SQUEEZE_NET_MODEL"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "$SQEEZE_NET_MODEL_URL"]]])

            // YoloV2
            checkout([$class: 'GitSCM',
                      branches: [[name: '*/master']],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "$YOLO_V2_MODEL"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "$YOLO_V2_MODEL_URL"]]])

            // deepSpeech
            checkout([$class: 'GitSCM',
                      branches: [[name: TENSORFLOW_DEEP_SPEECH_BRANCH]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${TENSORFLOW_DEEP_SPEECH}"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "${TENSORFLOW_DEEP_SPEECH_URL}"]]])


            // fastRCNN
            checkout([$class: 'GitSCM',
                      branches: [[name: TF_FASTRCNN_BRANCH]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${TF_FASTRCNN}"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "${TF_FASTRCNN_URL}"]]])

            // cocoapi
            checkout([$class: 'GitSCM',
                      branches: [[name: '*/master']],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${COCOAPI}"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                           url: "${COCOAPI_URL}"]]])

            // model nmt
            checkout([$class: 'GitSCM', 
                      branches: [[name: '*/master']], 
                      browser: [$class: 'AssemblaWeb', repoUrl: ''], 
                      doGenerateSubmoduleConfigurations: false, 
                      extensions: [[$class: 'RelativeTargetDirectory', 
                                     relativeTargetDir: "${TENSORFLOW_NMT}"]], 
                      submoduleCfg: [], 
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL", 
                                           url: "${TENSORFLOW_NMT_URL}"]]])

            // RFCN
            checkout([$class: 'GitSCM',
                      branches: [[name: TENSORFLOW_RFCN_BRANCH]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${TENSORFLOW_RFCN}"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "${TENSORFLOW_RFCN_URL}"]]])

            // transformerLanguage
            checkout([$class: 'GitSCM',
                      branches: [[name: TRANSFORMER_LANGUAGE_BRANCH]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${TRANSFORMER_LANGUAGE}"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                           url: "${TRANSFORMER_LANGUAGE_URL}"]]])

            // transformerSpeech
            checkout([$class: 'GitSCM',
                      branches: [[name: TRANSFORMER_SPEECH_BRANCH]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${TRANSFORMER_SPEECH}"],
                                   [$class: 'SubmoduleOption',
                                    disableSubmodules: false,
                                    parentCredentials: true,
                                    recursiveSubmodules: true,
                                    reference: '',
                                    trackingSubmodules: false]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "${TRANSFORMER_SPEECH_URL}"]]])

            // wavenet
            checkout([$class: 'GitSCM',
                      branches: [[name: WAVENET_BRANCH]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "$WAVENET_DIR"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                           url: "$WAVENET_URL"]]])
            
            // wideDeep
            checkout([$class: 'GitSCM',
                      branches: [[name: TENSORFLOW_WIDEDEEP_BRANCH]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${TENSORFLOW_WIDEDEEP}"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "${TENSORFLOW_WIDEDEEP_URL}"]]])

            // wavenet-magenta
            checkout([$class: 'GitSCM',
                      branches: [[name: "$WAVENET_MAGENTA_BRANCH"]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "$WAVENET_MAGENTA_DIR"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "$WAVENET_MAGENTA_URL"]]])
            
            // Shanghai OOB models ///////////
            // UNET
            checkout([$class: 'GitSCM',
                      branches: [[name: "${TENSORFLOW_UNET_BRANCH}"]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${TENSORFLOW_UNET}"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "${TENSORFLOW_UNET_URL}"]]])
            // DRAW
            checkout([$class: 'GitSCM',
                      branches: [[name: "${TENSORFLOW_DRAW_BRANCH}"]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${TENSORFLOW_DRAW}"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                           url: "${TENSORFLOW_DRAW_URL}"]]])
            // A3C + ALE
            checkout([$class: 'GitSCM',
                      branches: [[name: "${TENSORFLOW_A3C_BRANCH}"]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${TENSORFLOW_A3C}"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "${TENSORFLOW_A3C_URL}"]]])
            checkout([$class: 'GitSCM',
                      branches: [[name: "${TENSORFLOW_A3C_ALE_BRANCH}"]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${TENSORFLOW_A3C_ALE}"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                           url: "${TENSORFLOW_A3C_ALE_URL}"]]])
            // DCGAN
            checkout([$class: 'GitSCM',
                      branches: [[name: "${TENSORFLOW_DCGAN_BRANCH}"]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${TENSORFLOW_DCGAN}"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "${TENSORFLOW_DCGAN_URL}"]]])
            // 3DGAN
            checkout([$class: 'GitSCM',
                     branches: [[name: "${TENSORFLOW_3DGAN_BRANCH}"]],
                     browser: [$class: 'AssemblaWeb', repoUrl: ''],
                     doGenerateSubmoduleConfigurations: false,
                     extensions: [[$class: 'RelativeTargetDirectory',
                                   relativeTargetDir: "${TENSORFLOW_3DGAN}"]],
                     submoduleCfg: [],
                     userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                         url: "${TENSORFLOW_3DGAN_URL}"]]])

            // cje-algo
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: CJE_ALGO_BRANCH]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'cje-algo']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL",
                                                           url          : 'https://github.intel.com/AIPG/cje-algo.git']]])

            // inception_v3, resnet50
            checkout([$class: 'GitSCM',
                      branches: [[name: "${TENSORFLOW_INFERENCE_BRANCH}"]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "${TENSORFLOW_INFERENCE}"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                           url: "${TENSORFLOW_INFERENCE_URL}"]]])

        }

        stage('Install dependencies') {
            withEnv(["tensorflow_deep_speech_dir=${TENSORFLOW_DEEP_SPEECH}", "branch=${PRIVATE_TENSORFLOW_BRANCH}"]) {
                sh '''#!/bin/bash -x
                sudo sh -c 'sync; echo 1 > /proc/sys/vm/compact_memory; echo 1 > /proc/sys/vm/drop_caches' || true
                export PATH="/nfs/fm/disks/aipg_tensorflow_tools/gcc6.3/bin/:/nfs/fm/disks/aipg_tensorflow_tools/bazel/bin:$PATH"
                export LIBRARY_PATH=/usr/lib/x86_64-linux-gnu:$LIBRARY_PATH
                #virtualenv-3 -p /usr/bin/python $WORKSPACE/venv
                /usr/bin/python -m virtualenv $WORKSPACE/venv
                source $WORKSPACE/venv/bin/activate
                sudo touch /usr/include/stropts.h
                pip install -I subprocess32 'backports.weakref >= 1.0rc1' 'enum34 >= 1.1.6' autograd 'bleach >= 1.5.0' enum funcsigs 'future >= 0.17.1' futures grpc gevent 'grpcio >= 1.8.6' html5lib Markdown 'mock >= 2.0.0' msgpack-python 'numpy >= 1.14.5, < 2.0' pbr pip portpicker 'protobuf >= 3.6.1' scikit-learn 'scipy >= 0.15.1' setuptools 'six >= 1.10.0' 'tensorboard >= 1.13.0, < 1.14.0' Werkzeug 'wheel >= 0.26' h5py matplotlib opencv-python 'keras_preprocessing >= 1.0.5' cython Cython pillow lxml jupyter 'librosa >=0.6.1' sympy requests gym google-api-python-client oauth2client

                if [ "$branch" == "master" ]; then
                    pip install --upgrade tf-estimator-nightly
                fi

                echo y | pip uninstall joblib
                pip install joblib==0.11

                # ALE for A3C
                cd $WORKSPACE/Arcade-Learning-Environment
                cmake -DUSE_SDL=OFF -DUSE_RLGLUE=OFF -DBUILD_EXAMPLES=OFF .
                make -j 8
                pip install .
                
                # keras for 3dgan
                pip install keras
                
                cd $WORKSPACE/tensorflow-models/research/slim
                pip install -e .
                # for deepSpeech
                cd $WORKSPACE/${tensorflow_deep_speech_dir}
                mkdir -p data/lm
                cp /dataset/q2models/deepSpeech/dependencies/* ./data/lm/
                pip install -r requirements.txt
                pip install --upgrade ./native_client/deepspeech*.whl

                # install graph transform tool
                cd $WORKSPACE/private-tensorflow
                bazel build tensorflow/tools/graph_transforms:transform_graph
                bazel build tensorflow/tools/graph_transforms:summarize_graph

                # for transformerLanguage
                pip install --upgrade mesh-tensorflow tensorflow-probability tqdm
            '''
            }
        }

        stage('Get TF wheel and install') {

            downloadSpec = downloadLatestArtifacts(DOWNLOAD_BUILD, "${TARGET_PLATFORM}/*.whl")

            sh '''#!/bin/bash -x
            source $WORKSPACE/venv/bin/activate
            pip install --upgrade build/*.whl
            '''

            stash allowEmpty: true, includes: "build/*.whl", name: "wheelfile"
        }

        stage('Run benchmark') {
            sh '''#!/bin/bash -x
            source $WORKSPACE/venv/bin/activate
            export DATASET_LOCATION=${DATASET_LOCATION}
            chmod 775 $WORKSPACE/cje-tf/scripts/fixup_*
            $WORKSPACE/cje-tf/scripts/fixup_fastrcnn.sh
            $WORKSPACE/cje-tf/scripts/fixup_rfcn.sh
            $WORKSPACE/cje-tf/scripts/fixup_transformerSpeech.sh
	    '''

            for (model in models) {
                for (mode in modes) {
                    withEnv(["model=$model","mode=$mode","ERROR=$ERROR", "single_socket=${SINGLE_SOCKET}"]) {
                        sh '''#!/bin/bash -x
                    source $WORKSPACE/venv/bin/activate
                    chmod 775 $WORKSPACE/cje-tf/scripts/run_benchmark_q2_models.sh
                    $WORKSPACE/cje-tf/scripts/run_benchmark_q2_models.sh --model=${model} --mode=${mode} --single_socket=${single_socket}
                    if [ $? -eq 0 ] ; then
                        echo "running model ${model} success"
                        RESULT="SUCCESS"
                    else
                        echo "running model ${model} fail"
                        RESULT="FAILURE"
                        ERROR="1"
                    fi
                    '''
                    }
                }
            }
        }

        stage('Collect logs') {

            // Prepare logs
            def prepareLog = load("${CJE_TF_COMMON_DIR}/prepareLog.groovy")
            prepareLog(PRIVATE_TENSORFLOW_BRANCH, GIT_NAME, RUN_TYPE, SUMMARYLOG, SUMMARY_TITLE)

            for (model in models) {
                for (mode in modes) {
                    withEnv(["model=$model","mode=$mode","ERROR=$ERROR", "single_socket=${SINGLE_SOCKET}", "fullvalidation=${FULL_VALIDATION}"]) {
                    sh '''#!/bin/bash -x
                    chmod 775 $WORKSPACE/cje-tf/scripts/collect_logs_q2_models.sh
                    $WORKSPACE/cje-tf/scripts/collect_logs_q2_models.sh --model=${model} --mode=${mode}  --fullvalidation=${fullvalidation}  --single_socket=${single_socket}
                    if [ $? -eq 0 ] ; then
                        echo "running model ${model} ${mode} success"
                        RESULT="SUCCESS"
                    else
                        echo "running model ${model} ${mode} fail"
                        RESULT="FAILURE"
                        ERROR="1"
                    fi
                    '''
                    }
                }
            }
            echo "done running benchmark ${ERROR} ...end"
            if ( ERROR.equals("1") )
                currentBuild.result = "UNSTABLE"
            stash allowEmpty: true, includes: "*.log", name: "logfile"
        }

    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    } finally {
        // Success or failure, always send notifications

        withEnv(["SUMMARYLOG=$SUMMARYLOG"]) {
            def msg = readFile SUMMARYLOG

            def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
            notifyBuild(SLACK_CHANNEL, currentBuild.result, msg)
        }

        // Success or failure, always do artifacts
        stage('Archive Artifacts / Test Results') {
            dir("$WORKSPACE" + "/publish") {
                unstash "logfile"
                unstash "wheelfile"

                archiveArtifacts artifacts: '*.log, build/*.whl', excludes: null
                fingerprint: true

                def server = Artifactory.server 'ubit-artifactory-or'
                def uploadSpec = """{
              "files": [
               {
                   "pattern": "*",
                   "target": "aipg-local/aipg-tf/${env.JOB_NAME}/${env.BUILD_NUMBER}/"
               }
               ]
            }"""
                def buildInfo = server.upload(uploadSpec)
                server.publishBuildInfo(buildInfo)

            }
            dir("$WORKSPACE") {
                archiveArtifacts artifacts: 'summary_nightly.log', excludes: null
            }
        }

        // only post to dashboard if POST_TO_DASHBOARD is set to true
        if (POST_TO_DASHBOARD) {

            stage('Post to AIBT dashboard') {

                def postAll2AIBTdashboard = load("${CJE_TF_COMMON_DIR}/postAll2AIBTdashboard.groovy")

                LOGS_DIR = "${WORKSPACE}"
                LOGS_TYPE = [ "latency", "throughput"]
                FRAMEWORK = 'tensorflow'
                FRONTEND = 'tensorflow'
                RUNTYPE = 'tfdo-inference'
                DATATYPE = 'None'
                postAll2AIBTdashboard(RUNTYPE, FRAMEWORK, FRONTEND, TARGET_DASHBOARD, LOGS_DIR, LOGS_TYPE,models, DATATYPE)

            } // stage
        } // if POST_TO_DASHBOARD

    } // finally

} // node

