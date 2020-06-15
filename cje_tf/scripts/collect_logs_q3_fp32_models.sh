#!/bin/bash
#collect_logs.sh --model=[NCF] --mode=[training/inference] --single_socket=[True|False]
set -x

HOST=`uname -n | awk -F'.' '{print $1}'`
SHORTNAME=`echo $HOST | awk -F'-' '{print $2}'`
WORKDIR=`pwd`

if [ $# != "3" ]; then
    echo 'ERROR:'
    echo "Expected 3 parameter got $#"
    printf 'Please use following parameters:
    --model=<model to run>
    --mode=training|inference
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
        --mode=*)
            MODE=`echo $i | sed "s/${PATTERN}//"`;;
        --single_socket=*)
            SINGLE_SOCKET=`echo $i | sed "s/${PATTERN}//"`;;
        *)
            echo "Parameter $i not recognized."; exit 1;;
    esac
done

LOGFILE_THROUGHPUT=${WORKDIR}/benchmark_${MODEL}_${MODE}_${HOST}_throughput.log
LOGFILE_LATENCY=${WORKDIR}/benchmark_${MODEL}_${MODE}_${HOST}_latency.log

SUMMARYLOG=${WORKDIR}/summary_${HOST}.log
SUMMARYTXT=${WORKDIR}/summary_nightly.log
if [ $(hostname |grep -i skx |wc -l) -eq 1 ];then 
    MACHINE_TYPE=SKX
elif [ $(hostname |grep -i clx |wc -l) -eq 1 ];then 
    MACHINE_TYPE=CLX
else 
    MACHINE_TYPE=Others
fi 
DATATYPE='fp32'

STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
STATUS_LATENCY="CHECK_LOG_FOR_ERROR"


if [ ${MODEL} == "NCF" ] ; then

    if [ ${MODE} == "inference" ] ; then

        echo "model is ${MODEL}"

        if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
           [ $(grep 'Average recommendations/sec' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
            STATUS_THROUGHPUT="SUCCESS"
        fi

        if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
           [ $(grep 'Average recommendations/sec' ${LOGFILE_LATENCY} | wc -l) != 0 ] ; then
            STATUS_LATENCY="SUCCESS"
            echo "${STATUS_LATENCY}"
        fi

    else
        echo "${MODEL} training is skipped for Q3"
    fi

elif [ ${MODEL} == "Wide_Deep_Census" ] ; then

    if [ ${MODE} == "training" ] ; then

        echo "model is ${MODEL}"

        if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
           [ $(grep 'Throughput is:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then

            STATUS_THROUGHPUT="SUCCESS"
            STATUS_LATENCY="SUCCESS"
        fi
    else
        echo "${MODEL} inference is skipped for Q3"
    fi

elif [ ${MODEL} == "Wide_Deep_Criteo" ] ; then

    if [ ${MODE} == "training" ] ; then

        echo "model is ${MODEL}"

        if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
           [ $(grep 'Train Throughput' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then

            STATUS_THROUGHPUT="SUCCESS"
            STATUS_LATENCY="SUCCESS"
        fi
    else
        echo "${MODEL} inference is skipped for Q3"
    fi

elif [ ${MODEL} == "Inception_ResNet_v2" ] || [ ${MODEL} == "MobileNet_v1" ]; then
    
    if [ ${MODE} == "training" ] ; then
        if [ $(grep 'Total samples/sec:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
            STATUS_THROUGHPUT="SUCCESS"
        fi
    fi

else
    echo "model is not recognized"
    STATUS_THROUGHPUT="FAILURE"
fi


if [ ${MODEL} == "NCF" ] ; then

    if [ ${MODE} == "inference" ] ; then

        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}

        # throughput batch size 256 for NCF
        BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        FPS=$(grep 'Average recommendations/sec' ${LOGFILE_THROUGHPUT} | sed "s/.*: //g" | sed "s/ (.*//g")

        if [ ${SINGLE_SOCKET} == "true" ]; then
            echo "single_socket $FPS times 2"
            FPS=$(echo | awk -v value=$FPS  '{ result=value * 2; printf("%.2f", result) }')
        fi
        echo "  Average recommendations/sec: ${FPS}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        #latency: batch size 1
        echo ""
        echo "${MODEL}_${MODE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
        BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        fps=$(grep 'Average recommendations/sec' ${LOGFILE_LATENCY} | sed "s/.*: //g" | sed "s/ (.*//g")
        latency=$(echo |awk -v bs=${BATCH_SIZE} -v fps=${fps} '{printf("%.3f", bs*1000/fps)}')
        echo "  Average recommendations/sec: ${latency}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    else
        echo "${MODEL} training is skipped for Q3"
    fi

elif [ ${MODEL} == "Wide_Deep_Census" ] ; then

    if [ ${MODE} == "training" ] ; then

        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
        echo "  Batch Size: 40" >> ${SUMMARYLOG}
        BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)

        acc=$(grep 'accuracy:' ${LOGFILE_THROUGHPUT} | sed "s/.*] //g" |sed 's/[^0-9.]//g' )
        echo " accuracy: ${acc}" >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};$(echo ${acc} |sed 's/[^0-9.]//g');${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        FPS=$(grep 'Throughput is' ${LOGFILE_THROUGHPUT})
        echo "  ${FPS}" >> ${SUMMARYLOG}
        echo "" >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};$(echo ${FPS} |sed 's/[^0-9.]//g');${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    else
        echo "${MODEL} inference is skipped for Q3"
    fi

elif [ ${MODEL} == "Wide_Deep_Criteo" ] ; then

    if [ ${MODE} == "training" ] ; then

        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
        echo "Batch Size: 128" >> ${SUMMARYLOG}
        BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)

        grep 'Accuracy:' ${LOGFILE_THROUGHPUT} >> ${SUMMARYLOG}
        ACCURACY=$(grep 'Accuracy:' ${LOGFILE_THROUGHPUT} |sed 's/[^0-9.]//g')
        grep 'Train Throughput:' ${LOGFILE_THROUGHPUT} >> ${SUMMARYLOG}
        FPS=$(grep 'Train Throughput:' ${LOGFILE_THROUGHPUT} |sed 's/[^0-9.]//g')

        echo "" >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${ACCURACY};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    else
        echo "${MODEL} inference is skipped for Q3"
    fi

elif [ ${MODEL} == "Inception_ResNet_v2" ] || [ ${MODEL} == "MobileNet_v1" ] ; then
        
    if [ ${MODE} == "training" ];then
        
        echo "------------- ${MODEL} ${MODE} Throughput -------------"
        BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
        FPS=$(grep 'Total samples/sec:' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
        
        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        echo "  Total samples/sec: ${FPS}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
    
    else
        echo "${MODEL} inference is skipped for Q3"    
    fi

else
    echo "model is not recognized"
fi

if [ ${STATUS_THROUGHPUT} == "SUCCESS" ] ||  [ ${STATUS_LATENCY} == "SUCCESS" ]; then
    exit 0
else
    exit 1
fi
