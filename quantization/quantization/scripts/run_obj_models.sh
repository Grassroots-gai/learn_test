#!/bin/bash
set -x
# input model_name
if [[ -z ${model_name} ]]; then
	model_name="ssd_mobilenet_v1"
fi

# input precision
if [[ -z ${precision} ]]; then
    precision="fp32"
fi

if [[ ${precision} == "fp32" ]]; then
    pb_dir="/tf_dataset/pre-train-model-oob/object_detection/${model_name}/frozen_inference_graph.pb"
else
    pb_dir="${WORKSPACE}/${model_name}_quantization.pb"
fi

# input mode
if [[ -z ${mode} ]]; then
	mode="throughput"
fi

log="${WORKSPACE}/${model_name}"
mkdir -p ${log}
log_dir=${log}/${model_name}_inference_${precision}_${mode}

eval_script=${WORKSPACE}/dlft_oob_performance/tensorflow/models/research/object_detection/inference/infer_detections.py
TF_RECORD_FILES=/tf_dataset/dataset/pre_ci/coco_val.record
set +x

# get cpu information for multi-instance
nsockets=$( lscpu | grep 'Socket(s)' | cut -d: -f2 | xargs echo -n)
ncores_per_socket=${ncores_per_socket:=$( lscpu | grep 'Core(s) per socket' | cut -d: -f2 | xargs echo -n)}
total_cores=$((nsockets * ncores_per_socket))
# set intel BKC
export KMP_BLOCKTIME=1
export KMP_AFFINITY=granularity=fine,verbose,compact,1,0
export TF_MKL_OPTIMIZE_PRIMITIVE_MEMUSE=false

if [[ ${mode} == "latency" ]]; then
    ncores_per_instance=4
    inter_op=1
    intra_op=${ncores_per_instance}
else
    ncores_per_instance=${ncores_per_socket}
    inter_op=1
    intra_op=${ncores_per_instance}
fi

cmd="python ${eval_script} \
    --input_tfrecord_paths=${TF_RECORD_FILES} \
    --output_tfrecord_path=./validation_detections.tfrecord-00000-of-00001 \
    --inference_graph=${pb_dir} \
    --discard_image_pixels true \
    --num_inter_threads=${inter_op} \
    --num_intra_threads=${intra_op} \
    --iterations 100 "

# run multi-instance benchmark
export OMP_NUM_THREADS=${ncores_per_instance}
for((j=0;$j<${total_cores};j=$(($j + ${ncores_per_instance}))));
do
   numactl -l -C "$j-$((j + ncores_per_instance -1)),$((j + total_cores))-$((j + total_cores + ncores_per_instance- 1))" \
   ${cmd} 2>&1|tee ${log_dir}_${total_cores}_${ncores_per_instance}_${j}.log &
done

wait

# log collect
if [[ ${mode} == "latency" ]]; then
    latency=$(grep "median" ${log_dir}_*.log  | sed -e s"/.*= //" | awk 'BEGIN{sum=0}{sum+=$1}END{printf("%.3f\n",sum/NR)}')
fi

if [[ ${mode} == "throughput" ]]; then
    throughput=$(grep "median" ${log_dir}_*.log  | sed -e s"/.*= //" | awk '{printf("%.3f\n", 1000 / $1)}'| awk 'BEGIN{sum=0}{sum+=$1}END{print sum}')
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
    echo "TensorFlow;CLX8280;${log_precision};${model_name};Inference;Throughput;1;${throughput};${BUILD_URL}artifact/${model_name}" |tee -a ${summary_log}
fi
