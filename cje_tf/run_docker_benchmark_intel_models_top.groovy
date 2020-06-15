// This scripts lauches the benchmark run from intel-model zoo:
// Currently 4 jobs using this script:
//     - Intel-Models-Benchmark-fp32-py3-Trigger: running fp32 models in py3
//         - maskrcnn
//         - ssd_vgg16
//     - Intel-Models-Benchmark-fp32-Trigger: running fp32 models in py2
//         - faster_rcnn
//         - inception_resnet_v2
//         - inceptionv3
//         - inceptionv4
//         - mobilenet_v1
//         - ncf
//         - resnet101
//         - resnet50
//         - resnet50v1_5
//         - rfcn
//         - ssd-mobilenet
//         - transformer_language
//         - wide_deep
//     - Intel-Models-Benchmark-int8-py3-Trigger: running int8 models in py3
//         - - ssd_vgg16
//         - wide_deep_large_ds
//     - Intel-Models-Benchmark-int8-Trigger: running int8 models in py2
//         - inception_resnet_v2
//         - inceptionv3
//         - inceptionv4
//         - resnet101
//         - resnet50
//         - resnet50v1_5
//         - rfcn
//         - ssd-mobilenet
// models integrated on Jenkin:
//     - faster_rcnn(py2, fp32/int8)
//     - inception_resnet_v2(py2, fp32/int8)
//     - inceptionv3(py2, fp32/int8)
//     - inceptionv4(py2, fp32/int8)
//     - maskrcnn(py3, fp32)
//     - mobilenet_v1(py2, fp32) 
//     - NCF(py2, fp32)
//     - resnet50(py2, fp32/int8)
//     - resnet50v1_5(py2, fp32/int8)
//     - resnet101(py2, fp32/int8)
//     - rfcn(py2, fp32/int8)
//     - ssd-mobilenet(py2, fp32/int8)
//     - ssd_vgg16(py3, fp32/int8))
//     - transformer_language(py2, fp32)
//     - wide_deep(py2, fp32)
//     - wide_deep_large_ds(py3, fp32/int8)

import groovy.json.JsonSlurperClassic

Boolean SINGLE_SOCKET = true
CJE_TF = 'cje-tf'
CJE_TF_COMMON_DIR = "$CJE_TF/common"
GIT_CREDENTIAL = "lab_tfbot"

http_proxy="http://proxy-us.intel.com:911"
https_proxy="https://proxy-us.intel.com:912"

// set default value for NODE_LABEL
NODE_LABEL = 'nervana-skx101.fm.intel.com'
if ('NODE_LABEL' in params) {
    echo "NODE_LABEL in params"
    if (params.NODE_LABEL != '') {
        NODE_LABEL = params.NODE_LABEL
    }
}
echo "NODE_LABEL: $NODE_LABEL"

NODE_MODEL_LABEL = 'aipg-ra-skx-51.ra.intel.com'
if ('NODE_MODEL_LABEL' in params) {
    echo "NODE_MODEL_LABEL in params"
    if (params.NODE_MODEL_LABEL != '') {
        NODE_MODEL_LABEL = params.NODE_MODEL_LABEL
    }
}
echo "NODE_MODEL_LABEL: $NODE_MODEL_LABEL"

