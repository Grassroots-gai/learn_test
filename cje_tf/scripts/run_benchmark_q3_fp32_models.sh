#!/bin/bash
# run_benchmark.sh --model=[NCF] --mode=[training/inference] --single_socket=[True|False]
set -x

HOST=`uname -n | awk -F'.' '{print $1}'`
WORKDIR=`pwd`

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
echo "    SINGLE_SOCKET: ${SINGLE_SOCKET}"
echo "    SERVERNAME: ${SERVERNAME}"
echo "    MODELS: ${MODELS}"
echo "    MODES: ${MODES}"
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
        apt-get install libsm6 libxext6 python-tk numactl -y || yum install libsm6 libxext6 python-tk numactl -y
        ;;
    *)
	which_p=$(which python)
        p_path=$(ls -l $which_p)
        p_path_org=`echo $p_path | awk -F'-> ' '{print $2}'`
        ln -sf /usr/bin/python2.7 /usr/bin/python
        apt-get clean; apt-get update -y || yum clean all; yum update -y
        apt-get install libsm6 libxext6 python-tk numactl -y || yum install libsm6 libxext6 python-tk numactl -y
        ln -sf ${p_path_org} ${which_p} 
        ;;
esac

### install common dependencies
${PIP} install requests

if [ "${TENSORFLOW_BRANCH}" == "master" ]; then
    ${PIP} install --upgrade tf-estimator-nightly
fi

