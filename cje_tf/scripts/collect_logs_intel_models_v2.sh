#!/bin/bash

#collect_logs_intel_models_v2.sh --model=[inceptionv3/mobilenet_v1/resnet50/resnet101] --mode=[training/inference] --datatype=[fp32/int8]
# this script collects the benchmark numbers for models runs from intel-model zoo, including:
# models:
#     - bert(py3, fp32/int8)
#     - inceptionv3(py3, fp32/int8)
#     - mlperf_gnmt(py3, fp32)
#     - resnet101(py3, fp32/int8)
#     - resnet50(py3, fp32/int8)
#     - resnet50v1_5(py3, fp32/int8)
#     - rfcn(py3, fp32/int8)
#     - ssd-mobilenet(py3, fp32/int8)
#     - ssd-resnet34(py3, fp32/int8)
#     - mobilenet_v1(py3, int8)
#     - densenet169(py3, fp32)
#     - inceptionv4(py3, fp32/int8)
# mode:
#     - training
#     - inference
#
# datatype:
#     - fp32
#     - int8

set -x

HOST=`uname -n | awk -F'.' '{print $1}'`
WORKDIR=`pwd`

if [ $# != "3" ]; then
    echo 'ERROR:'
    echo "Expected 3 parameter got $#"
    printf 'Please use following parameters:
    --model=<model to run>
    --mode=training|inference
    --datatype=fp32|int8
    '
    echo 'All parameters are obligatory.'
    exit 1
fi

PATTERN='[-a-zA-Z0-9_]*='
for i in "$@"; do
    case $i in
        --model=*)
            MODEL=`echo $i | sed "s/${PATTERN}//"`
            ;;
        --mode=*)
            MODE=`echo $i | sed "s/${PATTERN}//"`
            ;;
        --datatype=*)
            DATATYPE=`echo $i | sed "s/${PATTERN}//"`
            ;;
        *)
            echo "Parameter $i not recognized." 
            exit 1
            ;;
    esac
done

LOGFILES_ACCURACY=`find ${WORKDIR}/${MODEL} -name benchmark_${MODEL}_${MODE}_${DATATYPE}_accuracy*`
LOGFILES_LATENCY=`find ${WORKDIR}/${MODEL} -name benchmark_${MODEL}_${MODE}_${DATATYPE}_latency*`
LOGFILES_THROUGHPUT=`find ${WORKDIR}/${MODEL} -name benchmark_${MODEL}_${MODE}_${DATATYPE}_throughput*`

SUMMARYLOG=${WORKDIR}/summary_${HOST}.log
SUMMARYTXT=${WORKDIR}/summary_nightly.log

STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
STATUS_ACCURACY="CHECK_LOG_FOR_ERROR"

