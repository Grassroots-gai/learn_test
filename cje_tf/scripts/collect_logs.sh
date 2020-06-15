#!/bin/bash
#collect_logs.sh --model=[resnet50/inception3/vgg16/ds2/SSDvgg16/nmt/mnist/resnet32cifar10/cifar10/dcgan/gnmt] --mode=[training/inference] --fullvalidation=[true|false] --single_socket=[true|false]
set -x

HOST=`uname -n | awk -F'.' '{print $1}'`
SHORTNAME=`echo $HOST | awk -F'-' '{print $2}'`
WORKDIR=`pwd`
source ${WORKDIR}/venv/bin/activate
echo ${WORKDIR}

if [ $# != "4" ]; then
    echo 'ERROR:'
    echo "Expected 1 parameter got $#"
    printf 'Please use following parameters:
    --model=<model to run>
    --mode=training|inference
    --fullvalidation=true|false
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
        --fullvalidation=*)
            FULLVALIDATION=`echo $i | sed "s/${PATTERN}//"`;;
        --single_socket=*)
            SINGLE_SOCKET=`echo $i | sed "s/${PATTERN}//"`;;
        *)
            echo "Parameter $i not recognized."; exit 1;;
    esac
done

LOGFILE=${WORKDIR}/benchmark_${MODEL}_${MODE}_${HOST}.log
CIFAR10EVALLOGFILE=${WORKDIR}/benchmark_${MODEL}_${MODE}_eval_${HOST}.log
SUMMARYLOG=${WORKDIR}/summary_${HOST}.log
DS2_TRAINING_BATCH_SIZE="256"
SSDVGG16_TRAINING_BATCH_SIZE="32"
SSDVGG16_INFERENCE_BATCH_SIZE="224"

THROUGHPUT_LOG=${WORKDIR}/benchmark_${MODEL}_${MODE}_${HOST}_throughput.log
LATENCY_LOG=${WORKDIR}/benchmark_${MODEL}_${MODE}_${HOST}_latency.log
ACCURACY_LOG=${WORKDIR}/benchmark_${MODEL}_${MODE}_${HOST}_accuracy.log

STATUS_THROUGHPUT="CHECK_LOG_FOR_ERROR"
STATUS_LATENCY="CHECK_LOG_FOR_ERROR"
STATUS_ACCURACY="CHECK_LOG_FOR_ERROR"

SUMMARYTXT=${WORKDIR}/summary_nightly.log
if [ $(hostname |grep -i skx |wc -l) -eq 1 ];then 
    MACHINE_TYPE=SKX
elif [ $(hostname |grep -i clx |wc -l) -eq 1 ];then 
    MACHINE_TYPE=CLX
else 
    MACHINE_TYPE=Others
fi 
DATATYPE='fp32'

echo ${LOGFILE}
echo ${THROUGHPUT_LOG}
echo ${LATENCY_LOG}
echo ${ACCURACY_LOG}

if [ ${MODEL} == "cifar10" ]; then
    echo "model is cifar10"
    if [ ${MODE} == "training" ] ; then
        if [ $(grep 'precision' ${CIFAR10EVALLOGFILE} | wc -l) != 0 ]; then
            STATUS="SUCCESS"
        else
            STATUS="CHECK_LOG_FOR_ERROR"
        fi
    else
        echo "cifar10 inference run skipped for now"
        exit 0
    fi

elif [ ${MODEL} == "dcgan" ]; then
    echo "model is DCGAN"
    if [ ${MODE} == "training" ] ; then
        if [ $(grep -i 'Error: ' ${LOGFILE} | wc -l) == 0 ] && 
           [ $(grep -i 'Images/sec' ${LOGFILE} | wc -l) != 0 ] ; then
            STATUS="SUCCESS"
        else
            STATUS="CHECK_LOG_FOR_ERROR"
        fi
    else
        echo "DCGAN inference run skipped for now"
        exit 0
    fi

elif [ ${MODEL} == "ds2" ]; then
    if [ ${MODE} == "training" ] ; then
        if [ $(grep 'examples/sec;' ${LOGFILE} | wc -l) != 0 ]; then
            STATUS="SUCCESS"
        else
            STATUS="FAILURE"
        fi
    else
        echo "ds2 inference run skipped for now"
        exit 0
    fi

