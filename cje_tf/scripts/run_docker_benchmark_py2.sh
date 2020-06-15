#!/bin/bash -x

# This script is designed to be run inside the docker
# with all the environment variables setup with the docker run command

python --version
server_id=`cat /etc/hostname`
echo "server_id: $server_id"

if [ -z ${WORKSPACE} ] ; then
    WORKSPACE="/workspace"
fi

if [ -z ${DATASET_LOCATION} ] ; then
    DATASET_LOCATION="/tf_dataset"
fi
DATASET_DIR="${DATASET_LOCATION}/dataset"

if [ -z ${GIT_NAME} ] ; then
    GIT_NAME="private-tensorflow"
fi

if [ -z ${RUN_BENCHMARK} ] ; then
    RUN_BENCHMARK=true
fi

if [ -z ${TARGET_PLATFORM} ] ; then
    TARGET_PLATFORM="avx"
fi

if [ -z ${TENSORFLOW_BRANCH} ] ; then
    TENSORFLOW_BRANCH="v1.8.0"
fi

if [ -z ${PYTHON} ] ; then
    PYTHON="2.7"
fi

if [ -z ${SINGLE_SOCKET} ] ; then
    SINGLE_SOCKET=true
fi

if [ -z ${SERVERNAME} ] ; then
    SERVERNAME="nervana-skx17"
fi

if [ -z ${RUN_Q1MODELS} ] ; then
    RUN_Q1MODELS=true
fi

if [ -z ${RUN_Q2MODELS} ] ; then
    RUN_Q2MODELS=false
fi

#
# below are the dependencies that are needed when running with different models,
#

# debug
echo "before installing the dependencies......"
pip list

if [ "${TENSORFLOW_BRANCH}" == "master" ]; then
    pip install --upgrade tf-estimator-nighlty
fi

# for Q1 models
### install common dependencies
export DEBIAN_FRONTEND=noninteractive # This is to disable tzdata prompt
apt-get clean; apt-get update -y;
apt-get install cmake curl libsm6 libxext6 numactl python-tk -y
pip install requests

# addition dependencies
#apt -y install python-tk # for fastrcnn rfcn UNet
pip install tensorflow_probability # for transformerLanguage
pip install progressbar # for deepSpeech

pip install matplotlib
pip install opencv-python
if [ "${PYTHON}" == "2.7" ]; then
    apt-get install python-tk -y
else
    apt-get install python3-tk -y
fi

# for Q2 models
echo y | pip uninstall joblib
pip install joblib==0.11

pip install opencv-python
pip install Cython
pip install pillow
pip install lxml
pip install jupyter
pip install sympy
pip install gym
pip install google-api-python-client
pip install oauth2client

# 3DGAN
pip install h5py

# ALE for A3C
cd $WORKSPACE/Arcade-Learning-Environment
cmake -DUSE_SDL=OFF -DUSE_RLGLUE=OFF -DBUILD_EXAMPLES=OFF .
make -j 8
pip install .

# keras for 3dgan
pip install keras
pip install scikit-learn==0.19

# for wavenet
pip install librosa==0.6.1

cd $WORKSPACE/tensorflow-models/research/slim
pip install -e .

