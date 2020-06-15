#!/bin/bash
set -x

# models:
# resnet50_eager
# resnet50_graph
# resnet50_minidata
# resnet56_eager
# resnet56_graph

# mode:
# training


HOST=`uname -n | awk -F'.' '{print $1}'`
WORKDIR=`pwd`

PATTERN='[-a-zA-Z0-9_]*='
MODEL=`echo $1 | sed "s/${PATTERN}//"`

LOGFILE_THROUGHPUT=`find ${WORKDIR} -name benchmark_${MODEL}_train_throughput*`
LOGFILE_ACCURACY=`find ${WORKDIR} -name benchmark_${MODEL}_train_accuracy*`

SUMMARYLOG=${WORKDIR}/summary_${HOST}.log
SUMMARYTXT=${WORKDIR}/summary_nightly.log

STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
STATUS_ACCURACY="CHECK_LOG_FOR_ERROR"

collect_logs_resnet50_eager(){

    echo "***** collect_logs_resnet50_eager *****"

    case "$LOGFILE_THROUGHPUT" in
        *skx*)
            MACHINE_TYPE="SKX"
            ;;
        *clx*)
            MACHINE_TYPE="CLX"
            ;;
        *)
            MACHINE_TYPE="OTHER"
            ;;
    esac

    # check throughput log status
    STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
    if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
       [ $(grep '100/100' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
        STATUS_THROUGHPUT="SUCCESS"
    fi

    # throughput
    echo "${MACHINE_TYPE} ${MODEL}_Train Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | tail -1 | sed "s/[^0-9]*//g")
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    THROUGHPUT=$(grep 'avg_exp_per_second' $LOGFILE_THROUGHPUT | awk -F': ' '{print $NF}' |sed 's/[^0-9.,]//g')
    THROUGHPUT=$(echo | awk -v throughput=$THROUGHPUT '{ result=throughput; printf("%.2f", result) }')
    echo "  Throughput: ${THROUGHPUT}" >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    echo "${MODEL};train;${MACHINE_TYPE};fp32;Throughput;${BATCH_SIZE};${THROUGHPUT};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

}

collect_logs_resnet50_graph(){

    echo "***** collect_logs_resnet50_graph *****"

    case "$LOGFILE_THROUGHPUT" in
        *skx*)
            MACHINE_TYPE="SKX"
            ;;
        *clx*)
            MACHINE_TYPE="CLX"
            ;;
        *)
            MACHINE_TYPE="OTHER"
            ;;
    esac

    # check throughput log status
    STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
    if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
       [ $(grep '100/100' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
        STATUS_THROUGHPUT="SUCCESS"
    fi

    # throughput
    echo "${MACHINE_TYPE} ${MODEL}_Train Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | tail -1 | sed "s/[^0-9]*//g")
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    THROUGHPUT=$(grep 'avg_exp_per_second' $LOGFILE_THROUGHPUT | awk -F': ' '{print $NF}' |sed 's/[^0-9.,]//g')
    THROUGHPUT=$(echo | awk -v throughput=$THROUGHPUT '{ result=throughput; printf("%.2f", result) }')
    echo "  Throughput: ${THROUGHPUT}" >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    echo "${MODEL};train;${MACHINE_TYPE};fp32;Throughput;${BATCH_SIZE};${THROUGHPUT};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

}

collect_logs_resnet56_eager_cifar10(){

    echo "***** collect_logs_resnet56_eager *****"

    case "$LOGFILE_THROUGHPUT" in
        *skx*)
            MACHINE_TYPE="SKX"
            ;;
        *clx*)
            MACHINE_TYPE="CLX"
            ;;
        *)
            MACHINE_TYPE="OTHER"
            ;;
    esac

    # check throughput log status
    STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
    if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
       [ $(grep '97/97' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
        STATUS_THROUGHPUT="SUCCESS"
    fi

    # throughput
    echo "${MACHINE_TYPE} ${MODEL}_Train Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | tail -1 | sed "s/[^0-9]*//g")
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    THROUGHPUT=$(grep 'avg_exp_per_second' $LOGFILE_THROUGHPUT | awk -F': ' '{print $NF}' |sed 's/[^0-9.,]//g')
    THROUGHPUT=$(echo | awk -v throughput=$THROUGHPUT '{ result=throughput; printf("%.2f", result) }')
    echo "  Throughput: ${THROUGHPUT}" >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    echo "${MODEL};train;${MACHINE_TYPE};fp32;Throughput;${BATCH_SIZE};${THROUGHPUT};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

}

collect_logs_resnet56_graph_cifar10(){

    echo "***** collect_logs_resnet56_graph *****"

    case "$LOGFILE_THROUGHPUT" in
        *skx*)
            MACHINE_TYPE="SKX"
            ;;
        *clx*)
            MACHINE_TYPE="CLX"
            ;;
        *)
            MACHINE_TYPE="OTHER"
            ;;
    esac

    # check throughput log status
    STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
    if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
       [ $(grep '97/97' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
        STATUS_THROUGHPUT="SUCCESS"
    fi

    # throughput
    echo "${MACHINE_TYPE} ${MODEL}_Train Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | tail -1 | sed "s/[^0-9]*//g")
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    THROUGHPUT=$(grep 'avg_exp_per_second' $LOGFILE_THROUGHPUT | awk -F': ' '{print $NF}' |sed 's/[^0-9.,]//g')
    THROUGHPUT=$(echo | awk -v throughput=$THROUGHPUT '{ result=throughput; printf("%.2f", result) }')
    echo "  Throughput: ${THROUGHPUT}" >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    echo "${MODEL};train;${MACHINE_TYPE};fp32;Throughput;${BATCH_SIZE};${THROUGHPUT};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

}

collect_logs_resnet50_minidata(){

    echo "***** collect_logs_resnet50_minidata *****"

    case "$LOGFILE_ACCURACY" in
        *skx*)
            MACHINE_TYPE="SKX"
            ;;
        *clx*)
            MACHINE_TYPE="CLX"
            ;;
        *)
            MACHINE_TYPE="OTHER"
            ;;
    esac

    # check accuracy log status
    STATUS_ACCURACY="CHECK_LOG_FOR_ERROR"
    if [ $(grep 'Error: ' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
       [ $(grep 'TEST PASSED:' ${LOGFILE_ACCURACY} | wc -l) != 0 ]; then
        STATUS_ACCURACY="SUCCESS"
    fi

    # accuracy
    echo "${MACHINE_TYPE} ${MODEL}_Train Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
    BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_ACCURACY} | tail -1 | sed "s/[^0-9]*//g")
    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
    ACCURACY=$(grep "TEST PASSED: " ${LOGFILE_ACCURACY} | cut -d':' -f 2 | cut -d' ' -f 2)
    echo "  Accuracy: ${ACCURACY}" >> ${SUMMARYLOG}
    echo " " >> ${SUMMARYLOG}
    echo "${MODEL};train;${MACHINE_TYPE};fp32;Accuracy;${BATCH_SIZE};${ACCURACY};${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

}

case $MODEL in
    "resnet50_eager")
        echo "model is $MODEL"
        collect_logs_resnet50_eager
        ;;
    "resnet50_eager_1")
        echo "model is $MODEL"
        collect_logs_resnet50_eager
        ;;
    "resnet50_graph")
        echo "model is $MODEL"
        collect_logs_resnet50_graph
        ;;
    "resnet56_eager_cifar10")
        echo "model is $MODEL"
        collect_logs_resnet56_eager_cifar10
        ;;
    "resnet56_graph_cifar10")
        echo "model is $MODEL"
        collect_logs_resnet56_graph_cifar10
        ;;
    "resnet50_minidata")
        echo "model is $MODEL"
        collect_logs_resnet50_minidata
        ;;
esac