elif [ ${MODEL} == "SSDvgg16" ]; then
    if [ ${MODE} == "training" ] ; then
        if [ $(grep 'InvalidArgumentError' ${LOGFILE} | wc -l) == 0 ] && 
           [ $(grep 'ImportError' ${LOGFILE} | wc -l) == 0 ] && 
           [ $(grep -i 'nan' ${LOGFILE} | wc -l) == 0 ] && 
           [ $(grep 'Error: ' ${LOGFILE} | wc -l) == 0 ] && 
           [ $(grep 'Finished training' ${LOGFILE} | wc -l) != 0 ] ; then
            STATUS="SUCCESS"
        else
            STATUS="CHECK_LOG_FOR_ERROR"
        fi
    else
        if [ $(grep 'InvalidArgumentError' ${THROUGHPUT_LOG} | wc -l) == 0 ] && 
           [ $(grep 'ImportError' ${THROUGHPUT_LOG} | wc -l) == 0 ] && 
           [ $(grep -i 'nan' ${THROUGHPUT_LOG} | wc -l) == 0 ] && 
           [ $(grep 'Error: ' ${THROUGHPUT_LOG} | wc -l) == 0 ] && 
           [ $(grep 'Run benchmark batch \[100/100\]:' ${THROUGHPUT_LOG} | wc -l) != 0 ] ; then
            STATUS_THROUGHPUT="SUCCESS"
        fi

        if [ $(grep 'InvalidArgumentError' ${LATENCY_LOG} | wc -l) == 0 ] && 
           [ $(grep 'ImportError' ${LATENCY_LOG} | wc -l) == 0 ] && 
           [ $(grep -i 'nan' ${LATENCY_LOG} | wc -l) == 0 ] && 
           [ $(grep 'Error: ' ${LATENCY_LOG} | wc -l) == 0 ] && 
           [ $(grep 'Run benchmark batch \[1000/1000\]:' ${LATENCY_LOG} | wc -l) != 0 ] ; then
            STATUS_LATENCY="SUCCESS"
        fi

        if [ $(grep 'InvalidArgumentError' ${ACCURACY_LOG} | wc -l) == 0 ] && 
           [ $(grep 'ImportError' ${ACCURACY_LOG} | wc -l) == 0 ] && 
           [ $(grep -i 'nan' ${ACCURACY_LOG} | wc -l) == 0 ] && 
           [ $(grep 'Error: ' ${ACCURACY_LOG} | wc -l) == 0 ] && 
           [ $(grep 'mAP_VOC12' ${ACCURACY_LOG} | wc -l) != 0 ] ; then
            STATUS_ACCURACY="SUCCESS"
        fi

    fi

elif [ ${MODEL} == "nmt" ]; then
    if [ ${MODE} == "training" ] ; then
        if [ $(grep 'ImportError: ' ${LOGFILE} | wc -l) == 0 ] && 
           [ $(grep 'Error: ' ${LOGFILE} | wc -l) == 0 ] && 
           [ $(grep 'Best bleu' ${LOGFILE} | wc -l) != 0 ] ; then
            STATUS="SUCCESS"
        else
            STATUS="CHECK_LOG_FOR_ERROR"
        fi
    else
        if [ $(grep 'ImportError: ' ${LOGFILE} | wc -l) == 0 ] && 
           [ $(grep 'done' ${LOGFILE} | wc -l) != 0 ] && 
           [ $(grep 'num sentences' ${LOGFILE} | wc -l) != 0 ] && 
           [ $(grep 'bleu:' ${LOGFILE} | wc -l) != 0 ] ; then
            STATUS="SUCCESS"
        else
            STATUS="CHECK_LOG_FOR_ERROR"
        fi
    fi

elif [ ${MODEL} == "gnmt" ]; then
    if [ ${MODE} == "training" ] ; then
        if [ $(grep 'ImportError: ' ${LOGFILE} | wc -l) == 0 ] && 
           [ $(grep 'Error: ' ${LOGFILE} | wc -l) == 0 ] ; then
            STATUS="SUCCESS"
        else
            STATUS="CHECK_LOG_FOR_ERROR"
        fi
    else
        if [ $(grep 'ImportError: ' ${LOGFILE} | wc -l) == 0 ] && 
           [ $(grep 'done' ${LOGFILE} | wc -l) != 0 ] && 
           [ $(grep 'num sentences' ${LOGFILE} | wc -l) != 0 ] ; then
            STATUS="SUCCESS"
        else
            STATUS="CHECK_LOG_FOR_ERROR"
        fi
    fi