# for deepSpeech
# only running for nightly with private-tensorflow repo
# we don't run this for releas testing with tensorflow repo
# for some reason after this dependencies is installed, TF wheel gets re-installed with earlier version??
if [ "${GIT_NAME}" == "private-tensorflow" ]; then
    cd $WORKSPACE/tensorflow-DeepSpeech
    mkdir -p data/lm
    cp {DATASET_LOCATION}/dataset/deepSpeech/dependencies/* ./data/lm/
    pip install -r requirements.txt
    pip install --upgrade ./native_client/deepspeech*.whl
fi

# for WaveNet_Magenta
apt-get install libasound2-dev

# install graph transform tool
cd /$GIT_NAME
bazel build tensorflow/tools/graph_transforms:transform_graph
bazel build tensorflow/tools/graph_transforms:summarize_graph

echo "after installing the dependencies......"
pip list

echo "checking installed TF wheel....."
INSTALLED_VER=`pip list | grep 'tensorflow ' | awk -F' ' '{print $2}'`
echo "TF insalled version is: $INSTALLED_VER"
if [ "${GIT_NAME}" == "tensorflow" ]; then
    TF_BRANCH=`echo ${TENSORFLOW_BRANCH} | sed s/v//`
    if [ "$INSTALLED_VER" != "${TF_BRANCH}" ]; then
        echo "TF installed version does not match the current branch: $TENSORFLOW_BRANCH"
        PY_VER=${PYTHON//./}
        pip install --upgrade ${WORKSPACE}/build/tensorflow*cp${PY_VER}*.whl
    else
        echo "TF installed version match the current branch: $TENSORFLOW_BRANCH"
    fi
fi

echo 'Running with parameters:'
echo "    WORKSPACE: ${WORKSPACE}"
echo "    DATASET_LOCATION: ${DATASET_LOCATION}"
echo '    Mounted volumes:'
echo "        ${WORKSPACE} mounted on: /workspace"
echo "        ${DATASET_LOCATION} mounted on: ${DATASET_LOCATION}"
echo "    GIT_NAME: ${GIT_NAME}"
echo "    RUN_BENCHMARK: ${RUN_BENCHMARK}"
echo "    TARGET_PLATFORM: ${TARGET_PLATFORM}"
echo "    TENSORFLOW_BRANCH: ${TENSORFLOW_BRANCH}"
echo "    PYTHON: ${PYTHON}"
echo "    SINGLE_SOCKET: ${SINGLE_SOCKET}"
echo "    SEVERNAME: ${SEVERNAME}"
echo "    MODELS: ${MODELS}"
echo "    MODES: ${MODES}"
echo ' '

cd ${WORKSPACE}

if [ "$RUN_BENCHMARK" == true ]; then

    echo "running benchmark testing"

    cd $WORKSPACE
    IFS=',' read -ra MODELS <<< "$MODELS"
    IFS=',' read -ra MODES <<< "$MODES"

    for model in "${MODELS[@]}"
    do

        if [ "$model" = 'dcgan' ]; then
            cd $WORKSPACE/dcgan-tf-benchmark
            ln -s ${DATASET_LOCATION}/dataset/dcgan/data data
        fi

        if [ "$model" = 'ds2' ]; then
            cd $WORKSPACE
            $WORKSPACE/cje-tf/scripts/fixup_ds2.sh
        fi

        if [ "$model" = 'cifar10' ]; then
            $WORKSPACE/cje-tf/scripts/fixup_cifar10.sh
        fi

        if [ "$model" = 'fastrcnn' ]; then
            mkdir -p /opt/tensorflow/protoc-3.3.0-linux-x86_64
            cd /opt/tensorflow/protoc-3.3.0-linux-x86_64
            curl -OL https://github.com/google/protobuf/releases/download/v3.3.0/protoc-3.3.0-linux-x86_64.zip
            unzip protoc-3.3.0-linux-x86_64.zip
            cd $WORKSPACE
            $WORKSPACE/cje-tf/scripts/fixup_fastrcnn.sh
        fi

        if [ "$model" = 'mnist' ]; then
            cd /tmp
            cp ${DATASET_LOCATION}/dataset/mnist/mnist_data.tar.gz .
            tar -xzvf mnist_data.tar.gz
        fi

        if [ "$model" = 'resnet32cifar10' ]; then
            cd $WORKSPACE
            $WORKSPACE/cje-tf/scripts/fixup_resnet32_cifar10.sh
        fi

        if [ "$model" = 'rfcn' ]; then
            cd $WORKSPACE
            $WORKSPACE/cje-tf/scripts/fixup_rfcn.sh
        fi

        if [ "$model" = 'SSDvgg16' ]; then
            cd $WORKSPACE
            $WORKSPACE/cje-tf/scripts/fixup_tensorflowSSD.sh
            cd $WORKSPACE
            if [ ! -d ${WORKSPACE}/data/voc ]; then
                mkdir -p ${WORKSPACE}/data/voc 
                ln -s ${DATASET_DIR}/SSDvgg16/voc/tfrecords ${WORKSPACE}/data/voc
            fi

        fi

        if [ "$model" = 'transformerSpeech' ]; then
            cd $WORKSPACE
            $WORKSPACE/cje-tf/scripts/fixup_transformerSpeech.sh
        fi

        for mode in "${MODES[@]}"
        do
            echo "running benchmark for $model, $mode "
            # Q1 models
            #Q1_MODELS="resnet50 inception3 vgg16 ds2 SSDvgg16 mnist resnet32cifar10 cifar10 dcgan"
            # Q2 models
            #Q2MODELS="inception_v4 inception_resnet_v2 SqueezeNet YoloV2 fastrcnn gnmt rfcn transformerLanguage transformerSpeech WaveNet wideDeep WaveNet_Magenta deepSpeech mobilenet_v1"
            # Checking the model we want to run is a Q1 model or Q2 model
            #echo $Q1_MODELS | grep -w -F -q $model
            cd $WORKSPACE
            if [ "$RUN_Q1MODELS" == true ]; then
                echo "$model is a Q1 model"
                $WORKSPACE/cje-tf/scripts/run_benchmark.sh --model=$model --mode=$mode --single_socket=$SINGLE_SOCKET
            fi

            if [ "$RUN_Q2MODELS" == true ]; then
                echo "$model is a Q2 model"
                $WORKSPACE/cje-tf/scripts/run_benchmark_q2_models.sh --model=$model --mode=$mode --single_socket=$SINGLE_SOCKET
            fi

            if [ $? -eq 0 ] ; then
                echo "running model ${model} success"
                RESULT="SUCCESS"
            else
                echo "running model ${model} fail"
                RESULT="FAILURE"
                ERROR="1"
            fi

            if [ -f "$WORKSPACE/benchmark_${model}_${mode}_${server_id}.log" ]; then
                mv "$WORKSPACE/benchmark_${model}_${mode}_${server_id}.log"  "$WORKSPACE/benchmark_${model}_${mode}_${SERVERNAME}.log"
            fi
            if [ -f "$WORKSPACE/benchmark_${model}_${mode}_eval_${server_id}.log" ]; then
                mv "$WORKSPACE/benchmark_${model}_${mode}_eval_${server_id}.log"  "$WORKSPACE/benchmark_${model}_${mode}_eval_${SERVERNAME}.log"
            fi
            if [ -f "$WORKSPACE/benchmark_${model}_${mode}_${server_id}_throughput.log" ]; then
                mv "$WORKSPACE/benchmark_${model}_${mode}_${server_id}_throughput.log"  "$WORKSPACE/benchmark_${model}_${mode}_${SERVERNAME}_throughput.log"
            fi
            if [ -f "$WORKSPACE/benchmark_${model}_${mode}_${server_id}_latency.log" ]; then
                mv "$WORKSPACE/benchmark_${model}_${mode}_${server_id}_latency.log"  "$WORKSPACE/benchmark_${model}_${mode}_${SERVERNAME}_latency.log"
            if [ -f "$WORKSPACE/benchmark_${model}_${mode}_${server_id}_accuracy.log" ]; then
                mv "$WORKSPACE/benchmark_${model}_${mode}_${server_id}_accuracy.log"  "$WORKSPACE/benchmark_${model}_${mode}_${SERVERNAME}_accuracy.log"
            fi
            fi
            if [ -f "$WORKSPACE/benchmark_${model}_${mode}_${server_id}_accuracy.log" ]; then
                mv "$WORKSPACE/benchmark_${model}_${mode}_${server_id}_accuracy.log"  "$WORKSPACE/benchmark_${model}_${mode}_${SERVERNAME}_accuracy.log"
            fi
            if [ -f "$WORKSPACE/benchmark_${model}_${mode}_${server_id}_accuracy.log" ]; then
                mv "$WORKSPACE/benchmark_${model}_${mode}_${server_id}_accuracy.log"  "$WORKSPACE/benchmark_${model}_${mode}_${SERVERNAME}_accuracy.log"
            fi
        done
    done

else

    model="resnet32cifar10"
    mode="training"

    echo "running convergence for $model, $mode "

    cd $WORKSPACE
    $WORKSPACE/cje-tf/scripts/run_benchmark.sh --model=$model --mode=$mode --single_socket=$SINGLE_SOCKET
    if [ $? -eq 0 ] ; then
        echo "running model ${model} success"
        RESULT="SUCCESS"
    else
        echo "running model ${model} fail"
        RESULT="FAILURE"
        ERROR="1"
    fi
    if [ -f "$WORKSPACE/benchmark_${model}_${mode}_${server_id}.log" ]; then
        mv "$WORKSPACE/benchmark_${model}_${mode}_${server_id}.log"  "$WORKSPACE/benchmark_${model}_${mode}_${SERVERNAME}.log"
    fi
    if [ -f "$WORKSPACE/benchmark_${model}_${mode}_eval_${server_id}.log" ]; then
        mv "$WORKSPACE/benchmark_${model}_${mode}_eval_${server_id}.log"  "$WORKSPACE/benchmark_${model}_${mode}_eval_${SERVERNAME}.log"
    fi

fi

