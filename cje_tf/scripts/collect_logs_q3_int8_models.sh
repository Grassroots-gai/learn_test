#!/bin/bash
#collect_logs.sh --model=[FastRCNN/YoloV2] --single_socket=[True|False]
set -x

HOST=`uname -n | awk -F'.' '{print $1}'`
WORKDIR=`pwd`

if [ $# != "2" ]; then
    echo 'ERROR:'
    echo "Expected 3 parameter got $#"
    printf 'Please use following parameters:
    --model=<model to run>
    --single_socket=true|false
    '
    echo 'All parameters are obligatory.'
    exit 1
fi

PATTERN='[-a-zA-Z0-9_]*='
for i in "$@"
do
    case $i in
        --model=*)
            MODEL=`echo $i | sed "s/${PATTERN}//"`;;
        --single_socket=*)
            SINGLE_SOCKET=`echo $i | sed "s/${PATTERN}//"`;;
        *)
            echo "Parameter $i not recognized."; exit 1;;
    esac
done

LOGFILE_THROUGHPUT=${WORKDIR}/benchmark_${MODEL}_inference_${servername}_throughput.log
LOGFILE_LATENCY=${WORKDIR}/benchmark_${MODEL}_inference_${servername}_latency.log
LOGFILE_ACCURACY=${WORKDIR}/benchmark_${MODEL}_inference_${servername}_accuracy.log

SUMMARYLOG=${WORKDIR}/summary_${servername}.log
SUMMARYTXT=${WORKDIR}/summary_nightly.log
if [ $(echo $servername |grep -i skx |wc -l) -eq 1 ];then 
    MACHINE_TYPE=SKX
elif [ $(echo $servername |grep -i clx |wc -l) -eq 1 ];then 
    MACHINE_TYPE=CLX
else 
    MACHINE_TYPE=Others
fi 
DATATYPE='int8'
MODE='inference'

STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
STATUS_ACCURACY="CHECK_LOG_FOR_ERROR"


