GIT_CREDENTIAL = "lab_tfbot"
GIT_CREDENTIAL_LAB = "lab_tfbot"

http_proxy="http://proxy-us.intel.com:911"
https_proxy="https://proxy-us.intel.com:912"

echo "llsu-----> This is the right run docker benchmark intel model groovy!"

// set default value for NODE_LABEL
NODE_LABEL = 'nervana-skx101.fm.intel.com'
if ('NODE_LABEL' in params) {
    echo "NODE_LABEL in params"
    if (params.NODE_LABEL != '') {
        NODE_LABEL = params.NODE_LABEL
    }
}
echo "NODE_LABEL: $NODE_LABEL"

// set default value for CJT-TF BRANCH
CJE_TF_BRANCH = 'master'
if ('CJE_TF_BRANCH' in params) {
    echo "CJE_TF_BRANCH in params"
    if (params.CJE_TF_BRANCH != '') {
        CJE_TF_BRANCH = params.CJE_TF_BRANCH
    }
}
echo "CJE_TF_BRANCH:  $CJE_TF_BRANCH"

// setting CJE_ALGO branch
CJE_ALGO_BRANCH = '*/master'
if ('CJE_ALGO_BRANCH' in params) {
    echo "CJE_ALGO_BRANCH in params"
    if (params.CJE_ALGO_BRANCH != '') {
        CJE_ALGO_BRANCH = params.CJE_ALGO_BRANCH
        echo CJE_ALGO_BRANCH
    }
}
echo "CJE_ALGO_BRANCH:  $CJE_ALGO_BRANCH"

// set default INTEL_MODELS_URL value: 
// internal: https://gitlab.devtools.intel.com/intelai/models.git or
// external: https://github.com/IntelAI/models.git
INTEL_MODELS_URL = 'https://gitlab.devtools.intel.com/intelai/models.git'
if ('INTEL_MODELS_URL' in params) {
    echo "INTEL_MODELS_URL in params"
    if (params.INTEL_MODELS_URL != '') {
        INTEL_MODELS_URL = params.INTEL_MODELS_URL
    }
}
echo "INTEL_MODELS_URL: $INTEL_MODELS_URL"

// set default INTEL_MODELS_BRANCH value                
INTEL_MODELS_BRANCH = 'master'
if ('INTEL_MODELS_BRANCH' in params) {
    echo "INTEL_MODELS_BRANCH in params"
    if (params.INTEL_MODELS_BRANCH != '') {
        INTEL_MODELS_BRANCH = params.INTEL_MODELS_BRANCH
        echo INTEL_MODELS_BRANCH
    }
}
echo "INTEL_MODELS_BRANCH: $INTEL_MODELS_BRANCH"

// set default value for TENSORFLOW_BRANCH
TENSORFLOW_BRANCH = 'master'
if ('TENSORFLOW_BRANCH' in params){
    echo "TENSORFLOW_BRANCH in params"
    if (params.TENSORFLOW_BRANCH != '') {
        TENSORFLOW_BRANCH = params.TENSORFLOW_BRANCH
        echo TENSORFLOW_BRANCH
    }
}
echo "TENSORFLOW_BRANCH:  $TENSORFLOW_BRANCH"

DOCKER_IMAGE = ''
if ('DOCKER_IMAGE' in params) {
    echo "DOCKER_IMAGE in params"
    if (params.DOCKER_IMAGE != '') {
        DOCKER_IMAGE = params.DOCKER_IMAGE
        echo DOCKER_IMAGE
    }
}
echo "DOCKER_IMAGE: $DOCKER_IMAGE"

// setting SLACK_CHANNEL with some default value or get input from params
SLACK_CHANNEL = '#tensorflow-jenkins'
if ('SLACK_CHANNEL' in params) {
    echo "SLACK_CHANNEL in params"
    if (params.SLACK_CHANNEL != '') {
        SLACK_CHANNEL = params.SLACK_CHANNEL
        echo SLACK_CHANNEL
    }
}
echo "SLACK_CHANNEL: $SLACK_CHANNEL"

// setting SUMMARY_TITLE with some default value or get input from params
SUMMARY_TITLE = ''
if ('SUMMARY_TITLE' in params) {
    echo "SUMMARY_TITLE in params"
    if (params.SUMMARY_TITLE != '') {
        SUMMARY_TITLE = params.SUMMARY_TITLE
        echo SUMMARY_TITLE
    }
}
echo "SUMMARY_TITLE: $SUMMARY_TITLE"

MR_NUMBER = ''
if ('MR_NUMBER' in params) {
    echo "MR_NUMBER in params"
    if (params.MR_NUMBER != '') {
        MR_NUMBER = params.MR_NUMBER
        echo MR_NUMBER
    }
}
echo "MR_NUMBER: $MR_NUMBER"

MR_SOURCE_BRANCH = ''
if ('MR_SOURCE_BRANCH' in params) {
    echo "MR_SOURCE_BRANCH in params"
    if (params.MR_SOURCE_BRANCH != '') {
        MR_SOURCE_BRANCH = params.MR_SOURCE_BRANCH
        echo MR_SOURCE_BRANCH
    }
}
echo "MR_SOURCE_BRANCH: $MR_SOURCE_BRANCH"

MR_MERGE_BRANCH = ''
if ('MR_MERGE_BRANCH' in params) {
    echo "MR_MERGE_BRANCH in params"
    if (params.MR_MERGE_BRANCH != '') {
        MR_MERGE_BRANCH = params.MR_MERGE_BRANCH
        echo MR_MERGE_BRANCH
    }
}
echo "MR_MERGE_BRANCH: $MR_MERGE_BRANCH"

DOCKER_IMAGE_NAMESPACE = 'amr-registry.caas.intel.com/aipg-tf/mr'
if ('DOCKER_IMAGE_NAMESPACE' in params) {
    echo "DOCKER_IMAGE_NAMESPACE in params"
    if (params.DOCKER_IMAGE_NAMESPACE != '') {
        DOCKER_IMAGE_NAMESPACE = params.DOCKER_IMAGE_NAMESPACE
        echo DOCKER_IMAGE_NAMESPACE
    }
}
echo "DOCKER_IMAGE_NAMESPACE: $DOCKER_IMAGE_NAMESPACE"

GIT_NAME = 'tensorflow'
if ('GIT_NAME' in params) {
    echo "GIT_NAME in params"
    if (params.GIT_NAME != '') {
        GIT_NAME = params.GIT_NAME
        echo GIT_NAME
    }
}
echo "GIT_NAME: $GIT_NAME"