elif [ ${MODEL} == "mnist" ]; then
    if [ ${MODE} == "training" ] ; then
        if [ $(grep 'ImportError: ' ${LOGFILE} | wc -l) == 0 ] && 
           [ $(grep 'Error: ' ${LOGFILE} | wc -l) == 0 ] && 
           [ $(grep 'test accuracy' ${LOGFILE} | wc -l) != 0 ] ; then
            MNIST_ACCUR=`grep "test accuracy" ${LOGFILE} | awk -F' ' '{print   $3}'`
            MNIST_EXP=0.98
            STATUS=$(echo $MNIST_ACCUR $MNIST_EXP | awk '{if ($1 > $2) print "SUCCESS"; else print "CHECK_LOG_FOR_ERROR" }')
        else
            STATUS="CHECK_LOG_FOR_ERROR"
        fi
    else
        echo "mnist inference run skipped for now"
        exit 0
    fi

elif [ ${MODEL} == "resnet32cifar10" ]; then
    if [ ${MODE} == "training" ] ; then
        if [ $(grep 'ImportError: ' ${LOGFILE} | wc -l) == 0 ] && 
           [ $(grep 'Error: ' ${LOGFILE} | wc -l) == 0 ] && 
           [ $(grep 'accuracy' ${LOGFILE} | wc -l) != 0 ] ; then
            if [ ${FULLVALIDATION} == true ] ; then
                RN32CIFAR10_ACCUR=`grep "'accuracy'" ${LOGFILE} | tail -n 1 | awk -F',' '{print $3}' | awk -F':' '{print $2}' | awk -F'}' '{print $1}'`
                echo $RN32CIFAR10_ACCUR
                RN32CIFAR10_EXP1=0.92
                RN32CIFAR10_EXP2=0.9249
                STATUS=$(echo $RN32CIFAR10_ACCUR $RN32CIFAR10_EXP1 $RN32CIFAR10_EXP2 | awk '{if ($1 < $2) print "FAIL"; else if ($1 > $3) print "SUCCESS"; else print "CHECK_LOG" }')
            else
                RN32CIFAR10_ACCUR=`grep train_accuracy ${LOGFILE} | tail -1 | awk -F',' '{print $3}' | awk -F'=' '{print $2}' | awk -F' ' '{print $1}'`
                echo $RN32CIFAR10_ACCUR
                RN32CIFAR10_EXP=0.80
                STATUS=$(echo $RN32CIFAR10_ACCUR $RN32CIFAR10_EXP | awk '{if ($1 > $2) print "SUCCESS"; else print "CHECK_LOG_FOR_ERROR" }')
            fi
        else
            STATUS="CHECK_LOG_FOR_ERROR"
        fi
    else
        echo "resnet32 with cifar10 inference run skipped for now"
        exit 0
    fi

elif [ $(echo ${LOGFILE} | grep -i ConvNet | wc -l) == 0 ]; then
    if [ $(grep 'ImportError: ' ${LOGFILE} | wc -l) == 0 ] && 
       [ $(grep -i 'nan' ${LOGFILE} | wc -l) == 0 ] && 
       [ $(grep 'Error: ' ${LOGFILE} | wc -l) == 0 ] && 
       [ $(grep 'total images/sec:' ${LOGFILE} | wc -l) != 0 ] ; then
        STATUS="SUCCESS"
    else
        STATUS="CHECK_LOG_FOR_ERROR"
    fi

else
    if [ $(grep 'sec / batch' ${LOGFILE} | wc -l) != 0 ]; then
        STATUS="SUCCESS"
    else
        STATUS="CHECK_LOG_FOR_ERROR"
    fi
fi