if [ ${DOWNLOAD_WHEEL} == "true" ]; then
    echo "downloading TF wheel"
    ${PIP} install --upgrade build/*.whl
fi

echo "after installing the dependencies......"
${PIP} list

DATASET_DIR=${DATASET_LOCATION}/dataset
PRE_TRAINED_MODEL_DIR=${DATASET_LOCATION}/pre-trained-models
NCF_DIR=${WORKDIR}/tensorflow-models
Wide_Deep_Census_Dir=${WORKDIR}/tensorflow-models-wide-deep-census
Wide_Deep_Criteo_Dir=${WORKDIR}/tensorflow-models-wide-deep-criteo

single_socket_arg=""
if [ ${SINGLE_SOCKET} == "true" ]; then
    single_socket_arg="-s"
fi

IFS=',' read -ra MODELS <<< "$MODELS"
IFS=',' read -ra MODES <<< "$MODES"

for model in "${MODELS[@]}"
do
    for mode in "${MODES[@]}"
    do

    LOGFILE_THROUGHPUT=${WORKDIR}/benchmark_${model}_${mode}_${SERVERNAME}_throughput.log
    LOGFILE_LATENCY=${WORKDIR}/benchmark_${model}_${mode}_${SERVERNAME}_latency.log

    echo ${LOGFILE_THROUGHPUT}
    echo ${LOGFILE_LATENCY}


    if [ ${model} == "NCF" ]; then

        echo "model is ${model}"

        cd ${NCF_DIR}

        export PYTHONPATH=$PYTHONPATH:pwd:pwd/slim
        ${PIP} install -r official/requirements.txt

        if [ ${mode} == "inference" ] ; then

            # run batch size 128 for throughput
            cmd="python run_tf_benchmark.py -b=256 -m=${model} $single_socket_arg -f -v -d=/tf_dataset/dataset/NCF/ml-1m -c=/tf_dataset/pre-trained-models/NCF/fp32/ncf_trained_movielens_1m/ >> ${LOGFILE_THROUGHPUT} 2>&1"

            echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
            echo "Batch Size: 256" >> ${LOGFILE_THROUGHPUT}
            eval $cmd >> ${LOGFILE_THROUGHPUT}

            # run batch size 1 for latency
            cmd="python run_tf_benchmark.py -b=1 -m=${model} $single_socket_arg -f -v -d=/tf_dataset/dataset/NCF/ml-1m -c=/tf_dataset/pre-trained-models/NCF/fp32/ncf_trained_movielens_1m/ >> ${LOGFILE_LATENCY} 2>&1"

            echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
            echo "Batch Size: 1" >> ${LOGFILE_LATENCY}
            eval $cmd >> ${LOGFILE_LATENCY}

        else
            echo "skipping ${model} training for Q3"
        fi

        if [ $? -eq 0 ] ; then
            echo "success"
            STATUS="SUCCESS"
        else
            echo "failure"
            STATUS="FAILURE"
        fi
    fi

    if [ ${model} == "Wide_Deep_Census" ]; then

        echo "model is ${model}"

        cd ${Wide_Deep_Census_Dir}/official/wide_deep

        if [ ${mode} == "training" ] ; then

            # run batch size 40
            cmd="python run_tf_benchmark.py -d /tf_dataset/dataset/wideDeep -b 40 >> ${LOGFILE_THROUGHPUT} 2>&1"

            echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
            echo "Batch Size: 40" >> ${LOGFILE_THROUGHPUT}
            eval $cmd >> ${LOGFILE_THROUGHPUT}

        else
            echo "skipping ${model} inference for Q3"
        fi

        if [ $? -eq 0 ] ; then
            echo "success"
            STATUS="SUCCESS"
        else
            echo "failure"
            STATUS="FAILURE"
        fi
    fi

    if [ ${model} == "Wide_Deep_Criteo" ]; then

        echo "model is ${model}"

        cd ${Wide_Deep_Criteo_Dir}/official/wide_deep

        cp -r /tf_dataset/dataset/wide_deep_kaggle/dataset_files .

        if [ ${mode} == "training" ] ; then

            # run batch size 128 for throughput
            cmd="python run_tf_benchmark.py --batch-size 128 --num_inter_threads 28 --num_intra_threads 4 --omp_num_threads 4 -v >> ${LOGFILE_THROUGHPUT} 2>&1"

            echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
            echo "Batch Size: 128" >> ${LOGFILE_THROUGHPUT}
            eval $cmd >> ${LOGFILE_THROUGHPUT}

        else
            echo "skipping ${model} inference for Q3"
        fi

        if [ $? -eq 0 ] ; then
            echo "success"
            STATUS="SUCCESS"
        else
            echo "failure"
            STATUS="FAILURE"
        fi
    fi

    if [ ${model} == "Inception_ResNet_v2" ]; then
        echo "model is ${model}"
        cd ${WORKDIR}/tensorflow-${model}
        # data location
        dataset_path="${DATASET_DIR}/TF_Imagenet_FullData"
        if [ ${mode} == "training" ] ; then
            rm -rf ./tmp_ckpt
            cmd="python run_tf_benchmark.py -b=64 -m=inception_resnet_v2 --train_dir=./tmp_ckpt -d=${dataset_path}
                                            >> ${LOGFILE_THROUGHPUT} 2>&1"
            echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
            echo "Batch Size: 64" >> ${LOGFILE_THROUGHPUT}
            eval $cmd >> ${LOGFILE_THROUGHPUT}
        else
            echo "skipping ${model} ${mode} for Q3"
        fi

        if [ $? -eq 0 ] ; then
            echo "success"
            STATUS="SUCCESS"
        else
            echo "failure"
            STATUS="FAILURE"
        fi
    fi

    if [ ${model} == "MobileNet_v1" ]; then
        echo "model is ${model}"
        cd ${WORKDIR}/tensorflow-${model}

        # data location
        dataset_path="${DATASET_DIR}/TF_Imagenet_FullData"

        if [ ${mode} == "training" ] ; then
            rm -rf ./tmp_ckpt && mkdir ./tmp_ckpt
            cmd="python run_tf_benchmark.py -b=64 -m=mobilenet_v1 -c ./tmp_ckpt -d=${dataset_path}
                                            >> ${LOGFILE_THROUGHPUT} 2>&1"
            echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
            echo "Batch Size: 64" >> ${LOGFILE_THROUGHPUT}
            eval $cmd >> ${LOGFILE_THROUGHPUT}
        else
            echo "skipping ${model} ${mode} for Q3"
        fi

        if [ $? -eq 0 ] ; then
            echo "success"
            STATUS="SUCCESS"
        else
            echo "failure"
            STATUS="FAILURE"
        fi
    fi

    done
done