GIT_URL = 'https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git'
if ('GIT_URL' in params) {
    echo "GIT_URL in params"
    if (params.GIT_URL != '') {
        GIT_URL = params.GIT_URL
        echo GIT_URL
    }
}
echo "GIT_URL: $GIT_URL"

PYTHON_VERSION = ''
if ('PYTHON_VERSION' in params) {
    echo "PYTHON_VERSION in params"
    if (params.PYTHON_VERSION != '') {
        PYTHON_VERSION = params.PYTHON_VERSION
        echo PYTHON_VERSION
    }
}
echo "PYTHON_VERSION: $PYTHON_VERSION"

MODEL=""
if ('MODEL' in params) {
    echo "MODEL in params"
    if (params.MODEL != '') {
        MODEL = params.MODEL
        echo MODEL
    }
}
echo "MODEL: $MODEL"

// set default value for MODES to run
MODES="training,inference"
if ('MODES' in params) {
    echo "MODES in params"
    if (params.MODES != '') {
        MODES = params.MODES
        echo MODES
    }
}
echo "MODES: $MODES"

DATA_TYPE="fp32,int8"
if ('DATA_TYPE' in params) {
    echo "DATA_TYPE in params"
    if (params.DATA_TYPE != '') {
        DATA_TYPE = params.DATA_TYPE
        echo DATA_TYPE
    }
}
echo "DATA_TYPE: $DATA_TYPE"

// set default value for PERFORMANCE to run
PERFORMANCE="latency,throughput,accuracy"
if ('PERFORMANCE' in params) {
    echo "PERFORMANCE in params"
    if (params.PERFORMANCE != '') {
        PERFORMANCE = params.PERFORMANCE
        echo PERFORMANCE
    }
}
echo "PERFORMANCE: $PERFORMANCE"

// set default value for DATASET_LOCATION
DATASET_LOCATION = '/tf_dataset'
if ('DATASET_LOCATION' in params) {
    echo "DATASET_LOCATION in params"
    if (params.DATASET_LOCATION != '') {
        DATASET_LOCATION = params.DATASET_LOCATION
        echo DATASET_LOCATION
    }
}
echo "DATASET_LOCATION: $DATASET_LOCATION"

MODEL_PROP_DATA=""
if ('MODEL_PROP_DATA' in params) {
    echo "MODEL_PROP_DATA in params"
    if (params.MODEL_PROP_DATA != '') {
        MODEL_PROP_DATA = params.MODEL_PROP_DATA
        echo MODEL_PROP_DATA
    }
}
echo "MODEL_PROP_DATA: $MODEL_PROP_DATA"

MODEL_PROP_DATA_ACCURACY=""
if ('MODEL_PROP_DATA_ACCURACY' in params) {
    echo "MODEL_PROP_DATA_ACCURACY in params"
    if (params.MODEL_PROP_DATA_ACCURACY != '') {
        MODEL_PROP_DATA_ACCURACY = params.MODEL_PROP_DATA_ACCURACY
        echo MODEL_PROP_DATA_ACCURACY
    }
}
echo "MODEL_PROP_DATA_ACCURACY: $MODEL_PROP_DATA_ACCURACY"

MODEL_PROP_DATA_INT8=""
if ('MODEL_PROP_DATA_INT8' in params) {
    echo "MODEL_PROP_DATA_INT8 in params"
    if (params.MODEL_PROP_DATA_INT8 != '') {
        MODEL_PROP_DATA_INT8 = params.MODEL_PROP_DATA_INT8
        echo MODEL_PROP_DATA_INT8
    }
}
echo "MODEL_PROP_DATA_INT8: $MODEL_PROP_DATA_INT8"

MODEL_PROP_GRAPH_FP32=""
if ('MODEL_PROP_GRAPH_FP32' in params) {
    echo "MODEL_PROP_GRAPH_FP32 in params"
    if (params.MODEL_PROP_GRAPH_FP32 != '') {
        MODEL_PROP_GRAPH_FP32 = params.MODEL_PROP_GRAPH_FP32
        echo MODEL_PROP_GRAPH_FP32
    }
}
echo "MODEL_PROP_GRAPH_FP32: $MODEL_PROP_GRAPH_FP32"

MODEL_PROP_GRAPH_INT8=""
if ('MODEL_PROP_GRAPH_INT8' in params) {
    echo "MODEL_PROP_GRAPH_INT8 in params"
    if (params.MODEL_PROP_GRAPH_INT8 != '') {
        MODEL_PROP_GRAPH_INT8 = params.MODEL_PROP_GRAPH_INT8
        echo MODEL_PROP_GRAPH_INT8
    }
}
echo "MODEL_PROP_GRAPH_INT8: $MODEL_PROP_GRAPH_INT8"

BATCHSIZE_THROUGHPUT=""
if ('BATCHSIZE_THROUGHPUT' in params) {
    echo "BATCHSIZE_THROUGHPUT in params"
    if (params.BATCHSIZE_THROUGHPUT != '') {
        BATCHSIZE_THROUGHPUT = params.BATCHSIZE_THROUGHPUT
        echo BATCHSIZE_THROUGHPUT
    }
}
echo "BATCHSIZE_THROUGHPUT: $BATCHSIZE_THROUGHPUT"

BATCHSIZE_ACCURACY=""
if ('BATCHSIZE_ACCURACY' in params) {
    echo "BATCHSIZE_ACCURACY in params"
    if (params.BATCHSIZE_ACCURACY != '') {
        BATCHSIZE_ACCURACY = params.BATCHSIZE_ACCURACY
        echo BATCHSIZE_ACCURACY
    }
}
echo "BATCHSIZE_ACCURACY: $BATCHSIZE_ACCURACY"

MODEL_CHECK_POINT=""
if ('MODEL_CHECK_POINT' in params) {
    echo "MODEL_CHECK_POINT in params"
    if (params.CHECK_POINT != '') {
        MODEL_CHECK_POINT = params.MODEL_CHECK_POINT
        echo MODEL_CHECK_POINT
    }
}
echo "MODEL_CHECK_POINT: $MODEL_CHECK_POINT"

MODEL_DIR=""
if ('MODEL_DIR' in params) {
    echo "MODEL_DIR in params"
    if (params.MODEL_DIR != '') {
        MODEL_DIR = params.MODEL_DIR
        echo MODEL_DIR
    }
}
echo "MODEL_DIR: $MODEL_DIR"

