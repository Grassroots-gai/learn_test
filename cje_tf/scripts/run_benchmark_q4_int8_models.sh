#!/bin/bash
# run_benchmark.sh --model=[FastRCNN|YoloV2] --single_socket=[True|False]
set -x

HOST=`uname -n | awk -F'.' '{print $1}'`
WORKDIR=`pwd`

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
if [ -z ${DOWNLOAD_WHEEL} ] ; then
    DOWNLOAD_WHEEL=false
fi

echo 'Running with parameters:'
echo "    WORKSPACE: ${WORKSPACE}"
echo "    DATASET_LOCATION: ${DATASET_LOCATION}"
echo '    Mounted volumes:'
echo "        ${WORKSPACE} mounted on: /workspace"
echo "        ${DATASET_LOCATION} mounted on: ${DATASET_LOCATION}"
echo "    TARGET_PLATFORM: ${TARGET_PLATFORM}"
echo "    TENSORFLOW_BRANCH: ${TENSORFLOW_BRANCH}"
echo "    PYTHON: ${PYTHON}"
echo "    SINGLE_SOCKET: ${SINGLE_SOCKET}"
echo "    SERVERNAME: ${SERVERNAME}"
echo "    MODELS: ${MODELS}"
echo "    DOWNLOAD_WHEEL: ${DOWNLOAD_WHEEL}"
echo ' '

p_version=$(python -V 2>&1)
case "$p_version" in
*3.4*)
    PIP="python3.4 -m pip"
    ;;
*3.5*)
    PIP="python3.5 -m pip"
    ;;
*3.6*)
    PIP="python3.6 -m pip"
    ;;
*3.7*)
    PIP="python3.7 -m pip"
    ;;
*)
    PIP="pip"
    ;;
esac

# debug
echo "before installing the dependencies......"
${PIP} list

export DEBIAN_FRONTEND=noninteractive # This is to disable tzdata prompt

# because yum doesn't support python3, temporarily to use python2 to do yum stuff
p_version=$(python -V 2>&1)
case "$p_version" in
    *2.7*)
        apt-get clean; apt-get update -y || yum clean all; yum update -y
        apt-get install libsm6 libxext6 python-tk numactl wget -y || yum install libsm6 libxext6 python-tk numactl wget -y
        ;;
    *)
	which_p=$(which python)
        p_path=$(ls -l $which_p)
        p_path_org=`echo $p_path | awk -F'-> ' '{print $2}'`
        ln -sf /usr/bin/python2.7 /usr/bin/python
        apt-get clean; apt-get update -y || yum clean all; yum update -y
        apt-get install libsm6 libxext6 python-tk numactl wget -y || yum install libsm6 libxext6 python-tk numactl wget -y
        ln -sf ${p_path_org} ${which_p} 
        ;;
esac

### install common dependencies
${PIP} install opencv-python
${PIP} install cython
${PIP} install matplotlib
${PIP} install pillow
${PIP} install lxml
${PIP} install jupyter
${PIP} install slim
${PIP} install requests
${PIP} install contextlib2
${PIP} install numpy
# installing hashlib with pip fails. Please see this thread:
# https://askubuntu.com/questions/770262/python-hashlib-fails-to-install-pip
easy_install hashlib

if [ "${TENSORFLOW_BRANCH}" == "master" ]; then
    ${PIP} install --upgrade tf-estimator-nightly
fi

