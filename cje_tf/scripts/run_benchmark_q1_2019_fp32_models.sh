#!/bin/bash
set -x
# this script runs the 2019 Q1 fp32 models, including:
#     - resnet50
#     - inceptionv3
#     - 
# this script assumes the following environment variable has been set:
#     - WORKSPACE: this is the workspace under Jenkin job, e.g.e /dataset/cje/aipg-tf/workspace/<Jenkin_Job_Name>
#     - DATASET_LOCATION: this is the root of the dataset location, e.g. by default all dataset is under /dataset
#     - TARGET_PLATFORM: can be avx/avx2/avx512, currently not used
#     - TENSORFLOW_BRANCH:  currently not used
#     - SINGLE_SOCKET: whether to run single socket or not
#     - SERVERNAME: the host name of the Jenkin node
#     - MODELS: list of models to run separated by comma, e.g. resnet50, inceptionv3
#     - MODES: list of modes to run separated by comma, e.g. training,inference

if [ -z ${WORKSPACE} ] ; then
    WORKSPACE=`pwd`
fi
if [ -z ${DATASET_LOCATION} ] ; then
    DATASET_LOCATION="/tf_dataset"
fi
if [ -z ${TARGET_PLATFORM} ] ; then
    TARGET_PLATFORM="avx2"
fi
if [ -z ${TENSORFLOW_BRANCH} ] ; then
    TENSORFLOW_BRANCH="master"
fi
if [ -z ${SINGLE_SOCKET} ] ; then
    SINGLE_SOCKET=true
fi
if [ -z ${SERVERNAME} ] ; then
    SERVERNAME=`uname -n | awk -F'.' '{print $1}'`
fi
if [ -z ${DOCKER_IMAGE} ] ; then
    DOCKER_IMAGE="amr-registry.caas.intel.com/aipg-tf/qa:nightly-master-avx2-devel-mkl"
fi

echo 'Running with parameters:'
echo "    WORKSPACE: ${WORKSPACE}"
echo "    DATASET_LOCATION: ${DATASET_LOCATION}"
echo '    Mounted volumes:'
echo "        WORKSPACE mounted on: /workspace"
echo "        DATASET_LOCATION mounted on: ${DATASET_LOCATION}"
echo "    TARGET_PLATFORM: ${TARGET_PLATFORM}"
echo "    TENSORFLOW_BRANCH: ${TENSORFLOW_BRANCH}"
echo "    SINGLE_SOCKET: ${SINGLE_SOCKET}"
echo "    SERVERNAME: ${SERVERNAME}"
echo "    MODELS: ${MODELS}"
echo "    MODES: ${MODES}"

HTTP_PROXY="http://proxy-us.intel.com:911"
HTTPS_PROXY="https://proxy-us.intel.com:912"

WORKDIR=${WORKSPACE}
DATASET_DIR=${DATASET_LOCATION}/dataset
PRETRAINED_MODELS_DIR=${DATASET_LOCATION}/pre-trained-models
MODELS_DIR=${WORKDIR}/tensorflow-models/benchmarks/


single_socket_arg=""
if [ ${SINGLE_SOCKET} == "true" ]; then
    single_socket_arg="-s"
fi

run_benchmark () {

    model=$1
    mode=$2

    LOGFILE_THROUGHPUT=${WORKDIR}/benchmark_${model}_${mode}_${SERVERNAME}_throughput.log
    LOGFILE_LATENCY=${WORKDIR}/benchmark_${model}_${mode}_${SERVERNAME}_latency.log
    LOGFILE_ACCURACY=${WORKDIR}/benchmark_${model}_${mode}_${SERVERNAME}_accuracy.log

    echo ${LOGFILE_THROUGHPUT}
    echo ${LOGFILE_LATENCY}
    echo ${LOGFILE_ACCURACY}

    if [ ${model} == "inceptionv3" ]; then

        echo "model is ${model}"

        cd ${MODELS_DIR}

        if [ ${mode} == "inference" ] ; then
            
            # run batch size 100 for accuracy
            cmd="python launch_benchmark.py --model-name inceptionv3 --precision fp32 --mode inference --framework tensorflow --accuracy-only --batch-size 100 --data-location ${DATASET_DIR}/TF_Imagenet_FullData --docker-image ${DOCKER_IMAGE} --in-graph ${PRETRAINED_MODELS_DIR}/inceptionv3/fp32/freezed_inceptionv3.pb -- https_proxy=${HTTPS_PROXY} http_proxy=${HTTP_PROXY} DEBIAN_FRONTEND=noninteractive >> ${LOGFILE_ACCURACY} 2>&1" 
            
            echo "RUNCMD: $cmd " >& ${LOGFILE_ACCURACY}
            echo "Batch Size: 100" >> ${LOGFILE_ACCURACY}
            eval $cmd >> ${LOGFILE_ACCURACY}

        else
            echo "skipping ${model} ${mode} for Q1 2019"
        fi

        if [ $? -eq 0 ] ; then
            echo "success"
            STATUS="SUCCESS"
        else
            echo "failure"
            STATUS="FAILURE"
        fi

    elif [ ${model} == "resnet50" ]; then

        echo "model is ${model}"

        cd ${MODELS_DIR}

        if [ ${mode} == "inference" ] ; then
            
            # run batch size 100 for accuracy
            cmd="python launch_benchmark.py --model-name resnet50 --precision fp32 --mode inference --framework tensorflow --accuracy-only --batch-size 100 --socket-id 0 --data-location ${DATASET_DIR}/TF_Imagenet_FullData --docker-image ${DOCKER_IMAGE} --in-graph ${PRETRAINED_MODELS_DIR}/resnet50/fp32/freezed_resnet50.pb -- https_proxy=${HTTPS_PROXY} http_proxy=${HTTP_PROXY} DEBIAN_FRONTEND=noninteractive >> ${LOGFILE_ACCURACY} 2>&1" 
            
            echo "RUNCMD: $cmd " >& ${LOGFILE_ACCURACY}
            echo "Batch Size: 100" >> ${LOGFILE_ACCURACY}
            eval $cmd >> ${LOGFILE_ACCURACY}

        else
            echo "skipping ${model} ${mode} for Q1 2019"
        fi

        if [ $? -eq 0 ] ; then
            echo "success"
            STATUS="SUCCESS"
        else
            echo "failure"
            STATUS="FAILURE"
        fi
    fi   

}


IFS=',' read -ra MODELS <<< "$MODELS"
IFS=',' read -ra MODES <<< "$MODES"

# looping through all the models and modes
for model in "${MODELS[@]}"
do
    for mode in "${MODES[@]}"
    do

        run_benchmark $model $mode

    done
done


