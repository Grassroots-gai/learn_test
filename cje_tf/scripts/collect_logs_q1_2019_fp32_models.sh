#!/bin/bash
#collect_logs.sh --model=[NCF] --mode=[training/inference] --single_socket=[True|False]
set -x

HOST=`uname -n | awk -F'.' '{print $1}'`
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
LOGFILE_ACCURACY=${WORKDIR}/benchmark_${MODEL}_${MODE}_${HOST}_accuracy.log

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
STATUS_ACCURACY="CHECK_LOG_FOR_ERROR"


if [ ${MODEL} == "inceptionv3" ] ; then
    if [ ${MODE} == "inference" ] ; then

        echo "model is ${MODEL}"

        # accuracy getting from the throughput run log with batch 100
	    if [ $(grep 'Error: ' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
	       [ $(grep 'Processed 50000 images' ${LOGFILE_ACCURACY} | wc -l) != 0 ] ; then
                STATUS_ACCURACY="SUCCESS"
	    fi
    else
        echo "${MODEL} ${mode} is skipped for Q1 2019"
    fi

elif [ ${MODEL} == "resnet50" ] ; then
    if [ ${MODE} == "inference" ] ; then

        echo "model is ${MODEL}"

        # accuracy getting from the throughput run log with batch 100
	    if [ $(grep 'Error: ' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
	       [ $(grep 'Processed 50000 images' ${LOGFILE_ACCURACY} | wc -l) != 0 ] ; then
                STATUS_ACCURACY="SUCCESS"
	    fi
    else
        echo "${MODEL} ${mode} is skipped for Q1 2019"
    fi

else
    echo "model is not recognized"
    LOGFILE_ACCURACY="FAILURE"
fi


if [ ${MODEL} == "inceptionv3" ] ; then

    if [ ${MODE} == "inference" ] ; then
	
        # accuracy log with batch 100
	    echo "${MODEL}_inference Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
	    echo "Batch Size: 100" >> ${SUMMARYLOG}
	    ACCURACY=$(grep 'Processed 50000 images' ${LOGFILE_ACCURACY} | awk -F= '{print $2}')
	    echo "  Accuracy: ${ACCURACY}" >> ${SUMMARYLOG}
	    echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${ACCURACY};${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    else
        echo "${MODEL} ${mode} is skipped for Q1 2019"
    fi

elif [ ${MODEL} == "resnet50" ] ; then

    if [ ${MODE} == "inference" ] ; then
	
        # accuracy log with batch 100
	    echo "${MODEL}_inference Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
	    echo "Batch Size: 100" >> ${SUMMARYLOG}
	    ACCURACY=$(grep 'Processed 50000 images' ${LOGFILE_ACCURACY} | awk -F= '{print $2}')
	    echo "  Accuracy: ${ACCURACY}" >> ${SUMMARYLOG}
	    echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${ACCURACY};${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    else
        echo "${MODEL} ${mode} is skipped for Q1 2019"
    fi

else
    echo "model is not recognized"
fi

if [ ${STATUS_THROUGHPUT} == "SUCCESS" ] ||  [ ${STATUS_LATENCY} == "SUCCESS" ] || [ ${STATUS_ACCURACY} == "SUCCESS"; then
    exit 0
else
    exit 1
fi

