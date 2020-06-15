#!/bin/bash
set -x
# this script runs the 2018 Q4 fp32 models, including:
#     - ResNet101
#     - FaceNet
#     - MTCC
#     - SSDMobilenet
# this script assumes the following environment variable has been set:
#     - WORKSPACE: this is the workspace under Jenkin job, e.g.e /dataset/cje/aipg-tf/workspace/<Jenkin_Job_Name>
#     - DATASET_LOCATION: this is the root of the dataset location, e.g. by default all dataset is under /dataset
#     - TARGET_PLATFORM: can be avx/avx2/avx512, currently not used
#     - TENSORFLOW_BRANCH:  currently not used
#     - SINGLE_SOCKET: whether to run single socket or not
#     - SERVERNAME: the host name of the Jenkin node
#     - MODELS: list of models to run separated by comma, e.g. FaceNet,MTCC,ResNet101
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
if [ -z ${DOWNLOAD_WHEEL} ] ; then
    DOWNLOAD_WHEEL=false
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
echo "    DOWNLOAD_WHEEL: ${DOWNLOAD_WHEEL}"

WORKDIR=${WORKSPACE}
DATASET_DIR=${DATASET_LOCATION}/dataset
PRE_TRAINED_MODEL_DIR=${DATASET_LOCATION}/pre-trained-models
RESNET101_DIR=${WORKDIR}/tensorflow-inference-resnet101/int8_models_benchmark
FACENET_DIR=${WORKDIR}/tensorflow-FaceNet
MTCC_DIR=${WORKDIR}/tensorflow-MTCC
DENSENET169_DIR=${WORKDIR}/tensorflow-DenseNet169
SSDMobilenet_DIR=${WORKDIR}/tensorflow-models/research/

single_socket_arg=""
if [ ${SINGLE_SOCKET} == "true" ]; then
    single_socket_arg="-s"
fi

