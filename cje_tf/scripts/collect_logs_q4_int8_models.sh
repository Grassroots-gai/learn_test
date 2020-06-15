#!/bin/bash
#collect_logs.sh --model=[FastRCNN/YoloV2] --single_socket=[True|False]
set -x

HOST=`uname -n | awk -F'.' '{print $1}'`
SHORTNAME=`echo $HOST | awk -F'-' '{print $2}'`
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

LOGFILE_THROUGHPUT=${WORKDIR}/benchmark_${MODEL}_inference_${HOST}_throughput.log
LOGFILE_LATENCY=${WORKDIR}/benchmark_${MODEL}_inference_${HOST}_latency.log
LOGFILE_ACCURACY=${WORKDIR}/benchmark_${MODEL}_inference_${HOST}_accuracy.log

SUMMARYLOG=${WORKDIR}/summary_${HOST}.log
SUMMARYTXT=${WORKDIR}/summary_nightly.log

if [ $(hostname |grep -i skx |wc -l) -eq 1 ];then 
    MACHINE_TYPE=SKX
elif [ $(hostname |grep -i clx |wc -l) -eq 1 ];then 
    MACHINE_TYPE=CLX
else 
    MACHINE_TYPE=Others
fi 
DATATYPE='int8'
MODE='inference'

STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
STATUS_ACCURACY="CHECK_LOG_FOR_ERROR"



if [ ${MODEL} == "ResNet101" ]; then

    echo "model is ${MODEL}"

    if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
       [ $(grep 'steps = 200,' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
        STATUS_THROUGHPUT="SUCCESS"
    fi
    if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
       [ $(grep 'steps = 200,' ${LOGFILE_LATENCY} | wc -l) != 0 ] ; then
        STATUS_LATENCY="SUCCESS"
    fi
    if [ $(grep 'Error: ' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
       [ $(grep 'Processed 50000 images.' ${LOGFILE_ACCURACY} | wc -l) != 0 ] ; then
        STATUS_ACCURACY="SUCCESS"
    fi

elif [ ${MODEL} == "InceptionV4" ]; then

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

else
    
    echo "model is not recognized"
    STATUS_THROUGHPUT="FAILURE"

fi



if [ ${MODEL} == "ResNet101" ] ; then

    echo "${MODEL}_inference Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g")
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    grep 'steps = 200,' ${LOGFILE_THROUGHPUT} >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    FPS=$(grep 'steps = 50,' ${LOGFILE_THROUGHPUT} |cut -f4 -d' ')
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    echo "${MODEL}_inference Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    grep 'steps = 200,' ${LOGFILE_LATENCY} >> ${SUMMARYLOG}
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

elif [ ${MODEL} == "InceptionV4" ] ; then

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

else
    echo "model is not recognized"
    STATUS_THROUGHPUT="FAILURE"
fi


if [ ${STATUS_THROUGHPUT} == "SUCCESS" ] &&  [ ${STATUS_LATENCY} == "SUCCESS" ]; then
    exit 0
else
    exit 1
fi
