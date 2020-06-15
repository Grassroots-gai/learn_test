#!/bin/bash -x

echo "summaryLog is $summaryLog"
if [ "${summaryLog}" == "" ]; then
    HOST=`uname -n | awk -F'.' '{print $1}'`
    WORKDIR=`pwd`
    summaryLog=${WORKDIR}/summary_${HOST}.log
fi
echo "summaryLog is $summaryLog"
cat ${summaryLog}

SUMMARYTXT=${WORKSPACE}/summary_nightly.log
echo "SUMMARYTXT is $SUMMARYTXT"
if [ $(hostname |grep -i skx |wc -l) -eq 1 ];then 
    MACHINE_TYPE=SKX
elif [ $(hostname |grep -i clx |wc -l) -eq 1 ];then 
    MACHINE_TYPE=CLX
else 
    MACHINE_TYPE=Others
fi 
DATATYPE='fp32'

if [ $# != "4" ]; then
    echo 'ERROR:'
    echo "Expected 4 parameter got $#"
    printf 'Please use following parameters:
    --model=<model to run>
    --mode=training|inference
    --fullvalidation=true|false
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
            model=`echo $i | sed "s/${pattern}//"`;;
        --mode=*)
            mode=`echo $i | sed "s/${pattern}//"`;;
        --fullvalidation=*)
            fullvalidation=`echo $i | sed "s/${pattern}//"`;;
        --single_socket=*)
            single_socket=`echo $i | sed "s/${pattern}//"`;;
        *)
            echo "Parameter $i not recognized."; exit 1;;
    esac
done


logfile_throughput="Q2_models_${model}_throughput.log"
logfile_latency="Q2_models_${model}_latency.log"

status_throughput="CHECK_LOG_FOR_ERROR"
status_latency="CHECK_LOG_FOR_ERROR"


if [ "${model}" == "DenseNet" ];then
    if [ "${mode}" == "inference" ];then
     
        if [ $(grep 'Error: ' ${logfile_throughput} | wc -l) == 0 ] &&
           [ $(grep 'through-put:' ${logfile_throughput} | wc -l) != 0 ] ; then
            status_throughput="SUCCESS"
        fi
        if [ $(grep 'Error: ' ${logfile_latency} | wc -l) == 0 ] &&
           [ $(grep 'latency:' ${logfile_latency} | wc -l) != 0 ] ; then
            status_latency="SUCCESS"
        fi

        echo "summaryLog: $summaryLog"
        echo "DenseNet Throughput: ${status_throughput} " >> ${summaryLog}

        # throughput batch size 128
        BATCH_SIZE=$(grep -i "Batch Size" ${logfile_throughput} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${summaryLog}
        FPS=$(grep 'through-put: ' ${logfile_throughput} | tail -1 | awk -F'through-put:' '{print $2}' | sed "s/[^0-9.]*//g")
        if [ ${single_socket} == "true" ]; then
            echo "single_socket $FPS times 2"
           FPS=$(echo | awk -v value=$FPS  '{ result=value * 2; printf("%.2f", result) }')
        fi
        echo "  Total images/sec: ${FPS}" >>  ${summaryLog}
        echo " " >>  ${summaryLog}
        echo "${model};${mode};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${logfile_throughput##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        #latency: batch size 1
        echo ""
        echo "DenseNet Latency: ${status_latency} " >>  ${summaryLog}
        BATCH_SIZE=$(grep -i "Batch Size" ${logfile_latency} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >>  ${summaryLog}
        latency=$(grep 'latency:' ${logfile_latency} | tail -1 | awk -F'latency:' '{print $2}' | sed "s/[^0-9.]*//g")
        echo "  Latency ms/step: ${latency}" >>  ${summaryLog}
        batch_size_1_throughput=$(echo | awk -v value=$latency  '{ result=1000/value; printf("%.2f", result) }')
        echo "  Throughput images/sec: ${batch_size_1_throughput}" >>  ${summaryLog}
        echo " " >>  ${summaryLog}
        echo "${model};${mode};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${logfile_latency##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
    else
        echo "${model} training is skipped for now"
        exit 0
    fi
        
elif [ "${model}" == "3DUNet" ] || [ "${model}" == "MaskRCNN" ];then 
    if [ "${mode}" == "inference" ]; then
    
        if [ $(grep 'Total samples/sec:' ${logfile_throughput} | wc -l) != 0 ]; then
            status_throughput="SUCCESS"
        fi
        if [ $(grep 'Time spent per BATCH:' ${logfile_latency} | wc -l) != 0 ]; then
            status_latency="SUCCESS"
        fi
        
        echo "${model} Throughput: ${status_throughput} " >> ${summaryLog}

        # throughput batch size default
        BATCH_SIZE=$(grep -i "Batch Size" ${logfile_throughput} | sed "s/[^0-9]*//g" | tail -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${summaryLog}
        FPS=$(grep 'Total samples/sec:' ${logfile_throughput} | tail -1 | sed "s/[^0-9.]*//g")
        if [ "${status_throughput}" == "SUCCESS" ]; then 
            if [ ${single_socket} == "true" ]; then
                echo "single_socket $FPS times 2"
                FPS=$(echo | awk -v value=$FPS  '{ result=value * 2; printf("%.2f", result) }')
            fi
            echo "  Total samples/sec: ${FPS}" >>  ${summaryLog}
            echo " " >>  ${summaryLog}
        else
            echo "  Total samples/sec: not found" >>  ${summaryLog}
            echo " " >>  ${summaryLog}
        fi
        echo "${model};${mode};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${logfile_throughput##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        #latency: batch size 1
        echo ""
        echo "${model} Latency: ${status_latency} " >>  ${summaryLog}
        BATCH_SIZE=$(grep -i "Batch Size" ${logfile_latency} | sed "s/[^0-9]*//g" | tail -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >>  ${summaryLog}
        latency=$(grep 'Time spent per BATCH:' ${logfile_latency} | tail -1 | sed "s/[^0-9.]*//g")
        if [ "${status_latency}" == "SUCCESS" ]; then 
            echo "  Latency ms/batch: ${latency}" >>  ${summaryLog}
            batch_size_1_throughput=$(echo | awk -v value=$latency  '{ result=1000/value; printf("%.2f", result) }')
            echo "  Throughput batch/sec: ${batch_size_1_throughput}" >>  ${summaryLog}
            echo " " >>  ${summaryLog}
        else
            echo "  Latency ms/batch: not found" >>  ${summaryLog}
            echo " " >>  ${summaryLog}
        fi
        echo "${model};${mode};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${logfile_latency##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
    else
        echo "${model} training is skipped for now"
        exit 0
    fi
else
    echo "${model} is not recognized"
    exit 1
fi


#if [ "${status_latency}" == "SUCCESS" ] && [ "${status_throughput}" == "SUCCESS" ]; then
#    exit 0
#else
#    exit 1
#fi
