#!/bin/bash
set -x
# input model_name
if [[ -z ${model_name} ]]; then
	model_name="inception_v1"
fi

if [[ -z ${DATASET_DIR} ]]; then
	DATASET_DIR="/tf_dataset/dataset/TF_Imagenet_FullData"
fi

# input precision
if [[ -z ${precision} ]]; then
    precision="fp32"
fi

if [[ ${precision} == "fp32" ]]; then
    pb_dir="/tf_dataset/pre-train-model-slim/pbfile/frozen_pb/frozen_${model_name}.pb"
else
    pb_dir="${WORKSPACE}/${model_name}_quantization.pb"
fi

# input mode
if [[ -z ${mode} ]]; then
	mode="throughput"
fi

if [[ ${mode} == "latency" ]]; then
	  batch_size=1
else
	  batch_size=100
fi

# set up output and image_size
image_size=224
if [[ ${model_name} == inception_v1 ]]; then
	output="InceptionV1/Logits/Predictions/Reshape_1"
elif [[ ${model_name} == inception_v2 ]]; then
	output="InceptionV2/Predictions/Reshape_1"
elif [[ ${model_name} == inception_v4 ]]; then
	output="InceptionV4/Logits/Predictions"
	image_size=299
elif [[ ${model_name} == vgg_16 ]]; then
        output="vgg_16/fc8/squeezed"
elif [[ ${model_name} == vgg_19 ]]; then
        output="vgg_19/fc8/squeezed"
elif [[ ${model_name} == mobilenet_v2 ]]; then
        output="MobilenetV2/Predictions/Reshape_1"
elif [[ ${model_name} == mobilenet_v1 ]]; then
        output="MobilenetV1/Predictions/Reshape_1"
elif [[ ${model_name} == nasnet_large ]] || [[ ${model_name} == pnasnet_large ]]; then
	output="final_layer/predictions"
	image_size=331
else
	output="${model_name}/predictions/Reshape_1"
fi

log="${WORKSPACE}/${model_name}"
mkdir -p ${log}
log_dir=${log}/${model_name}_inference_${precision}_${mode}

eval_script=${WORKSPACE}/dlft_oob_performance/tensorflow/models/research/slim/eval_image_classifier_optimize.py

# get cpu information for multi-instance
nsockets=$( lscpu | grep 'Socket(s)' | cut -d: -f2 | xargs echo -n)
ncores_per_socket=${ncores_per_socket:=$( lscpu | grep 'Core(s) per socket' | cut -d: -f2 | xargs echo -n)}
total_cores=$((nsockets * ncores_per_socket))
# set intel BKC
export KMP_BLOCKTIME=1
export KMP_AFFINITY=granularity=fine,verbose,compact,1,0
export TF_MKL_OPTIMIZE_PRIMITIVE_MEMUSE=false

if [[ ${mode} == "accuracy" ]]; then

    logfile=${log_dir}.log
    # orginaze the cmd
    cmd="python ${eval_script} --env=mkl --batch-size=${batch_size} \
        -e 1 -a ${ncores_per_socket} -g ${pb_dir} --steps=100 --num-cores=28 -i input -o ${output} \
        --image_size=${image_size} -d ${DATASET_DIR} --images_num=50000 --accuracy-only"

    export OMP_NUM_THREADS=${ncores_per_socket}
    echo "RUNCMD: $cmd " >& ${logfile}
    echo "Batch Size: ${batch_size}" >> ${logfile}
    eval $cmd >> ${logfile}

else

    if [[ ${mode} == "latency" ]]; then
        ncores_per_instance=4
        inter_op=1
        intra_op=${ncores_per_instance}
    else
        ncores_per_instance=${ncores_per_socket}
        inter_op=1
        intra_op=${ncores_per_instance}
    fi

    cmd="python ${eval_script} --env=mkl --batch-size=${batch_size} \
      -e ${inter_op} -a ${intra_op} -g ${pb_dir} --steps=100 --num-cores=${ncores_per_instance} -i input -o ${output} \
      --image_size=${image_size}"

    echo "llsu--->$cmd"

    # run multi-instance benchmark
    export OMP_NUM_THREADS=${ncores_per_instance}
    echo "llsu--OMP_NUM_THREADS-->${OMP_NUM_THREADS}"
    echo "llsu--inter_op-->${inter_op}"
    for((j=0;$j<${total_cores};j=$(($j + ${ncores_per_instance}))));
    do
       numactl -l -C "$j-$((j + ncores_per_instance -1)),$((j + total_cores))-$((j + total_cores + ncores_per_instance- 1))" \
       ${cmd} 2>&1|tee ${log_dir}_${total_cores}_${ncores_per_instance}_${j}.log &
    done

    wait

fi

# log collect
if [[ ${mode} == "latency" ]]; then
    latency=$(grep 'Latency' ${log_dir}_*.log | sed -e s";.*: ;;" | sed -e s"; ms;;" | awk 'BEGIN{sum=0}{sum+=$1}END{printf("%.3f\n",sum/NR)}')
elif [[ ${mode} == "throughput" ]]; then
    throughput=$(grep 'Throughput' ${log_dir}_*.log | sed -e s";.*: ;;" | sed -e s"; images/sec;;"| awk 'BEGIN{sum=0}{sum+=$1}END{print sum}')
else
    accuracy=$(grep '(Top1 accuracy, Top5 accuracy) =' ${log_dir}.log |tail -1 |sed -e 's/.*(//;s/,.*//')
fi

# log written
if [[ $precision == "fp32" ]]; then
  log_precision="FP32"
elif [[ $precision == "int8" ]]; then
  log_precision="INT8"
fi

if [[ ${mode} == "latency" ]]; then
    echo "TensorFlow;CLX8280;${log_precision};${model_name};Inference;Latency;1;${latency};${BUILD_URL}artifact/${model_name}" |tee -a ${summary_log}
elif [[ ${mode} == "throughput" ]]; then
    echo "TensorFlow;CLX8280;${log_precision};${model_name};Inference;Throughput;${batch_size};${throughput};${BUILD_URL}artifact/${model_name}" |tee -a ${summary_log}
else
    echo "TensorFlow;CLX8280;${log_precision};${model_name};Inference;Accuracy;${batch_size};${accuracy};${BUILD_URL}artifact/${model_name}" |tee -a ${summary_log}
fi