// This scripts lauches the benchmark run from intel-model zoo:
// Currently 2 jobs using this script:
//     - Intel-Models-V2-Benchmark-py3-Trigger: running fp32 models in py3
//         - inceptionv3
//         - resnet101
//         - resnet50
//         - resnet50v1_5
//         - ssd-mobilenet
//         - bert
//         - ssd-resnet34
//         - rfcn
//         - mobilenet_v1
//         - densenet169
//         - inceptionv4
//         - bert_official

// models integrated on Jenkin:
//     - inceptionv3(py3, fp32/int8)
//     - resnet101(py3, fp32/int8)
//     - resnet50(py3, fp32/int8)
//     - resnet50v1_5(py3, fp32/int8)
//     - ssd-mobilenet(py3, fp32/int8)
//     - bert(py3, fp32/int8)
//     - ssd-resnet34(py3, fp32/int8)
//     - rfcn(py3, fp32/int8)
//     - mobilenet_v1(py3, fp32/int8)
//     - densenet169(py3, fp32)
//     - inceptionv4(py3, fp32/int8)
//     - bert_official(py3, fp32)

// edit by suyue
// pre exist 'inceptionv3','resnet101','resnet50','resnet50v1_5','ssd-mobilenet'
// new add 'ssd-resnet34','bert','rfcn'

// edit by xue
// pre exist 'mobilenet_v1','inceptionv4'
// new add 'densenet169', bert_official
 
import groovy.json.JsonSlurperClassic

Boolean SINGLE_SOCKET = true
CJE_TF = 'cje-tf'
CJE_TF_COMMON_DIR = "$CJE_TF/common"
GIT_CREDENTIAL = "lab_tfbot"

http_proxy="http://proxy-us.intel.com:911"
https_proxy="https://proxy-us.intel.com:912"

// lists of int8 and fp32 TF2.0 models enabled for internal check only for py3
model_List_int8 = ['inceptionv3','mobilenet_v1','resnet101','resnet50','resnet50v1_5','ssd-mobilenet','bert','ssd-resnet34','rfcn','wide_deep_large_ds','inceptionv4']
model_List_fp32 = ['inceptionv3','mlperf_gnmt','resnet101','resnet50','resnet50v1_5','ssd-mobilenet','bert','ssd-resnet34','rfcn','wide_deep','wide_deep_large_ds','densenet169','inceptionv4','bert_official','mobilenet_v1']

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
    }
}
echo "CJE_TF_BRANCH:  $CJE_TF_BRANCH"

// setting CJE_ALGO branch
CJE_ALGO_BRANCH = '*/master'
if ('CJE_ALGO_BRANCH' in params) {
    echo "CJE_ALGO_BRANCH in params"
    if (params.CJE_ALGO_BRANCH != '') {
        CJE_ALGO_BRANCH = params.CJE_ALGO_BRANCH
    }
}
echo "CJE_ALGO_BRANCH:  $CJE_ALGO_BRANCH"

// setting SLACK_CHANNEL with some default value or get input from params
SLACK_CHANNEL = '#tensorflow-jenkins'
if ('SLACK_CHANNEL' in params) {
    echo "SLACK_CHANNEL in params"
    if (params.SLACK_CHANNEL != '') {
        SLACK_CHANNEL = params.SLACK_CHANNEL
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
    }
}
echo "INTEL_MODELS_URL: $INTEL_MODELS_URL"