if [ ${MODEL} == "FastRCNN" ] ; then

    echo "model is ${MODEL}"

    if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
       [ $(grep 'Avg. Duration per Step' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
            STATUS_THROUGHPUT="SUCCESS"
            STATUS_LATENCY="SUCCESS"
    fi
    if [ $(grep 'Error' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
       [ $(grep 'Average Precision' ${LOGFILE_ACCURACY} | wc -l) != 0 ] ; then
            STATUS_ACCURACY="SUCCESS"
    fi

elif [ ${MODEL} == "SSDMobilenet" ] ; then
    
    echo "model is ${MODEL}"

    if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
       [ $(grep 'Avg. Duration per Step' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
            STATUS_THROUGHPUT="SUCCESS"
            STATUS_LATENCY="SUCCESS"
    fi
    if [ $(grep 'Error' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
       [ $(grep 'Average Precision' ${LOGFILE_ACCURACY} | wc -l) != 0 ] ; then
            STATUS_ACCURACY="SUCCESS"
    fi

elif [ ${MODEL} == "SSD-VGG16" ] ; then
    
    echo "model is ${MODEL}"

    if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
       [ $(grep 'Run benchmark batch \[100/100\]:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
            STATUS_THROUGHPUT="SUCCESS"
    fi
    if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
       [ $(grep 'Run benchmark batch \[1000/1000\]:' ${LOGFILE_LATENCY} | wc -l) != 0 ] ; then
            STATUS_LATENCY="SUCCESS"
    fi
    if [ $(grep 'Error' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
       [ $(grep 'mAP_VOC12' ${LOGFILE_ACCURACY} | wc -l) != 0 ] ; then
            STATUS_ACCURACY="SUCCESS"
    fi

elif [ ${MODEL} == "YoloV2" ]; then

    echo "model is ${MODEL}"

    if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
       [ $(grep 'throughput\[med\]' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
        STATUS_THROUGHPUT="SUCCESS"
    fi
    if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
       [ $(grep 'latency\[median\]' ${LOGFILE_LATENCY} | wc -l) != 0 ] ; then
        STATUS_LATENCY="SUCCESS"
    fi
    if [ $(grep 'Error: ' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
       [ $(grep 'mAP =' ${LOGFILE_ACCURACY} | wc -l) != 0 ] ; then
        STATUS_ACCURACY="SUCCESS"
    fi

elif [ ${MODEL} == "ResNet50" ]; then

    echo "model is ${MODEL}"

    if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
       [ $(grep 'steps = 50,' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
        STATUS_THROUGHPUT="SUCCESS"
    fi
    if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
       [ $(grep 'steps = 50,' ${LOGFILE_LATENCY} | wc -l) != 0 ] ; then
        STATUS_LATENCY="SUCCESS"
    fi
    if [ $(grep 'Error: ' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
       [ $(grep 'Processed 50000 images.' ${LOGFILE_ACCURACY} | wc -l) != 0 ] ; then
        STATUS_ACCURACY="SUCCESS"
    fi

elif [ ${MODEL} == "InceptionV3" ] || [ ${MODEL} == "mobilenetv1" ] ; then

    echo "model is ${MODEL}"

    if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
       [ $(grep 'steps = 50,' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
        STATUS_THROUGHPUT="SUCCESS"
    fi
    if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
       [ $(grep 'steps = 50,' ${LOGFILE_LATENCY} | wc -l) != 0 ] ; then
        STATUS_LATENCY="SUCCESS"
    fi
    if [ $(grep 'Error: ' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
       [ $(grep 'Processed 49920 images.' ${LOGFILE_ACCURACY} | wc -l) != 0 ] ; then
        STATUS_ACCURACY="SUCCESS"
    fi

elif [ ${MODEL} == "InceptionResNetV2" ]; then

    echo "model is ${MODEL}"

    if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
       [ $(grep 'Average time:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]  &&
       [ $(grep 'Throughput:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
        STATUS_THROUGHPUT="SUCCESS"
    fi
    if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
       [ $(grep 'Average time:' ${LOGFILE_LATENCY} | wc -l) != 0 ]  &&
       [ $(grep 'Throughput:' ${LOGFILE_LATENCY} | wc -l) != 0 ] &&
       [ $(grep 'Latency:' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
        STATUS_LATENCY="SUCCESS"
    fi
    if [ $(grep 'Error: ' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
       [ $(grep 'Processed 50000 images.' ${LOGFILE_ACCURACY} | wc -l) != 0 ] ; then
        STATUS_ACCURACY="SUCCESS"
    fi
elif [ ${MODEL} == "RFCN" ] ; then

echo "model is ${MODEL}"

if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
    [ $(grep 'Avg. Duration per Step' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
        STATUS_THROUGHPUT="SUCCESS"
        STATUS_LATENCY="SUCCESS"
fi
if [ $(grep 'Error' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
    [ $(grep ' Average Precision  (AP) @\[ IoU=0.50:0.95 | area=   all | maxDets=100 \]' ${LOGFILE_ACCURACY} | wc -l) != 0 ] ; then
        STATUS_ACCURACY="SUCCESS"
fi
else
    
    echo "model is not recognized"
    STATUS_THROUGHPUT="FAILURE"

fi


if [ ${MODEL} == "FastRCNN" ] ; then

    echo "${MODEL}_inference Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
    
    # throughput batch size 1 for FastRCNN
    echo "Batch Size: 1" >> ${SUMMARYLOG}
    grep 'Avg. Duration per Step' ${LOGFILE_THROUGHPUT} >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g")

    latency=$(grep 'Avg. Duration per Step:' ${LOGFILE_THROUGHPUT} | tail -n 1 | sed "s/.*://g")
    FPS=$(echo | awk -v value=$latency  '{ result=1/value; printf("%.2f", result) }')
    latency=$(echo |awk -v latency=$latency '{ print latency*1000 }')
    echo "Throughput = ${FPS}" >> ${SUMMARYLOG}
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    #latency: batch size 1
    echo "" >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g")
    echo "${MODEL}_inference Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
    echo "Batch Size: 1" >> ${SUMMARYLOG}
    grep 'Average Precision' ${LOGFILE_ACCURACY} | head -n 2 >> ${SUMMARYLOG}
    echo "" >> ${SUMMARYLOG}
    m_ap=$(grep 'Average Precision' ${LOGFILE_ACCURACY} | head -n 2 |awk '{printf("%s,", $NF)}')
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${m_ap};${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

elif [ ${MODEL} == "SSDMobilenet" ] ; then

    echo "${MODEL}_inference Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}

    # throughput batch size 1 for FastRCNN
    BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g")
    echo "Batch Size: 1" >> ${SUMMARYLOG}
    grep 'Avg. Duration per Step' ${LOGFILE_THROUGHPUT} >> ${SUMMARYLOG}
    latency=$(grep 'Avg. Duration per Step:' ${LOGFILE_THROUGHPUT} | tail -n 1 | sed "s/.*://g")
    FPS=$(echo | awk -v value=$latency  '{ result=1/value; printf("%.2f", result) }')
    latency=$(echo |awk -v latency=$latency '{ print latency*1000 }')
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    #latency: batch size 1
    BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g")
    echo "" >> ${SUMMARYLOG}
    echo "${MODEL}_inference Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
    echo "Batch Size: 1" >> ${SUMMARYLOG}
    grep 'Average Precision' ${LOGFILE_ACCURACY} | head -n 2 >> ${SUMMARYLOG}
    echo "" >> ${SUMMARYLOG}
    m_ap=$(grep 'Average Precision' ${LOGFILE_ACCURACY} | head -n 2 |awk '{printf("%s,", $NF)}')
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${m_ap};${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
elif [ ${MODEL} == "SSD-VGG16" ] ; then

    echo "${MODEL}_inference Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}

    # throughput batch size 224 for SSD-VGG16 
    BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g")
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    grep 'Run benchmark batch \[100/100\]:' ${LOGFILE_THROUGHPUT} >> ${SUMMARYLOG}
    FPS=$(grep 'Run benchmark batch \[100/100\]:' ${LOGFILE_THROUGHPUT} |sed 's/.*://')
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
    echo " " >> ${SUMMARYLOG}

    echo "${MODEL}_inference Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
    # latency batch size 1 for SSD-VGG16
    BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    grep 'Run benchmark batch \[1000/1000\]:' ${LOGFILE_LATENCY} >> ${SUMMARYLOG}
    latency=$(grep 'Run benchmark batch \[100/100\]:' ${LOGFILE_LATENCY} |sed 's/.*://')
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
    echo " " >> ${SUMMARYLOG}

    #latency: batch size 224
    echo " " >> ${SUMMARYLOG}
    echo "${MODEL}_inference Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
    echo "Batch Size: 224" >> ${SUMMARYLOG}
    grep 'mAP_VOC12' ${LOGFILE_ACCURACY} | head -n 2 >> ${SUMMARYLOG}
    m_ap=$(grep 'Average Precision' ${LOGFILE_ACCURACY} | head -n 2 |awk '{printf("%s,", $NF)}')
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${m_ap};${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
    echo " " >> ${SUMMARYLOG}

elif [ ${MODEL} == "ResNet50" ] ; then

    echo "${MODEL}_inference Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g")
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    grep 'steps = 50,' ${LOGFILE_THROUGHPUT} >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    FPS=$(grep 'steps = 50,' ${LOGFILE_THROUGHPUT} |cut -f4 -d' ')
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    echo "${MODEL}_inference Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    grep 'steps = 50,' ${LOGFILE_LATENCY} >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    FPS=$(grep 'steps = 50,' ${LOGFILE_LATENCY} |cut -f4 -d' ')
    latency=$(echo |awk -v fps=${FPS} -v bs=${BATCH_SIZE} '{printf("%.3f",bs/fps*1000)}')
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    echo "${MODEL}_inference Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
    echo "Batch Size: 100" >> ${SUMMARYLOG}
    grep 'Processed 50000 images.' ${LOGFILE_ACCURACY} | head -n 2 >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    accuracy=$(grep 'Processed 50000 images.' ${LOGFILE_ACCURACY} | head -n 2 |sed -e 's/.*(//;s/).*//')
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${accuracy};${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

elif [ ${MODEL} == "YoloV2" ] ; then

    echo "${MODEL}_inference Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}

    # throughput batch size 8
    BATCH_SIZE=$(grep "batch size =" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g")
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    FPS=$(grep 'throughput\[med\] =' ${LOGFILE_THROUGHPUT} |sed 's/[^0-9.]//g')
    echo "  throughput[med] = ${FPS} image/sec" >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    # latency: batch size 1
    echo "${MODEL}_inference Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    latency=$(grep 'latency\[median\] =' ${LOGFILE_LATENCY} |sed 's/[^0-9.]//g')
    echo "  latency[median] = ${latency} ms" >> ${SUMMARYLOG}
    throughput=$(echo | awk -v value=$latency  '{ result=1000 / value; printf("%.2f", result) }')
    echo "  throughput images/sec: ${throughput}" >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    #accuracy: batch size 1
    echo "${MODEL}_inference Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep "batch size =" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g")
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    accuracy=$(grep 'mAP =' ${LOGFILE_ACCURACY} | awk -F'=' '{print $2}')
    echo "  accuracy = ${accuracy}" >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${accuracy};${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

elif [ ${MODEL} == "InceptionV3" ] ; then

    echo "${MODEL}_inference Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g")
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    grep 'steps = 50,' ${LOGFILE_THROUGHPUT} >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    FPS=$(grep 'steps = 50,' ${LOGFILE_THROUGHPUT} |cut -f4 -d' ')
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    echo "${MODEL}_inference Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    grep 'steps = 50,' ${LOGFILE_LATENCY} >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    FPS=$(grep 'steps = 50,' ${LOGFILE_LATENCY} |cut -f4 -d' ')
    latency=$(echo |awk -v fps=${FPS} -v bs=${BATCH_SIZE} '{printf("%.3f",bs/fps*1000)}')
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    echo "${MODEL}_inference Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
    echo "Batch Size: 128" >> ${SUMMARYLOG}
    grep 'Processed 49920 images.' ${LOGFILE_ACCURACY} | head -n 2 >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    accuracy=$(grep 'Processed 49920 images.' ${LOGFILE_ACCURACY} | head -n 2 |sed -e 's/.*(//;s/).*//')
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${accuracy};${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

elif [ ${MODEL} == "mobilenetv1" ] ; then

    echo "${MODEL}_inference Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g")
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    grep 'steps = 50,' ${LOGFILE_THROUGHPUT} >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    FPS=$(grep 'steps = 50,' ${LOGFILE_THROUGHPUT} |cut -f4 -d' ')
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    echo "${MODEL}_inference Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    grep 'steps = 50,' ${LOGFILE_LATENCY} >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    FPS=$(grep 'steps = 50,' ${LOGFILE_LATENCY} |cut -f4 -d' ')
    latency=$(echo |awk -v fps=${FPS} -v bs=${BATCH_SIZE} '{printf("%.3f",bs/fps*1000)}')
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    echo "${MODEL}_inference Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
    echo "Batch Size: 240" >> ${SUMMARYLOG}
    grep 'Processed 49920 images.' ${LOGFILE_ACCURACY} | head -n 2 >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    accuracy=$(grep 'Processed 49920 images.' ${LOGFILE_ACCURACY} | head -n 2 |sed -e 's/.*(//;s/).*//')
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${accuracy};${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

elif [ ${MODEL} == "InceptionResNetV2" ] ; then

    echo "${MODEL}_inference Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g")
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    grep 'Average time:' ${LOGFILE_THROUGHPUT} >> ${SUMMARYLOG}
    grep 'Throughput:' ${LOGFILE_THROUGHPUT} >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    FPS=$(grep 'Throughput:' ${LOGFILE_THROUGHPUT} |sed "s/[^0-9.]//g")
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    echo "${MODEL}_inference Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    grep 'Average time:' ${LOGFILE_LATENCY} >> ${SUMMARYLOG}
    grep 'Throughput:' ${LOGFILE_LATENCY} >> ${SUMMARYLOG}
    grep 'Latency:' ${LOGFILE_LATENCY} >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    latency=$(grep 'Latency:' ${LOGFILE_LATENCY} | sed "s/[^0-9.]//g")
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    echo "${MODEL}_inference Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
    echo "Batch Size: 100" >> ${SUMMARYLOG}
    grep 'Processed 50000 images.' ${LOGFILE_ACCURACY} | head -n 2 >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    accuracy=$(grep 'Processed 50000 images.' ${LOGFILE_ACCURACY} | head -n 2 |sed -e 's/.*(//;s/).*//')
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${accuracy};${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

elif [ ${MODEL} == "RFCN" ] ; then

    echo "${MODEL}_inference Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
    echo "${MODEL}_inference Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    #grep 'Avg. Duration per Step' ${LOGFILE_LATENCY} >> ${SUMMARYLOG}
    tmp=$(grep 'Avg. Duration per Step' ${LOGFILE_THROUGHPUT} | awk -F':' '{print $2}')
    latency=$(echo | awk -v value=$tmp '{ result=value * 1000; printf("%.2f", result) }')
    echo "  latency = ${latency}" >> ${SUMMARYLOG}
    throughput=$(echo | awk -v value=$tmp  '{ result=1 / value; printf("%.2f", result) }')
    echo "  throughput images/sec: ${throughput}" >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${throughput};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    echo "${MODEL}_inference Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
    tmp=$(grep ' Average Precision  (AP) @\[ IoU=0.50:0.95 | area=   all | maxDets=100 \]' ${LOGFILE_ACCURACY} | awk -F" = " '{print $NF}')
    accuracy=$(echo | awk -v value=$tmp '{ result=value * 100; printf("%.2f", result) }')
    echo "  accuracy: ${accuracy}" >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${accuracy};${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
else
    echo "model is not recognized"
    STATUS_THROUGHPUT="FAILURE"
fi


if [ ${STATUS_THROUGHPUT} == "SUCCESS" ] &&  [ ${STATUS_LATENCY} == "SUCCESS" ]; then
    exit 0
else
    exit 1
fi
