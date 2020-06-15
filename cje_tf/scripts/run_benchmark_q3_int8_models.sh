#!/bin/bash
# run_benchmark.sh --model=[FastRCNN|YoloV2] --single_socket=[True|False]
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
        apt-get install libsm6 libxext6 python-tk numactl wget curl unzip -y || yum install libsm6 libxext6 python-tk numactl wget curl unzip -y
        ;;
    *)
	which_p=$(which python)
        p_path=$(ls -l $which_p)
        p_path_org=`echo $p_path | awk -F'-> ' '{print $2}'`
        ln -sf /usr/bin/python2.7 /usr/bin/python
        apt-get clean; apt-get update -y || yum clean all; yum update -y
        apt-get install libsm6 libxext6 python-tk numactl wget curl unzip -y || yum install libsm6 libxext6 python-tk numactl wget curl unzip -y
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

DATASET_DIR=${DATASET_LOCATION}/dataset
PRE_TRAINED_MODEL_DIR=${DATASET_LOCATION}/pre-trained-models
FASTRCNN_BATCHSIZE=1
FASTRCNN_DIR=${WORKDIR}/tensorflow-models-fastrcnn
SSDMOBILENET_DIR=${WORKDIR}/tensorflow-models-ssdmobilenet
SSDMOBILENET_BATCHSIZE=1
YOLO_V2_DIR=${WORKDIR}/tensorflow-YoloV2
SSD_VGG_16_DIR=${WORKDIR}/tensorflow-ssdvgg16
RESNET50_DIR=${WORKDIR}/tensorflow-inference/int8_models_benchmark
INCEPTIONV3_DIR=${WORKDIR}/tensorflow-inference/int8_models_benchmark
INCEPTION_RESNET_V2_DIR=${WORKDIR}/tensorflow-inference/int8_models_benchmark
MOBILENET_V1_DIR=${WORKDIR}/tensorflow-inference/int8_models_benchmark
RFCN_DIR=${WORKDIR}/tensorflow-inference/int8_models_benchmark

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

    if [ $model == "FastRCNN" ] ; then

        echo "model is $model"

        COCOAPI_DIR=${WORKDIR}/cocoapi

        # COCO API installation
        # make sure the cocoapi repo is checked out: git clone https://github.com/cocodataset/cocoapi.git
        cd $COCOAPI_DIR/PythonAPI
        make
        rm -rf ${FASTRCNN_DIR}/research/pycocotools
        cp -r pycocotools ${FASTRCNN_DIR}/research/

        # install dependency
        rm -rf /opt/protoc-3.3.0
        mkdir -p /opt/protoc-3.3.0
        cd /opt/protoc-3.3.0
        curl -OL https://github.com/google/protobuf/releases/download/v3.3.0/protoc-3.3.0-linux-x86_64.zip
        unzip protoc-3.3.0-linux-x86_64.zip

        export PATH=/opt/protoc-3.3.0/bin:$PATH

        cd ${FASTRCNN_DIR}/research
        protoc object_detection/protos/*.proto --python_out=.
        export PYTHONPATH=$PYTHONPATH:`pwd`:`pwd`/slim:`pwd`/object_detection

        # run batch size 1 for throughput and latency
        cmd="python eval_int8_model_inference.py -k -v ${single_socket_arg} -d ${DATASET_DIR}/fastrcnn/dataset/ -g ${FASTRCNN_DIR}/research/final_test_cwise.pb >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 1" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

        # run batch size 1 for accuracy
        cmd="python eval_int8_model_inference.py -a -v ${single_socket_arg} -d ${DATASET_DIR}/fastrcnn/dataset/ -g ${FASTRCNN_DIR}/research/final_test_cwise.pb >> ${LOGFILE_ACCURACY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_ACCURACY}
        echo "Batch Size: 1" >> ${LOGFILE_ACCURACY}
        eval $cmd >> ${LOGFILE_ACCURACY}

        if [ $? -eq 0 ] ; then
            echo "success"
            STATUS="SUCCESS"
        else
            echo "failure"
            STATUS="FAILURE"
        fi
    fi

    if [ $model == "SSDMobilenet" ] ; then

        echo "model is $model"

        COCOAPI_DIR=${WORKDIR}/cocoapi

        # COCO API installation
        # make sure the cocoapi repo is checked out: git clone https://github.com/cocodataset/cocoapi.git
        cd $COCOAPI_DIR/PythonAPI
        make
        rm -rf ${SSDMOBILENET_DIR}/research/pycocotools
        cp -r pycocotools ${SSDMOBILENET_DIR}/research/

        # install dependency
        rm -rf /opt/protoc-3.3.0
        mkdir -p /opt/protoc-3.3.0
        cd /opt/protoc-3.3.0
        curl -OL https://github.com/google/protobuf/releases/download/v3.3.0/protoc-3.3.0-linux-x86_64.zip
        unzip protoc-3.3.0-linux-x86_64.zip

        export PATH=/opt/protoc-3.3.0/bin:$PATH

        cd ${SSDMOBILENET_DIR}/research
        protoc object_detection/protos/*.proto --python_out=.
        export PYTHONPATH=$PYTHONPATH:`pwd`:`pwd`/slim:`pwd`/object_detection

        # run batch size 1 for throughput and latency
        cmd="python eval_int8_model_inference.py -k -v ${single_socket_arg} -d ${DATASET_DIR}/fastrcnn/dataset/ -g ${PRE_TRAINED_MODEL_DIR}/SSDMobilenet/int8/int8_final.pb >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 1" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

        # run batch size 1 for accuracy
        cmd="python eval_int8_model_inference.py -a -v ${single_socket_arg} -d ${DATASET_DIR}/fastrcnn/dataset/ -g ${PRE_TRAINED_MODEL_DIR}/SSDMobilenet/int8/int8_final.pb >> ${LOGFILE_ACCURACY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_ACCURACY}
        echo "Batch Size: 1" >> ${LOGFILE_ACCURACY}
        eval $cmd >> ${LOGFILE_ACCURACY}

        if [ $? -eq 0 ] ; then
            echo "success"
            STATUS="SUCCESS"
        else
            echo "failure"
            STATUS="FAILURE"
        fi
    fi

    if [ $model == "SSD-VGG16" ] ; then

        echo "model is $model"

        cd ${SSD_VGG_16_DIR}

        # run batch size 224 for accuracy
        cmd="python eval_int8_model_inference.py -g ${PRE_TRAINED_MODEL_DIR}/ssd-vgg16/int8/final_intel_qmodel_ssd.pb -a -d ${DATASET_DIR}/SSDvgg16/data -b 224 >> ${LOGFILE_ACCURACY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_ACCURACY}
        echo "Batch Size: 224" >> ${LOGFILE_ACCURACY}
        eval $cmd >> ${LOGFILE_ACCURACY}

        # run batch size 224 for throughput
        cmd="python eval_int8_model_inference.py -g ${PRE_TRAINED_MODEL_DIR}/ssd-vgg16/int8/final_intel_qmodel_ssd.pb -k -s -b 224 >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 224" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

        # run batch size 1 for latency
        cmd="python eval_int8_model_inference.py -g ${PRE_TRAINED_MODEL_DIR}/ssd-vgg16/int8/final_intel_qmodel_ssd.pb -k -s -b 1 >> ${LOGFILE_LATENCY} 2>&1"

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

    if [ $model == "YoloV2" ] ; then

        echo "model is $model"

        # install dependency if there is any
        apt-get install -y libsm6 libxext6

        # Copy GroundTruth Data for accuracy
        cp -r ${DATASET_DIR}/yolo_v2/ground-truth/ ${YOLO_V2_DIR}/mAP

        cd ${YOLO_V2_DIR}

        # run batch size 1 for latency using benchmark-only

        cmd="python eval_int8_model_inference.py \
                    --in-graph ${PRE_TRAINED_MODEL_DIR}/YoloV2/int8/yolov2-voc.pb \
                    --data-location ${DATASET_DIR}/yolo_v2/JPEGImages/ \
                    --metaLoad ${PRE_TRAINED_MODEL_DIR}/YoloV2/int8/yolov2-voc.meta \
                    ${single_socket_arg} \
                    --batch-size 1 \
                    --benchmark-only >> ${LOGFILE_LATENCY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
        echo "Batch Size: 1" >> ${LOGFILE_LATENCY}
        eval $cmd >> ${LOGFILE_LATENCY}

        if [ $? -eq 0 ] ; then
            echo "DBG: success: running model $model batch size 1 for latency"
            STATUS="SUCCESS"
        else
            echo "failure"
            echo "DBG: failure: running model $model batch size 1 for latency"
            STATUS="FAILURE"
        fi

        # run batch size 8 for throughput using benchmark-only
        cmd="python eval_int8_model_inference.py \
                    --in-graph ${PRE_TRAINED_MODEL_DIR}/YoloV2/int8/yolov2-voc.pb \
                    --data-location ${DATASET_DIR}/yolo_v2/JPEGImages/ \
                    --metaLoad ${PRE_TRAINED_MODEL_DIR}/YoloV2/int8/yolov2-voc.meta \
                    ${single_socket_arg} \
                    --batch-size 8 \
                    --benchmark-only >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 1" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

        if [ $? -eq 0 ] ; then
            echo "DBG: success: running model $model batch size 8 for throughput"
            STATUS="SUCCESS"
        else
            echo "failure"
            echo "DBG: failure: running model $model batch size 8 for throughput"
            STATUS="FAILURE"
        fi

        # run batch size 1 for accuracy using accuracy-only
        cmd="python eval_int8_model_inference.py \
                    --in-graph ${PRE_TRAINED_MODEL_DIR}/YoloV2/int8/yolov2-voc.pb \
                    --data-location ${DATASET_DIR}/yolo_v2/JPEGImages/ \
                    --metaLoad ${PRE_TRAINED_MODEL_DIR}/YoloV2/int8/yolov2-voc.meta \
                    ${single_socket_arg} \
                    --batch-size 1 \
                    --accuracy-only >> ${LOGFILE_ACCURACY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_ACCURACY}
        echo "Batch Size: 1" >> ${LOGFILE_ACCURACY}
        eval $cmd >> ${LOGFILE_ACCURACY}

        if [ $? -eq 0 ] ; then
            echo "DBG: success: running model $model batch size 1 for accuracy"
            STATUS="SUCCESS"
        else
            echo "DBG: failure: running model $model batch size 1 for accuracy"
            STATUS="FAILURE"
        fi
    fi

    if [ $model == "ResNet50" ] ; then

        echo "model is $model"

        cd ${RESNET50_DIR}

        # run batch size 100 for accuracy
        cmd="python eval_int8_model_inference.py -m resnet50 -a \
        -d ${DATASET_DIR}/TF_Imagenet_FullData \
        -g ${PRE_TRAINED_MODEL_DIR}/resnet50/int8/final_int8_resnet50.pb --batch-size=100 >> ${LOGFILE_ACCURACY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_ACCURACY}
        echo "Batch Size: 100" >> ${LOGFILE_ACCURACY}
        eval $cmd >> ${LOGFILE_ACCURACY}

        # run batch size 224 for throughput
        cmd="python eval_int8_model_inference.py -m resnet50 -s -k \
        -g ${PRE_TRAINED_MODEL_DIR}/resnet50/int8/final_int8_resnet50.pb  \
        --batch-size=128 --num-inter-threads=2 --num-intra-threads=28 >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 128" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

        # run batch size 1 for latency
        cmd="python eval_int8_model_inference.py -m resnet50 -s -k \
        -g ${PRE_TRAINED_MODEL_DIR}/resnet50/int8/final_int8_resnet50.pb  \
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

    if [ $model == "InceptionV3" ] ; then

        echo "model is $model"

        cd ${INCEPTIONV3_DIR}

        # run batch size 224 for accuracy
        cmd="python eval_int8_model_inference.py -m inceptionv3 -a \
        -d ${DATASET_DIR}/TF_Imagenet_FullData \
        -g ${PRE_TRAINED_MODEL_DIR}/inceptionv3/int8/final_int8_inceptionv3.pb --batch-size=128 \
        --input-height 299 --input-width 299 >> ${LOGFILE_ACCURACY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_ACCURACY}
        echo "Batch Size: 128" >> ${LOGFILE_ACCURACY}
        eval $cmd >> ${LOGFILE_ACCURACY}

        # run batch size 128 for throughput
        cmd="python eval_int8_model_inference.py -m inceptionv3 -s -k \
         -g ${PRE_TRAINED_MODEL_DIR}/inceptionv3/int8/final_int8_inceptionv3.pb \
         --batch-size=128 --num-inter-threads=2 --num-intra-threads=28 \
         --input-height 299 --input-width 299  >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 128" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

        # run batch size 1 for latency
        cmd="python eval_int8_model_inference.py -m inceptionv3 -s -k \
         -g ${PRE_TRAINED_MODEL_DIR}/inceptionv3/int8/final_int8_inceptionv3.pb \
         --batch-size=1 --num-inter-threads=2 --num-intra-threads=28 \
         --input-height 299 --input-width 299  >> ${LOGFILE_LATENCY} 2>&1"

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

    if [ $model == "InceptionResNetV2" ] ; then

        echo "model is $model"

        cd ${INCEPTION_RESNET_V2_DIR}

        # run batch size 100 for accuracy
        cmd="python eval_int8_model_inference.py -m inception_resnet_v2 -a -b 100 -v \
        -d ${DATASET_DIR}/TF_Imagenet_FullData \
        -g ${PRE_TRAINED_MODEL_DIR}/inception_resnet_v2/int8/final_int8_inception_resnet_v2_graph.pb \
         >> ${LOGFILE_ACCURACY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_ACCURACY}
        echo "Batch Size: 100" >> ${LOGFILE_ACCURACY}
        eval $cmd >> ${LOGFILE_ACCURACY}

        # run batch size 128 for throughput
        cmd="python eval_int8_model_inference.py -m inception_resnet_v2 -s -k \
        -g ${PRE_TRAINED_MODEL_DIR}/inception_resnet_v2/int8/final_int8_inception_resnet_v2_graph.pb --batch-size=128 \
         >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 128" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

        # run batch size 1 for latency
        cmd="python eval_int8_model_inference.py -m inception_resnet_v2 -s -k \
        -g ${PRE_TRAINED_MODEL_DIR}/inception_resnet_v2/int8/final_int8_inception_resnet_v2_graph.pb --batch-size=1 \
         >> ${LOGFILE_LATENCY} 2>&1"

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

    if [ $model == "mobilenetv1" ] ; then

        echo "model is $model"

        cd ${MOBILENET_V1_DIR}

        # run batch size 240 for accuracy
        cmd="python eval_int8_model_inference.py -m mobilenetv1 -a -b 240 -v \
        -d ${DATASET_DIR}/TF_Imagenet_FullData \
        -g ${PRE_TRAINED_MODEL_DIR}/mobilenet_v1/int8/final_int8_mkl_depthwise_perchannel_Relu6.pb \
         >> ${LOGFILE_ACCURACY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_ACCURACY}
        echo "Batch Size: 240" >> ${LOGFILE_ACCURACY}
        eval $cmd >> ${LOGFILE_ACCURACY}

        # run batch size 240 for throughput
        cmd="python eval_int8_model_inference.py -m mobilenetv1 -s -k \
        -g ${PRE_TRAINED_MODEL_DIR}/mobilenet_v1/int8/final_int8_mkl_depthwise_perchannel_Relu6.pb --batch-size=240 \
         >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 240" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

        # run batch size 1 for latency
        cmd="python eval_int8_model_inference.py -m mobilenetv1 -s -k \
        -g ${PRE_TRAINED_MODEL_DIR}/mobilenet_v1/int8/final_int8_mkl_depthwise_perchannel_Relu6.pb --batch-size=1 \
         >> ${LOGFILE_LATENCY} 2>&1"

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


    if [ $model == "RFCN" ] ; then

        echo "model is $model"

        cd ${RFCN_DIR}/RFCN

        ./install_dependencies.sh

        cd models/research/object_detection/
        chmod 777 metrics
        cd metrics
        chmod 777 offline_eval_map_corloc.py

        sed -i.bak 162s/eval_input_config/eval_input_configs/ offline_eval_map_corloc.py
        sed -i.bak 91s/input_config/input_config[0]/ offline_eval_map_corloc.py
        sed -i.bak 92s/input_config/input_config[0]/ offline_eval_map_corloc.py
        sed -i.bak 95s/input_config/input_config[0]/ offline_eval_map_corloc.py

        cd ${RFCN_DIR}

        # run batch size 1 for throughtput and latency
        cmd="python eval_int8_model_inference.py -vkg /${PRE_TRAINED_MODEL_DIR}/rfcn/int8/per-channel-int8-rfcn-graph.pb \
        -m RFCN -l RFCN/models/ -d ${DATASET_DIR}/coco_dataset/raw-data/val2017 -x 500
         >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 1" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

        # model batch size is 1 which means throughtput is latency
        #cat ${LOGFILE_THROUGHPUT} >> ${LOGFILE_LATENCY}

        # run batch size 240 for accuracy
        #cmd="python eval_int8_model_inference.py -vag /${PRE_TRAINED_MODEL_DIR}/rfcn/int8/per-channel-int8-rfcn-graph.pb \
        #-m RFCN -l RFCN/models -d ${DATASET_DIR}/coco_dataset/coco_val.record-00000-of-00001 -q accuracy_message"
        cmd="python eval_int8_model_inference.py -vag /${PRE_TRAINED_MODEL_DIR}/rfcn/int8/per-channel-int8-rfcn-graph.pb \
        -m RFCN -l RFCN/models -d ${DATASET_DIR}/fastrcnn/dataset/../coco_val.record -q accuracy_message"

        echo "RUNCMD: $cmd " >& ${LOGFILE_ACCURACY}
        echo "Batch Size: 1" >> ${LOGFILE_ACCURACY}
        eval $cmd >> ${LOGFILE_ACCURACY}

        if [ $? -eq 0 ] ; then
            echo "success"
            STATUS="SUCCESS"
        else
            echo "failure"
            STATUS="FAILURE"
        fi
    fi
done
