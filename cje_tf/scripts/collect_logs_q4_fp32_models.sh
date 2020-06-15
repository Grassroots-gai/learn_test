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


if [ ${MODEL} == "FaceNet" ] ; then
    if [ ${MODE} == "inference" ] ; then

        echo "model is ${MODEL}"

        if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
	   [ $(grep 'Total samples/sec: ' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
	   STATUS_THROUGHPUT="SUCCESS"
	fi
	if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
	   [ $(grep 'Time spent per BATCH:' ${LOGFILE_LATENCY} | wc -l) != 0 ] ; then
	    STATUS_LATENCY="SUCCESS"
	fi
        # accuracy getting from the throughput run log with batch 100
	if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
	   [ $(grep 'Accuracy:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
            STATUS_ACCURACY="SUCCESS"
	fi
    else
        echo "${MODEL} ${mode} is skipped for Q4"
    fi
	   
elif [ ${MODEL} == "MTCC" ] ; then
    if [ ${MODE} == "inference" ] ; then
        echo "model is ${MODEL}"

        if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
	   [ $(grep 'Throughput is:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
	   STATUS_THROUGHPUT="SUCCESS"
	fi
        # latency getting from the throughput run log
	if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
	   [ $(grep 'Latency is:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
	    STATUS_LATENCY="SUCCESS"
	fi
        # accuracy getting from the throughput run log
	if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
	   [ $(grep 'Accuracy:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
            STATUS_ACCURACY="SUCCESS"
	fi
    else
        echo "${MODEL} ${mode} is skipped for Q4"
    fi

elif [ ${MODEL} == "ResNet101" ] ; then

    if [ ${MODE} == "inference" ] ; then

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
	   
    else
        echo "${MODEL} ${mode} is skipped for Q4"
    fi
	
elif [ ${MODEL} == "DenseNet169" ] ; then

    if [ ${MODE} == "inference" ] ; then

        echo "model is ${MODEL}"

	if [ $(grep 'Error: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
	   [ $(grep 'steps = 100,' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
	   STATUS_THROUGHPUT="SUCCESS"
	fi
        
	if [ $(grep 'Error: ' ${LOGFILE_LATENCY} | wc -l) == 0 ] &&
	   [ $(grep 'Latency:' ${LOGFILE_LATENCY} | wc -l) != 0 ] ; then
	    STATUS_LATENCY="SUCCESS"
	fi
        
	if [ $(grep 'Error: ' ${LOGFILE_ACCURACY} | wc -l) == 0 ] &&
	   [ $(grep -Eo "0\.[0-9]+" ${LOGFILE_ACCURACY} | wc -l) != 0 ] ; then
           STATUS_ACCURACY="SUCCESS"
	fi
	   
    else
	echo "${MODEL} ${mode} is skipped for Q4"
    fi

elif [ ${MODEL} == "SSDMobilenet" ] ; then

    if [ ${MODE} == "inference" ] ; then

        echo "model is ${MODEL}"

        # run once with batch size 1 to get latency and throughput, no accuracy reported
        if [ $(grep 'ERROR: ' ${LOGFILE_THROUGHPUT} | wc -l) == 0 ] &&
	   [ $(grep 'Latency:' ${LOGFILE_THROUGHPUT} | wc -l) != 0 ] ; then
	    STATUS_THROUGHPUT="SUCCESS"
	    STATUS_LATENCY="SUCCESS"
	fi
    else
        echo "${MODEL} ${mode} is skipped for Q4"
    fi

else
    echo "model is not recognized"
    STATUS_THROUGHPUT="FAILURE"
fi


if [ ${MODEL} == "FaceNet" ] ; then

    if [ ${MODE} == "inference" ] ; then

	echo "${MODEL}_inference Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
	BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g")
	echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
        FPS=$(grep "Total samples/sec: " ${LOGFILE_THROUGHPUT} | awk -F': ' '{print $2}' | awk -F' ' '{print $1}')
	echo "  Total samples/sec: ${FPS} samples/sec" >> ${SUMMARYLOG}
	echo " " >> ${SUMMARYLOG}
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

	echo "${MODEL}_inference Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
	BATCH_SIZE=$(grep -i "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g")
	echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
	LATENCY=$(grep 'Time spent per BATCH:' ${LOGFILE_LATENCY} | awk -F': ' '{print $2}' | awk -F' ' '{print $1}')
	echo "  Time spent per BATCH: ${LATENCY} ms" >> ${SUMMARYLOG}
	echo " " >> ${SUMMARYLOG}
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

        # accuracy getting from the throughput run log with batch 100
	echo "${MODEL}_inference Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
	echo "Batch Size: 100" >> ${SUMMARYLOG}
	ACCURACY=$(grep 'Accuracy:' ${LOGFILE_THROUGHPUT} | awk -F': ' '{print $2}')
	echo "  Accuracy: ${ACCURACY}" >> ${SUMMARYLOG}
	echo " " >> ${SUMMARYLOG}
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${ACCURACY};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    else
        echo "${MODEL} ${mode} is skipped for Q4"
    fi

elif [ ${MODEL} == "MTCC" ] ; then

    if [ ${MODE} == "inference" ] ; then

	echo "${MODEL}_inference Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
	BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g")
	echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
	FPS=$(grep "Throughput is:" ${LOGFILE_THROUGHPUT} | awk -F',' '{print $2}' | awk -F': ' '{print $2}') 
	echo "  Throughput: ${FPS}" >> ${SUMMARYLOG}
	echo " " >> ${SUMMARYLOG}
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

	echo "${MODEL}_inference Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
	BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g")
	echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
	grep 'steps = 200,' ${LOGFILE_LATENCY} >> ${SUMMARYLOG}
	LATENCY=$(grep "Latency is:" ${LOGFILE_THROUGHPUT} | awk -F',' '{print $1}' | awk -F': ' '{print $2}') 
	echo "  Latency: ${LATENCY}" >> ${SUMMARYLOG}
	echo " " >> ${SUMMARYLOG}
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

	echo "${MODEL}_inference Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
	BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g")
	echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
	ACCURACY=$(grep 'Accuracy:' ${LOGFILE_THROUGHPUT} | awk -F': ' '{print $2}')
	echo "  Accuracy: ${ACCURACY}" >> ${SUMMARYLOG}
	echo " " >> ${SUMMARYLOG}
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${ACCURACY};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    else
        echo "${MODEL} ${mode} is skipped for Q4"
    fi

elif [ ${MODEL} == "ResNet101" ] ; then

    if [ ${MODE} == "inference" ] ; then

        echo "${MODEL}_${MODE} Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}        

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

    else
        echo "${MODEL} ${mode} is skipped for Q4"
    fi

 elif [ ${MODEL} == "DenseNet169" ] ; then

		if [ ${MODE} == "inference" ] ; then

		echo "${MODEL}_inference Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
		BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g")
		echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
		FPS=$(grep "steps = 100," ${LOGFILE_THROUGHPUT} | awk -F',' '{print $2}' | awk -F' ' '{print $1}') 
		echo "  Throughput: ${FPS} images/sec" >> ${SUMMARYLOG}
		echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

		echo "${MODEL}_inference Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
		BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_LATENCY} | sed "s/[^0-9]*//g")
		echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
		grep 'steps = 100,' ${LOGFILE_LATENCY} >> ${SUMMARYLOG}
		LATENCY=$(grep "Latency:" ${LOGFILE_LATENCY} | tail -1 | awk -F' ' '{print $2}') 
		echo "  Latency: ${LATENCY} ms" >> ${SUMMARYLOG}
		echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${LOGFILE_LATENCY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

		echo "${MODEL}_inference Accuracy: ${STATUS_ACCURACY} " >> ${SUMMARYLOG}
		BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_ACCURACY} | sed "s/[^0-9]*//g")
		echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
		ACCURACY=$(grep -Eo "0\.[0-9]+" ${LOGFILE_ACCURACY} | tail -1)
		echo "  Accuracy: ${ACCURACY}" >> ${SUMMARYLOG}
		echo " " >> ${SUMMARYLOG}
        echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Accuracy;${BATCH_SIZE};${ACCURACY};${LOGFILE_ACCURACY##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

		else
			echo "${MODEL} ${mode} is skipped for Q4"
		fi

elif [ ${MODEL} == "SSDMobilenet" ] ; then

    if [ ${MODE} == "inference" ] ; then

	echo "${MODEL}_inference Throughput: ${STATUS_THROUGHPUT} " >> ${SUMMARYLOG}
	BATCH_SIZE=$(grep "Batch Size:" ${LOGFILE_THROUGHPUT} | sed "s/[^0-9]*//g")
	echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
	LATENCY=$(grep "Latency:" ${LOGFILE_THROUGHPUT} | awk -F', ' '{print $3}' | awk -F'= ' '{print $2}') 
        FPS=$(echo | awk -v latency=$LATENCY '{ result= 1000 / latency; printf("%.2f", result) }')
	echo "  Throughput: ${FPS}" >> ${SUMMARYLOG}
	echo " " >> ${SUMMARYLOG}
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Throughput;${BATCH_SIZE};${FPS};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

	echo "${MODEL}_inference Latency: ${STATUS_LATENCY} " >> ${SUMMARYLOG}
	echo "  Batch Size: ${BATCH_SIZE}" >> ${SUMMARYLOG}
	echo "  Latency: ${LATENCY}" >> ${SUMMARYLOG}
	echo " " >> ${SUMMARYLOG}
    echo "${MODEL};${MODE};${MACHINE_TYPE};${DATATYPE};Latency;${BATCH_SIZE};${LATENCY};${LOGFILE_THROUGHPUT##*/};${JOB_NAME};${BUILD_ID}" >> ${SUMMARYTXT}

    else
        echo "${MODEL} ${mode} is skipped for Q4"
    fi

else
    echo "model is not recognized"
fi

if [ ${STATUS_THROUGHPUT} == "SUCCESS" ] ||  [ ${STATUS_LATENCY} == "SUCCESS" ]; then
    exit 0
else
    exit 1
fi