tail -5 ${LOGFILE}
#if [ ${STATUS} == "SUCCESS" ]; then
    if [ ${MODEL} == "cifar10" ]; then
        if [ ${MODE} == "training" ] ; then
            echo "${MODEL}_${MODE}: ${STATUS} " >> ${SUMMARYLOG}
            accu=`grep "precision" ${CIFAR10EVALLOGFILE}  | awk -F' ' '{print $7}'`
            accuracy=$(echo | awk -v tmp=$accu '{ result=tmp * 100 ; printf("%.2f", result) }')
            echo "  test accuracy:  $accuracy" >> ${SUMMARYLOG}
            echo "  accuracy:  " >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${accu};${CIFAR10EVALLOGFILE##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        else
            echo "cifar10 inference run skipped for now"
            exit 0
        fi

    elif [ ${MODEL} == "dcgan" ]; then
        if [ ${MODE} == "training" ] ; then
            echo "${MODEL}_${MODE}: ${STATUS} " >> ${SUMMARYLOG}
            batch_size=`grep -i "Batch size" ${LOGFILE}  | awk -F' ' '{print $4}' | awk -F'.' '{print $1}'`
            imagePerSec=`grep -i 'Images/sec' ${LOGFILE} | awk -F' ' '{print $3}'`
            echo "  batch size: $batch_size" >> ${SUMMARYLOG}
            echo "  total images/sec: $imagePerSec" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${imagePerSec};${LOGFILE##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        else
            echo "DCGAN inference run skipped for now"
            exit 0
        fi

    elif [ ${MODEL} == "ds2" ] ; then
        if [ ${MODE} == "training" ] ; then
            echo "${MODEL}_${MODE}: ${STATUS} " >> ${SUMMARYLOG}
            echo "  batch size: ${DS2_TRAINING_BATCH_SIZE}" >> ${SUMMARYLOG}
            count=`grep " step " ${LOGFILE} | wc -l | xargs echo -n`
            echo "line count is $count"
            grep " step " ${LOGFILE} | awk -F' ' '{print $8}' | sed 's/^(//' > tmp.log
            sum=$(awk ' {   for (i=1; i<= NF; i++) sum+=$i } END {print sum}' tmp.log)
            echo "sum is $sum"
            echo "count is ...$count..."
            avg=$(echo | awk -v total=$sum -v count=$count '{ result=total / count; printf("%.2f", result) }')
            echo "  examples/sec:  $avg" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${avg};${LOGFILE##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            rm tmp.log
        fi
    elif [ ${MODEL} == "mnist" ] ; then
        if [ ${MODE} == "training" ] ; then
            echo "${MODEL}_${MODE}: ${STATUS} " >> ${SUMMARYLOG}
            echo "  test accuracy:  ${MNIST_ACCUR}" >> ${SUMMARYLOG}
            result=$(echo $MNIST_ACCUR $MNIST_EXP | awk '{if ($1 > $2) print "accuracy: meet expectation"; else print "accuracy: below expectation" }')
            echo "  $result" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${result};${LOGFILE##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        fi
    elif [ ${MODEL} == "resnet32cifar10" ] ; then
        if [ ${MODE} == "training" ] ; then
            echo "${MODEL}_${MODE}: ${STATUS} " >> ${SUMMARYLOG}
            echo "  test accuracy:  ${RN32CIFAR10_ACCUR}" >> ${SUMMARYLOG}
            if [ ${FULLVALIDATION} == "true" ] ; then 
                result=$(echo $RN32CIFAR10_ACCUR $RN32CIFAR10_EXP1 $RN32CIFAR10_EXP2 | awk '{if ($1 < $2) print "accuracy: not SOTA"; else if ($1 >= $3) print "accuracy: SOTA convergence";  else print "accuracy: ~SOTA, re-run to verify"}')
            else
                result=$(echo $RN32CIFAR10_ACCUR $RN32CIFAR10_EXP | awk '{if ($1 > $2) print "accuracy: convergence looks good"; else print "accuracy: not SOTA" }')
            fi
            echo "  $result" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${RN32CIFAR10_ACCUR};${LOGFILE##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        fi
    elif [ $(echo ${LOGFILE} | grep -i ConvNet | wc -l) == 0 ]; then
        echo "${MODEL}_${MODE}: ${STATUS} " >> ${SUMMARYLOG}
        if [ ${MODEL} == "SSDvgg16" ] ; then
            if [ ${MODE} == "training" ] ; then

                count=`grep "global step" ${LOGFILE} | wc -l | xargs echo -n`
                echo "line count is $count"
                grep "global step" ${LOGFILE} | awk -F' ' '{print $7}' | sed 's/^(//' > tmp.log
                sum=$(awk ' {   for (i=1; i<= NF; i++) sum+=$i } END {print sum}' tmp.log)
                echo "sum is $sum"
                echo "count is ...$count..."
                avg=$(echo | awk -v total=$sum -v count=$count '{ result=total / count; printf("%.2f", result) }')
                imagePerSec=$(echo | awk -v batch=$SSDVGG16_TRAINING_BATCH_SIZE -v tmp=$avg '{ result=batch / tmp; printf("%.2f", result) }')
                echo "avg sec/step:  $avg" 
                echo "  batch size: ${SSDVGG16_TRAINING_BATCH_SIZE}" >> ${SUMMARYLOG}
                echo "  total images/sec: $imagePerSec" >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${imagePerSec};${LOGFILE##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
                rm tmp*.log
            else
                echo "${MODEL}_inference Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}

                # throughput batch size 224 for SSD-VGG16 
                BATCH_SIZE=$(grep "Batch Size:" ${THROUGHPUT_LOG} | sed "s/[^0-9]*//g")
                fps=$(grep 'Run benchmark batch' ${THROUGHPUT_LOG} |sed -e "s+.*: *++;s+ .*++" |awk '
                    BEGIN { i = 0; sum = 0 }
                    {
                        if(NR > 10) {
                            i++;
                            sum+=$1;
                        }
                    }
                    END { avg_fps = sum / i; printf("%.2f", avg_fps) }
                ')
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                echo "Average fps: ${fps} images/sec" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${fps};${THROUGHPUT_LOG##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

                echo "${MODEL}_inference Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
                # latency batch size 1 for SSD-VGG16
                BATCH_SIZE=$(grep -i "Batch Size:" ${LATENCY_LOG} | sed "s/[^0-9]*//g" | head -n 1)
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                fps=$(grep 'Run benchmark batch' ${LATENCY_LOG} |sed -e "s+.*: *++;s+ .*++" |awk '
                    BEGIN { i = 0; sum = 0 }
                    {
                        if(NR > 10) {
                            i++;
                            sum+=$1;
                        }
                    }
                    END { avg_fps = sum / i; printf("%.2f", avg_fps) }
                ')
                latency=$(echo |awk -v bs=${BATCH_SIZE} -v fps=${fps} '{ printf("%.3f", bs / fps * 1000) }')
                echo "Latency ms: ${latency}" >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LATENCY_LOG##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

                #latency: batch size 224
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL}_inference Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
                BATCH_SIZE=$(grep "Batch Size:" ${ACCURACY_LOG} | sed "s/[^0-9]*//g")
                echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
                grep 'mAP_VOC12' ${ACCURACY_LOG} | head -n 2 >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                m_ap=$(grep 'mAP_VOC12' ${ACCURACY_LOG} | head -n 2 |awk '{printf("%s", $3)}')
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${m_ap};${ACCURACY_LOG##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            fi
            echo " " >> ${SUMMARYLOG}
        elif [ ${MODEL} == "nmt" ] ; then
            BATCH_SIZE=$(grep -i "batch_size" ${LOGFILE} | sed "s/[^0-9]*//g" | head -n 1)
            echo "  batch size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            if [ ${MODE} == "training" ] ; then
                count=`grep "wps" ${LOGFILE} | grep -v Final | grep -v Best | wc -l`
                grep "wps" ${LOGFILE} | grep -v Final | grep -v Best | awk -F' ' '{print $8}' | sed -E "s/([0-9]*.[0.9]*)K/\1/" > tmp.log
                sum=$(awk ' {   for (i=1; i<= NF; i++) sum+=$i } END {print sum}' tmp.log)
                echo "sum is $sum"
                echo "count is ...$count..."
                avg=$(echo | awk -v total=$sum -v count=$count '{ result=total / count; printf("%.2f", result) }')
                echo "  total words/sec:  ${avg} K wps"  >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${avg};${LOGFILE##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
                rm tmp.log
            else 
                sec=`grep "bleu:" ${LOGFILE} | awk -F' ' '{print $2}'`
                numSentences=`grep "num sentences" ${LOGFILE} | awk -F' ' '{print $4}' | awk -F',' '{print $1}'`
                avg=$(echo | awk -v sec=$sec -v numSentences=$numSentences '{ result=numSentences / sec; printf("%.2f", result) }')
                if [ ${SINGLE_SOCKET} == "true" ]; then
                    echo "single_socket $avg times 2"
                    avg=$(echo | awk -v value=$avg  '{ result=value * 2; printf("%.2f", result) }')
                fi
                echo "  total sentences/sec:  ${avg}"  >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${avg};${LOGFILE##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            fi
        elif [ ${MODEL} == "gnmt" ] ; then
            BATCH_SIZE=$(grep -i "batch_size" ${LOGFILE} | sed "s/[^0-9]*//g" | head -n 1)
            echo "  batch size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            if [ ${MODE} == "training" ] ; then
                numSentences=`grep "num sentences" ${LOGFILE} | tail -1 | awk -F', ' '{print $2}' | awk -F' ' '{print $3}'`
                time=`grep "num sentences" ${LOGFILE} | tail -1 | awk -F', ' '{print $4}' | awk -F' ' '{print $2}' | sed "s/[^0-9]*//g"`
                echo "num of sentences is $numSentences"
                echo "time is ...$time"
                sentencePerSec=$(echo | awk -v total=$numSentences -v time=$time '{ result=total / time; printf("%.2f", result) }')
                echo "  total sentences/sec:  ${sentencePerSec} "  >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${sentencePerSec};${LOGFILE##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            else 
                numSentences=`grep "num sentences" ${LOGFILE} | tail -1 | awk -F', ' '{print $2}' | awk -F' ' '{print $3}'`
                time=`grep "num sentences" ${LOGFILE} | tail -1 | awk -F', ' '{print $4}' | awk -F' ' '{print $2}' | sed "s/[^0-9]*//g"`
                echo "num of sentences is $numSentences"
                echo "time is ...$time"
                sentencePerSec=$(echo | awk -v total=$numSentences -v time=$time '{ result=total / time; printf("%.2f", result) }')
                if [ ${SINGLE_SOCKET} == "true" ]; then
                    echo "single_socket $sentencePerSec times 2"
                    sentencePerSec=$(echo | awk -v value=$sentencePerSec  '{ result=value * 2; printf("%.2f", result) }')
                fi
                echo "  total sentences/sec:  ${sentencePerSec}"  >> ${SUMMARYLOG}
                echo " " >> ${SUMMARYLOG}
                echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${sentencePerSec};${LOGFILE##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
            fi
        else
            # MODEL resnet50 or inception3
            BATCH_SIZE=$(grep -i "batch size" ${LOGFILE} | sed "s/[^0-9]*//g" | head -n 1)
            echo "  batch size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
            FPS=$(grep 'total images/sec:' ${LOGFILE} | sed "s/[^0-9.]*//g")
            if [ ${MODE} == "inference" ] ; then
                if [ ${SINGLE_SOCKET} == "true" ]; then
                    echo "single_socket $FPS times 2"
                    FPS=$(echo | awk -v value=$FPS  '{ result=value * 2; printf("%.2f", result) }')
                fi
            fi
            echo "  total images/sec: ${FPS}" >> ${SUMMARYLOG}
            echo " " >> ${SUMMARYLOG}
            echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
        fi
    else
        BATCH_SIZE=$(grep -i "batch_size" ${LOGFILE} | sed "s/[^0-9]*//g")
        SUMMARY_TIME=$(grep 'sec / batch' ${LOGFILE} | tail -n 1 | sed "s/.*steps, //" | sed "s/ .*//") #get time
        SUMMARY_TIME=$(echo | awk -v time=$SUMMARY_TIME '{ result = time * 1000; printf("%f", result) }') #convert from seconds to ms
        FPS=$(echo | awk -v time=$SUMMARY_TIME -v batch=$BATCH_SIZE '{ result= batch * 1000 / time; printf("%f", result) }')
    fi
#fi

if [ ${STATUS} == "SUCCESS" ]; then
    exit 0
else
    exit 1
fi

