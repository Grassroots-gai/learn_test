#!/bin/bash

#collect_logs_intel_models.sh --model=[inceptionv3/mobilenet_v1/resnet50/resnet101] --mode=[training/inference] --datatype=[fp32/int8]
# this script collects the benchmark numbers for models runs from intel-model zoo, including:
# models:
#     - faster_rcnn(py2, fp32)
#     - inceptionv3(py2, fp32/int8)
#     - inceptionv4(py2, fp32/int8)
#     - maskrcnn(py3, fp32)
#     - mobilenet_v1, fixed on develop branch
#     - NCF 
#     - resnet50(py2, fp32/int8)
#     - resnet50v1_5(py2, fp32/int8)
#     - resnet101(py2, fp32/int8)
#     - rfcn(py2, fp32/int8
#     - ssd-mobilenet(py2, fp32/int8)
#     - ssd_vgg16(py3, fp32/int8))
#     - transformer_language
#     - wide_deep(py2, fp32)
# 
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

collect_logs_faster_rcnn() {

    echo "***** collect_logs_faster_rcnn *****"

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
                LATENCY=$(grep "Time spent per BATCH:" ${LOGFILE_LATENCY} | awk -F': ' '{print $2}' | awk -F' ' '{print $1*1000}')
                echo "  Latency msec/images: ${LATENCY}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            
                # throughput
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                THROUGHPUT=$(echo | awk -v tmp=$LATENCY '{ result= 1 / tmp * 1000; printf("%.2f", result) }')
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
                LATENCY=$(grep 'Avg. Duration per Step:' ${LOGFILE_LATENCY} | tail -n 1 | sed "s/.*://g" |awk '{print $1*1000}')
                echo "  Latency msec/images: ${LATENCY}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

                # throughput
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                THROUGHPUT=$(echo | awk -v value=$LATENCY  '{ result=1/value*1000; printf("%.2f", result) }')
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
                # if [ ${MODEL} == "inceptionv3" ] || [ ${MODEL} == "inception_resnet_v2" ]; then
                #     LATENCY=$(grep 'Latency:' ${LOGFILE_LATENCY} | sed "s/[^0-9.]*//g")
                #     echo "  Latency ms: ${LATENCY}" >> ${SUMMARYLOG}
                # elif [ ${MODEL} == "inceptionv4" ]; then
                #     grep 'steps = 50,' ${LOGFILE_LATENCY} >> ${SUMMARYLOG}
                # fi
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
                    # grep 'steps = 50,' ${LOGFILE_THROUGHPUT} >> ${SUMMARYLOG}
                    THROUGHPUT=$(grep 'steps = 50,' ${LOGFILE_THROUGHPUT} |tail -1 |awk '{print $4}')
                    echo "  images/sec: ${THROUGHPUT}" >> ${SUMMARYLOG}
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
                if [ ${MODEL} == "inception_resnet_v2" ]; then
                    BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_LATENCY} | tail -1 | sed "s/[^0-9]*//g")
                    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                    LATENCY=$(grep 'Latency:' ${LOGFILE_LATENCY} | sed "s/[^0-9.]*//g")
                    echo "  Latency ms: ${LATENCY}" >> ${SUMMARYLOG}
                    echo " " >> ${SUMMARYLOG}
		    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
                else
                    BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
                    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                    grep 'steps = 50,' ${LOGFILE_LATENCY} >> ${SUMMARYLOG}
                    echo " " >> ${SUMMARYLOG}
		    THROUGHPUT=$(grep 'steps = 50,' ${LOGFILE_LATENCY} |cut -f4 -d' ')
                    LATENCY=$(echo |awk -v bs=${BATCH_SIZE} -v fps=${THROUGHPUT} '{printf("%.3f",bs/fps*1000)}')
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
                if [ ${MODEL} == "inception_resnet_v2" ]; then
                    BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | tail -1 | sed "s/[^0-9]*//g")
                    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                    THROUGHPUT=$(grep 'Throughput:' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
                    echo "  Total images/sec: ${THROUGHPUT}" >> ${SUMMARYLOG}
                    echo " " >> ${SUMMARYLOG}
                    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
                else
                    BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
                    echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                    grep 'steps = 50,' ${LOGFILE_THROUGHPUT} >> ${SUMMARYLOG}
                    echo " " >> ${SUMMARYLOG}
		    THROUGHPUT=$(grep 'steps = 50,' ${LOGFILE_THROUGHPUT} |cut -f4 -d' ')
                    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
                fi
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

        if [ ${DATATYPE} == "fp32" ]; then
            
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
                   [ $(grep 'Latency ms/step = ' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
                    STATUS_LATENCY="SUCCESS"
                fi

                # latency
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_LATENCY} | tail -1 | sed "s/[^0-9]*//g")
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                LATENCY=$(grep 'Latency ms/step = ' ${LOGFILE_LATENCY} | awk -F'= ' '{print $2}')
                echo "  Latency ms: ${LATENCY}" >> ${SUMMARYLOG}
                THROUGHPUT=$(grep 'Total images/sec =' ${LOGFILE_THROUGHPUT} | awk -F'= ' '{print $2}') 
                echo "  Total images/sec: ${THROUGHPUT}" >> ${SUMMARYLOG}
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
                   [ $(grep 'Total images/sec = ' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                    STATUS_THROUGHPUT="SUCCESS"
                fi
           
                # throughput
                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | tail -1 | sed "s/[^0-9]*//g")
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                THROUGHPUT=$(grep 'Total images/sec =' ${LOGFILE_THROUGHPUT} | awk -F'= ' '{print $2}') 
                echo "  Total images/sec: ${THROUGHPUT}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
   
            done

        else // DATATYPE is int8
        fi


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
                   [ $(grep 'Average time per step:' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
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
                LATENCY=$(grep 'Average time per step:' ${LOGFILE_LATENCY} | awk -F':' '{print $2 * 1000}')
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
	    ACCURACY=$(grep 'Average Precision' ${LOGFILE_ACCURACY} | head -n 2 |awk '{printf("%s,", $NF)}')

            echo "  Accuracy: ${ACCURACY}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${ACCURACY};${MODEL}/${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
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
	           [ $(grep 'Latency:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
	            STATUS_LATENCY="SUCCESS"
	            STATUS_THROUGHPUT="SUCCESS"
	        fi

                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
	        BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" |head -n 1)
       	        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
	        LATENCY=$(grep "Latency:" ${LOGFILE_THROUGHPUT} | awk -F', ' '{print $3}' | awk -F'= ' '{print $2}') 
                echo "  Latency: ${LATENCY}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                FPS=$(echo | awk -v latency=$LATENCY '{ result= 1000 / latency; printf("%.2f", result) }')
                echo "  Throughput: ${FPS}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            else 

                STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
                STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
                if [ $(grep 'ERROR: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
                   [ $(grep 'Avg. Duration ' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
                    STATUS_LATENCY="SUCCESS"
                    STATUS_THROUGHPUT="SUCCESS"
                fi

                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" |head -n 1)
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                LATENCY=$(grep "Avg. Duration " ${LOGFILE_THROUGHPUT} | awk -F':' '{print $2}' | head -n 1)
                LA_MS=$(echo | awk -v l=$LATENCY '{ printf("%.2f", l * 1000) }')
                echo "  Latency ms: ${LA_MS}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LA_MS};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

                echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                FPS=$(echo | awk -v latency=$LA_MS '{ result= 1000 / latency; printf("%.2f", result) }')
                echo "  Total images/sec: ${FPS}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${MODEL}/${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            fi
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

collect_logs_wide_deep_large_ds() {

    echo "***** collect_logs_wide_deep_large_ds *****"

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

        # check accuracy log status
        STATUS_ACCURACY="CHECK_LOG_FOR_ERROR"
    if [ $(grep 'Error: ' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
       [ $(grep 'Classification accuracy' ${LOGFILE_ACCURACY} | wc -l) != 0 ]; then
            STATUS_ACCURACY="SUCCESS"
    fi

        # accuracy
        echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
        BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        ACCURACY=$(grep 'Classification accuracy' ${LOGFILE_ACCURACY} | awk -F ': ' '{print $2}' |sed 's/[^0-9.,]//g')
        ACCURACY=$(echo | awk -v value=$ACCURACY  '{ printf("%.2f", value) }')
        echo "  Accuracy: ${ACCURACY}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}

        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${ACCURACY};${MODEL}/${LOGFILE_ACCURACY##*/}" >> ${SUMMARYTXT}

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
               [ $(grep 'Average Latency (ms/batch) ' ${LOGFILE_LATENCY} | wc -l) != 0 ]; then
               STATUS_LATENCY="SUCCESS"
            fi

            # latency
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_LATENCY} | tail -1 | sed "s/[^0-9]*//g" )
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            LATENCY=$(grep 'Average Latency (ms/batch) ' ${LOGFILE_LATENCY} | sed "s/[^0-9.]*//g")
            LATENCY=$(echo | awk -v value=$LATENCY  '{ printf("%.2f", value) }')
            echo "  Latency ms: ${LATENCY}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${MODEL}/${LOGFILE_LATENCY##*/}" >> ${SUMMARYTXT}
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
               [ $(grep 'Throughput is (records/sec) ' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
               STATUS_THROUGHPUT="SUCCESS"
            fi

            # throughput
            echo "${MACHINE_TYPE} ${MODEL}_${MODE}_${DATATYPE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
            BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | tail -1 | sed "s/[^0-9]*//g")
            echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            THROUGHPUT=$(grep 'Throughput is (records/sec) ' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
            echo "  Total records/sec: ${THROUGHPUT}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${THROUGHPUT};${MODEL}/${LOGFILE_THROUGHPUT##*/}" >> ${SUMMARYTXT}
        done
    else
        echo "${MODEL} ${mode} is skipped "
    fi

}

case $MODEL in
    "faster_rcnn")
       echo "model is $MODEL"
       collect_logs_faster_rcnn
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
    *)
       echo "model $MODEL is not recognized"
       ;;
esac
