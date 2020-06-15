#!/bin/bash
#collect_logs.sh --model=[deepSpeech/inception_v4/inception_resnet_v2/fastrcnn/gnmt/rfcn/transformerLanguage/transformerSpeech/wideDeep] --mode=[training/inference] --single_socket=[True|False]
set -x

HOST=`uname -n | awk -F'.' '{print $1}'`
SHORTNAME=`echo $HOST | awk -F'-' '{print $2}'`
WORKDIR=`pwd`


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


if [ ${MODEL} == "inception_v4" ] || [ ${MODEL} == "inception_resnet_v2" ] || [ ${MODEL} == "mobilenet_v1" ]; then

    echo "model is ${MODEL}"

    if [ ${MODE} == "inference" ] ; then
        if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
           [ $(grep 'Total images/sec' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
            STATUS_THROUGHPUT="SUCCESS"
        fi
        if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
           [ $(grep 'Total images/sec' ${LOGFILE_LATENCY} | wc -l) != 0 ] ; then
            STATUS_LATENCY="SUCCESS"
        fi
    else
        echo "${MODEL} training is skipped for now"
        exit 0
    fi

elif [ ${MODEL} == "inceptionv3" ] || [ ${MODEL} == "resnet50" ]; then

    echo "model is ${MODEL}"
    if [ ${MODE} == "inference" ] ; then
        if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
           [ $(grep 'Throughput:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
            STATUS_THROUGHPUT="SUCCESS"
        fi
        if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
           [ $(grep 'Latency:' ${LOGFILE_LATENCY} | wc -l) != 0 ] ; then
            STATUS_LATENCY="SUCCESS"
        fi
    else
        echo "${MODEL} training is skipped for now"
        exit 0
    fi

elif [ ${MODEL} == "deepSpeech" ] ; then

    echo "model is ${MODEL}"

    if [ ${MODE} == "inference" ] ; then

        if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
           [ $(grep 'NotFoundError' ${LOGFILE_LATENCY} | wc -l) == 0 ] && 
           [ $(grep 'examples/sec' ${LOGFILE_LATENCY} | wc -l) != 0 ] && 
           [ $(grep 'sec/batch' ${LOGFILE_LATENCY} | wc -l) != 0 ] ; then
            STATUS_LATENCY="SUCCESS"
            STATUS_THROUGHPUT="SUCCESS"
        fi

    else

        echo "${MODEL} training is skipped for now"
        exit 0

    fi

elif [ ${MODEL} == "fastrcnn" ] ; then

    echo "model is ${MODEL}"

    if [ ${MODE} == "inference" ] ; then

        if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
           [ $(grep 'Time spent per BATCH:' ${LOGFILE_LATENCY} | wc -l) != 0 ] ; then
            STATUS_LATENCY="SUCCESS"
            STATUS_THROUGHPUT="SUCCESS"
        fi

    else

        if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
           [ $(grep 'Finished training' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
            STATUS_THROUGHPUT="SUCCESS"
        fi

    fi

elif [ ${MODEL} == "gnmt" ]; then

    echo "model is ${MODEL}"

    if [ ${MODE} == "inference" ] ; then

        if [ $(grep 'ImportError: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] && 
           [ $(grep 'throughput of the model' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
            STATUS_THROUGHPUT="SUCCESS"
        fi
        if [ $(grep 'ImportError: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] && 
           [ $(grep 'latency of the model' ${LOGFILE_LATENCY} | wc -l) != 0 ] ; then
            STATUS_LATENCY="SUCCESS"
        fi

    else 

        echo "${MODEL} training is skipped for now"
        exit 0

        #if [ $(grep 'ImportError: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] && 
        #   [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] ; then
        #    STATUS_THROUGHPUT="SUCCESS"
        #fi
    fi

elif [ ${MODEL} == "rfcn" ] ; then

    echo "model is ${MODEL}"

    if [ ${MODE} == "inference" ] ; then

        if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
           [ $(grep 'Average time per step:' ${LOGFILE_LATENCY} | wc -l) != 0 ] ; then
            STATUS_LATENCY="SUCCESS"
            STATUS_THROUGHPUT="SUCCESS"
        fi

    else

        echo "${MODEL} training is skipped for now"
        exit 0

    fi

elif [ ${MODEL} == "SqueezeNet" ] || [ ${MODEL} == "YoloV2" ]; then
    if [ ${MODE} == "inference" ] ; then
        if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
           [ $(grep 'throughput\[med\]' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
            STATUS_THROUGHPUT="SUCCESS"
        fi
        if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
           [ $(grep 'latency\[median\]' ${LOGFILE_LATENCY} | wc -l) != 0 ] ; then
            STATUS_LATENCY="SUCCESS"
        fi
    else
        echo "${MODEL} training is skipped for now"
        exit 0
    fi

elif [ ${MODEL} == "transformerLanguage" ]; then

    echo "model is ${MODEL}"

    if [ ${MODE} == "inference" ] ; then

        if [ $(grep 'ImportError: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] && 
           [ $(grep 'Inference time' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] && 
           [ $(grep 'Throughput =' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
            STATUS_THROUGHPUT="SUCCESS"
        fi
        if [ $(grep 'ImportError: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] && 
           [ $(grep 'Inference time' ${LOGFILE_LATENCY} | wc -l) != 0 ] && 
           [ $(grep 'Latency =' ${LOGFILE_LATENCY} | wc -l) != 0 ] ; then
            STATUS_LATENCY="SUCCESS"
        fi

    else 

        echo "${MODEL} training is skipped for now"
        exit 0

    fi

elif [ ${MODEL} == "transformerSpeech" ]; then

    echo "model is ${MODEL}"

    if [ ${MODE} == "inference" ] ; then

        if [ $(grep 'ImportError: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] && 
           [ $(grep 'Completed inference' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] && 
           [ $(grep 'tokens/sec' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then 
            STATUS_THROUGHPUT="SUCCESS"
        fi
        if [ $(grep 'ImportError: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] && 
           [ $(grep 'Completed inference' ${LOGFILE_LATENCY} | wc -l) != 0 ] && 
           [ $(grep 'tokens/sec' ${LOGFILE_LATENCY} | wc -l) != 0 ] ; then 
            STATUS_LATENCY="SUCCESS"
        fi

    else 

        echo "${MODEL} training is skipped for now"
        exit 0

        #if [ $(grep 'ImportError: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] && 
        #   [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
        #   [ $(grep 'Loss for final step: ' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ]; then
        #    STATUS_THROUGHPUT="SUCCESS"
        #fi
    fi

elif [ ${MODEL} == "WaveNet" ] ; then
    if [ ${MODE} == "inference" ] ; then
        if [ $(grep 'Average Throughput' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] &&
           [ $(grep 'Average Latency' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
            STATUS_THROUGHPUT="SUCCESS"
            STATUS_LATENCY="SUCCESS"
        fi
    else
        echo "${MODEL} training is skipped for now"
        exit 0
    fi
elif [ ${MODEL} == "wideDeep" ] ; then
    echo "model is ${MODEL}"

    if [ ${MODE} == "inference" ] ; then
        if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
           [ $(grep 'Throughput is:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
            STATUS_THROUGHPUT="SUCCESS"
        fi
        if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
           [ $(grep 'Latency is:' ${LOGFILE_LATENCY} | wc -l) != 0 ] ; then
            STATUS_LATENCY="SUCCESS"
        fi
    else
        echo "${MODEL} training is skipped for now"
        exit 0
    fi
elif [ ${MODEL} == "WaveNet_Magenta" ] ; then
    if [ ${MODE} == "inference" ] ; then
        if [ $(grep 'Average Samples / sec' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] &&
           [ $(grep 'Average msec / sample:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
            STATUS_THROUGHPUT="SUCCESS"
            STATUS_LATENCY="SUCCESS"
        fi
    else
        echo "${MODEL} training is skipped for now"
        exit 0
    fi
    
# Shanghai OOB Models
elif [ ${MODEL} == "UNet" ] || [ ${MODEL} == "A3C" ] || [ ${MODEL} == "3DGAN" ] || [ ${MODEL} == "DRAW" ] || [ ${MODEL} == "DCGAN" ]; then

    if [ ${MODE} == "inference" ] ; then
        if [ $(grep 'Time spent per BATCH:' ${LOGFILE_LATENCY} | wc -l) != 0 ];then
            STATUS_LATENCY="SUCCESS"
            #STATUS_THROUGHPUT="SUCCESS"
        fi
        
        if [ $(grep 'Total samples/sec:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
            #STATUS_LATENCY="SUCCESS"
            STATUS_THROUGHPUT="SUCCESS"
        fi
    else
        echo "${MODEL} training is skipped for now"
        exit 0
    fi

else
    echo "model is not recognized"
    STATUS_THROUGHPUT="FAILURE"
fi


#tail -5 ${LOGFILE}

if [ ${MODEL} == "inception_v4" ] || [ ${MODEL} == "inception_resnet_v2" ] || [ ${MODEL} == "mobilenet_v1" ]; then

    if [ ${MODE} == "inference" ] ; then

        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}

        # throughput batch size 128 for inception_v4, inception_resnet_v2,  100 for mobilenet
        BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        FPS=$(grep 'Total images/sec' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
        if [ ${SINGLE_SOCKET} == "true" ]; then
            echo "single_socket $FPS times 2"
            FPS=$(echo | awk -v value=$FPS  '{ result=value * 2; printf("%.2f", result) }')
        fi
        echo "  Total images/sec: ${FPS}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        #latency: batch size 1
        echo ""
        echo "${MODEL}_${MODE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
        BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        latency=$(grep 'Latency ms/step' ${LOGFILE_LATENCY} | sed "s/[^0-9.]*//g")
        echo "  Latency ms/step: ${latency}" >> ${SUMMARYLOG}
        throughput=$(grep 'Total images/sec' ${LOGFILE_LATENCY} | sed "s/[^0-9.]*//g")
        echo "  Throughput images/sec: ${throughput}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    fi

elif [ ${MODEL} == "inceptionv3" ] || [ ${MODEL} == "resnet50" ]; then

    if [ ${MODE} == "inference" ] ; then

        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}

        # throughput batch size 128 for inceptionv3, resnet50
        BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        FPS=$(grep 'Throughput:' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
        if [ ${SINGLE_SOCKET} == "true" ]; then
            echo "single_socket $FPS times 2"
            FPS=$(echo | awk -v value=$FPS  '{ result=value * 2; printf("%.2f", result) }')
        fi
        echo "  Total images/sec: ${FPS}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        #latency: batch size 1
        echo ""
        echo "${MODEL}_${MODE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
        BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        latency=$(grep 'Latency:' ${LOGFILE_LATENCY} | sed "s/[^0-9.]*//g")
        echo "  Latency ms/step: ${latency}" >> ${SUMMARYLOG}
        throughput=$(grep 'Throughput:' ${LOGFILE_LATENCY} | sed "s/[^0-9.]*//g")
        echo "  Total images/sec: ${throughput}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
    else
        echo "${MODEL} training is skipped for now"
        exit 0
    fi
elif [ ${MODEL} == "deepSpeech" ] ; then

    if [ ${MODE} == "inference" ] ; then

        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}

        # throughput: batch size 1
        BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        # I 0.0970 examples/sec; 10.304 sec/batch
        EPS=`grep "examples/sec" ${LOGFILE_LATENCY} | awk -F' ' '{print $2}'`
        if [ ${SINGLE_SOCKET} == "true" ]; then
            echo "single_socket $EPS times 2"
            EPS=$(echo | awk -v value=$EPS  '{ result=value * 2; printf("%.2f", result) }')
        fi
        echo "  Total examples/sec: ${EPS}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${EPS};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        # latency: batch size 1
        echo ""
        echo "${MODEL}_${MODE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}

        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        latency=`grep "sec/batch" ${LOGFILE_LATENCY} | awk -F' ' '{print $4}'`
        echo "  Latency sec/batch: ${latency}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    fi

elif [ ${MODEL} == "fastrcnn" ] ; then

    if [ ${MODE} == "inference" ] ; then

        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}

        # throughput: batch size 1
        BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        timeSpent=`grep "Time spent per BATCH:" ${LOGFILE_LATENCY} | awk -F' ' '{print $5}'`
        echo "timeSpent: ${timeSpent}"
        imagePerSec=$(echo | awk -v batch=$BATCH_SIZE -v tmp=$timeSpent '{ result=batch / tmp; printf("%.2f", result) }')
        echo " imagePerSec: $imagePerSec"
        if [ ${SINGLE_SOCKET} == "true" ]; then
            echo "single_socket $imagePerSec times 2"
            imagePerSec=$(echo | awk -v value=$imagePerSec  '{ result=value * 2; printf("%.2f", result) }')
        fi
        echo "  Total images/sec: ${imagePerSec}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${imagePerSec};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        # latency: batch size 1
        echo ""
        echo "${MODEL}_${MODE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}

        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        latency=`grep "Time spent per BATCH:" ${LOGFILE_LATENCY} | awk -F' ' '{print $5}'`
        echo "  Latency sec/step: ${latency}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    else

        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
        BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        msecStep=`grep 'global step' ${LOGFILE_THROUGHPUT} |sed -e "s+.*(++;s+ .*++" |awk '
            BEGIN { i = 0; sum = 0 }
            {
                if(NR > 10) {
                    i++;
                    sum+=$1;
                }
            }
            END {
                avg_ms = sum / i * 1000;
                printf("%.3f", avg_ms)
            }
        '`
        echo "msec/step: ${msecStep}"
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${msecStep};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
        imagePerSec=$(echo | awk -v batch=$BATCH_SIZE -v tmp=$msecStep '{ result=batch / tmp * 1000; printf("%.2f", result) }')
        echo " imagePerSec: $imagePerSec"
        echo "  Total images/sec: ${imagePerSec}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${imagePerSec};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    fi

elif [ ${MODEL} == "gnmt" ] ; then

    if [ ${MODE} == "inference" ] ; then

        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
        # throughput batch size 32
        BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        #numSentences=`grep "num sentences" ${LOGFILE_THROUGHPUT}| awk -F' ' '{print $4}' | awk -F',' '{print $1}'`
        #echo "num of sentences is $numSentences"
        #time=`grep "num sentences" ${LOGFILE_THROUGHPUT} | awk -F' ' '{print $11}' | awk -F's' '{print $1}'`
        #sentencePerSec=$(echo | awk -v total=$numSentences -v time=$time '{ result=total / time; printf("%.2f", result) }')
        throughput=`grep 'throughput of the model' ${LOGFILE_THROUGHPUT} | awk -F' ' '{print $7}'`
        if [ ${SINGLE_SOCKET} == "true" ]; then
            echo "single_socket $throughput times 2"
            sentencePerSec=$(echo | awk -v value=$throughput  '{ result=value * 2; printf("%.2f", result) }')
        fi
        echo "  total sentences/sec:  ${sentencePerSec}"  >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${sentencePerSec};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        # latency: batch size 1
        echo ""
        echo "${MODEL}_${MODE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
        BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        #numSentences=`grep "num sentences" ${LOGFILE_LATENCY}| awk -F' ' '{print $4}' | awk -F',' '{print $1}'`
        #echo "num of sentences is $numSentences"
        #time=`grep "num sentences" ${LOGFILE_LATENCY} | awk -F' ' '{print $11}' | awk -F's' '{print $1}'`
        #sentencePerSec=$(echo | awk -v total=$numSentences -v time=$time '{ result=total / time; printf("%.2f", result) }')
        latency=`grep 'latency of the model' ${LOGFILE_LATENCY} | awk -F' ' '{print $7}'`
        echo "  latency ms/sentences:  ${latency}"  >> ${SUMMARYLOG}
        throughput=$(echo | awk -v value=$latency  '{ result=1000 / value; printf("%.2f", result) }')
        echo "  throughput sentences/sec: ${throughput}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    else

        echo "${MODEL} training is skipped for now"
        exit 0
        #numSentences=`grep "num sentences" ${LOGFILE_THROUGHPUT} | tail -1 | awk -F', ' '{print $2}' | awk -F' ' '{print $3}'`
        #time=`grep "num sentences" ${LOGFILE_THROUGHPUT} | tail -1 | awk -F', ' '{print $4}' | awk -F' ' '{print $2}' | sed "s/[^0-9]*//g"`
        #echo "num of sentences is $numSentences"
        #echo "time is ...$time"
        #sentencePerSec=$(echo | awk -v total=$numSentences -v time=$time '{ result=total / time; printf("%.2f", result) }')
        #echo "  total sentences/sec:  ${sentencePerSec} "  >> ${SUMMARYLOG}
        #echo " " >> ${SUMMARYLOG}

    fi

elif [ ${MODEL} == "rfcn" ] ; then

    if [ ${MODE} == "inference" ] ; then

        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}

        # throughput: batch size 1
        BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        timeSpent=`grep "Average time per step:" ${LOGFILE_LATENCY} | awk -F' ' '{print $5}'`
        echo "timeSpent: ${timeSpent}"
        imagePerSec=$(echo | awk -v batch=$BATCH_SIZE -v tmp=$timeSpent '{ result=batch / tmp; printf("%.2f", result) }')
        echo " imagePerSec: $imagePerSec"
        if [ ${SINGLE_SOCKET} == "true" ]; then
            echo "single_socket $imagePerSec times 2"
            imagePerSec=$(echo | awk -v value=$imagePerSec  '{ result=value * 2; printf("%.2f", result) }')
        fi
        echo "  Total images/sec: ${imagePerSec}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${imagePerSec};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        # latency: batch size 1
        echo ""
        echo "${MODEL}_${MODE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}

        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        latency=`grep "Average time per step:" ${LOGFILE_LATENCY} | awk -F' ' '{print $5*1000}'`
        echo "  Latency ms/step: ${latency}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    fi

elif [ ${MODEL} == "SqueezeNet" ] ; then
    if [ ${MODE} == "inference" ] ; then
        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}

        # throughput batch size 64
        BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        FPS=$(grep 'throughput\[med\] =' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
        if [ ${SINGLE_SOCKET} == "true" ]; then
            echo "single_socket $FPS times 2"
            FPS=$(echo | awk -v value=$FPS  '{ result=value * 2; printf("%.2f", result) }')
        fi
        echo "  throughput[med] = ${FPS}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        #latency: batch size 1
        echo ""
        echo "${MODEL}_${MODE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
        BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        latency=$(grep 'latency\[median\] =' ${LOGFILE_LATENCY} | sed "s/[^0-9.]*//g")
        echo "  latency[median] = ${latency}" >> ${SUMMARYLOG}
        throughput=$(grep 'throughput\[med\] =' ${LOGFILE_LATENCY} | sed "s/[^0-9.]*//g")
        echo "  throughput[median] = ${throughput}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
    fi

elif [ ${MODEL} == "YoloV2" ] ; then
    if [ ${MODE} == "inference" ] ; then
        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}

        # throughput batch size 8
        BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        FPS=$(grep 'throughput\[med\] =' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
        if [ ${SINGLE_SOCKET} == "true" ]; then
            echo "single_socket $FPS times 2"
            FPS=$(echo | awk -v value=$FPS  '{ result=value * 2; printf("%.2f", result) }')
        fi
        echo "  throughput[med] = ${FPS}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        #latency: batch size 1
        echo ""
        echo "${MODEL}_${MODE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
        BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        latency=$(grep 'latency\[median\] =' ${LOGFILE_LATENCY} | sed "s/[^0-9.]*//g")
        echo "  latency[median] = ${latency}" >> ${SUMMARYLOG}
        throughput=$(grep 'throughput\[med\] =' ${LOGFILE_LATENCY} | sed "s/[^0-9.]*//g")
        echo "  throughput[median] = ${throughput}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
    fi

elif [ ${MODEL} == "transformerLanguage" ] ; then

    if [ ${MODE} == "inference" ] ; then

        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
        # throughput batch size 32
        BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        throughput=`grep "Throughput =" ${LOGFILE_THROUGHPUT} | awk -F'= ' '{print $2}' | awk -F' ' '{print $1}'`
        if [ ${SINGLE_SOCKET} == "true" ]; then
            echo "single_socket $throughput times 2"
            sentencePerSec=$(echo | awk -v value=$throughput  '{ result=value * 2; printf("%.2f", result) }')
        fi
        echo "  total sentences/sec:  ${sentencePerSec}"  >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${sentencePerSec};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        # latency: batch size 1
        echo ""
        echo "${MODEL}_${MODE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
        BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        latency=`grep "Latency =" ${LOGFILE_LATENCY} | awk -F'= ' '{print $2}' | awk -F' ' '{print $1}'`

        echo "  latency ms/sentences:  ${latency}"  >> ${SUMMARYLOG}
        batch_size_1_throughput=$(echo | awk -v value=$latency  '{ result=1000/value; printf("%.2f", result) }')
        echo "  throughput sentences/sec: ${batch_size_1_throughput}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    fi

elif [ ${MODEL} == "transformerSpeech" ] ; then

    if [ ${MODE} == "inference" ] ; then

        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
        # throughput batch size 128
        BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        tokensPerSec=`grep "tokens/sec" ${LOGFILE_THROUGHPUT} | tail -1 | awk -F' ' '{print $5}'`
        if [ ${SINGLE_SOCKET} == "true" ]; then
            echo "single_socket $tokensPerSec times 2"
            tokensPerSec=$(echo | awk -v value=$tokensPerSec  '{ result=value * 2; printf("%.2f", result) }')
        fi
        echo "  total tokens/sec:  ${tokensPerSec}"  >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${tokensPerSec};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        #latency: batch size 1
        echo ""
        echo "${MODEL}_${MODE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
        BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        count=`grep "tokens/sec" ${LOGFILE_LATENCY} | wc -l | xargs echo -n`
        echo "line count is $count"
        grep "tokens/sec" ${LOGFILE_LATENCY} | awk -F' ' '{print $5}' | sed 's/^(//' > tmp.log
        sum=$(awk ' {   for (i=1; i<= NF; i++) sum+=$i } END {print sum}' tmp.log)
        echo "sum is $sum"
        echo "count is ...$count..."
        avg=$(echo | awk -v total=$sum -v count=$count '{ result=total / count; printf("%.2f", result) }')
        echo "avg tokens/sec:  $avg" 
        secPerToken=$(echo | awk -v batch=$BATCH_SIZE -v tmp=$avg '{ result=batch / tmp; printf("%.2f", result) }')
        echo "secPerToken is ...$secPerToken"
        echo "  Latency sec/token:  ${secPerToken}"  >> ${SUMMARYLOG}
        batch_size_1_throughput=$(echo | awk -v value=$secPerToken  '{ result=1/value; printf("%.2f", result) }')
        echo "  throughput token/sec: ${batch_size_1_throughput}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${secPerToken};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
        rm tmp.log

    else

        echo "${MODEL} training is skipped for now"
        exit 0
        #echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
        #BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
        #echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        #secs=`grep "step =" ${LOGFILE_THROUGHPUT} | tail -1 | awk -F'(' '{print $2}' | awk -F' ' '{print $1}'`
        ## batch size is 6M
        #size=6000000
        #recPerSec=$(echo | awk -v total=$size -v time=$secs '{ result=total / time; printf("%.2f", result) }')
        #echo "  total records/sec:  ${recPerSec} "  >> ${SUMMARYLOG}
        #echo " " >> ${SUMMARYLOG}

    fi

elif [ ${MODEL} == "WaveNet" ] ; then
    if [ ${MODE} == "inference" ] ; then
        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}

        # throughput batch size 1
        BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        FPS=$(grep 'Average Throughput' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
        if [ ${SINGLE_SOCKET} == "true" ]; then
            echo "single_socket $FPS times 2"
            FPS=$(echo | awk -v value=$FPS  '{ result=value * 2; printf("%.2f", result) }')
        fi
        echo "  Average Throughput = ${FPS}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        #latency: batch size 1
        echo ""
        echo "${MODEL}_${MODE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
        BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        latency=$(grep 'Average Latency' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
        echo "  Average Latency = ${latency}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
    fi
elif [ ${MODEL} == "WaveNet_Magenta" ] ; then
    if [ ${MODE} == "inference" ] ; then
        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}

        # throughput batch size 1
        BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        FPS=$(grep 'Average Samples / sec:' ${LOGFILE_THROUGHPUT} | awk -F'Average Samples / sec:' '{print $2}' | sed "s/[^0-9.]*//g")
        if [ ${SINGLE_SOCKET} == "true" ]; then
            echo "single_socket $FPS times 2"
            FPS=$(echo | awk -v value=$FPS  '{ result=value * 2; printf("%.2f", result) }')
        fi
        echo "  Average Throughput = ${FPS}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        #latency: batch size 1
        echo ""
        echo "${MODEL}_${MODE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
        BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        latency=$(grep 'Average msec / sample:' ${LOGFILE_THROUGHPUT} | awk -F'Average msec / sample:' '{print $2}' | sed "s/[^0-9.]*//g")
        echo "  Average Latency = ${latency}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
    fi
elif [ ${MODEL} == "wideDeep" ] ; then

    if [ ${MODE} == "inference" ] ; then

        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}

        # throughput batch size 1024
        BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        RPS=$(grep 'Throughput is: ' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
        if [ ${SINGLE_SOCKET} == "true" ]; then
            echo "single_socket $RPS times 2"
            RPS=$(echo | awk -v value=$RPS  '{ result=value * 2; printf("%.2f", result) }')
        fi
        echo "  Total records/sec: ${RPS}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${RPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        #latency: batch size 1
        echo ""
        echo "${MODEL}_${MODE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
        BATCH_SIZE=$(grep -i "Batch Size" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g" | head -n 1)
        echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        latency=$(grep 'Latency is: ' ${LOGFILE_LATENCY} | sed "s/[^0-9.]*//g")
        latency=$(echo | awk -v value=$latency  '{ result=value * 1000; printf("%.2f", result) }')
        echo "  Latency ms/record: ${latency}" >> ${SUMMARYLOG}
        batch_size_1_throughput=$(echo | awk -v value=$latency  '{ result=1000/value; printf("%.2f", result) }')
        echo "  Throughput records/sec: ${batch_size_1_throughput}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    fi

# Shanghai OOB Models
elif [ ${MODEL} == "UNet" ] || [ ${MODEL} == "A3C" ] || [ ${MODEL} == "3DGAN" ] || [ ${MODEL} == "DRAW" ] ; then

    if [ ${MODE} == "inference" ] ; then
    
        # batch size grep partition
        if [ ${MODEL} == "DRAW" ]; then
            GREP_PARTITION="batchsize"
        else
            GREP_PARTITION="batch size"
        fi
        
        # latency
        echo ""
        echo "${MODEL}_${MODE} Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
        
        BATCH_SIZE=$(grep -i "${GREP_PARTITION}" ${LOGFILE_LATENCY} |sed -e "s/number:.*//;s/[^0-9.]*//g"  |tail -1)
        echo "  Batch Size = ${BATCH_SIZE}" >> ${SUMMARYLOG}
        
        latency=$(grep 'Time spent per BATCH:' ${LOGFILE_LATENCY} | sed "s/[^0-9.]*//g")
        echo "  Average Latency ms/batch = ${latency}" >> ${SUMMARYLOG}
        batch_size_1_throughput=$(echo | awk -v value=$latency  '{ result=1000/value; printf("%.2f", result) }')
        echo "  Average Throuput batch/sec = ${batch_size_1_throughput}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${latency};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
        
        # throughput
        echo ""
        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
        
        BATCH_SIZE=$(grep -i "${GREP_PARTITION}" ${LOGFILE_THROUGHPUT} |sed -e "s/number:.*//;s/[^0-9.]*//g" |tail -1)
        echo "  Batch Size = ${BATCH_SIZE}" >> ${SUMMARYLOG}
        
        FPS=$(grep 'Total samples/sec:' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
        if [ ${SINGLE_SOCKET} == "true" ]; then
            echo "single_socket $FPS times 2"
            FPS=$(echo | awk -v value=$FPS  '{ result=value * 2; printf("%.2f", result) }')
        fi
        echo "  Average samples/sec = ${FPS}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
    fi
    
elif [ ${MODEL} == "DCGAN" ]; then ### only throughput

    if [ ${MODE} == "inference" ] ; then
        
        # status
        echo ""
        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
        
        # Batch Size 100
        BATCH_SIZE=$(grep -i "batch size" ${LOGFILE_THROUGHPUT} |sed "s/[^0-9.]*//g"  |tail -1)
        echo "  Batch Size = ${BATCH_SIZE}" >> ${SUMMARYLOG}
        
        # throughput
        FPS=$(grep 'Total samples/sec:' ${LOGFILE_THROUGHPUT} | sed "s/[^0-9.]*//g")
        if [ ${SINGLE_SOCKET} == "true" ]; then
            echo "single_socket $FPS times 2"
            FPS=$(echo | awk -v value=$FPS  '{ result=value * 2; printf("%.2f", result) }')
        fi
        echo "  Average samples/sec = ${FPS}" >> ${SUMMARYLOG}
        echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}
    fi

else
    echo "model is not recognized"
    STATUS_THROUGHPUT="FAILURE"
fi


if [ ${STATUS_THROUGHPUT} == "SUCCESS" ] &&  [ ${STATUS_LATENCY} == "SUCCESS" ]; then
    exit 0
else
    exit 1
fi
