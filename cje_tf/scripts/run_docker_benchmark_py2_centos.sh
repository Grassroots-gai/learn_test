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
    DATASET_LOCATION="/dataset"
fi

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

# because yum doesn't support python3, temporarily to use python2 to do yum stuff
p_version=$(python -V 2>&1)
case "$p_version" in
    *2.7*)
        yum clean all; yum update -y; yum install numactl cmake libSM libXext python-pip tkinter -y
        ;;
    *)
	which_p=$(which python)
        p_path=$(ls -l $which_p)
        p_path_org=`echo $p_path | awk -F'-> ' '{print $2}'`
        ln -sf /usr/bin/python2.7 /usr/bin/python
        yum clean all; yum update -y; yum install numactl cmake libSM libXext python34-tkinter -y
        ln -sf ${p_path_org} ${which_p} 
        ;;
esac

${PIP} install requests
${PIP} install matplotlib
${PIP} install opencv-python

# for Q2 models
echo y | ${PIP} uninstall joblib
${PIP} install joblib==0.11

${PIP} install opencv-python
${PIP} install Cython
${PIP} install pillow
${PIP} install lxml
${PIP} install jupyter
${PIP} install sympy
${PIP} install gym
${PIP} install google-api-python-client
${PIP} install oauth2client
# for 1.13
${PIP} install absl

# 3DGAN
${PIP} install h5py

# ALE for A3C
cd $WORKSPACE/Arcade-Learning-Environment
cmake -DUSE_SDL=OFF -DUSE_RLGLUE=OFF -DBUILD_EXAMPLES=OFF .
make -j 8
${PIP} install .

# keras for 3dgan
${PIP} install keras

# for wavenet
# we aren't running wavenet for this test
#${PIP} install librosa==0.6.1

cd $WORKSPACE/tensorflow-models/research/slim
${PIP} install -e .

# for deepSpeech, temporarily disable
#cd $WORKSPACE/tensorflow-DeepSpeech
#mkdir -p data/lm
#cp {DATASET_LOCATION}/deepSpeech/dependencies/* ./data/lm/
#${PIP} install -r requirements.txt
#${PIP} install --upgrade ./native_client/deepspeech*.whl

# for WaveNet_Magenta
# apt-get install libasound2-dev
# we aren't running wavenet
# yum install alsa-lib-devel -y

# install graph transform tool
# for centos container, tensorflow is not cloned and not installed, 
# need to clone tensorflow otherwise this will fail
cd /$GIT_NAME
bazel build tensorflow/tools/graph_transforms:transform_graph
bazel build tensorflow/tools/graph_transforms:summarize_graph

echo "after installing the dependencies......"
${PIP} list

# 03/07/2019, based on ashahba, no wheel installed in centos container, it’s user’s responsibility to install or build the version they like
echo "checking installed TF wheel....."
INSTALLED_VER=`${PIP} list | grep 'tensorflow ' | awk -F' ' '{print $2}'`
echo "TF insalled version is: $INSTALLED_VER"
PY_VER=${PYTHON//./}
${PIP} install --upgrade ${WORKSPACE}/build/*tensorflow*cp${PY_VER}*.whl
#if [ "${GIT_NAME}" == "tensorflow" ]; then
#    TF_BRANCH=`echo ${TENSORFLOW_BRANCH} | sed s/v//`
#    if [ "$INSTALLED_VER" != "${TENSORFLOW_BRANCH}" ]; then
#        echo "TF installed version does not match the current branch: $TENSORFLOW_BRANCH"
#        PY_VER=${PYTHON//./}
#        ${PIP} install --upgrade ${WORKSPACE}/build/tensorflow*cp${PY_VER}*.whl
#    else
#        echo "TF installed version match the current branch: $TENSORFLOW_BRANCH"
#    fi
#fi

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

${PIP} list
cd ${WORKSPACE}

if [ ! -d "/dataset" ]; then
    mkdir /dataset
fi

# for resnet32cifar10 dataset
if [ ! -d "/dataset/cifar-10-batches-bin" ]; then
    cd /dataset
    cp ${DATASET_LOCATION}/cifar-10-binary.tar.gz .
    tar -xzvf cifar-10-binary.tar.gz
fi

if [ "$RUN_BENCHMARK" == true ]; then

    echo "running benchmark testing"

    cd $WORKSPACE
    IFS=',' read -ra MODELS <<< "$MODELS"
    IFS=',' read -ra MODES <<< "$MODES"

    for model in "${MODELS[@]}"
    do

        if [ "$model" = 'dcgan' ]; then
            cd $WORKSPACE/dcgan-tf-benchmark
            ln -s ${DATASET_LOCATION}/dcgan/data data
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
            cp ${DATASET_LOCATION}/mnist_data.tar.gz .
            tar -xzvf mnist_data.tar.gz
        fi

        if [ "$model" = 'resnet32cifar10' ]; then
            cd $WORKSPACE
            $WORKSPACE/cje-tf/scripts/fixup_resnet32_cifar10.sh
        fi

        if [ "$model" = 'SSDvgg16' ]; then
            cd $WORKSPACE
            $WORKSPACE/cje-tf/scripts/fixup_tensorflowSSD.sh
            cd $WORKSPACE
            if [ ! -d ${WORKSPACE}/data/voc ]; then
                mkdir -p ${WORKSPACE}/data/voc
                ln -s ${DATASET_LOCATION}/voc-data/tfrecords ${WORKSPACE}/data/voc
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
