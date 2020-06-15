// Dynamic function cloneTFBenchmark(q1models, q2models)
//
// Parameters:
//
//     q1models     clone q1 models
//                  q1 models repo including: 
//                      resnet50, 
//                      inception3, 
//                      vgg16, 
//                      ds2, 
//                      SSDvgg16, 
//                      mnist, 
//                      resnet32cifar10, 
//                      cifar10, 
//                      dcgan
//
//     q2models     clone q2 models 
//                  q2 models repo including: 
//                     3DGAN, 
//                     A3C, 
//                     DCGAN, 
//                     deepSpeech, 
//                     DRAW, 
//                     fastrcnn, 
//                     gnmt, 
//                     inception_resnet_v2,
//                     inceptionv3, 
//                     inception_v4,
//                     mobilenet_v1, 
//                     resnet50
//                     rfcn, 
//                     SqueezeNet, 
//                     transformerLanguage, 
//                     transformerSpeech, 
//                     WaveNet, 
//                     wideDeep, 
//                     WaveNet_Magenta, 
//                     UNet,  
//                     YoloV2, 
//
// Returns: nothing
//
// External dependencies: None

String GIT_CREDENTIAL = "lab_tfbot"
String GIT_CREDENTIAL_LAB = "lab_tfbot"

def call(Boolean runQ1Models, Boolean runQ2Models) {

    echo "------- running cloneTFBenchmark -------"

    if ( runQ1Models ) {
        cloneQ1Models()
    }

    if ( runQ2Models ) {
        cloneQ2Models()
    }
}

return this;

def cloneQ1Models() {

    echo "------- running cloneQ1Models -------"
 
    TENSORFLOW_SSD_VGG_16_INFERENCE_BRANCH = 'andrew/fp32-SSDvgg16'
    TENSORFLOW_SSD_VGG_16_TRAINING_BRANCH = 'NCHW'
    PRIVATE_TENSORFLOW_BENCHMARKS_BRANCH = 'master'
    TENSORFLOW_CIFAR10_BRANCH = 'v1.13.0'

    try { 

        checkout([$class: 'GitSCM', 
                  branches: [[name: PRIVATE_TENSORFLOW_BENCHMARKS_BRANCH]], 
                  browser: [$class: 'AssemblaWeb', repoUrl: ''], 
                  doGenerateSubmoduleConfigurations: false, 
                  extensions: [[$class: 'RelativeTargetDirectory', 
                                relativeTargetDir: 'private-tensorflow-benchmarks']], 
                  submoduleCfg: [], 
                  userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                       url: 'https://gitlab.devtools.intel.com/TensorFlow/Shared/private-tensorflow-benchmarks.git']]])

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
        
        // tensorflow-SSD - SSDvgg16 Inference
        checkout([$class: 'GitSCM', 
                  branches: [[name: TENSORFLOW_SSD_VGG_16_INFERENCE_BRANCH]], 
                  browser: [$class: 'AssemblaWeb', repoUrl: ''], 
                  doGenerateSubmoduleConfigurations: false, 
                  extensions: [[$class: 'RelativeTargetDirectory', 
                                relativeTargetDir: 'tensorflow-SSD-Inference']], 
                  submoduleCfg: [], 
                  userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                       url: 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-SSD.git']]])

        // tensorflow-SSD - SSDvgg16 Inference
        checkout([$class: 'GitSCM', 
                  branches: [[name: TENSORFLOW_SSD_VGG_16_TRAINING_BRANCH]], 
                  browser: [$class: 'AssemblaWeb', repoUrl: ''], 
                  doGenerateSubmoduleConfigurations: false, 
                  extensions: [[$class: 'RelativeTargetDirectory', 
                                relativeTargetDir: 'tensorflow-SSD-Training']], 
                  submoduleCfg: [], 
                  userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                       url: 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-SSD.git']]])

        // tensorflow-SSD - SSDvgg16 Training
        checkout([$class: 'GitSCM', 
                  branches: [[name: TENSORFLOW_SSD_VGG_16_TRAINING_BRANCH]], 
                  browser: [$class: 'AssemblaWeb', repoUrl: ''], 
                  doGenerateSubmoduleConfigurations: false, 
                  extensions: [[$class: 'RelativeTargetDirectory', 
                                relativeTargetDir: 'tensorflow-SSD-Training']], 
                  submoduleCfg: [], 
                  userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                       url: 'https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-SSD.git']]])

        // models - cifar10, resnet32 w/cifar10
        checkout([$class: 'GitSCM', 
                  branches: [[name: TENSORFLOW_CIFAR10_BRANCH ]], 
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
        

    } catch(e) {

        echo "===================="
        echo "ERROR: Exception caught in module which clones the tensorflow repo - cloneTFBenchmarkQ1Models()"
        echo "ERROR: ${e}"
        echo "===================="

        echo ' '
        echo "Build marked as FAILURE"
        currentBuild.result = 'FAILURE'

    }  // catch
}


def cloneQ2Models() {

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
    TRANSFORMER_LANGUAGE_BRANCH='*/master'

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

    echo "------- running cloneQ2Models -------"

    try { 

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
                  userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
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
           
        // Shanghai OOB models 
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
                  userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
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

        // inception_v3, resnet50
        checkout([$class: 'GitSCM',
                  branches: [[name: "${TENSORFLOW_INFERENCE_BRANCH}"]],
                  browser: [$class: 'AssemblaWeb', repoUrl: ''],
                  doGenerateSubmoduleConfigurations: false,
                  extensions: [[$class: 'RelativeTargetDirectory',
                                relativeTargetDir: "${TENSORFLOW_INFERENCE}"]],
                  submoduleCfg: [],
                  userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                       url: "${TENSORFLOW_INFERENCE_URL}"]]])

    } catch(e) {

        echo "===================="
        echo "ERROR: Exception caught in module which clones the tensorflow repo - cloneTFBenchmarkQ2Models()"
        echo "ERROR: ${e}"
        echo "===================="

        echo ' '
        echo "Build marked as FAILURE"
        currentBuild.result = 'FAILURE'

    }  // catch
}