install_dependencies () {

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

    ### dependencies for MTCC
    ${PIP} install opencv-python
    ${PIP} install easydict

    if [ "${TENSORFLOW_BRANCH}" == "master" ]; then
        ${PIP} install --upgrade tf-estimator-nightly
    fi

    if [ ${DOWNLOAD_WHEEL} == "true" ]; then
        echo "downloading TF wheel"
        ${PIP} install --upgrade build/*.whl
    fi

    # debug
    echo "after installing the dependencies......"
    ${PIP} list

}

run_benchmark () {

    model=$1
    mode=$2

    LOGFILE_THROUGHPUT=${WORKDIR}/benchmark_${model}_${mode}_${SERVERNAME}_throughput.log
    LOGFILE_LATENCY=${WORKDIR}/benchmark_${model}_${mode}_${SERVERNAME}_latency.log
    LOGFILE_ACCURACY=${WORKDIR}/benchmark_${model}_${mode}_${SERVERNAME}_accuracy.log

    echo ${LOGFILE_THROUGHPUT}
    echo ${LOGFILE_LATENCY}
    echo ${LOGFILE_ACCURACY}

    if [ ${model} == "FaceNet" ]; then

        echo "model is ${model}"

        cd ${FACENET_DIR}

        if [ ${mode} == "inference" ] ; then
            
            # run batch size 100 for both throughput and accuracy
            cmd="python run_tf_benchmark.py -b 100 -f -c ${PRE_TRAINED_MODEL_DIR}/facenet-model/fp32/ -d ${DATASET_DIR}/lfw_mtcnnpy_160/ -v >> ${LOGFILE_THROUGHPUT} 2>&1" 
            
            echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
            echo "Batch Size: 100" >> ${LOGFILE_THROUGHPUT}
            eval $cmd >> ${LOGFILE_THROUGHPUT}

            # run batch size 1 for latency            
            cmd="python run_tf_benchmark.py -b 1 -f -c ${PRE_TRAINED_MODEL_DIR}/facenet-model/fp32/ -d ${DATASET_DIR}/lfw_mtcnnpy_160/ -v >> ${LOGFILE_LATENCY} 2>&1"

            echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
            echo "Batch Size: 1" >> ${LOGFILE_LATENCY}
            eval $cmd >> ${LOGFILE_LATENCY}

        else
            echo "skipping ${model} ${mode} for Q4"
        fi

        if [ $? -eq 0 ] ; then
            echo "success"
            STATUS="SUCCESS"
        else
            echo "failure"
            STATUS="FAILURE"
        fi
    fi

    if [ ${model} == "MTCC" ]; then

        echo "model is ${model}"

        cd ${MTCC_DIR}

        if [ ${mode} == "inference" ] ; then
            
            # run batch size 1 for accuracy, latency and throughput
            cmd="python run_tf_benchmark.py -f -s -d ${DATASET_DIR}/lfw/ >> ${LOGFILE_THROUGHPUT} 2>&1" 
            
            echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
            echo "Batch Size: 1" >> ${LOGFILE_THROUGHPUT}
            eval $cmd >> ${LOGFILE_THROUGHPUT}

        else
            echo "skipping ${model} ${mode} for Q4"
        fi

        if [ $? -eq 0 ] ; then
            echo "success"
            STATUS="SUCCESS"
        else
            echo "failure"
            STATUS="FAILURE"
        fi
    fi

    if [ ${model} == "ResNet101" ]; then

        echo "model is ${model}"

        cd ${RESNET101_DIR}

        if [ ${mode} == "inference" ] ; then
            
            # run batch size 128 for throughput
            cmd="python eval_int8_model_inference.py -m resnet101 -s -k -g ${PRE_TRAINED_MODEL_DIR}/resnet101/fp32/optimized_graph.pb  --batch-size=128 --num-inter-threads=2 --num-intra-threads=28 >> ${LOGFILE_THROUGHPUT} 2>&1"
            
            echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
            echo "Batch Size: 128" >> ${LOGFILE_THROUGHPUT}
            eval $cmd >> ${LOGFILE_THROUGHPUT}

            # run batch size 1 for latency            
            cmd="python eval_int8_model_inference.py -m resnet101 -s -k -g ${PRE_TRAINED_MODEL_DIR}/resnet101/fp32/optimized_graph.pb --batch-size=1 --num-inter-threads=2 --num-intra-threads=28 >> ${LOGFILE_LATENCY} 2>&1"

            echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
            echo "Batch Size: 1" >> ${LOGFILE_LATENCY}
            eval $cmd >> ${LOGFILE_LATENCY}

            # run batch size 100 for accuracy
            cmd="python eval_int8_model_inference.py -m resnet101 -s -a -d ${DATASET_DIR}/TF_Imagenet_FullData -g ${PRE_TRAINED_MODEL_DIR}/resnet101/fp32/optimized_graph.pb --batch-size=100 >> ${LOGFILE_ACCURACY} 2>&1"

            echo "RUNCMD: $cmd " >& ${LOGFILE_ACCURACY}
            echo "Batch Size: 100" >> ${LOGFILE_ACCURACY}
            eval $cmd >> ${LOGFILE_ACCURACY}

        else
            echo "skipping ${model} ${mode} for Q4"
        fi
		if [ $? -eq 0 ] ; then
            echo "success"
            STATUS="SUCCESS"
        else
            echo "failure"
            STATUS="FAILURE"
        fi
    fi

    if [ ${model} == "DenseNet169" ]; then

        echo "model is ${model}"

        cd ${DENSENET169_DIR}


        if [ ${mode} == "inference" ] ; then
            
            # run batch size 100 for throughput
	    cmd="python run_tf_benchmark.py -b 100 -m=densenet169 -s -f -v -g ${PRE_TRAINED_MODEL_DIR}/densenet-169/fp32/frozen_graph/frozen_densnet_bs_100.pb -p -d ${DATASET_DIR}/TF_Imagenet_FullData >> ${LOGFILE_THROUGHPUT} 2>&1" 
            
            echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
            echo "Batch Size: 100" >> ${LOGFILE_THROUGHPUT}
            eval $cmd >> ${LOGFILE_THROUGHPUT}

            # run batch size 1 for latency            
	    cmd="python run_tf_benchmark.py -b 1 -m=densenet169 -s -f -v -g ${PRE_TRAINED_MODEL_DIR}/densenet-169/fp32/frozen_graph/frozen_densnet_bs_1.pb -p -d ${DATASET_DIR}/TF_Imagenet_FullData >> ${LOGFILE_LATENCY} 2>&1"

            echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
            echo "Batch Size: 1" >> ${LOGFILE_LATENCY}
            eval $cmd >> ${LOGFILE_LATENCY}

            # run batch size 100 for accuracy
	    cmd="python run_tf_benchmark.py -b 100 -m=densenet169 -s -f -v -g ${PRE_TRAINED_MODEL_DIR}/densenet-169/fp32/frozen_graph/frozen_densnet_bs_100.pb -p -d ${DATASET_DIR}/TF_Imagenet_FullData -a >> ${LOGFILE_ACCURACY} 2>&1" 
            
            echo "RUNCMD: $cmd " >& ${LOGFILE_ACCURACY}
            echo "Batch Size: 100" >> ${LOGFILE_ACCURACY}
            eval $cmd >> ${LOGFILE_ACCURACY}
        else
	    echo "skipping ${model} ${mode} for Q4"
	fi

	if [ $? -eq 0 ] ; then
	    echo "success"
	    STATUS="SUCCESS"
	else
	    echo "failure"
	    STATUS="FAILURE"
	fi
    fi

    if [ ${model} == "SSDMobilenet" ]; then

        echo "model is ${model}"

        cd ${SSDMobilenet_DIR}

        export PYTHONPATH=$PYTHONPATH:`pwd`:`pwd`/slim:`pwd`/object_detection
        export KMP_AFFINITY='granularity=fine,compact,1,0'
        pushd object_detection/inference
        sed -e "s/intra_op_parallelism_threads=56/intra_op_parallelism_threads=28/" infer_detections.py > tmp.py
        mv tmp.py infer_detections.py
        popd

        if [ ${mode} == "inference" ] ; then
            
            export PYTHONPATH=$PYTHONPATH:`pwd`:`pwd`/slim:`pwd`/object_detection
            export KMP_AFFINITY='granularity=fine,compact,1,0'
            pushd object_detection/inference
            sed -e "s/intra_op_parallelism_threads=56/intra_op_parallelism_threads=28/" infer_detections.py > tmp.py
            mv tmp.py infer_detections.py
            popd

            # run batch size 1 for accuracy, latency and throughput
            cmd="numactl -m 1 python object_detection/inference/infer_detections.py --input_tfrecord_paths=${PRE_TRAINED_MODEL_DIR}/SSDMobilenet/fp32/coco_val.record --output_tfrecord_path=${PRE_TRAINED_MODEL_DIR}/SSDMobilenet/fp32/SSD-mobilenet-out.tfrecord --inference_graph=${PRE_TRAINED_MODEL_DIR}/SSDMobilenet/fp32/frozen_inference_graph.pb --discard_image_pixels=True >> ${LOGFILE_THROUGHPUT} 2>&1"
            
	    echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
            echo "Batch Size: 1" >> ${LOGFILE_THROUGHPUT}
            eval $cmd >> ${LOGFILE_THROUGHPUT}

        else
            echo "skipping ${model} ${mode} for Q4"
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

install_dependencies

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