// set default TENSORFLOW_MODELS_BRANCH value                
TENSORFLOW_MODELS_BRANCH = 'master'
if ('TENSORFLOW_MODELS_BRANCH' in params) {
    echo "TENSORFLOW_MODELS_BRANCH in params"
    if (params.TENSORFLOW_MODELS_BRANCH != '') {
        TENSORFLOW_MODELS_BRANCH = params.TENSORFLOW_MODELS_BRANCH
        echo TENSORFLOW_MODELS_BRANCH
    }
}
echo "TENSORFLOW_MODELS_BRANCH: $TENSORFLOW_MODELS_BRANCH"

// set default TENSORFLOW_TENSOR2TENSOR_BRANCH value
TENSORFLOW_TENSOR2TENSOR_BRANCH = 'master'
if ('TENSORFLOW_TENSOR2TENSOR_BRANCH' in params) {
    echo "TENSORFLOW_TENSOR2TENSOR_BRANCH in params"
    if (params.TENSORFLOW_TENSOR2TENSOR_BRANCH != '') {
        TENSORFLOW_TENSOR2TENSOR_BRANCH = params.TENSORFLOW_TENSOR2TENSOR_BRANCH
        echo TENSORFLOW_TENSOR2TENSOR_BRANCH
    }
}
echo "TENSORFLOW_TENSOR2TENSOR_BRANCH: $TENSORFLOW_TENSOR2TENSOR_BRANCH"

// set default MODEL_ARGS value
MODEL_ARGS = ''
if ('MODEL_ARGS' in params) {
    echo "MODEL_ARGS in params"
    if (params.MODEL_ARGS != '') {
        MODEL_ARGS = params.MODEL_ARGS
        echo MODEL_ARGS
    }
}
echo "MODEL_ARGS: $MODEL_ARGS"

// set default MODEL_ARGS_ACCURACY value
MODEL_ARGS_ACCURACY = ''
if ('MODEL_ARGS_ACCURACY' in params) {
    echo "MODEL_ARGS_ACCURACY in params"
    if (params.MODEL_ARGS_ACCURACY != '') {
        MODEL_ARGS_ACCURACY = params.MODEL_ARGS_ACCURACY
        echo MODEL_ARGS_ACCURACY
    }
}
echo "MODEL_ARGS_ACCURACY: $MODEL_ARGS_ACCURACY"

// set default MODEL_ARGS_FP32 value
MODEL_ARGS_FP32 = ''
if ('MODEL_ARGS_FP32' in params) {
    echo "MODEL_ARGS_FP32 in params"
    if (params.MODEL_ARGS_FP32 != '') {
        MODEL_ARGS_FP32 = params.MODEL_ARGS_FP32
        echo MODEL_ARGS_FP32
    }
}
echo "MODEL_ARGS_FP32: $MODEL_ARGS_FP32"

// set default MODEL_ARGS_INT8 value
MODEL_ARGS_INT8 = ''
if ('MODEL_ARGS_INT8' in params) {
    echo "MODEL_ARGS_INT8 in params"
    if (params.MODEL_ARGS_INT8 != '') {
        MODEL_ARGS_INT8 = params.MODEL_ARGS_INT8
        echo MODEL_ARGS_INT8
    }
}
echo "MODEL_ARGS_INT8: $MODEL_ARGS_INT8"

// set default MODEL_NUM_INTER_THREADS value
MODEL_NUM_INTER_THREADS = ''
if ('MODEL_NUM_INTER_THREADS' in params) {
    echo "MODEL_NUM_INTER_THREADS in params"
    if (params.MODEL_NUM_INTER_THREADS != '') {
        MODEL_NUM_INTER_THREADS = params.MODEL_NUM_INTER_THREADS
        echo MODEL_NUM_INTER_THREADS
    }
}
echo "MODEL_NUM_INTER_THREADS: $MODEL_NUM_INTER_THREADS"

// set default MODEL_NUM_INTRA_THREADS value
MODEL_NUM_INTRA_THREADS = ''
if ('MODEL_NUM_INTRA_THREADS' in params) {
    echo "MODEL_NUM_INTRA_THREADS in params"
    if (params.MODEL_NUM_INTRA_THREADS != '') {
        MODEL_NUM_INTRA_THREADS = params.MODEL_NUM_INTRA_THREADS
        echo MODEL_NUM_INTRA_THREADS
    }
}
echo "MODEL_NUM_INTRA_THREADS: $MODEL_NUM_INTRA_THREADS"

// set default MODEL_DATA_NUM_INTER_THREADS value
MODEL_DATA_NUM_INTER_THREADS = ''
if ('MODEL_DATA_NUM_INTER_THREADS' in params) {
    echo "MODEL_DATA_NUM_INTER_THREADS in params"
    if (params.MODEL_DATA_NUM_INTER_THREADS != '') {
        MODEL_DATA_NUM_INTER_THREADS = params.MODEL_DATA_NUM_INTER_THREADS
        echo MODEL_DATA_NUM_INTER_THREADS
    }
}
echo "MODEL_DATA_NUM_INTER_THREADS: $MODEL_DATA_NUM_INTER_THREADS"

// set default MODEL_DATA_NUM_INTRA_THREADS value
MODEL_DATA_NUM_INTRA_THREADS = ''
if ('MODEL_DATA_NUM_INTRA_THREADS' in params) {
    echo "MODEL_DATA_NUM_INTRA_THREADS in params"
    if (params.MODEL_DATA_NUM_INTRA_THREADS != '') {
        MODEL_DATA_NUM_INTRA_THREADS = params.MODEL_DATA_NUM_INTRA_THREADS
        echo MODEL_DATA_NUM_INTRA_THREADS
    }
}
echo "MODEL_DATA_NUM_INTRA_THREADS: $MODEL_DATA_NUM_INTRA_THREADS"

// check default value for POST_TO_DASHBOARD
Boolean POST_TO_DASHBOARD = false
if (params.POST_TO_DASHBOARD == 'true'){
    echo "params.POST_TO_DASHBOARD is true"
    POST_TO_DASHBOARD=true
}
else
    echo "params.POST_TO_DASHBOARD is false"
echo "POST_TO_DASHBOARD: ${POST_TO_DASHBOARD}"



// setting TARGET_DASHBOARD default to be production or get input from params
TARGET_DASHBOARD = 'production'
if ('TARGET_DASHBOARD' in params) {
    echo "TARGET_DASHBOARD in params"
    if (params.TARGET_DASHBOARD != '') {
        TARGET_DASHBOARD = params.TARGET_DASHBOARD
        echo TARGET_DASHBOARD
    }
}