// set default INTEL_MODELS_BRANCH value                
INTEL_MODELS_BRANCH = 'master'
if ('INTEL_MODELS_BRANCH' in params) {
    echo "INTEL_MODELS_BRANCH in params"
    if (params.INTEL_MODELS_BRANCH != '') {
        INTEL_MODELS_BRANCH = params.INTEL_MODELS_BRANCH
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

def BuildParams(data_type,model,node) {
    def MODELS_CONFIG =
        '''{
            "bert":
            {
                "in-graph-fp32":        "pre-trained-models/bert_v2/fp32/optimized_fp32_bert.pb",
                "in-graph-int8":        "pre-trained-models/bert_v2/int8/quantized_int8_final.pb",
                "batchsize_throughput": "1",
                "model_arg":            "num-cores=28",
                "num_inter_threads":    "1",
                "num_intra_threads":    "28",
            },
            "fastrcnn":
            {
                "data-location":        "dataset/fastrcnn/coco-data/",
                "in-graph-fp32":        "pre-trained-models/fastrcnn/fp32/checkpoint/faster_rcnn_resnet50_coco_2018_01_28/frozen_inference_graph.pb",
                "in-graph-int8":        "pre-trained-models/fastrcnn/int8/models_fastrcnn_int8_pretrained_model.pb",
                "checkpoint":           "pre-trained-models/fastrcnn/fp32/checkpoint",
                "model-sourcedir":      "models",
                "batchsize_throughput": "1",
                "batchsize_accuracy":   "1",
                "model_arg":            "config_file=pipeline.config"
            },
            "inceptionv3": 
            {
                "data-location": "dataset/TF_Imagenet_FullData",
                "in-graph-fp32": "pre-trained-models/inceptionv3/fp32/freezed_inceptionv3.pb",
                "in-graph-int8": "pre-trained-models/inceptionv3/int8/final_int8_inceptionv3.pb",
                "batchsize_throughput": "128",
                "batchsize_accuracy": "100",
                "model_arg":            "warmup_steps=50 steps=500"
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
                "data-location-accuracy": "dataset/TF_Imagenet_FullData",
                "in-graph-fp32": "pre-trained-models/inception_v4/fp32/inceptionv4_fp32_pretrained_model.pb",
                "in-graph-int8": "pre-trained-models/inception_v4/int8/inceptionv4_int8_pretrained_model.pb",
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
            "mlperf_gnmt":
            {
                "data-location":        "dataset/mlperf_gnmt_tf2.0",
                "in-graph-fp32":        "pre-trained-models/mlperf_gnmt_tf2.0/freezed_gnmt.pb",
                "batchsize_accuracy":   "1",
                "batchsize_throughput": "128",
            },
            "mobilenet_v1":
            { 
                "data-location":        "dataset/TF_Imagenet_FullData",
                "in-graph-fp32":        "pre-trained-models/mobilenet_v1/fp32/mobilenetv1_fp32_pretrained_model_new.pb",
                "in-graph-int8":        "pre-trained-models/mobilenet_v1/int8/mobilenetv1_int8_pretrained_model_new.pb",
                "checkpoint":           "pre-trained-models/mobilenet_v1/fp32",
                "batchsize_accuracy":   "100",
                "batchsize_throughput": "112",
                "model_arg":            "input_height=224 input_width=224 warmup-steps=100 steps=1000 input_layer='input' output_layer='MobilenetV1/Predictions/Reshape_1'",
                "num_inter_threads":    "1",
                "num_intra_threads":    "28",
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
                "batchsize_accuracy":   "100",
                "model_arg":            "warmup_steps=50 steps=1500"
            },     
            "resnet50v1_5" :
            {
                "data-location":        "dataset/TF_Imagenet_FullData",
                "in-graph-fp32":        "pre-trained-models/resnet50v1_5/fp32/resnet50_v1.pb",
                "in-graph-int8":        "pre-trained-models/resnet50v1_5/int8/resnet50v1_5_int8_pretrained_model.pb",
                "batchsize_throughput": "128",
                "batchsize_accuracy":   "100",
                "model_arg":            "warmup_steps=50 steps=1500"
            },     
            "resnet101" :
            {
                "data-location":        "dataset/TF_Imagenet_FullData",
                "in-graph-fp32":        "pre-trained-models/resnet101/fp32/optimized_graph.pb",
                "in-graph-int8":        "pre-trained-models/resnet101/int8/resnet101_pad_fused.pb",
                "batchsize_throughput": "128",
                "batchsize_accuracy":   "100",
                "model_arg":            "warmup_steps=50 steps=500"
            },
            "rfcn":
            {
                "data-location-accuracy":"dataset/fastrcnn/coco-data/coco_val.record",
                "data-location":        "dataset/coco_dataset/raw-data/val2017",
                "data-location-int8":   "dataset/coco_dataset/raw-data/val2017",
                "in-graph-fp32":        "pre-trained-models/rfcn/fp32/rfcn_resnet101_coco_2018_01_28/frozen_inference_graph.pb",
                "in-graph-int8":        "pre-trained-models/rfcn/int8/final_fused_pad_and_conv.pb",
                "checkpoint":           "pre-trained-models/rfcn/fp32/rfcn_resnet101_coco_2018_01_28",
                "model-sourcedir":      "models-rfcn",
                "batchsize_throughput": "1",
                "batchsize_accuracy":   "1",
                "model_arg_int8":       "number_of_steps=500",
                "model_arg_fp32":       "number_of_steps=500",
                "model_arg_accuracy":   "split=accuracy_message"
            },
            "ssd-mobilenet":
            {
                "data-location":        "dataset/SSDMobilenet/coco_val.record",
                "data-location-int8":   "dataset/SSDMobilenet/coco_val.record",
                "in-graph-fp32":        "pre-trained-models/SSDMobilenet/fp32/frozen_inference_graph.pb",
                "in-graph-int8":        "pre-trained-models/SSDMobilenet/int8/ssdmobilenet_int8_pretrained_model_tr.pb",
                "model-sourcedir":      "models-ssdmobilenet",
                "num_inter_threads":    "1",
                "num_intra_threads":    "28",
                "batchsize_accuracy":   "1",
                "batchsize_throughput": "1",
            },
            "ssd-resnet34":
            {
                "data-location-accuracy": "dataset/ssd-resnet34",
                "in-graph-fp32":        "pre-trained-models/ssd-resnet34/fp32/ssd_resnet34_fp32_bs1_pretrained_model.pb",
                "in-graph-int8":        "pre-trained-models/ssd-resnet34/int8/ssd_resnet34_int8_bs1_pretrained_model.pb",
                "model-sourcedir":      "models-ssdresnet34",
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
                "data-location":        "dataset/wideDeep_v2",
                "checkpoint":           "pre-trained-models/wideDeep_v2/fp32",
                "model-sourcedir":      "models-widedeep",
                "batchsize_accuracy":   "1",
                "batchsize_throughput": "1024",
            },
            "wide_deep_large_ds":
            {
                "data-location":        "dataset/wide_deep_kaggle/train_preprocessed_test.tfrecords",
                "in-graph-fp32":        "pre-trained-models/wide_deep_large_ds/fp32/wide_deep_fp32_pretrained_model.pb",
                "in-graph-int8":        "pre-trained-models/wide_deep_large_ds/int8/wide_deep_int8_pretrained_model.pb",
                "batchsize_accuracy":   "1000",
                "batchsize_throughput": "512",
                "model_arg":            "num_parallel_batches=1",
            },
            "densenet169":
            {
                "data-location-accuracy":"dataset/TF_Imagenet_FullData",
                "in-graph-fp32":        "pre-trained-models/densenet-169/fp32/densenet169_fp32_pretrained_model.pb",
                "batchsize_throughput": "100",
                "batchsize_accuracy":   "100",
                "model_arg":            "input_height=224 input_width=224 warmup_steps=20 steps=100 input_layer='input' output_layer='densenet169/predictions/Reshape_1'",
            },
            "bert_official":
            {   
                "data-location":        "dataset/bert_official/XNLI-1.0",
                "checkpoint":           "dataset/bert_official/XNLI-1.0/chinese_L-12_H-768_A-12",
                "model-sourcedir":      "models-bert_official",
                "batchsize_throughput": "8",
                "num_inter_threads":    "1",
                "num_intra_threads":    "28",
                "model_arg":            "task-name=XNLI max-seq-length=128 learning-rate=5e-5"
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
    modeljobParams += string(name: "DATA_TYPE", value: "${data_type}")
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
    modeljobParams += string(name: "MR_NUMBER", value: "${MR_NUMBER}")
    modeljobParams += string(name: "MR_SOURCE_BRANCH", value: "${MR_SOURCE_BRANCH}")
    modeljobParams += string(name: "MR_MERGE_BRANCH", value: "${MR_MERGE_BRANCH}")
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

    job_data_types=DATA_TYPE.split(',')
    job_models=MODELS.split(',')
    job_nodes=NODE_MODEL_LABEL.split(',')

    job_data_types.each { job_data_type ->
        job_models.each { job_model ->
            // check the model with the enable list
            echo "llsu ---job_data_type--> ${job_data_type}"
            if (job_data_type == 'fp32' && !model_List_fp32.contains(job_model))
            {
                echo "llsu ---job_data_type fp32 do not contains ${job_model}"
                return true
            }
            if (job_data_type == 'int8' && !model_List_int8.contains(job_model)) {
                echo "llsu ---job_data_type int8 do not contains ${job_model}"
                return true
            }
            job_nodes.each { job_node ->
                jobs["${job_data_type}_${job_model}_${job_node}"] = {
                    catchError {
                        stage("Run Model ${job_model} on ${job_node} of ${job_data_type}") {
                            // execute build
                            echo "llsu ---> ${job_data_type}, ${job_model}, ${job_node}"
                            if ( MR_NUMBER == '')
                                projectName = 'run-v2-benchmark-intel-model-zoo-general'
                            else
                                projectName = 'run-v2-mr-benchmark-intel-model-zoo-general'

                            def downstreamJob = build job: projectName, propagate: false, parameters: BuildParams(job_data_type,job_model,job_node)

                            downstreamJobStatus = downstreamJob.result
                            downstreamJobNumber = downstreamJob.number

                            if (downstreamJobStatus == 'SUCCESS') {
                                catchError {

                                    copyArtifacts(
                                            projectName: projectName,
                                            selector: specific("${downstreamJobNumber}"),
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

    datatypes.each { datatype ->
        models.each { model ->
            echo "model is ${model}"
            // check the model with the enable list
            if (datatype == 'fp32' && !model_List_fp32.contains(model))
            {
                return true
            }
            if (datatype == 'int8' && !model_List_int8.contains(model))
            {
                return true
            }
            modes.each { mode ->
                echo "mode is ${mode}"
                withEnv(["current_model=$model","current_mode=$mode", "current_type=$datatype"]) {

                    sh '''#!/bin/bash -x
                        chmod 775 $WORKSPACE/cje-tf/scripts/collect_logs_intel_models_v2.sh
                        $WORKSPACE/cje-tf/scripts/collect_logs_intel_models_v2.sh --model=${current_model} --mode=${current_mode} --datatype=${current_type}                
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
        SUMMARYTXT = "${WORKSPACE}/summary_nightly.log"
	 writeFile file: SUMMARYTXT, text: "Model,Mode,Server,Data_Type,Use_Case,Batch_Size,Result\n"
   
        // slack notification
        def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
        notifyBuild(SLACK_CHANNEL, 'STARTED', '') 

        stage("build") {

            doBuild(MODELS)
        }

        stage("Collect Logs") {

            workspace_volumn="${WORKSPACE}:/workspace"
            dataset_volume="${DATASET_LOCATION}:${DATASET_LOCATION}"

            // get commit info inside the docker
            docker.image("$DOCKER_IMAGE").inside("--env \"http_proxy=${http_proxy}\" \
                    --env \"https_proxy=${https_proxy}\" \
                    --volume ${workspace_volumn} \
                    --volume ${dataset_volume} \
                    --env DATASET_LOCATION=$DATASET_LOCATION \
                    --env SINGLE_SOCKET=$SINGLE_SOCKET \
                    --env SERVERNAME=$SERVERNAME \
                    --env MODELS=$MODELS \
                    --env MODES=$MODES \
                    --env TENSORFLOW_DIR=/$GIT_NAME \
                    --privileged \
                    -u root:root") {
                    sh '''#!/bin/bash -x 
                    python /workspace/cje-tf/scripts/get_tf_sourceinfo.py --tensorflow_dir=${TENSORFLOW_DIR} --workspace_dir=${WORKSPACE}
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
