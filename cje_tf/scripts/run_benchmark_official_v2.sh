#!/bin/bash
set -x

HOST=`uname -n | awk -F'.' '{print $1}'`
WORKDIR=`pwd`

if [ -z ${WORKSPACE} ] ; then
    WORKSPACE=`pwd`
fi
if [ -z ${DATASET_LOCATION} ] ; then
    DATASET_LOCATION="${DATASET_LOCATION}"
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

### install common dependencies
${PIP} install requests
${PIP} install tensorflow_model_optimization
echo "after installing the dependencies......"
${PIP} list


### define variables

RESNET_DIR=${WORKDIR}/official-models/official/vision/image_classification
RESNET_DIR_resnet56=${WORKDIR}/official-models/official/benchmark/models
export PYTHONPATH="$PYTHONPATH:${WORKDIR}/official-models"

IFS=',' read -ra MODELS <<< "$MODELS"
for model in "${MODELS[@]}"
do
    echo "running benchmark for model: $model "
    LOGFILE_THROUGHPUT=${WORKDIR}/benchmark_${model}_train_throughput_${SERVERNAME}.log

    echo ${LOGFILE_THROUGHPUT}

    if [ $model == "resnet50_eager" ] ; then

        echo "model is $model"

        cd ${RESNET_DIR}

        # run batch size 128 for throughput
        cmd="KMP_BLOCKTIME=0 OMP_NUM_THREADS=56 python -m resnet_imagenet_main \
        --model_dir=/tmp/keras_resnet \
        --num_gpus=0 \
        --batch_size=128 \
        --train_epochs=1 \
        --train_steps=100 \
        --skip_eval=True \
        --enable_eager=True \
        --use_synthetic_data=false \
        --log_steps=1 \
        --data_dir /tf_dataset/dataset/TF_Imagenet_FullData/ \
        --run_eagerly \
        --distribution_strategy=off >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 128" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

    fi

    if [ $model == "resnet50_eager_1" ] ; then

        echo "model is $model"

        cd ${RESNET_DIR}

        # run batch size 1 for throughput
        cmd="KMP_BLOCKTIME=0 OMP_NUM_THREADS=56 python -m resnet_imagenet_main \
        --model_dir=/tmp/keras_resnet \
        --num_gpus=0 \
        --batch_size=1 \
        --train_epochs=1 \
        --train_steps=100 \
        --skip_eval=True \
        --enable_eager=True \
        --use_synthetic_data=false \
        --log_steps=1 \
        --data_dir /tf_dataset/dataset/TF_Imagenet_FullData/ \
        --run_eagerly \
        --distribution_strategy=off >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 1" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

    fi

    if [ $model == "resnet50_graph" ] ; then

        echo "model is $model"

        cd ${RESNET_DIR}

        # run batch size 128 for throughput
        cmd="KMP_BLOCKTIME=0 OMP_NUM_THREADS=56 python -m resnet_imagenet_main \
        --model_dir=/tmp/keras_resnet \
        --num_gpus=0 \
        --batch_size=128 \
        --train_epochs=1 \
        --train_steps=100 \
        --skip_eval=True \
        --use_synthetic_data=false \
        --log_steps=1 \
        --data_dir /tf_dataset/dataset/TF_Imagenet_FullData/ \
        --distribution_strategy=off >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 128" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

    fi

	if [ $model == "resnet56_eager_cifar10" ] ; then

        echo "model is $model"

        cd ${RESNET_DIR_resnet56}

        # run batch size 128 for throughput
        cmd="KMP_BLOCKTIME=0 OMP_NUM_THREADS=56 python -m resnet_cifar_main \
        --model_dir=/tmp/keras_resnet \
        --num_gpus=0 \
        --batch_size=512 \
        --train_epochs=1 \
        --train_steps=100 \
        --skip_eval=True \
        --enable_eager=True \
        --use_synthetic_data=false \
        --log_steps=1 \
        --data_dir /tf_dataset/dataset/resnet32-cifar10/cifar-10-batches-bin \
        --run_eagerly \
        --distribution_strategy=off >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 512" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

    fi

    if [ $model == "resnet56_graph_cifar10" ] ; then

        echo "model is $model"

        cd ${RESNET_DIR_resnet56}

        # run batch size 128 for throughput
        cmd="KMP_BLOCKTIME=0 OMP_NUM_THREADS=56 python -m resnet_cifar_main \
        --model_dir=/tmp/keras_resnet \
        --num_gpus=0 \
        --batch_size=512 \
        --train_epochs=1 \
        --train_steps=100 \
        --skip_eval=True \
        --use_synthetic_data=false \
        --log_steps=1 \
        --data_dir /tf_dataset/dataset/resnet32-cifar10/cifar-10-batches-bin \
        --distribution_strategy=off >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 512" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

    fi

    if [ $model == "resnet50_minidata" ] ; then

        echo "model is $model"

        LOGFILE_ACCURACY=${WORKDIR}/benchmark_${model}_train_accuracy_${SERVERNAME}.log

        echo ${LOGFILE_ACCURACY}

        cd ${RESNET_DIR}

        echo "checkout to specific branch: 3dee3b86713ae25f5da9aa8238e059c7f30cf021"
        git checkout 3dee3b86713ae25f5da9aa8238e059c7f30cf021
        git branch
        # Apply the attached code patch
        git apply ${WORKSPACE}/cje-tf/patch/accuracy_test.patch

        # run batch size 128 for throughput
        cmd="KMP_BLOCKTIME=0 OMP_NUM_THREADS=56 python -m resnet_imagenet_main \
        --model_dir=/tmp/keras_resnet50 \
        --num_gpus=0 \
        --batch_size=128 \
        --train_epochs=1 \
        --skip_eval=True \
        --use_synthetic_data=false \
        --log_steps=1 \
        --data_dir ${DATASET_LOCATION}/dataset/TF_Imagenet_MiniData_Half \
        --distribution_strategy=off \
        --saved_model=${DATASET_LOCATION}/pre-trained-models/resnet50_minidata/checkpoints_A/saved_model \
        --golden_saved_model=${DATASET_LOCATION}/pre-trained-models/resnet50_minidata/golden_checkpoints_B/saved_model >> ${LOGFILE_ACCURACY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_ACCURACY}
        echo "Batch Size: 128" >> ${LOGFILE_ACCURACY}
        eval $cmd >> ${LOGFILE_ACCURACY}

        echo "checkout back to master"
        git reset --hard
        git checkout master
        git branch

    fi


done