// set default value for CJT-TF BRANCH
CJE_TF_BRANCH = 'master'
if ('CJE_TF_BRANCH' in params) {
    echo "CJE_TF_BRANCH in params"
    if (params.CJE_TF_BRANCH != '') {
        CJE_TF_BRANCH = params.CJE_TF_BRANCH
        echo CJE_TF_BRANCH
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
SUMMARY_TITLE = 'intel-model zoo nightly benchmark'
if ('SUMMARY_TITLE' in params) {
    echo "SUMMARY_TITLE in params"
    if (params.SUMMARY_TITLE != '') {
        SUMMARY_TITLE = params.SUMMARY_TITLE
        echo SUMMARY_TITLE
    }
}
echo "SUMMARY_TITLE: $SUMMARY_TITLE"

// set default INTEL_MODELS_URL value: 
// internal: https://gitlab.devtools.intel.com/intelai/models.git or
// external: https://github.com/IntelAI/models.git
INTEL_MODELS_URL = 'https://gitlab.devtools.intel.com/intelai/models.git'
if ('INTEL_MODELS_URL' in params) {
    echo "INTEL_MODELS_URL in params"
    if (params.INTEL_MODELS_URL != '') {
        INTEL_MODELS_URL = params.INTEL_MODELS_URL
        echo INTEL_MODELS_URL
    }
}
echo "INTEL_MODELS_BRANCH: $INTEL_MODELS_BRANCH"

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

DOCKER_IMAGE = 'amr-registry.caas.intel.com/aipg-tf/qa:nightly-master-avx2-devel-mkl'
if ('DOCKER_IMAGE' in params) {
    echo "DOCKER_IMAGE in params"
    if (params.DOCKER_IMAGE != '') {
        DOCKER_IMAGE = params.DOCKER_IMAGE
        echo DOCKER_IMAGE
    }
}
echo "DOCKER_IMAGE: $DOCKER_IMAGE"

// set default value for MODELS to run
// TODO: list the Q1 2019 fp32 models here separated by comma
MODELS="inceptionv3,resnet50"
if ('MODELS' in params) {
    echo "MODELS in params"
    if (params.MODELS != '') {
        MODELS = params.MODELS
        echo MODELS
    }
}
echo "MODELS: $MODELS"

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

// set tensor2tensor branch/commit TENSORFLOW_TENSOR2TENSOR_BRANCH
TENSORFLOW_TENSOR2TENSOR_BRANCH = 'master'
if ('TENSORFLOW_TENSOR2TENSOR_BRANCH' in params) {
    echo "TENSORFLOW_TENSOR2TENSOR_BRANCH in params"
    if (params.TENSORFLOW_TENSOR2TENSOR_BRANCH != '') {
        TENSORFLOW_TENSOR2TENSOR_BRANCH = params.TENSORFLOW_TENSOR2TENSOR_BRANCH
        echo TENSORFLOW_TENSOR2TENSOR_BRANCH
    }
}
echo "TENSORFLOW_TENSOR2TENSOR_BRANCH: $TENSORFLOW_TENSOR2TENSOR_BRANCH"

TENSORFLOW_BRANCH = 'master'
if ('TENSORFLOW_BRANCH' in params) {
    echo "TENSORFLOW_BRANCH in params"
    if (params.TENSORFLOW_BRANCH != '') {
        TENSORFLOW_BRANCH = params.TENSORFLOW_BRANCH
        echo TENSORFLOW_BRANCH
    }
}
echo "TENSORFLOW_BRANCH: $TENSORFLOW_BRANCH"

PR_NUMBER = ''
if ('PR_NUMBER' in params) {
    echo "PR_NUMBER in params"
    if (params.PR_NUMBER != '') {
        PR_NUMBER = params.PR_NUMBER
        echo PR_NUMBER
    }
}
echo "PR_NUMBER: $PR_NUMBER"

DOCKER_IMAGE_NAMESPACE = 'amr-registry.caas.intel.com/aipg-tf/pr'
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

PYTHON_VERSION = '3'
if ('PYTHON_VERSION' in params) {
    echo "PYTHON_VERSION in params"
    if (params.PYTHON_VERSION != '') {
        PYTHON_VERSION = params.PYTHON_VERSION
        echo PYTHON_VERSION
    }
}
echo "PYTHON_VERSION: $PYTHON_VERSION"

// check if we want to download the wheel from somewhere 
if (params.DOWNLOAD_WHEEL)
    echo "params.DOWNLOAD_WHEEL is true"
else
    echo "params.DOWNLOAD_WHEEL is false"
Boolean DOWNLOAD_WHEEL=params.DOWNLOAD_WHEEL

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

def jsonParse(def json) {
    def slurper = new groovy.json.JsonSlurper()
    def modelconf = slurper.parseText(json)
    return modelconf
}

def BuildParams(model,node) {
    def MODELS_CONFIG =
        '''{
            "faster_rcnn":
            {
                "data-location":        "dataset/fastrcnn/coco-data/",
                "data-location-accuracy": "dataset/fastrcnn/coco-data/coco_val.record",
                "data-location-int8":   "dataset/fastrcnn/dataset",
                "in-graph-fp32":        "pre-trained-models/fastrcnn/fp32/faster_rcnn_resnet50_fp32_coco/frozen_inference_graph.pb",
                "in-graph-int8":        "pre-trained-models/fastrcnn/int8/models_fastrcnn_int8_pretrained_model.pb",
                "checkpoint":           "pre-trained-models/fastrcnn/fp32/faster_rcnn_resnet50_fp32_coco",
                "model-sourcedir":      "models",
                "batchsize_throughput": "1",
                "batchsize_accuracy":   "1",
                "model_arg_int8":       "number_of_steps=5000",
                "model_arg_fp32":       "config_file=pipeline.config"
            },
            "inceptionv3": 
            {
                "data-location": "dataset/TF_Imagenet_FullData",
                "in-graph-fp32": "pre-trained-models/inceptionv3/fp32/freezed_inceptionv3.pb",
                "in-graph-int8": "pre-trained-models/inceptionv3/int8/final_int8_inceptionv3.pb",
                "batchsize_throughput": "128",
                "batchsize_accuracy": "100"
            },
            "inception_resnet_v2": 
            {
                "data-location": "dataset/TF_Imagenet_FullData",
                "in-graph-fp32": "pre-trained-models/inception_resnet_v2/fp32/inception_resnet_v2_fp32_pretrained_model.pb",
                "in-graph-int8": "pre-trained-models/inception_resnet_v2/int8/final_int8_inception_resnet_v2_graph.pb",
                "batchsize_throughput": "128",
                "batchsize_accuracy": "100"
            },
            "inceptionv4": 
            {
                "data-location": "dataset/TF_Imagenet_FullData",
                "in-graph-fp32": "pre-trained-models/inception_v4/fp32/inceptionv4_fp32_pretrained_model.pb",
                "in-graph-int8": "pre-trained-models/inception_v4/int8/final_inception4_int8.pb",
                "batchsize_throughput": "240",
                "batchsize_accuracy": "100"
            },
            "maskrcnn":
            {
                "data-location":        "dataset/MaskRCNN/COCO2014",
                "model-sourcedir":      "Mask-RCNN",
                "batchsize_accuracy":   "1",
                "batchsize_throughput": "1",
            },
            "mobilenet_v1":
            { 
                "data-location":        "dataset/TF_Imagenet_FullData",
                "in-graph-fp32":        "pre-trained-models/mobilenet_v1/fp32/mobilenet_v1_1.0_224_frozen.pb",
                "checkpoint":           "pre-trained-models/mobilenet_v1/fp32",
                "model-sourcedir":      "models",
                "batchsize_accuracy":   "100",
                "batchsize_throughput": "256",
            },
            "ncf":
            {
                "checkpoint":           "pre-trained-models/NCF/fp32/ncf_trained_movielens_1m",
                "model-sourcedir":      "models-ncf",
                "batchsize_accuracy":   "256",
                "batchsize_throughput": "256",
            },
            "resnet50" :
            {
                "data-location":        "dataset/TF_Imagenet_FullData",
                "in-graph-fp32":        "pre-trained-models/resnet50/fp32/freezed_resnet50.pb",
                "in-graph-int8":        "pre-trained-models/resnet50/int8/final_int8_resnet50.pb",
                "batchsize_throughput": "128",
                "batchsize_accuracy":   "100"
            },     
            "resnet50v1_5" :
            {
                "data-location":        "dataset/TF_Imagenet_FullData",
                "in-graph-fp32":        "pre-trained-models/resnet50v1_5/fp32/resnet50_v1.pb",
                "in-graph-int8":        "pre-trained-models/resnet50v1_5/int8/resnet50v1_5_int8_pretrained_model.pb",
                "batchsize_throughput": "128",
                "batchsize_accuracy":   "100"
            },     
            "resnet101" :
            {
                "data-location":        "dataset/TF_Imagenet_FullData",
                "in-graph-fp32":        "pre-trained-models/resnet101/fp32/optimized_graph.pb",
                "in-graph-int8":        "pre-trained-models/resnet101/int8/resnet101_pad_fused.pb",
                "batchsize_throughput": "128",
                "batchsize_accuracy":   "100"
            },
            "rfcn":
            {
                "data-location-accuracy":"dataset/fastrcnn/coco-data/coco_val.record",
                "data-location":        "dataset/fastrcnn/coco-data",
                "data-location-int8":   "dataset/coco_dataset/raw-data/val2017",
                "in-graph-fp32":        "pre-trained-models/rfcn/fp32/rfcn_resnet101_coco_2018_01_28/frozen_inference_graph.pb",
                "in-graph-int8":        "pre-trained-models/rfcn/int8/per-channel-int8-rfcn-graph.pb",
                "checkpoint":           "pre-trained-models/rfcn/fp32/rfcn_resnet101_coco_2018_01_28",
                "model-sourcedir":      "models-rfcn",
                "batchsize_throughput": "1",
                "batchsize_accuracy":   "1",
                "model_arg_int8":       "number_of_steps=500",
                "model_arg_fp32":       "config_file=rfcn_pipeline.config",
                "model_arg_accuracy":   "split=accuracy_message"
            },
            "ssd-mobilenet":
            {
                "data-location":        "pre-trained-models/SSDMobilenet/fp32/coco_val.record",
                "data-location-int8":   "dataset/coco_dataset/raw-data/val2017",
                "in-graph-fp32":        "pre-trained-models/SSDMobilenet/fp32/frozen_inference_graph.pb",
                "in-graph-int8":        "pre-trained-models/SSDMobilenet/int8/ssdmobilenet_int8_pretrained_model.pb",
                "model-sourcedir":      "models-ssdmobilenet",
                "batchsize_accuracy":   "1",
                "batchsize_throughput": "1",
            },
            "ssd_vgg16":
            {
                "data-location":        "dataset/SSDvgg16/val2017_tfrecords",
                "in-graph-fp32":        "pre-trained-models/ssd-vgg16/fp32/models_ssdvgg16_fp32_pretrained_model.pb",
                "in-graph-int8":        "pre-trained-models/ssd-vgg16/int8/models_ssdvgg16_int8_pretrained_model.pb",
                "model-sourcedir":      "SSD.TensorFlow",
                "batchsize_accuracy":   "1",
                "batchsize_throughput": "1",
                "model_arg":            "warmup-steps=100 steps=500",
                "num_inter_threads":    "11",
                "num_intra_threads":    "21",
                "data_num_inter_threads": "21",
                "data_num_intra_threads": "28"
            },
            "transformer_language":
            {
                "data-location":        "dataset/transformerLanguage",
                "checkpoint":           "pre-trained-models/transformerLanguage/fp32",
                "model-sourcedir":      "tensor2tensor",
                "batchsize_accuracy":   "1",
                "batchsize_throughput": "32",
                "model_arg":            "decode_from_file=newstest2015.en reference=newstest2015.de"
            },
            "wide_deep":
            {
                "data-location":        "dataset/wideDeep",
                "checkpoint":           "pre-trained-models/wideDeep/fp32",
                "model-sourcedir":      "models-widedeep",
                "batchsize_accuracy":   "1",
                "batchsize_throughput": "1024",
            },
            "wide_deep_large_ds":
            {
                "data-location":        "dataset/wide_deep_kaggle/train_preprocessed_test.tfrecords",
                "in-graph-fp32":        "pre-trained-models/wide_deep_large_ds/fp32/wide_deep_fp32_pretrained_model.pb",
                "in-graph-int8":        "pre-trained-models/wide_deep_large_ds/int8/wide_deep_int8_pretrained_model.pb",
                "model-sourcedir":      "models-widedeep_largeds",
                "batchsize_accuracy":   "1000",
                "batchsize_throughput": "512",
            }
    }'''

    def modelconf = jsonParse(MODELS_CONFIG)

    string datalocation = modelconf."${model}"."data-location"
    string datalocation_accuracy = modelconf."${model}"."data-location-accuracy"
    string datalocation_int8 = modelconf."${model}"."data-location-int8"
    string checkpoint = modelconf."${model}"."checkpoint"
    string graphfp32 = modelconf."${model}"."in-graph-fp32" 
    string graphint8 = modelconf."${model}"."in-graph-int8" 
    string batchsize_thr = modelconf."${model}"."batchsize_throughput" 
    string batchsize_acc = modelconf."${model}"."batchsize_accuracy" 
    string model_sourcedir = modelconf."${model}"."model-sourcedir"
    string extra_arguments = modelconf."${model}"."model_arg"
    string extra_arguments_accuracy = modelconf."${model}"."model_arg_accuracy"
    string extra_arguments_fp32 = modelconf."${model}"."model_arg_fp32"
    string extra_arguments_int8 = modelconf."${model}"."model_arg_int8"
    string num_inter_threads = modelconf."${model}"."num_inter_threads"
    string num_intra_threads = modelconf."${model}"."num_intra_threads"
    string data_num_inter_threads = modelconf."${model}"."data_num_inter_threads"
    string data_num_intra_threads = modelconf."${model}"."data_num_intra_threads"

    echo "model_sourcedir: $model_sourcedir"
    echo "checkpoint: $checkpoint"
    echo "extra_arguments: $extra_arguments"
    echo "extra_arguments_accuracy: $extra_arguments_accuracy"
    echo "extra_arguments_fp32: $extra_arguments_fp32"
    echo "extra_arguments_int8: $extra_arguments_int8"

    List modeljobParams = []

    modeljobParams += string(name: "NODE_LABEL", value: "${node}")
    modeljobParams += string(name: "CJE_TF_BRANCH", value: "${CJE_TF_BRANCH}")
    modeljobParams += string(name: "INTEL_MODELS_URL", value: "${INTEL_MODELS_URL}")
    modeljobParams += string(name: "INTEL_MODELS_BRANCH", value: "${INTEL_MODELS_BRANCH}")
    modeljobParams += string(name: "MODEL", value: "${model}")
    modeljobParams += string(name: "DOCKER_IMAGE", value: "${DOCKER_IMAGE}") 
    modeljobParams += string(name: "MODES", value: "${MODES}")
    modeljobParams += string(name: "DATA_TYPE", value: "${DATA_TYPE}")
    modeljobParams += string(name: "PERFORMANCE", value: "${PERFORMANCE}")
    modeljobParams += string(name: "DATASET_LOCATION", value: "${DATASET_LOCATION}")   
    modeljobParams += string(name: "MODEL_PROP_DATA", value: "${datalocation}")
    modeljobParams += string(name: "MODEL_PROP_DATA_ACCURACY", value: "${datalocation_accuracy}")
    modeljobParams += string(name: "MODEL_PROP_DATA_INT8", value: "${datalocation_int8}")
    modeljobParams += string(name: "MODEL_CHECK_POINT", value: "${checkpoint}")
    modeljobParams += string(name: "MODEL_PROP_GRAPH_FP32", value: "${graphfp32}")
    modeljobParams += string(name: "MODEL_PROP_GRAPH_INT8", value: "${graphint8}")
    modeljobParams += string(name: "MODEL_DIR", value: "${model_sourcedir}")
    modeljobParams += string(name: "BATCHSIZE_THROUGHPUT", value: "${batchsize_thr}")
    modeljobParams += string(name: "BATCHSIZE_ACCURACY", value: "${batchsize_acc}")
    modeljobParams += string(name: "MODEL_ARGS", value: "${extra_arguments}")
    modeljobParams += string(name: "POST_TO_DASHBOARD", value: "${POST_TO_DASHBOARD}")
    modeljobParams += string(name: "TARGET_DASHBOARD", value: "${TARGET_DASHBOARD}")
    modeljobParams += string(name: "CJE_ALGO_BRANCH", value: "${CJE_ALGO_BRANCH}")
    modeljobParams += string(name: "MODEL_NUM_INTER_THREADS", value: "${num_inter_threads}")
    modeljobParams += string(name: "MODEL_NUM_INTRA_THREADS", value: "${num_intra_threads}")
    modeljobParams += string(name: "MODEL_DATA_NUM_INTER_THREADS", value: "${data_num_inter_threads}")
    modeljobParams += string(name: "MODEL_DATA_NUM_INTRA_THREADS", value: "${data_num_intra_threads}")
    modeljobParams += string(name: "MODEL_ARGS_ACCURACY", value: "${extra_arguments_accuracy}")
    modeljobParams += string(name: "MODEL_ARGS_FP32", value: "${extra_arguments_fp32}")
    modeljobParams += string(name: "MODEL_ARGS_INT8", value: "${extra_arguments_int8}")
    modeljobParams += string(name: "DOCKER_IMAGE_NAMESPACE", value: "${DOCKER_IMAGE_NAMESPACE}")
    modeljobParams += string(name: "PR_NUMBER", value: "${PR_NUMBER}")
    modeljobParams += string(name: "GIT_URL", value: "${GIT_URL}")
    modeljobParams += string(name: "GIT_NAME", value: "${GIT_NAME}")
    modeljobParams += string(name: "PYTHON_VERSION", value: "${PYTHON_VERSION}")
    modeljobParams += string(name: "DOWNLOAD_WHEEL", value: "${DOWNLOAD_WHEEL}")
    modeljobParams += string(name: "DOWNLOAD_WHEEL_FROM_JOB", value: "${DOWNLOAD_WHEEL_FROM_JOB}")
    modeljobParams += string(name: "DOWNLOAD_WHEEL_NAME", value: "${DOWNLOAD_WHEEL_NAME}")

    return modeljobParams
}

def doBuild(
    String models) {

    def jobs = [:]
    
    job_models=MODELS.split(',')
    job_nodes=NODE_MODEL_LABEL.split(',')

    job_models.each { job_model ->
        job_nodes.each { job_node ->
            jobs["${job_model}_${job_node}"] = {
                catchError {
                    stage("Run Models ${job_model} on ${job_node}") {
                        // execute build
                        echo "${job_model}, ${job_node}"
                        def downstreamJob = build job: "run-benchmark-intel-model-zoo-general", propagate: false, parameters: BuildParams(job_model,job_node)


                        if (downstreamJob.getResult() == 'SUCCESS') {
                            catchError {

                                copyArtifacts(
                                        projectName: "run-benchmark-intel-model-zoo-general",
                                        selector: specific("${downstreamJob.getNumber()}"),
                                        filter: '*.log',
                                        fingerprintArtifacts: true,
                                        target: "${job_model}/")

                                // Archive in Jenkins
                                archiveArtifacts artifacts: "${job_model}/*"
                            }
                        }
                    }
                }
            }
        }
    }

    parallel jobs

}

def collectBenchmarkLog(String models, String modes, Boolean single_socket, String datatypes) {

    echo "---------------------------------------------------------"
    echo "------------  running collectBenchnmarkLog  -------------"
    echo "---------------------------------------------------------"

    echo "models: $models"
    echo "modes: $modes"
    echo "datatypes: $datatypes"
    SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
            returnStdout: true).trim()

    models=models.split(',')
    modes=modes.split(',')
    datatypes=datatypes.split(',')

    models.each { model ->
        echo "model is ${model}"
        modes.each { mode ->
            echo "mode is ${mode}"
            datatypes.each { datatype ->
                withEnv(["current_model=$model","current_mode=$mode", "current_type=$datatype"]) {

                    sh '''#!/bin/bash -x
                        chmod 775 $WORKSPACE/cje-tf/scripts/collect_logs_intel_models.sh
                        $WORKSPACE/cje-tf/scripts/collect_logs_intel_models.sh --model=${current_model} --mode=${current_mode} --datatype=${current_type}                
                    '''
                }
            }
        }
    }
    echo "done running collectBenchmarkLog ......."
    stash allowEmpty: true, includes: "*.log", name: "logfile"

}

node( NODE_LABEL ) {
    
    try {    
        cleanup()
        deleteDir()

        // pull the cje-tf
        dir('cje-tf') {
            checkout scm
        }

        SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",returnStdout: true).trim()
        SUMMARYLOG = "${WORKSPACE}/summary_${SERVERNAME}.log"
        sh (script:"touch $SUMMARYLOG", returnStdout: true).trim()
        SUMMARYTXT = "${WORKSPACE}/summary_nightly.log"
        writeFile file: SUMMARYTXT, text: "Model,Mode,Server,Data_Type,Use_Case,Batch_Size,Result\n"
   
        // slack notification
        def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
        notifyBuild(SLACK_CHANNEL, 'STARTED', '') 

        stage("build") {
            doBuild(MODELS)
        }

        stage("Collect Logs") {

            withEnv(["tensorflow_dir=${GIT_NAME}"]) {
                sh '''#!/bin/bash -x
                    # get git commit info 
                    python ${WORKSPACE}/cje-tf/scripts/get_tf_sourceinfo.py --tensorflow_dir=${tensorflow_dir} --workspace_dir=${WORKSPACE}
                '''
            }

            // Prepare logs
            def prepareLog = load("${CJE_TF_COMMON_DIR}/prepareLog.groovy")
            prepareLog(INTEL_MODELS_BRANCH, "", "", SUMMARYLOG, SUMMARY_TITLE)
           
            collectBenchmarkLog(MODELS, MODES, SINGLE_SOCKET, DATA_TYPE)
        }

    } catch (e) {
            // If there was an exception thrown, the build failed
            currentBuild.result = "FAILED"
            throw e

    } finally {

        // Success or failure, always send notifications
        withEnv(["SUMMARYLOG=$SUMMARYLOG"]) {
            echo "$SUMMARYLOG"
            def msg = readFile SUMMARYLOG

            def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
            notifyBuild(SLACK_CHANNEL, currentBuild.result, msg)
        }

        stage('Archive Artifacts ') {
            dir("$WORKSPACE") {
                archiveArtifacts artifacts: '*.log, */*.log', excludes: null
                fingerprint: true

            }
        }

    }  //try
     
    echo "===== ${env.BUILD_URL}, ${env.JOB_NAME},${env.BUILD_NUMBER} ====="
}