// check if we want to download the wheel from somewhere 
Boolean DOWNLOAD_WHEEL = false
if (params.DOWNLOAD_WHEEL=="true"){
    echo "params.DOWNLOAD_WHEEL is true"
    DOWNLOAD_WHEEL=true
}
else
    echo "params.DOWNLOAD_WHEEL is false"
echo "DOWNLOAD_WHEEL: ${DOWNLOAD_WHEEL}"

// use this parameter only if DOWNLOAD_WHEEL is true 
// if this parameter is set, 
//     we'll download the build from the specified jenkin job's last successful artifact
//     along with the wheel's name pattern specified in the  DOWNLOAD_WHEEL_NAME
DOWNLOAD_WHEEL_FROM_JOB = ''
if ('DOWNLOAD_WHEEL_FROM_JOB' in params) {
    echo "DOWNLOAD_WHEEL_FROM_JOB in params"
    if (params.DOWNLOAD_WHEEL_FROM_JOB != '') {
        DOWNLOAD_WHEEL_FROM_JOB = params.DOWNLOAD_WHEEL_FROM_JOB
        echo DOWNLOAD_WHEEL_FROM_JOB
    }
}
echo "downloading build artifact from $DOWNLOAD_WHEEL_FROM_JOB jenkin job"

// this parameter specify which wheel to be downloaded
// use this parameter only if DOWNLOAD_WHEEL is true 
// use in conjunction with DOWNLOAD_WHEEL_FROM_JOB above, and specify the wheel's pattern 
DOWNLOAD_WHEEL_NAME = ''
if ('DOWNLOAD_WHEEL_NAME' in params) {
    echo "DOWNLOAD_WHEEL_NAME in params"
    if (params.DOWNLOAD_WHEEL_NAME != '') {
        DOWNLOAD_WHEEL_NAME = params.DOWNLOAD_WHEEL_NAME
        echo DOWNLOAD_WHEEL_NAME
    }
}
echo "downloading wheel from $DOWNLOAD_WHEEL_NAME"

// specify DOWNLOAD_WHEEL_FROM_JOB_NUMBER, if not set, default is lastSuccessfulBuild
DOWNLOAD_WHEEL_FROM_JOB_NUMBER = 'lastSuccessfulBuild'
if( 'DOWNLOAD_WHEEL_FROM_JOB_NUMBER' in params ) {
    echo "DOWNLOAD_WHEEL_FROM_JOB_NUMBER in params"
    if ( DOWNLOAD_WHEEL_FROM_JOB_NUMBER != '' ) {
        DOWNLOAD_WHEEL_FROM_JOB_NUMBER = params.DOWNLOAD_WHEEL_FROM_JOB_NUMBER
        echo DOWNLOAD_WHEEL_FROM_JOB_NUMBER
    }
}
echo " DOWNLOAD_WHEEL_FROM_JOB_NUMBER: $DOWNLOAD_WHEEL_FROM_JOB_NUMBER"