if [ ${DOWNLOAD_WHEEL} == "true" ]; then
    echo "downloading TF wheel"
    ${PIP} install --upgrade build/*.whl
fi

echo "after installing the dependencies......"
${PIP} list

### define variables

RESNET101_DIR=${WORKDIR}/tensorflow-inference-resnet101/int8_models_benchmark
INCEPTIONV4_DIR=${WORKDIR}/tensorflow-inference-inceptionv4/int8_models_benchmark

single_socket_arg=""
if [ ${SINGLE_SOCKET} == "true" ]; then
    single_socket_arg="-s"
fi

IFS=',' read -ra MODELS <<< "$MODELS"
for model in "${MODELS[@]}"
do
    echo "running benchmark for model: $model "
    LOGFILE_THROUGHPUT=${WORKDIR}/benchmark_${model}_inference_${SERVERNAME}_throughput.log
    LOGFILE_LATENCY=${WORKDIR}/benchmark_${model}_inference_${SERVERNAME}_latency.log
    LOGFILE_ACCURACY=${WORKDIR}/benchmark_${model}_inference_${SERVERNAME}_accuracy.log

    echo ${LOGFILE_THROUGHPUT}
    echo ${LOGFILE_LATENCY}
    echo ${LOGFILE_ACCURACY}

    if [ $model == "ResNet101" ] ; then

        echo "model is $model"

        cd ${RESNET101_DIR}

        # run batch size 100 for accuracy
        cmd="python eval_int8_model_inference.py -m resnet101 -s -a \
        -d ${DATASET_LOCATION}/dataset/TF_Imagenet_FullData \
        -g ${DATASET_LOCATION}/pre-trained-models/resnet101/int8/resnet101_pad_fused.pb --batch-size=100 >> ${LOGFILE_ACCURACY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_ACCURACY}
        echo "Batch Size: 100" >> ${LOGFILE_ACCURACY}
        eval $cmd >> ${LOGFILE_ACCURACY}

        # run batch size 128 for throughput
        cmd="python eval_int8_model_inference.py -m resnet101 -s -k \
        -g ${DATASET_LOCATION}/pre-trained-models/resnet101/int8/resnet101_pad_fused.pb  \
        --batch-size=128 --num-inter-threads=2 --num-intra-threads=28 >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 128" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

        # run batch size 1 for latency
        cmd="python eval_int8_model_inference.py -m resnet101 -s -k \
        -g ${DATASET_LOCATION}/pre-trained-models/resnet101/int8/resnet101_pad_fused.pb  \
        --batch-size=1 --num-inter-threads=2 --num-intra-threads=28 >> ${LOGFILE_LATENCY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
        echo "Batch Size: 1" >> ${LOGFILE_LATENCY}
        eval $cmd >> ${LOGFILE_LATENCY}

        if [ $? -eq 0 ] ; then
            echo "success"
            STATUS="SUCCESS"
        else
            echo "failure"
            STATUS="FAILURE"
        fi
    fi

	if [ $model == "InceptionV4" ] ; then

        echo "model is $model"

        cd ${INCEPTIONV4_DIR}

        # run batch size 240 for accuracy
        cmd="python eval_int8_model_inference.py -m inceptionv4 -s -a \
        -d ${DATASET_LOCATION}/dataset/TF_Imagenet_FullData \
        -g ${DATASET_LOCATION}/pre-trained-models/inception_v4/int8/final_inception4_int8.pb --batch-size=240  >> ${LOGFILE_ACCURACY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_ACCURACY}
        echo "Batch Size: 240" >> ${LOGFILE_ACCURACY}
        eval $cmd >> ${LOGFILE_ACCURACY}

        # run batch size 240 for throughput
        cmd="python eval_int8_model_inference.py -m inceptionv4 -s -k \
         -g ${DATASET_LOCATION}/pre-trained-models/inception_v4/int8/final_inception4_int8.pb \
         --batch-size=240 --num-inter-threads=2 --num-intra-threads=28  >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 240" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

        # run batch size 1 for latency
        cmd="python eval_int8_model_inference.py -m inceptionv4 -s -k \
         -g ${DATASET_LOCATION}/pre-trained-models/inception_v4/int8/final_inception4_int8.pb \
         --batch-size=1 --num-inter-threads=2 --num-intra-threads=28  >> ${LOGFILE_LATENCY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
        echo "Batch Size: 1" >> ${LOGFILE_LATENCY}
        eval $cmd >> ${LOGFILE_LATENCY}

        if [ $? -eq 0 ] ; then
            echo "success"
            STATUS="SUCCESS"
        else
            echo "failure"
            STATUS="FAILURE"
        fi
    fi
done