collect_logs_fastrcnn() {

    echo "***** collect_logs_fastrcnn *****"

    if [ ${MODE} == "inference" ]; then

        for LOGFILE_ACCURACY in $LOGFILES_ACCURACY; do
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

            # check accuracy status
            STATUS_ACCURACY="CHECK_LOG_FOR_ERROR"
            if [ $(grep 'ImportError' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
               [ $(grep 'Average Precision' ${LOGFILE_ACCURACY} | wc -l) != 0 ]; then
                STATUS_ACCURACY="SUCCESS"
            fi

            # accuracy 
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g" | head -n 1)
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            grep 'Average Precision' ${LOGFILE_ACCURACY} | head -n 2 >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            m_ap=$(grep 'Average Precision' ${LOGFILE_ACCURACY} | head -n 2 |awk '{printf("%s,", $NF)}')
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${m_ap};${MODEL}/${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
 
        done

        for LOGFILE_LATENCY in $LOGFILES_LATENCY; do
            case "$LOGFILE_LATENCY" in
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

            if [ ${DATATYPE} == "fp32" ]; then

                # check latency & throughput log status 
                STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
                STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
                if [ $(grep 'ImportError: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
                   [ $(grep 'Time spent per BATCH:' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
                    STATUS_LATENCY="SUCCESS"
                    STATUS_THROUGHPUT="SUCCESS"
                fi

                # latency
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_LATENCY} | head -n 1 | sed "s/[^0-9]*//g")
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                LATENCY=$(grep "Time spent per BATCH:" ${LOGFILE_LATENCY} | awk -F': ' '{print $2}' | awk -F' ' '{print $1}')
                echo "  Latency sec/images: ${LATENCY}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            
                # throughput
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                THROUGHPUT=$(echo | awk -v tmp=$LATENCY '{ result= 1 / tmp; printf("%.2f", result) }')
                echo "  Total images/sec: ${THROUGHPUT}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

            else // DATATYPE is int8

                # check latency & throughput log status 
                STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
                STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
                if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
                   [ $(grep 'Avg. Duration per Step' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
                        STATUS_THROUGHPUT="SUCCESS"
                        STATUS_LATENCY="SUCCESS"
                fi
    
                # latency
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_LATENCY} | head -n 1 | sed "s/[^0-9]*//g")
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                grep 'Avg. Duration per Step' ${LOGFILE_LATENCY} >> ${SUMMARYLOG}
                LATENCY=$(grep 'Avg. Duration per Step:' ${LOGFILE_LATENCY} | tail -n 1 | sed "s/.*://g")
                echo "  Latency sec/images: ${LATENCY}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

                # throughput
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                THROUGHPUT=$(echo | awk -v value=$LATENCY  '{ result=1/value; printf("%.2f", result) }')
                echo "  Throughput: ${THROUGHPUT}" >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            fi

        done

    else
        echo "${MODEL} ${mode} is skipped "
    fi

}

collect_logs_inceptionv3() {

    echo "***** collect_logs_inceptionv3 for model ${MODEL} *****"

    if [ ${MODE} == "inference" ]; then

        # check accuracy log status 
        # we may have multiple accuracy log files running on different platform, eg. skx, clx
        for LOGFILE_ACCURACY in $LOGFILES_ACCURACY; do
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

            STATUS_ACCURACY="CHECK_LOG_FOR_ERROR"

	    if [ $(grep 'Error: ' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
	       [ $(grep 'Processed 50000 images' ${LOGFILE_ACCURACY} | wc -l) != 0 ]; then
                STATUS_ACCURACY="SUCCESS"
	    fi

            # accuracy 
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g" | head -n 1)
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            ACCURACY=$(grep 'Processed 50000 images' ${LOGFILE_ACCURACY} | awk -F= '{print $2}' |sed 's/[^0-9.,]//g')
            echo "  Accuracy: ${ACCURACY}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${ACCURACY};${MODEL}/${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        done
        
        if [ ${DATATYPE} == "fp32" ]; then
            
            for LOGFILE_LATENCY in $LOGFILES_LATENCY; do
                case "$LOGFILE_LATENCY" in
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

                # check latency log status 
                STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
                if [ ${MODEL} == "inceptionv3" ] || [ ${MODEL} == "inception_resnet_v2" ]; then
                    if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
                       [ $(grep 'Latency:' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
                        STATUS_LATENCY="SUCCESS"
                    fi
                elif [ ${MODEL} == "inceptionv4" ]; then
                    if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
                       [ $(grep 'steps = 50,' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
                        STATUS_LATENCY="SUCCESS"
                    fi
                fi

                # latency
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_LATENCY} | tail -1 | sed "s/[^0-9]*//g")
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                if [ ${MODEL} == "inceptionv3" ] || [ ${MODEL} == "inception_resnet_v2" ] || [ ${MODEL} == "inceptionv4" ]; then
                    LATENCY=$(grep 'Latency:' ${LOGFILE_LATENCY} | sed "s/[^0-9.]*//g")
                    echo "  Latency ms: ${LATENCY}" >> ${SUMMARYLOG}
                fi
                echo " " >> ${SUMMARYLOG}
                
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            done
            
            for LOGFILE_THROUGHPUT in $LOGFILES_THROUGHPUT; do
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
                if [ ${MODEL} == "inceptionv3" ] || [ ${MODEL} == "inception_resnet_v2" ]; then
                    if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
                       [ $(grep 'Throughput:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                        STATUS_THROUGHPUT="SUCCESS"
                    fi
                elif [ ${MODEL} == "inceptionv4" ]; then
                    if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
                       [ $(grep 'steps = 50,' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                         STATUS_THROUGHPUT="SUCCESS"
                    fi
                fi

                # throughput
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | tail -1 | sed "s/[^0-9]*//g")
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                if [ ${MODEL} == "inceptionv3" ] || [ ${MODEL} == "inception_resnet_v2" ]; then
                    THROUGHPUT=$(grep 'Throughput:' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
                    echo "  Total images/sec: ${THROUGHPUT}" >> ${SUMMARYLOG}
                elif [ ${MODEL} == "inceptionv4" ]; then
                    THROUGHPUT=`grep "steps = " ${LOGFILE_THROUGHPUT} | tail -n 5 | cut -d',' -f 2 | cut -d' ' -f 2 | awk 'BEGIN{sum=0}{sum+=$1}END{printf("%.3f\n",sum/NR)}'`
                    echo "  Total images/sec: ${THROUGHPUT}"  >> ${SUMMARYLOG}
                fi
                echo " " >> ${SUMMARYLOG}

                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            done

        else // DATATYPE is int8

            for LOGFILE_LATENCY in $LOGFILES_LATENCY; do
                case "$LOGFILE_LATENCY" in
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

                STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
                if [ ${MODEL} == "inception_resnet_v2" ]; then
                    if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
                       [ $(grep 'Latency:' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
                        STATUS_LATENCY="SUCCESS"
                    fi
                else
                    if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
                       [ $(grep 'steps = 50,' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
                        STATUS_LATENCY="SUCCESS"
                    fi
                fi

                # latency
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
                if [ ${MODEL} == "inception_resnet_v2" ] || [ ${MODEL} == "inceptionv4" ]; then
                    BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_LATENCY} | tail -1 | sed "s/[^0-9]*//g")
                    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                    LATENCY=$(grep 'Latency:' ${LOGFILE_LATENCY} | sed "s/[^0-9.]*//g")
                    echo "  Latency ms: ${LATENCY}" >> ${SUMMARYLOG}
                    echo " " >> ${SUMMARYLOG}
		                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
                else
                    BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
                    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                    THROUGHPUT=$(grep "Average throughput for batch size 1:" ${LOGFILE_LATENCY} | awk -F': ' '{print $2}' | awk -F' ' '{print $1}')
                    LATENCY=$(echo | awk -v t=$THROUGHPUT '{ r = 1000 / t; printf("%.2f", r)}')
                    echo "  Latency ms: ${LATENCY}" >> ${SUMMARYLOG}
                    echo " " >> ${SUMMARYLOG}
                    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
                fi
            done

            for LOGFILE_THROUGHPUT in $LOGFILES_THROUGHPUT; do
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
                if [ ${MODEL} == "inception_resnet_v2" ]; then
                    if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
                       [ $(grep 'Throughput:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                        STATUS_THROUGHPUT="SUCCESS"
                    fi
                else
                    if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
                       [ $(grep 'steps = 50,' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                         STATUS_THROUGHPUT="SUCCESS"
                    fi
                fi

                # throughput
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}

                BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | tail -1 | sed "s/[^0-9]*//g")
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                if [ ${MODEL} == "inception_resnet_v2" ]; then
                    THROUGHPUT=$(grep 'Throughput:' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
                    echo "  Total images/sec: ${THROUGHPUT}" >> ${SUMMARYLOG}
                elif [ ${MODEL} == "inceptionv4" ]; then
                    THROUGHPUT=`grep "steps = " ${LOGFILE_THROUGHPUT} | tail -n 5 | cut -d',' -f 2 | cut -d' ' -f 2 | awk 'BEGIN{sum=0}{sum+=$1}END{printf("%.3f\n",sum/NR)}'`
                    echo "  Total images/sec: ${THROUGHPUT}"  >> ${SUMMARYLOG}
                else
                    TEMP_THROUGHPUT=$(grep "Average throughput for batch size 128: " ${LOGFILE_THROUGHPUT} | awk -F': ' '{print $2}' | awk -F' ' '{print $1}')
                    THROUGHPUT=$(echo | awk -v t=$TEMP_THROUGHPUT '{ printf("%.2f", t)}')
                    echo "  Total images/sec: ${THROUGHPUT}" >> ${SUMMARYLOG}
                fi

                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            done

        fi  
            
    else
        echo "${MODEL} ${mode} is skipped "
    fi

}

collect_logs_maskrcnn() {

    echo "***** collect_logs_maskrcnn *****"

    if [ ${MODE} == "inference" ]; then

        # check log status , only THROUGHPUT file
        for LOGFILE_THROUGHPUT in $LOGFILES_THROUGHPUT; do
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

            STATUS_ACCURACY="CHECK_LOG_FOR_ERROR"
            STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
            STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"

	    if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
               [ $(grep 'Average Precision' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] &&
	       [ $(grep 'Time spent per BATCH:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] && 
	       [ $(grep 'Total samples/sec:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                STATUS_ACCURACY="SUCCESS"
                STATUS_LATENCY="SUCCESS"
                STATUS_THROUGHPUT="SUCCESS"
	    fi
            
            if [ ${DATATYPE} == "fp32" ]; then

                echo "datatype is ${DATATYPE}"

                # accuracy 
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size: " ${LOGFILE_THROUGHPUT} | tail -1 | sed "s/[^0-9]*//g")
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                grep 'Average Precision' ${LOGFILE_THROUGHPUT} | head -n 2 >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}

                # latency
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                LATENCY=$(grep 'Time spent per BATCH: ' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
                echo "  Latency ms: ${LATENCY}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            
                # throughput
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                THROUGHPUT=$(grep 'Total samples/sec:' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
                echo "  Total images/sec: ${THROUGHPUT}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

            else // DATATYPE is int8
            fi

        done

    else
        echo "${MODEL} ${mode} is skipped "
    fi

}

collect_logs_mlperf_gnmt() {

    echo "***** collect_logs_mlperf_gnmt *****"

    if [ ${MODE} == "inference" ]; then
	
        if [ ${DATATYPE} == "fp32" ]; then

            for LOGFILE_ACCURACY in $LOGFILES_ACCURACY; do
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

                # check log status 
                STATUS_ACCURACY="CHECK_LOG_FOR_ERROR"
	        if [ $(grep 'Error: ' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
	           [ $(grep 'Accuracy metric bleu' ${LOGFILE_ACCURACY} | wc -l) != 0 ]; then
                    STATUS_ACCURACY="SUCCESS"
	        fi

                # accuracy 
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g" | head -n 1)
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
	        ACCURACY=$(grep 'Accuracy metric bleu' ${LOGFILE_ACCURACY} | awk -F: '{print $2}' |sed 's/[^0-9.,]//g')
                echo "  Accuracy metric bleu: ${ACCURACY}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${ACCURACY};${MODEL}/${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

            done

            for LOGFILE_LATENCY in $LOGFILES_LATENCY; do
                case "$LOGFILE_LATENCY" in
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

                # check log status 
                STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
                if [ $(grep 'ImportError: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] && 
                   [ $(grep 'Average Prediction Latency:' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
                    STATUS_LATENCY="SUCCESS"
                fi 

                # latency: batch size 1
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                LATENCY=`grep "Average Prediction Latency: " ${LOGFILE_LATENCY} | awk -F': ' '{print $2}' | awk -F' ' '{print $1 * 1000}'`
                echo "  Latency ms/sentences:  ${LATENCY}"  >> ${SUMMARYLOG}
                #batch_size_1_throughput=$(echo | awk -v value=$LATENCY  '{ result=1000/value; printf("%.2f", result) }')
                #echo "  Throughput sentences/sec: ${batch_size_1_throughput}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            done

            for LOGFILE_THROUGHPUT in $LOGFILES_THROUGHPUT; do
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

                STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
                if [ $(grep 'ImportError: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] && 
                   [ $(grep 'Overall Throughput' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                    STATUS_THROUGHPUT="SUCCESS"
                fi

                # throughput
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                THROUGHPUT=`grep "Overall Throughput" ${LOGFILE_THROUGHPUT} | awk -F': ' '{print $2}' | awk -F' ' '{print $1}'`
                echo "  Total sentences/sec:  ${THROUGHPUT}"  >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            done

        else // DATATYPE is int8

        fi

    else
        echo "${MODEL} ${mode} is skipped "
    fi

}

collect_logs_mobilenet_v1() {

    echo "***** collect_logs_mobilenet_v1 *****"

    if [ ${MODE} == "inference" ]; then

        for LOGFILE_ACCURACY in $LOGFILES_ACCURACY; do
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

            # check log status 
            STATUS_ACCURACY="CHECK_LOG_FOR_ERROR"
	      if [ $(grep 'Error: ' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
	          [ $(grep 'Processed 50000 images' ${LOGFILE_ACCURACY} | wc -l) != 0 ]; then
                STATUS_ACCURACY="SUCCESS"
	      fi

            # accuracy 
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g" | head -n 1)
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            ACCURACY=$(grep 'Processed 50000 images' ${LOGFILE_ACCURACY} | awk -F= '{print $2}' |sed 's/[^0-9.,]//g')
            echo "  Accuracy: ${ACCURACY}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${ACCURACY};${MODEL}/${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        done

        echo "datatype is ${DATATYPE}"

        for LOGFILE_LATENCY in $LOGFILES_LATENCY; do
            case "$LOGFILE_LATENCY" in
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

            # check log status
            STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
            if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
               [ $(grep 'steps = ' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
                STATUS_LATENCY="SUCCESS"
            fi

            # latency
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_LATENCY} | tail -1 | sed "s/[^0-9]*//g")
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            LATENCY=$(grep "images/sec" ${LOGFILE_LATENCY} | cut -d',' -f 2 | cut -d' ' -f 2  | tail -n 100 | awk 'BEGIN{sum=0}{sum+=$1}END{printf("%.3f\n",sum/NR)}')
            FPS=$(echo | awk -v latency=$LATENCY '{ result= 1000 / latency; printf("%.3f", result) }')
            echo "  Latency ms: ${FPS}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${FPS};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
        done

        for LOGFILE_THROUGHPUT in $LOGFILES_THROUGHPUT; do
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

            # check log status
            STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
            if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
               [ $(grep 'steps = ' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                STATUS_THROUGHPUT="SUCCESS"
            fi

            # throughput
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | tail -1 | sed "s/[^0-9]*//g")
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            THROUGHPUT=$(grep "images/sec" ${LOGFILE_THROUGHPUT} | cut -d',' -f 2 | cut -d' ' -f 2  | tail -n 100 | awk 'BEGIN{sum=0}{sum+=$1}END{printf("%.3f\n",sum/NR)}')
            echo "  Total images/sec: ${THROUGHPUT}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        done

    else
        echo "${MODEL} ${mode} is skipped "
    fi

}

collect_logs_ncf() {

    echo "***** collect_logs_ncf *****"

    if [ ${MODE} == "inference" ]; then

        if [ ${DATATYPE} == "fp32" ]; then

            echo "datatype is ${DATATYPE}"
        
            for LOGFILE_ACCURACY in $LOGFILES_ACCURACY; do
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

                # check log status 
                STATUS_ACCURACY="CHECK_LOG_FOR_ERROR"
	        if [ $(grep 'Error: ' ${LOGFILE_ACCURACY} | wc -l) == 0 ]; then
                     STATUS_ACCURACY="SUCCESS"
                fi

                # accuracy ???
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g" | head -n 1)
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
	        #ACCURACY=$(grep 'Processed 50000 images' ${LOGFILE_ACCURACY} | awk -F= '{print $2}' |sed 's/[^0-9.,]//g')
                echo "  Accuracy: ${ACCURACY}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${ACCURACY};${MODEL}/${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

            done

            for LOGFILE_LATENCY in $LOGFILES_LATENCY; do
                case "$LOGFILE_LATENCY" in
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

                # check log status 
                STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
                if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
                   [ $(grep 'Average recommendations/sec' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                    STATUS_LATENCY="SUCCESS"
                fi

                # latency
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_LATENCY} | tail -1 | sed "s/[^0-9]*//g")
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                THROUGHPUT=$(grep 'Average recommendations/sec' ${LOGFILE_LATENCY} | sed "s/.*: //g" | sed "s/ (.*//g")
                LATENCY=$(echo |awk -v bs=${BATCH_SIZE} -v fps=${THROUGHPUT} '{printf("%.3f",bs/fps*1000)}')
                echo "  Average recommendations/sec: ${LATENCY}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

            done

            for LOGFILE_THROUGHPUT in $LOGFILES_THROUGHPUT; do
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

                # check log status 
                STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
                if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
                   [ $(grep 'Average recommendations/sec' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                    STATUS_THROUGHPUT="SUCCESS"
                fi
            
                # throughput
                # currently failing
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | tail -1 | sed "s/[^0-9]*//g")
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                THROUGHPUT=$(grep 'Average recommendations/sec' ${LOGFILE_THROUGHPUT} | sed "s/.*: //g" | sed "s/ (.*//g")
                echo "  Average recommendations/sec: ${THROUGHPUT}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

            done

        else // DATATYPE is int8
        fi
    else
        echo "${MODEL} ${mode} is skipped "
    fi

}

collect_logs_resnet50() {

    echo "***** collect_logs_resnet50 *****"

    if [ ${MODE} == "inference" ]; then
 
        # check accuracy log status 
        # we may have multiple accuracy log files running on different platform, eg. skx, clx
        for LOGFILE_ACCURACY in $LOGFILES_ACCURACY; do
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
	       [ $(grep 'Processed 50000 images' ${LOGFILE_ACCURACY} | wc -l) != 0 ]; then
                STATUS_ACCURACY="SUCCESS"
	    fi

            # accuracy 
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g" | head -n 1)
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
	    ACCURACY=$(grep 'Processed 50000 images' ${LOGFILE_ACCURACY} | awk -F= '{print $2}' |sed 's/[^0-9.,]//g')
            echo "  Accuracy: ${ACCURACY}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${ACCURACY};${MODEL}/${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        done

        for LOGFILE_LATENCY in $LOGFILES_LATENCY; do
            case "$LOGFILE_LATENCY" in
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

            # check latency log status 
            STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
            if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
               [ $(grep 'Latency:' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
                STATUS_LATENCY="SUCCESS"
            fi

            # latency
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_LATENCY} | tail -1 | sed "s/[^0-9]*//g")
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            LATENCY=$(grep 'Latency:' ${LOGFILE_LATENCY} | sed "s/[^0-9.]*//g")
            echo "  Latency ms: ${LATENCY}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        done
            
        for LOGFILE_THROUGHPUT in $LOGFILES_THROUGHPUT; do
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
               [ $(grep 'Throughput:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                STATUS_THROUGHPUT="SUCCESS"
            fi

            # throughput
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | tail -1 | sed "s/[^0-9]*//g")
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            THROUGHPUT=$(grep 'Throughput:' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
            echo "  Total images/sec: ${THROUGHPUT}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        done

    else
        echo "${MODEL} ${mode} is skipped "
    fi

}

collect_logs_resnet50v1_5() {

    echo "***** collect_logs_resnet50v1_5 *****"

    if [ ${MODE} == "inference" ]; then
 
        # check accuracy log status 
        # we may have multiple accuracy log files running on different platform, eg. skx, clx
        for LOGFILE_ACCURACY in $LOGFILES_ACCURACY; do
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
	       [ $(grep 'Processed 50000 images' ${LOGFILE_ACCURACY} | wc -l) != 0 ]; then
                STATUS_ACCURACY="SUCCESS"
	    fi

            # accuracy 
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g" | head -n 1)
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
	    ACCURACY=$(grep 'Processed 50000 images' ${LOGFILE_ACCURACY} | awk -F= '{print $2}' |sed 's/[^0-9.,]//g')
            echo "  Accuracy: ${ACCURACY}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${ACCURACY};${MODEL}/${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        done

        for LOGFILE_LATENCY in $LOGFILES_LATENCY; do
            case "$LOGFILE_LATENCY" in
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

            # check latency log status 
            STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
            if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
               [ $(grep 'Latency:' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
                STATUS_LATENCY="SUCCESS"
            fi

            # latency
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_LATENCY} | tail -1 | sed "s/[^0-9]*//g")
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            LATENCY=$(grep 'Latency:' ${LOGFILE_LATENCY} | sed "s/[^0-9.]*//g")
            echo "  Latency ms: ${LATENCY}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
        
        done
            
        for LOGFILE_THROUGHPUT in $LOGFILES_THROUGHPUT; do
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
               [ $(grep 'Throughput:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                STATUS_THROUGHPUT="SUCCESS"
            fi

            # throughput
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | tail -1 | sed "s/[^0-9]*//g")
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            THROUGHPUT=$(grep 'Throughput:' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
            echo "  Total images/sec: ${THROUGHPUT}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        done

    else
        echo "${MODEL} ${mode} is skipped "
    fi

}
collect_logs_resnet101() {

    echo "***** collect_logs_resnet101 *****"
    
    if [ ${MODE} == "inference" ]; then
	
        # check accuracy log status 
        # we may have multiple accuracy log files running on different platform, eg. skx, clx
        for LOGFILE_ACCURACY in $LOGFILES_ACCURACY; do
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
               [ $(grep 'Processed 50000 images' ${LOGFILE_ACCURACY} | wc -l) != 0 ]; then
                STATUS_ACCURACY="SUCCESS"
	    fi

            # accuracy
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g" | head -n 1)
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
	    ACCURACY=$(grep 'Processed 50000 images' ${LOGFILE_ACCURACY} | awk -F= '{print $2}' |sed 's/[^0-9.,]//g')
            echo "  Accuracy: ${ACCURACY}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${ACCURACY};${MODEL}/${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        done

        for LOGFILE_LATENCY in $LOGFILES_LATENCY; do
            case "$LOGFILE_LATENCY" in
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

            # check latency log status 
            STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
            if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
               [ $(grep 'Latency:' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
               STATUS_LATENCY="SUCCESS"
            fi

            # latency
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_LATENCY} | tail -1 | sed "s/[^0-9]*//g" )
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            LATENCY=$(grep 'Latency:' ${LOGFILE_LATENCY} | awk -F' ' '{print $2}') 
            echo "  Latency ms: ${LATENCY}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        done
            
        for LOGFILE_THROUGHPUT in $LOGFILES_THROUGHPUT; do
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
            # check throughput log status 
            if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
               [ $(grep 'Throughput:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
               STATUS_THROUGHPUT="SUCCESS"
            fi
 
            # throughput
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | tail -1 | sed "s/[^0-9]*//g")
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            THROUGHPUT=$(grep 'Throughput:' ${LOGFILE_THROUGHPUT} | awk -F' ' '{print $2}') 
            echo "  Total images/sec: ${THROUGHPUT}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
 
        done

    else
        echo "${MODEL} ${mode} is skipped "
    fi

}

collect_logs_rfcn() {

    echo "***** collect_logs_rfcn *****"
    
    if [ ${MODE} == "inference" ]; then
	
        # check accuracy log status 
        # we may have multiple accuracy log files running on different platform, eg. skx, clx
        for LOGFILE_ACCURACY in $LOGFILES_ACCURACY; do
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
            if [ $(grep 'Processed 5000 images' ${LOGFILE_ACCURACY} | wc -l) != 0 ] &&
               [ $(grep 'Average Precision' ${LOGFILE_ACCURACY} | wc -l) != 0 ]; then
                STATUS_ACCURACY="SUCCESS"
	    fi

            # accuracy
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g" | head -n 1)
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            m_ap=$(grep 'Average Precision' ${LOGFILE_ACCURACY} | head -n 2 |awk '{printf("%s,", $NF)}')
            echo "Average Precision: ${m_ap}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${m_ap};${MODEL}/${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        done

        for LOGFILE_LATENCY in $LOGFILES_LATENCY; do
            case "$LOGFILE_LATENCY" in
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

            # check latency log status 
            STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
            if [ ${DATATYPE} == "fp32" ]; then
                if [ $(grep 'ImportError: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
                   [ $(grep 'Avg. Duration per Step' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
                   STATUS_LATENCY="SUCCESS"
                fi
            else
                if [ $(grep 'ImportError: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
                   [ $(grep 'Avg. Duration per Step:' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
                   STATUS_LATENCY="SUCCESS"
                fi
            fi

            # latency
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size: " ${LOGFILE_LATENCY} | tail -1 | sed "s/[^0-9]*//g" )
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            if [ ${DATATYPE} == "fp32" ]; then
                LATENCY=$(grep 'Avg. Duration per Step:' ${LOGFILE_LATENCY} | awk -F':' '{print $2 * 1000}')
                echo "  Latency: ${LATENCY} msec" >> ${SUMMARYLOG}
            else
                LATENCY=$(grep 'Avg. Duration per Step:' ${LOGFILE_LATENCY} | awk -F':' '{print $2 * 1000}')
                echo "  Latency: ${LATENCY} msec" >> ${SUMMARYLOG}
            fi
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

            # throughput
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            FPS=$(echo | awk -v latency=$LATENCY '{ result= 1000 / latency; printf("%.2f", result) }')
            echo "  Throughput: ${FPS}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        done
            
    else
        echo "${MODEL} ${mode} is skipped "
    fi

}

collect_logs_ssd-mobilenet() {

    echo "***** collect_logs_ssd-mobilenet *****"

    if [ ${MODE} == "inference" ]; then
	
        # check accuracy log status 
        for LOGFILE_ACCURACY in $LOGFILES_ACCURACY; do
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
               [ $(grep 'Average Precision' ${LOGFILE_ACCURACY} | wc -l) != 0 ]; then
                STATUS_ACCURACY="SUCCESS"
	        fi

            # accuracy
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g" | head -n 1)
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            m_ap=$(grep 'Average Precision' ${LOGFILE_ACCURACY} | head -n 2 |awk '{printf("%s,", $NF)}')
            echo "Average Precision: ${m_ap}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}


            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${m_ap};${MODEL}/${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        done


        for LOGFILE_THROUGHPUT in $LOGFILES_THROUGHPUT; do
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

                if [ ${DATATYPE} == "fp32" ]; then
                    STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
                    STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
                    if [ $(grep 'ERROR: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
                    [ $(grep 'Total samples/sec:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                    STATUS_LATENCY="SUCCESS"
                    STATUS_THROUGHPUT="SUCCESS"
                    fi

                    echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
                    BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" |head -n 1)
                    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                    LATENCY=$(grep "Time spent per BATCH:" ${LOGFILE_THROUGHPUT} | awk -F':    ' '{print $2}'| awk -F' ' '{print $1}')
                    LA_MS=$(echo | awk -v l=$LATENCY '{ printf("%.2f", l) }')
                    echo "  Latency ms: ${LA_MS}" >> ${SUMMARYLOG}
                    echo " " >> ${SUMMARYLOG}
                    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

                    echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                    FPS=$(grep "Total samples/sec:" ${LOGFILE_THROUGHPUT} | awk -F':    ' '{print $2}'| awk -F' ' '{print $1}')
                    FPS=$(echo | awk -v l=$FPS '{ printf("%.2f", l) }')
                    echo "  Total images/sec: ${FPS}" >> ${SUMMARYLOG}
                    echo " " >> ${SUMMARYLOG}
                    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

                else
                    STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
                    STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
                    if [ $(grep 'ERROR: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
                    [ $(grep 'Total samples/sec:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                    STATUS_LATENCY="SUCCESS"
                    STATUS_THROUGHPUT="SUCCESS"
                    fi

                    echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
                    BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" |head -n 1)
                    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                    LATENCY=$(grep "Time spent per BATCH:" ${LOGFILE_THROUGHPUT} | awk -F':    ' '{print $2}'| awk -F' ' '{print $1}')
                    LA_MS=$(echo | awk -v l=$LATENCY '{ printf("%.2f", l) }')
                    echo "  Latency ms: ${LA_MS}" >> ${SUMMARYLOG}
                    echo " " >> ${SUMMARYLOG}
                    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LA_MS};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}


                    echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                    FPS=$(grep "Total samples/sec:" ${LOGFILE_THROUGHPUT} | awk -F':    ' '{print $2}'| awk -F' ' '{print $1}')
                    FPS=$(echo | awk -v l=$FPS '{ printf("%.2f", l) }')
                    echo "  Total images/sec: ${FPS}" >> ${SUMMARYLOG}
                    echo " " >> ${SUMMARYLOG}
                    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
                fi

            done


    else
        echo "${MODEL} ${mode} is skipped "
    fi

}

collect_logs_ssd-resnet34() {
    echo "***** collect_logs_ssd-resnet34 *****"

    if [ ${MODE} == "inference" ]; then

        # check accuracy log status
        for LOGFILE_ACCURACY in $LOGFILES_ACCURACY; do
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
               [ $(grep 'Average Precision' ${LOGFILE_ACCURACY} | wc -l) != 0 ]; then
                STATUS_ACCURACY="SUCCESS"
	        fi

            # accuracy
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g" | head -n 1)
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            m_ap=$(grep 'Average Precision' ${LOGFILE_ACCURACY} | head -n 2 |awk '{printf("%s,", $NF)}')
            echo "Average Precision: ${m_ap}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}


            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${m_ap};${MODEL}/${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        done


        for LOGFILE_THROUGHPUT in $LOGFILES_THROUGHPUT; do
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

                STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
                STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
                if [ $(grep 'ERROR: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
	            [ $(grep 'Total samples/sec' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
	            STATUS_LATENCY="SUCCESS"
	            STATUS_THROUGHPUT="SUCCESS"
	            fi

                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
	            BATCH_SIZE=$(grep "Batchsize:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" |head -n 1)
       	        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
	            LATENCY=$(grep "Time spent per BATCH:" ${LOGFILE_THROUGHPUT}  | awk -F':    ' '{print $2}' |awk -F' ' '{print $1}'| head -n 1)
                LATENCY=$(echo | awk -v value=$LATENCY  '{printf("%.2f", value) }')
                echo "  Latency ms: ${LATENCY}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

                # currently failing
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                FPS=$(grep 'Total samples/sec' ${LOGFILE_THROUGHPUT} | awk -F':   ' '{print $2}' |awk -F' ' '{print $1}'| head -n 1)
                FPS=$(echo | awk -v value=$FPS  '{printf("%.2f", value) }')
                echo "  Throughput: ${FPS}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

            done

    else
        echo "${MODEL} ${mode} is skipped "
    fi

}

collect_logs_ssd_vgg16() {

    echo "***** collect_logs_ssd_vgg16 *****"

    if [ ${MODE} == "inference" ]; then
	
        # check accuracy log status 
        for LOGFILE_ACCURACY in $LOGFILES_ACCURACY; do
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

            # check accuracy status
            STATUS_ACCURACY="CHECK_LOG_FOR_ERROR"
            if [ $(grep 'ImportError' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
               [ $(grep 'Average Precision' ${LOGFILE_ACCURACY} | wc -l) != 0 ]; then
                STATUS_ACCURACY="SUCCESS"
            fi

            # accuracy 
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g" | head -n 1)
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            grep 'Average Precision' ${LOGFILE_ACCURACY} | head -n 2 >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            
            m_ap=$(grep 'Average Precision' ${LOGFILE_ACCURACY} | head -n 2 |awk '{printf("%s,", $NF)}')
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${m_ap};${MODEL}/${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        done

        for LOGFILE_THROUGHPUT in $LOGFILES_THROUGHPUT; do
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

            STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
            STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
            if [ $(grep 'ERROR: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
	       [ $(grep 'Latency:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] && 
               [ $(grep 'Throughput:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
	        STATUS_LATENCY="SUCCESS"
	        STATUS_THROUGHPUT="SUCCESS"
	    fi

            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
	    BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" |head -n 1)
       	    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
	    LATENCY=$(grep "Latency:" ${LOGFILE_THROUGHPUT} | awk -F': ' '{print $2}' | awk -F ' ' '{print $1}') 
            echo "  Latency ms: ${LATENCY}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            THROUGHPUT=$(grep "Throughput:" ${LOGFILE_THROUGHPUT} | awk -F': ' '{print $2}' | awk -F ' ' '{print $1}') 
            echo "  Throughput images/sec: ${THROUGHPUT}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
        
        done

    else
        echo "${MODEL} ${mode} is skipped "
    fi

}

collect_logs_transformer_language() {

    echo "***** collect_logs_transformer_language *****"

    if [ ${MODE} == "inference" ]; then
	
        if [ ${DATATYPE} == "fp32" ]; then

            # not running accuracy
            # echo "${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
            # BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g" | head -n 1)
            # echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
	    # ACCURACY=$(grep 'Processed 50000 images' ${LOGFILE_ACCURACY} | awk -F= '{print $2}')
	    # echo "  Accuracy: ${ACCURACY}" >> ${SUMMARYLOG}
	    # echo " " >> ${SUMMARYLOG}

            for LOGFILE_LATENCY in $LOGFILES_LATENCY; do
                case "$LOGFILE_LATENCY" in
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

                # check log status 
                STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
                if [ $(grep 'ImportError: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] && 
                   [ $(grep 'Inference time' ${LOGFILE_LATENCY} | wc -l) != 0 ] && 
                   [ $(grep 'Latency =' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
                    STATUS_LATENCY="SUCCESS"
                fi 

                # latency: batch size 1
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                LATENCY=`grep "Latency =" ${LOGFILE_LATENCY} | awk -F'= ' '{print $2}' | awk -F' ' '{print $1}'`
                echo "  Latency ms/sentences:  ${LATENCY}"  >> ${SUMMARYLOG}
                batch_size_1_throughput=$(echo | awk -v value=$LATENCY  '{ result=1000/value; printf("%.2f", result) }')
                echo "  Throughput sentences/sec: ${batch_size_1_throughput}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            done

            for LOGFILE_THROUGHPUT in $LOGFILES_THROUGHPUT; do
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

                STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
                if [ $(grep 'ImportError: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] && 
                   [ $(grep 'Inference time' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] && 
                   [ $(grep 'Throughput =' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                    STATUS_THROUGHPUT="SUCCESS"
                fi

                # throughput
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                # throughput batch size 32
                BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                THROUGHPUT=`grep "Throughput =" ${LOGFILE_THROUGHPUT} | awk -F'= ' '{print $2}' | awk -F' ' '{print $1}'`
                echo "  Total sentences/sec:  ${THROUGHPUT}"  >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            done

        else // DATATYPE is int8

        fi

    else
        echo "${MODEL} ${mode} is skipped "
    fi

}

collect_logs_wide_deep() {

    echo "***** collect_logs_wide_deep *****"

    if [ ${MODE} == "inference" ]; then
	
        if [ ${DATATYPE} == "fp32" ]; then

            # not running accuracy
	    # echo "${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
            # BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g" | head -n 1)
            # echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
	    # ACCURACY=$(grep 'Processed 50000 images' ${LOGFILE_ACCURACY} | awk -F= '{print $2}')
	    # echo "  Accuracy: ${ACCURACY}" >> ${SUMMARYLOG}
	    # echo "  Accuracy: not running accuracy" >> ${SUMMARYLOG}
	    # echo " " >> ${SUMMARYLOG}

            for LOGFILE_LATENCY in $LOGFILES_LATENCY; do
                case "$LOGFILE_LATENCY" in
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

                # check log status 
                STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
                if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
                   [ $(grep 'Latency is: %s ' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
                   STATUS_LATENCY="SUCCESS"
                fi

                # latency
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_LATENCY} | tail -1 | sed "s/[^0-9]*//g" )
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                LATENCY=$(grep 'Latency is: ' ${LOGFILE_LATENCY} | sed "s/[^0-9.]*//g")
                LATENCY=$(echo | awk -v value=$LATENCY  '{ result=value * 1000; printf("%.2f", result) }')
                echo "  Latency ms: ${LATENCY}" >> ${SUMMARYLOG}
                BATCH_SIZE_1_THROUGHPUT=$(echo | awk -v value=$LATENCY  '{ result=1000/value; printf("%.2f", result) }')
                echo "  Throughput records/sec: ${BATCH_SIZE_1_THROUGHPUT}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            done
            
            for LOGFILE_THROUGHPUT in $LOGFILES_THROUGHPUT; do
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

                STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
                if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
                   [ $(grep 'Throughput is: %s ' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                   STATUS_THROUGHPUT="SUCCESS"
                fi

                # throughput
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | tail -1 | sed "s/[^0-9]*//g")
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                THROUGHPUT=$(grep 'Throughput is: ' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
                THROUGHPUT=$(echo | awk -v value=$THROUGHPUT  '{ result=value; printf("%.2f", result) }')
                echo "  Total records/sec: ${THROUGHPUT}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            done

        else // DATATYPE is int8

        fi

    else
        echo "${MODEL} ${mode} is skipped "
    fi

}

collect_logs_bert(){
    echo "***** collect_logs_bert *****"

    if [ ${MODE} == "inference" ]; then

        for LOGFILE_THROUGHPUT in $LOGFILES_THROUGHPUT; do
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

            STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
            STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
            if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
               [ $(grep 'Avg Latency time of 1000 times ' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
               STATUS_THROUGHPUT="SUCCESS"
               STATUS_LATENCY="SUCCESS"
            fi

            # Latency
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" |head -n 1)
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            LATENCY=$(grep "Avg Latency time of 1000 times " ${LOGFILE_THROUGHPUT} | awk -F'= ' '{print $2}' | awk -F' ' '{print $1}')
            LA_MS=$(echo | awk -v t=$LATENCY '{ printf("%.2f", t)}')
            echo "  Latency ms: ${LA_MS}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LA_MS};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

            # Throughput
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            FPS=$(echo | awk -v latency=$LATENCY '{ result= 1000 / latency; printf("%.2f", result) }')
            echo "  Total images/sec: ${FPS}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
        done

    else
        echo "${MODEL} ${mode} is skipped "
    fi
}

collect_logs_wide_deep_large_ds(){
    echo "***** collect_logs_wide_deep_large_ds *****"

    if [ ${MODE} == "inference" ]; then

        # check accuracy log status
        for LOGFILE_ACCURACY in $LOGFILES_ACCURACY; do
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

            # check accuracy status
            STATUS_ACCURACY="CHECK_LOG_FOR_ERROR"
            if [ $(grep 'ImportError' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
               [ $(grep 'Classification accuracy' ${LOGFILE_ACCURACY} | wc -l) != 0 ]; then
                STATUS_ACCURACY="SUCCESS"
            fi

            # accuracy
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g" | head -n 1)
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            grep 'Classification accuracy' ${LOGFILE_ACCURACY} | head -n 2 >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}

            m_ap=$(grep 'Classicification accuracy' ${LOGFILE_ACCURACY} | head -n 2 |awk '{printf("%s,", $NF)}')
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${m_ap};${MODEL}/${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        done

        for LOGFILE_LATENCY in $LOGFILES_LATENCY; do
            case "$LOGFILE_LATENCY" in
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

            STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
            if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
               [ $(grep 'Average Latency ' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
               STATUS_LATENCY="SUCCESS"
            fi

            # Latency
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" |head -n 1)
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            LATENCY=$(grep "Average Latency " ${LOGFILE_LATENCY} | awk -F':  ' '{print $2}')
            LA_MS=$(echo | awk -v t=$LATENCY '{ printf("%.2f", t)}')
            echo "  Latency ms: ${LA_MS}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LA_MS};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
        done

        for LOGFILE_THROUGHPUT in $LOGFILES_THROUGHPUT; do
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

            STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
            if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
               [ $(grep 'Throughput is ' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
               STATUS_THROUGHPUT="SUCCESS"
            fi

            # Throughput
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" |head -n 1)
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            FPS=$(grep "Throughput is " ${LOGFILE_THROUGHPUT} | awk -F':  ' '{print $2}')
            echo "  Total records/sec: ${FPS}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
        done

    else
        echo "${MODEL} ${mode} is skipped "
    fi
}

collect_logs_densenet169() {

    echo "***** collect_logs_densenet169 *****"

    if [ ${MODE} == "inference" ]; then

        if [ ${DATATYPE} == "fp32" ]; then

            for LOGFILE_ACCURACY in $LOGFILES_ACCURACY; do
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

                # check log status
                STATUS_ACCURACY="CHECK_LOG_FOR_ERROR"
                if [ $(grep 'Error: ' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
                   [ $(grep 'Iteration time:' ${LOGFILE_ACCURACY} | wc -l) != 0 ]; then
                          STATUS_ACCURACY="SUCCESS"
                fi

                # accuracy
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g" | head -n 1)
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
	              ACCURACY=$(grep "Iteration time:" -A 1 ${LOGFILE_ACCURACY} | tail -n 1)
                echo "  Accuracy: ${ACCURACY}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${ACCURACY};${MODEL}/${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

            done

            for LOGFILE_LATENCY in $LOGFILES_LATENCY; do
                case "$LOGFILE_LATENCY" in
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

                # check log status
                STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
                if [ $(grep 'ImportError: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
                   [ $(grep 'Latency:' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
                    STATUS_LATENCY="SUCCESS"
                fi

                # latency: batch size 1
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                LATENCY=`grep "Latency:" ${LOGFILE_LATENCY} | cut -d':' -f 2 | cut -d' ' -f 2 | awk 'BEGIN{sum=0}{sum+=$1}END{printf("%.3f\n",sum/NR)}'`
                echo "  Latency ms: ${LATENCY}"  >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            done

            for LOGFILE_THROUGHPUT in $LOGFILES_THROUGHPUT; do
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

                STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
                if [ $(grep 'ImportError: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
                   [ $(grep 'steps = 100' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                    STATUS_THROUGHPUT="SUCCESS"
                fi

                # throughput
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                THROUGHPUT=`grep "steps = " ${LOGFILE_THROUGHPUT} | tail -n 10 | cut -d',' -f 2 | cut -d' ' -f 2 | awk 'BEGIN{sum=0}{sum+=$1}END{printf("%.3f\n",sum/NR)}'`
                echo "  Total images/sec: ${THROUGHPUT}"  >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            done

        else // DATATYPE is int8

        fi

    else
        echo "${MODEL} ${mode} is skipped "
    fi

}

collect_logs_bert_official() {

    echo "***** collect_logs_bert_official *****"

    if [ ${MODE} == "inference" ]; then

        if [ ${DATATYPE} == "fp32" ]; then

            for LOGFILE_LATENCY in $LOGFILES_LATENCY; do
                case "$LOGFILE_LATENCY" in
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

                # check log status
                STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
                if [ $(grep 'ImportError: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
                   [ $(grep 'latency_per_step = ' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
                    STATUS_LATENCY="SUCCESS"
                fi

                # latency: batch size 1
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                LATENCY=`grep "INFO:tensorflow:  latency_per_step = " ${LOGFILE_LATENCY} | cut -d'=' -f 2 | cut -d' ' -f 2`
                FPS=$(echo | awk -v latency=$LATENCY '{ result= 1000 * latency; printf("%.3f", result) }')
                echo "  Latency ms: ${FPS}"  >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${FPS};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            done

            for LOGFILE_THROUGHPUT in $LOGFILES_THROUGHPUT; do
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

                    STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
                    if [ $(grep 'ImportError: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
                       [ $(grep 'samples_per_sec = ' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                        STATUS_THROUGHPUT="SUCCESS"
                    fi

                    # throughput
                    echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                    BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
                    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                    THROUGHPUT=`grep "INFO:tensorflow:  samples_per_sec = " ${LOGFILE_THROUGHPUT} | cut -d'=' -f 2 | cut -d' ' -f 2`
                    FPS=$(echo | awk -v throughput=$THROUGHPUT '{ result= throughput; printf("%.3f", result) }')
                    echo "  Total samples/sec: ${FPS}"  >> ${SUMMARYLOG}
                    echo " " >> ${SUMMARYLOG}
                    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

                    # accuracy
                    echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                    BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
                    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                    ACCURACY=`grep "INFO:tensorflow:  eval_accuracy = " ${LOGFILE_THROUGHPUT} | cut -d'=' -f 2 | cut -d' ' -f 2`
                    echo "  Accuracy: ${ACCURACY}" >> ${SUMMARYLOG}
                    echo " " >> ${SUMMARYLOG}
                    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${ACCURACY};${MODEL}/${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            done

        else // DATATYPE is int8

        fi

    else
        echo "${MODEL} ${mode} is skipped "
    fi

}

case $MODEL in
    "bert")
       echo "model is $MODEL"
       collect_logs_bert
       ;;
    "fastrcnn")
       echo "model is $MODEL"
       collect_logs_fastrcnn
       ;;
    "inceptionv3")
       echo "model is $MODEL"
       collect_logs_inceptionv3
       ;;
    "inceptionv4")
       echo "model is $MODEL"
       collect_logs_inceptionv3
       ;;
    "inception_resnet_v2")
       echo "model is $MODEL"
       collect_logs_inceptionv3
       ;;
    "maskrcnn")
       echo "model is $MODEL"
       collect_logs_maskrcnn
       ;;
    "mlperf_gnmt")
       echo "model is $MODEL"
       collect_logs_mlperf_gnmt
       ;;
    "mobilenet_v1")
       echo "model is $MODEL"
       collect_logs_mobilenet_v1
       ;;
    "ncf")
       echo "model is $MODEL"
       collect_logs_ncf
       ;;
    "resnet50")
       echo "model is $MODEL"
       collect_logs_resnet50
       ;;
    "resnet50v1_5")
       echo "model is resnet50v1_5" 
       collect_logs_resnet50v1_5 
       ;;
    "resnet101")
       echo "model is $MODEL"
       collect_logs_resnet101
       ;;
    "rfcn")
       echo "model is $MODEL"
       collect_logs_rfcn
       ;;
    "ssd-mobilenet")
       echo "model is $MODEL"
       collect_logs_ssd-mobilenet
       ;;
    "ssd-resnet34")
       echo "model is $MODEL"
       collect_logs_ssd-resnet34
       ;;
    "ssd_vgg16")
       echo "model is $MODEL"
       collect_logs_ssd_vgg16
       ;;
    "transformer_language")
       echo "model is $MODEL"
       collect_logs_transformer_language
       ;;
    "wide_deep")
       echo "model is $MODEL"
       collect_logs_wide_deep
       ;;
    "wide_deep_large_ds")
       echo "model is $MODEL"
       collect_logs_wide_deep_large_ds
       ;;
    "densenet169")
       echo "model is $MODEL"
       collect_logs_densenet169
       ;;
     "bert_official")
       echo "model is $MODEL"
       collect_logs_bert_official
       ;;
    *)
       echo "model $MODEL is not recognized"
       ;;
esac