def cleanup() {

    try {
        sh '''#!/bin/bash -x
        cd $WORKSPACE
        sudo rm -rf *
        docker stop $(docker ps -a -q)
        echo Y | docker system prune -a                
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

def checkout_fastrcnn() {

    // tensorflow/models
    checkout([$class: 'GitSCM',
              branches: [[name: "$TENSORFLOW_MODELS_BRANCH"]],
              browser: [$class: 'AssemblaWeb', repoUrl: ''],
              doGenerateSubmoduleConfigurations: false,
              extensions: [[$class: 'RelativeTargetDirectory',
                            relativeTargetDir: 'models']],
              submoduleCfg: [],
              userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                               url: "https://github.com/tensorflow/models.git"]]])

    // cocoapi, checkout under models/cocoapi
    checkout([$class                           : 'GitSCM',
              branches                         : [[name: '*/master']],
              browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
              doGenerateSubmoduleConfigurations: false,
              extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                   relativeTargetDir: 'models/cocoapi']],
              submoduleCfg                     : [],
              userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL",
                                                   url          : 'https://github.com/cocodataset/cocoapi.git']]])
}

def checkout_maskrcnn() {

    checkout([$class: 'GitSCM',
              branches: [[name: '*/master']],
              browser: [$class: 'AssemblaWeb', repoUrl: ''],
              doGenerateSubmoduleConfigurations: false,
              extensions: [[$class: 'RelativeTargetDirectory',
                             relativeTargetDir: 'Mask-RCNN']],
              submoduleCfg: [],
              userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                   url: "https://github.com/matterport/Mask_RCNN.git"]]])

    // Mask-RCNN: MS coco API
    checkout([$class: 'GitSCM',
              branches: [[name: '*/master']],
              browser: [$class: 'AssemblaWeb', repoUrl: ''],
              doGenerateSubmoduleConfigurations: false,
              extensions: [[$class: 'RelativeTargetDirectory',
                             relativeTargetDir: 'Mask-RCNN/coco']],
              submoduleCfg: [],
              userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                   url: "https://github.com/waleedka/coco.git"]]])
}


def checkout_ncf() {

    TENSORFLOW_MODELS_NCF_BRANCH = 'v1.11'
    checkout([$class: 'GitSCM',
              branches: [[name: "$TENSORFLOW_MODELS_NCF_BRANCH"]],
              browser: [$class: 'AssemblaWeb', repoUrl: ''],
              doGenerateSubmoduleConfigurations: false,
              extensions: [[$class: 'RelativeTargetDirectory',
                            relativeTargetDir: 'models-ncf']],
              submoduleCfg: [],
              userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                   url: "https://github.com/tensorflow/models.git"]]])

    // ncf: intel-models checkout under the official tensorflow/models directory
    checkout([$class: 'GitSCM',
              branches: [[name: "$INTEL_MODELS_BRANCH"]],
              browser: [$class: 'AssemblaWeb', repoUrl: ''],
              doGenerateSubmoduleConfigurations: false,
              extensions: [[$class: 'RelativeTargetDirectory',
                            relativeTargetDir: 'models-ncf/intel-models']],
              submoduleCfg: [],
              userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                   url: "$INTEL_MODELS_URL"]]])
}

def checkout_rfcn() {

    TENSORFLOW_MODELS_RFCN_BRANCH = '6c21084503b27a9ab118e1db25f79957d5ef540b'
    checkout([$class: 'GitSCM',
              branches: [[name: "$TENSORFLOW_MODELS_RFCN_BRANCH"]],
              browser: [$class: 'AssemblaWeb', repoUrl: ''],
              doGenerateSubmoduleConfigurations: false,
              extensions: [[$class: 'RelativeTargetDirectory',
                            relativeTargetDir: 'models-rfcn']],
              submoduleCfg: [],
              userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                   url: "https://github.com/tensorflow/models.git"]]])
    echo "llsu---> git clone tensorflow pass?"
    sh 'pwd'
    //if( DATA_TYPE == 'int8'){
    echo "llsu---> git apply"
    sh '''#!/bin/bash -x
        echo "LLSu add git apply in rfcn"
        ls -lh
        echo "WORKSPACE = ${WORKSPACE}"
        cd ${WORKSPACE}/models-rfcn      
        git apply ${WORKSPACE}/intel-models/models/object_detection/tensorflow/rfcn/inference/tf-2.0.patch   
    '''
    //}

    // cocoapi, checkout under models-rfcn/cocoaip
    checkout([$class                           : 'GitSCM',
              branches                         : [[name: '*/master']],
              browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
              doGenerateSubmoduleConfigurations: false,
              extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                   relativeTargetDir: 'models-rfcn/cocoapi']],
              submoduleCfg                     : [],
              userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL",
                                                   url          : 'https://github.com/cocodataset/cocoapi.git']]])
}

def checkout_ssd_mobilenet() {

    TENSORFLOW_MODELS_SSD_MOBILENET_BRANCH = '20da786b078c85af57a4c88904f7889139739ab0'
    checkout([$class: 'GitSCM',
              branches: [[name: "$TENSORFLOW_MODELS_SSD_MOBILENET_BRANCH"]],
              browser: [$class: 'AssemblaWeb', repoUrl: ''],
              doGenerateSubmoduleConfigurations: false,
              extensions: [[$class: 'RelativeTargetDirectory',
                            relativeTargetDir: 'models-ssdmobilenet']],
              submoduleCfg: [],
              userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                   url: "https://github.com/tensorflow/models.git"]]])

    // cocoapi, checkout under models-ssdmobilenet/cocoaip
    checkout([$class                           : 'GitSCM',
              branches                         : [[name: '*/master']],
              browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
              doGenerateSubmoduleConfigurations: false,
              extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                   relativeTargetDir: 'models-ssdmobilenet/cocoapi']],
              submoduleCfg                     : [],
              userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL",
                                                   url          : 'https://github.com/cocodataset/cocoapi.git']]])
}

def checkout_ssd_resnet34() {

    TENSORFLOW_MODELS_SSD_RESNET34_BRANCH = 'f505cecde2d8ebf6fe15f40fb8bc350b2b1ed5dc'
    checkout([$class: 'GitSCM',
              branches: [[name: "$TENSORFLOW_MODELS_SSD_RESNET34_BRANCH"]],
              browser: [$class: 'AssemblaWeb', repoUrl: ''],
              doGenerateSubmoduleConfigurations: false,
              extensions: [[$class: 'RelativeTargetDirectory',
                            relativeTargetDir: 'models-ssdresnet34']],
              submoduleCfg: [],
              userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                   url: "https://github.com/tensorflow/models.git"]]])

    // cocoapi, checkout under models-ssdresnet34/cocoaip
    checkout([$class                           : 'GitSCM',
              branches                         : [[name: '*/master']],
              browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
              doGenerateSubmoduleConfigurations: false,
              extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                   relativeTargetDir: 'models-ssdresnet34/cocoapi']],
              submoduleCfg                     : [],
              userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL",
                                                   url          : 'https://github.com/cocodataset/cocoapi.git']]])
}

def checkout_ssd_vgg16() {

    SSD_TENSORFLOW_BRANCH = '2d8b0cb9b2e70281bf9dce438ff17ffa5e59075c'
    checkout([$class: 'GitSCM',
              branches: [[name: "$SSD_TENSORFLOW_BRANCH"]],
              browser: [$class: 'AssemblaWeb', repoUrl: ''],
              doGenerateSubmoduleConfigurations: false,
              extensions: [[$class: 'RelativeTargetDirectory',
                            relativeTargetDir: 'SSD.TensorFlow']],
              submoduleCfg: [],
              userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                   url: "https://github.com/HiKapok/SSD.TensorFlow.git"]]])

    checkout([$class                           : 'GitSCM',
              branches                         : [[name: '*/master']],
              browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
              doGenerateSubmoduleConfigurations: false,
              extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                   relativeTargetDir: 'SSD.TensorFlow/coco']],
              submoduleCfg                     : [],
              userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL",
                                                   url          : 'https://github.com/waleedka/coco.git']]])
}

def checkout_transformer_language() {

    checkout([$class: 'GitSCM',
              branches: [[name: "$TENSORFLOW_TENSOR2TENSOR_BRANCH"]],
              browser: [$class: 'AssemblaWeb', repoUrl: ''],
              doGenerateSubmoduleConfigurations: false,
              extensions: [[$class: 'RelativeTargetDirectory',
                            relativeTargetDir: 'tensor2tensor']],
              submoduleCfg: [],
              userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                   url: "https://github.com/tensorflow/tensor2tensor.git"]]])
}

def checkout_wide_deep() {
    checkout([$class: 'GitSCM',
              branches: [[name: '*/master']],
              browser: [$class: 'AssemblaWeb', repoUrl: ''],
              doGenerateSubmoduleConfigurations: false,
              extensions: [[$class: 'RelativeTargetDirectory',
                            relativeTargetDir: 'models-widedeep']],
              submoduleCfg: [],
              userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL",
                                   url: "https://github.com/tensorflow/models.git"]]])

    sh '''#!/bin/bash -x
        echo "fetch pull request first"
        echo "WORKSPACE = ${WORKSPACE}"
        cd ${WORKSPACE}/models-widedeep
        git fetch origin pull/7461/head:wide-deep-tf2                    
        git checkout wide-deep-tf2
    '''
}

def checkout_bert_official() {

    SSD_TENSORFLOW_BRANCH = '88a817c37f788702a363ff935fd173b6dc6ac0d6'
    checkout([$class                           : 'GitSCM',
              branches                         : [[name: "master"]],
              browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
              doGenerateSubmoduleConfigurations: false,
              extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                   relativeTargetDir: 'models-bert_official']],
              submoduleCfg                     : [],
              userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL",
                                                   url          : "https://github.com/google-research/bert.git"]]])
}

def runBenchmark(String model_name,String precision,String mode,Boolean accuracy,String framework,String batch_size,String socket_id,Boolean verbose,String in_docker_image,String in_data_location,String in_data_location_accuracy,String in_data_location_int8,String in_graph,String https_proxy,String http_proxy,String logfile, String model_dir, String checkpoint, String model_args, String num_inter_threads, String num_intra_threads, String data_num_inter_threads, String data_num_intra_threads, String model_args_accuracy, String model_args_fp32, String model_args_int8) {
                      
    withEnv(["model_name=$model_name", \
             "precision=$precision", \
             "mode=$mode", \
             "accuracy=$accuracy", \
             "framework=$framework", \
             "batch_size=$batch_size", \
             "socket_id=${socket_id}", \
             "verbose=${verbose}", \
             "in_docker_image=${in_docker_image}", \
             "in_data_location=${in_data_location}",\
             "in_data_location_accuracy=${in_data_location_accuracy}",\
             "in_data_location_int8=${in_data_location_int8}",\
             "in_graph=$in_graph", \
             "logfile=$logfile", \
             "model_sourcedir=$model_dir", \
             "model_checkpoint=$checkpoint", \
             "model_arguments=$model_args", \
             "model_num_inter_threads=$num_inter_threads", \
             "model_num_intra_threads=$num_intra_threads", \
             "model_data_num_inter_threads=$data_num_inter_threads", \
             "model_data_num_intra_threads=$data_num_intra_threads", \
             "model_arguments_accuracy=$model_args_accuracy", \
             "model_arguments_fp32=$model_args_fp32", \
             "model_arguments_int8=$model_args_int8"]) {

        sh '''#!/bin/bash -x
            echo "---------------------------------------------------------" 
   
            chmod 775 ${WORKSPACE}/cje-tf/scripts/run_benchmark_intel_models_v2.sh 
            ${WORKSPACE}/cje-tf/scripts/run_benchmark_intel_models_v2.sh    
                           
        '''

    } // withEnv
                 
}

node( NODE_LABEL) {
    // build steps that should happen on all nodes go here
    try {
        cleanup()
        deleteDir()
        
        SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",returnStdout: true).trim()
        SUMMARYLOG = "${WORKSPACE}/summary_${SERVERNAME}.log"

        stage("Checkout repository") {

            // cje-tf
            dir('cje-tf') {
                checkout scm
            }

            // check out TF source from different branch with different refspec depending on: 
            //     - if MR       : merge MR branch
            //     - if nightly  : master branch
            //     - if release  : release branch
            if (MR_NUMBER == '') {
                branch = "$TENSORFLOW_BRANCH"
                refspec = "+refs/heads/*:refs/remotes/$GIT_NAME/*"

                // tensorflow
                checkout([$class                           : 'GitSCM',
                          branches                         : [[name: "$branch"]],
                          browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                          doGenerateSubmoduleConfigurations: false,
                          extensions                       : [[$class: 'RelativeTargetDirectory',
                                                               relativeTargetDir: "$GIT_NAME"]],
                          submoduleCfg                     : [],
                          userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                               refspec: "${refspec}",
                                                               name: GIT_NAME,
                                                               url: "$GIT_URL"]]])
            }
            else {

                checkout([$class                        : 'GitSCM',
                    branches                            : [[name: "$MR_SOURCE_BRANCH"]],
                    doGenerateSubmoduleConfigurations   : false,
                    extensions                          : [
                                                            [$class: 'PreBuildMerge', 
                                                                options: [
                                                                        fastForwardMode: 'FF', 
                                                                        mergeRemote: "$GIT_NAME", 
                                                                        mergeStrategy: 'DEFAULT', 
                                                                        mergeTarget: "$MR_MERGE_BRANCH"
                                                                ]
                                                            ],
                                                            [$class: 'RelativeTargetDirectory',
                                                            relativeTargetDir: "$GIT_NAME"
                                                            ]
                                                          ],
                    submoduleCfg                        : [],
                    userRemoteConfigs                   : [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                                           name: GIT_NAME,
                                                           url: "$GIT_URL"]]
                    ])
            }

            // intel-models
            // this is required for all the models
            if ( MODEL == "bert" ){
                echo "INTEL_MODELS_BRANCH is $INTEL_MODELS_BRANCH"
                INTEL_MODELS_BRANCH = 'tencent/bert_tf2'
            }

            // intel-models for ssd-resnet34
            if ( MODEL == "ssd-resnet34" ) {
                INTEL_MODELS_BRANCH = 'guizili/ssdrn34_fix'
                echo "INTEL_MODELS_BRANCH is $INTEL_MODELS_BRANCH"
            }



            checkout([$class: 'GitSCM',
                      branches: [[name: "$INTEL_MODELS_BRANCH"]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: 'intel-models']],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$GIT_CREDENTIAL_LAB",
                                           url: "$INTEL_MODELS_URL"]]])

            echo "llsu--->The MODEL is ${MODEL}!"
            // fastrcnn
            if ( MODEL == "fastrcnn" ) {
                checkout_fastrcnn()
            }


            // Mask-RCNN
            if ( MODEL == "maskrcnn" ) {
                checkout_maskrcnn()
            }

            // ncf: tensorflow/models checkout with specific tag
            if ( MODEL == "ncf" ) {
                checkout_ncff()
            }

            // rfcn
            // tensorflow/models 
            if ( MODEL == "rfcn" ) {
                checkout_rfcn()
                echo "model is rfcn, numpy version 1.18.1 is incompatible with pycocotools==2.0.0, so set numpy==1.17.1"
                dir ( "$WORKSPACE/intel-models/benchmarks/object_detection/tensorflow/rfcn" ) {
                    sh'''#!/bin/bash -x
                       sed -i '1i numpy==1.17.1' requirements.txt     
                    '''
                }

            }

            // ssd-mobilenet
            // tensorflow/models - need tensorflow/models checkout with specific commit
            if ( MODEL == "ssd-mobilenet" ) {
                checkout_ssd_mobilenet()
            }

            // ssd-resnet34
            if ( MODEL == "ssd-resnet34" ) {
                checkout_ssd_resnet34()
                echo "model is ssd_resnet34, numpy version 1.18.1 is incompatible with pycocotools==2.0.0, so set numpy==1.17.1"
                dir ( "$WORKSPACE/intel-models/benchmarks/object_detection/tensorflow/ssd-resnet34" ) {
                    sh'''#!/bin/bash -x
                       sed -i '$i numpy==1.17.1' requirements.txt     
                    '''
                }

            }

            // ssd_vgg16
            if ( MODEL == "ssd_vgg16" ) {
                checkout_ssd_vgg16()
            }

            // transformer_language: checkout tensor2tensor with specific tag
            if ( MODEL == "transformer_language" ) {

                checkout_transformer_language()
            }

            // wide_deep
            // tensorflow/models - need tensorflow/models checkout with specific tag
            if ( MODEL == "wide_deep" ) {
                checkout_wide_deep()
            }

            //bert_official
            if ( MODEL == "bert_official" ) {
                checkout_bert_official()
            }

        } // stage "Checkout repository"
                
        stage("Run Model Performance Test") {

            String LOCAL_IN_GRAPH = ''
            String LOCAL_PY_VERSION = ''
           
            if ( MR_NUMBER != '' ) {
                // MR testing, find the corresponding docker container's tag based on MR_NUMBER, sha and PYTHON_VERSION
                dir ( "$WORKSPACE/$GIT_NAME" ) {
                    target_sha = sh (script:'git log -1 | grep Merge: | awk -F\' \' \'{print $2}\' | cut -c -7',
                              returnStdout: true).trim()
                    source_sha = sh (script:'git log -1 | grep Merge: | awk -F\' \' \'{print $3}\' | cut -c -7',
                              returnStdout: true).trim()
                    echo target_sha
                    echo source_sha
                    sha = "$target_sha$source_sha"
                    echo "sha is $sha"
                }
                docker_build_version="MR${MR_NUMBER}-${sha}" 
                echo docker_build_version
                if ( "${PYTHON_VERSION}" == "2" ) {
                    DOCKER_IMAGE = "${DOCKER_IMAGE_NAMESPACE}:${docker_build_version}-TF-v2-avx2-devel-mkl"
                    LOCAL_PY_VERSION = "py2"
                }
                else {
                    DOCKER_IMAGE = "${DOCKER_IMAGE_NAMESPACE}:${docker_build_version}-TF-v2-avx2-devel-mkl-py3"
                    LOCAL_PY_VERSION = "py3"
                }
            }
            else {
                // nightly or release testing, pickup the docker container as is defined in DOCKER_IMAGE
                if ( "${DOCKER_IMAGE}".contains("py3") ){
                    LOCAL_PY_VERSION = sh(script: "echo $DOCKER_IMAGE | rev | cut -d'-' -f 1 | rev",
                        returnStdout: true).trim()
                }
                else
                    LOCAL_PY_VERSION = "py2"

                if (DOWNLOAD_WHEEL) {

                    //def downloadWheel = load("${WORKSPACE}/cje-tf/common/downloadWheel.groovy")
                    //downloadWheel(DOWNLOAD_WHEEL_FROM_JOB, DOWNLOAD_WHEEL_NAME)

                    withEnv(["dockerImage=$DOCKER_IMAGE"]) {
                        py_version = sh (script:'echo $dockerImage | awk -F\'-\' \'{print $NF}\' | sed \'s/[^0-9]*//g\'', returnStdout: true).trim()
                        echo "py_verion: $py_version"
                    }
                    echo "py_version is: $py_version"
                    //download_wheel_name="*$py_version*.whl"
                    //download_wheel_name="*${py_version}m-manylinux2010_x86_64.whl"
                    DOWNLOAD_WHEEL_NAME="*${py_version}m-manylinux2010_x86_64.whl"
                    catchError {
                        copyArtifacts(
                                projectName: DOWNLOAD_WHEEL_FROM_JOB,
                                selector: specific("$DOWNLOAD_WHEEL_FROM_JOB_NUMBER"),
                                filter: DOWNLOAD_WHEEL_NAME,
                                fingerprintArtifacts: true,
                                target: "build/")

                        archiveArtifacts artifacts: "build/**"
                    }

                    withEnv([ "dockerImage=$DOCKER_IMAGE" ]) {
                        sh '''#!/bin/bash -x
                            docker_namespace=$(echo ${dockerImage} | cut -f1 -d:)
                            echo $docker_namespace
                            docker_tag=$(echo ${dockerImage} | cut -f2 -d:)
                            echo $docker_tag
                            docker run --init --name=test-${docker_tag} -d -u root:root -v ${WORKSPACE}:/workspace -e "WORKSPACE=/workspace" -e http_proxy=http://proxy-us.intel.com:911 -e https_proxy=https://proxy-us.intel.com:912 ${dockerImage} tail -f /dev/null
                            docker exec test-${docker_tag} /workspace/cje-tf/scripts/install_tf_wheel.sh
                            docker commit test-${docker_tag} local:qa-${docker_tag}
                            docker push local:qa-${docker_tag}
                            docker stop test-${docker_tag}
                        '''
                    }
                    docker_tag = sh (script:"echo ${DOCKER_IMAGE} | cut -f2 -d:",
                            returnStdout: true).trim()
                    DOCKER_IMAGE="local:qa-${docker_tag}"
                }
            }
            echo "docker image is ${DOCKER_IMAGE}"
            echo "LOCAL_PY_VERSION is ${LOCAL_PY_VERSION}"

            mode_array=MODES.split(',')
            performance_array=PERFORMANCE.split(',')
            data_type_arry=DATA_TYPE.split(',')

            dir ( WORKSPACE ) {
                // for inference mode
                if ('inference' in mode_array) {
                    echo "Running in Inference Mode"
                    data_type_arry.each { precision ->
                        echo "precision is ${precision}"
                        if ( precision == 'int8' ){ 
                            LOCAL_IN_GRAPH=MODEL_PROP_GRAPH_INT8
                        }
                        else {
                            LOCAL_IN_GRAPH=MODEL_PROP_GRAPH_FP32
                        }

                        // for model mlperf_gnmt, we need to copy additional dependencies tensorflow-addon whls for docker to install 
                        if ( MODEL == "mlperf_gnmt") {
                            job_name='get-tensorflow-addon-wheels'
                            catchError {
                                copyArtifacts(
                                    projectName: job_name,
                                    selector: specific("9"),
                                    filter: 'addon-artifact/*.whl',
                                    fingerprintArtifacts: true,
                                    target: "")

                                    archiveArtifacts artifacts: "addon-artifact/*.whl"
                            }
                            sh '''#!/bin/bash -x
                                ls -l addon-artifact/tensorflow_addons*.whl
                                cp addon-artifact/tensorflow_addons*.whl intel-models/benchmarks
                                ls -l intel-models/benchmarks/tensorflow_addons*.whl
                            ''' 

                        }

                        // accuracy
                        if ('accuracy' in performance_array){
                            runBenchmark("$MODEL", "$precision", "inference", true, "tensorflow", "${BATCHSIZE_ACCURACY}", "0", true, "$DOCKER_IMAGE", "${DATASET_LOCATION}/$MODEL_PROP_DATA", "${DATASET_LOCATION}/$MODEL_PROP_DATA_ACCURACY", "${DATASET_LOCATION}/$MODEL_PROP_DATA_INT8", "${DATASET_LOCATION}/${LOCAL_IN_GRAPH}","${https_proxy}", "${http_proxy}", "${WORKSPACE}/benchmark_${MODEL}_inference_${precision}_accuracy_${LOCAL_PY_VERSION}_${SERVERNAME}.log", "${MODEL_DIR}", "${DATASET_LOCATION}/${MODEL_CHECK_POINT}", "${MODEL_ARGS}", "${MODEL_NUM_INTER_THREADS}", "${MODEL_NUM_INTRA_THREADS}", "${MODEL_DATA_NUM_INTER_THREADS}", "${MODEL_DATA_NUM_INTRA_THREADS}", "${MODEL_ARGS_ACCURACY}", "${MODEL_ARGS_FP32}", "${MODEL_ARGS_INT8}")
                        }

                        // latency
                        if ('latency' in performance_array){
                            runBenchmark("$MODEL", "$precision", "inference", false, "tensorflow", "1", "0", true, "$DOCKER_IMAGE", "${DATASET_LOCATION}/$MODEL_PROP_DATA", "${DATASET_LOCATION}/$MODEL_PROP_DATA_ACCURACY","${DATASET_LOCATION}/$MODEL_PROP_DATA_INT8", "${DATASET_LOCATION}/${LOCAL_IN_GRAPH}", "${https_proxy}", "${http_proxy}", "${WORKSPACE}/benchmark_${MODEL}_inference_${precision}_latency_${LOCAL_PY_VERSION}_${SERVERNAME}.log", "${MODEL_DIR}", "${DATASET_LOCATION}/${MODEL_CHECK_POINT}", "${MODEL_ARGS}", "${MODEL_NUM_INTER_THREADS}", "${MODEL_NUM_INTRA_THREADS}", "${MODEL_DATA_NUM_INTER_THREADS}", "${MODEL_DATA_NUM_INTRA_THREADS}", "${MODEL_ARGS_ACCURACY}", "${MODEL_ARGS_FP32}", "${MODEL_ARGS_INT8}")
                        }
                    
                        // throughput
                        if ('throughput' in performance_array){
                            runBenchmark("$MODEL", "$precision", "inference", false, "tensorflow", "${BATCHSIZE_THROUGHPUT}", "0", true, "$DOCKER_IMAGE", "${DATASET_LOCATION}/$MODEL_PROP_DATA", "${DATASET_LOCATION}/$MODEL_PROP_DATA_ACCURACY", "${DATASET_LOCATION}/$MODEL_PROP_DATA_INT8", "${DATASET_LOCATION}/${LOCAL_IN_GRAPH}","${https_proxy}", "${http_proxy}", "${WORKSPACE}/benchmark_${MODEL}_inference_${precision}_throughput_${LOCAL_PY_VERSION}_${SERVERNAME}.log", "${MODEL_DIR}", "${DATASET_LOCATION}/${MODEL_CHECK_POINT}", "${MODEL_ARGS}", "${MODEL_NUM_INTER_THREADS}", "${MODEL_NUM_INTRA_THREADS}", "${MODEL_DATA_NUM_INTER_THREADS}", "${MODEL_DATA_NUM_INTRA_THREADS}", "${MODEL_ARGS_ACCURACY}", "${MODEL_ARGS_FP32}", "${MODEL_ARGS_INT8}")
                        }

                    } // each
                } // if inference

                // for training mode
                if ('training' in mode_array) {
                }
                
            }  // dir
                 
        }  // stage
                
    } catch (e) {

          // If there was an exception thrown, the build failed
          currentBuild.result = "FAILED"
          throw e

    } finally {

        stage('Archive Artifacts / Test Results') {
            archiveArtifacts artifacts: '*.log', excludes: null
                                fingerprint: true

        }

        if (POST_TO_DASHBOARD) {

            stage('Post to dashboard') {

                workspace_volumn="${WORKSPACE}:/workspace"
                dataset_volume="${DATASET_LOCATION}:${DATASET_LOCATION}"

                // get commit info inside the docker
                docker.image("$DOCKER_IMAGE").inside("--env \"http_proxy=${http_proxy}\" \
                        --env \"https_proxy=${https_proxy}\" \
                        --volume ${workspace_volumn} \
                        --volume ${dataset_volume} \
                        --env DATASET_LOCATION=$DATASET_LOCATION \
                        --privileged \
                        -u root:root") {
                        sh '''#!/bin/bash -x 
                        if [ -d "/private-tensorflow" ]; then
                            python /workspace/cje-tf/scripts/get_tf_sourceinfo.py --tensorflow_dir=/private-tensorflow --workspace_dir=/workspace
                        elif [ -d "/tensorflow" ]; then
                            python /workspace/cje-tf/scripts/get_tf_sourceinfo.py --tensorflow_dir=/tensorflow --workspace_dir=/workspace
                        fi
                        '''
                }

                // clone cje-algo for dashboard posting
                checkout([$class                           : 'GitSCM',
                          branches                         : [[name: CJE_ALGO_BRANCH]],
                          browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                          doGenerateSubmoduleConfigurations: false,
                          extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                               relativeTargetDir: 'cje-algo']],
                          submoduleCfg                     : [],
                          userRemoteConfigs                : [[credentialsId: "$GIT_CREDENTIAL",
                                                               url          : 'https://github.intel.com/AIPG/cje-algo.git']]])

                def post2AIBTdashboard = load("${WORKSPACE}/cje-tf/common/post2AIBTdashboard.groovy")

                logs_dir = "${WORKSPACE}"
                framework = 'tensorflow'
                frontend = 'tensorflow'                

                data_types=DATA_TYPE.split(',')
                data_types.each { data_type ->
                    def benchmark_logs = sh (script: "ls ${WORKSPACE}/benchmark*${data_type}*.log", returnStdout: true).trim()
                    if (data_type == "fp32") {
                        datatype = 'float32'
                    }
                    else {
                        datatype = data_type
                    }
                    for(String benchmark_log : benchmark_logs.split("\\r?\\n")) { 
                        if (benchmark_log.contains("accuracy")) {
                            run_type = 'tfdo-accuracy'
                        }
                        else {
                            run_type = 'tfdo-inference'
                        }
                        post2AIBTdashboard(run_type, framework, frontend, TARGET_DASHBOARD, logs_dir, benchmark_log, MODEL, datatype)
                    }
                }
        
            } // stage 

        } // if POST_TO_DASHBOARD 
    
    }  // finally
    
    echo "===== ${env.BUILD_URL}, ${env.JOB_NAME},${env.BUILD_NUMBER} ====="

} // node

