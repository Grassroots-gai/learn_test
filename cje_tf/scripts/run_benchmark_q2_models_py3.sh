#!/bin/bash -x

if [ -z ${DATASET_LOCATION} ] ; then
    DATASET_LOCATION="/tf_dataset"
fi

if [ -z ${WORKSPACE} ] ; then
    WORKSPACE="/workspace"
fi

DATASET_DIR="$DATASET_LOCATION/dataset"

# host=`uname -n | awk -F'.' '{print $1}'`

if [ $# != "3" ]; then
    echo 'ERROR:'
    echo "Expected 1 parameter got $#"
    printf 'Please use following parameters:
    --model=<model to run>
    --mode=training|inference
    --single_socket=true|false
    ' 
    echo 'All parameters are obligatory.'
    exit 1
fi

pattern='[-a-zA-Z0-9_]*='
for i in "$@"
do      
    case $i in
        --model=*)
            echo $i
            model=`echo $i | sed "s/${pattern}//"`;;
        --mode=*)
            mode=`echo $i | sed "s/${pattern}//"`;;
        --single_socket=*)
            single_socket=`echo $i | sed "s/${pattern}//"`;;
        *)
            echo "Parameter $i not recognized."; exit 1;;
    esac
done

# log files
logfile_throughput=${WORKSPACE}/Q2_models_${model}_throughput.log
logfile_latency=${WORKSPACE}/Q2_models_${model}_latency.log
echo ${logfile_throughput}
echo ${logfile_latency}
status="FAILURE"

cd ${WORKSPACE}/tensorflow-${model}
python --version

cmd=' python run_tf_benchmark.py '

# sockets
if [ "${single_socket}" == "true" ];then
    cmd+=' -s '
fi


if [ "${model}" == "DenseNet" ];then
    if [ "${mode}" == "inference" ];then
        cmd+=" -f "

        mkdir data_dir
        chmod 777 data_dir
        WORKDIR=`pwd`
        
        # throughput
        fps_cmd="${cmd} -b=128 -c=${WORKDIR}/ -d=${WORKDIR}/data_dir >> ${logfile_throughput} 2>&1"
        echo "RUNCMD: $fps_cmd " >& ${logfile_throughput}
        echo "Batch Size: 128" >> ${logfile_throughput}
        eval $fps_cmd >> ${logfile_throughput}
        
        # latency
        latency_cmd="${cmd} -b=1 -c=${WORKDIR}/ -d=${WORKDIR}/data_dir >> ${logfile_latency} 2>&1"
        echo "RUNCMD: $latency_cmd " >& ${logfile_latency}
        echo "Batch Size: 1" >> ${logfile_latency}
        eval $latency_cmd >> ${logfile_latency}
        
        if [ $? -eq 0 ] ; then
            echo "success"
            status="SUCCESS"
        else
            echo "failure"
            status="FAILURE"
        fi
    else
        echo "${model} ${mode} is skipped for now"
        exit 0
    fi
    
elif [ "${model}" == "3DUNet" ];then
    if [ "${mode}" == "inference" ];then
        cmd+=" -f "
        
        ln -sf ${DATASET_DIR}/3DUNet/brats/* ./brats

        # latency
        latency_cmd=" ${cmd} -b 1 >> ${logfile_latency} 2>&1"
        echo "RUNCMD: ${latency_cmd} " >& ${logfile_latency}
        echo "Batch Size: 1" >> ${logfile_latency}
        eval $latency_cmd >> ${logfile_latency}
        
        # throughput
        fps_cmd=" ${cmd} -b 1 >> ${logfile_throughput} 2>&1"
        echo "RUNCMD: ${fps_cmd} " >& ${logfile_throughput}
        echo "Batch Size: 1" >> ${logfile_throughput}
        eval $fps_cmd >> ${logfile_throughput}
        
        if [ $? -eq 0 ] ; then
            echo "success"
            status="SUCCESS"
        else
            echo "failure"
            status="FAILURE"
        fi
    else
        echo "${model} ${mode} is skipped for now"
        exit 0
    fi
        
elif [ "${model}" == "MaskRCNN" ];then
    if [ "${mode}" == "inference" ];then
        cmd+=" -f "

        # wget -q https://github.com/matterport/Mask_RCNN/releases/download/v2.0/mask_rcnn_coco.h5
        ln -sf ${DATASET_DIR}/MaskRCNN/mask_rcnn_coco.h5 .
        
        ls /usr/local/lib/python3*/dist-packages/pycocotools/coco.py
        sed -i "s/unicode/bytes/" /usr/local/lib/python3*/dist-packages/pycocotools/coco.py || true
        
        cmd+=" -m coco -d ${DATASET_DIR}/MaskRCNN/COCO2014 "
        
        # latency
        latency_cmd=" ${cmd} -b 1 >> ${logfile_latency} 2>&1"
        echo "RUNCMD: ${latency_cmd} " >& ${logfile_latency}
        echo "Batch Size: 1" >> ${logfile_latency}
        eval $latency_cmd >> ${logfile_latency}
        
        # throughput
        fps_cmd=" ${cmd} -b 1 >> ${logfile_throughput} 2>&1"
        echo "RUNCMD: ${fps_cmd} " >& ${logfile_throughput}
        echo "Batch Size: 1" >> ${logfile_throughput}
        eval $fps_cmd >> ${logfile_throughput}
        
        if [ $? -eq 0 ] ; then
            echo "success"
            status="SUCCESS"
        else
            echo "failure"
            status="FAILURE"
        fi
    else
        echo "${model} ${mode} is skipped for now"
        exit 0
    fi
    
else 
    echo "no such model: ${model}"
    exit 1
fi


if [ "$status" == "SUCCESS" ]; then 
    exit 0
else
    exit 1
fi
