#!/bin/bash -x

if [[ ${model_name} == "ssd-mobilenet" ]]; then
   cp /tf_dataset/sh_data/ssd_mobilenet/v1.5/coco_val.record ${WORKSPACE}/
   in_data_location="${WORKSPACE}/coco_val.record"
fi

if [[ ${model_name} == "mobilenet_v1" && ${precision} == "int8" && ${accuracy} == "false" ]] ; then
      in_data_location=null
fi


log="${WORKSPACE}/${model}"
mkdir -p ${log}
log_dir="${log}/${log_dir}"

model_sourcedir=null
model_sourcedir_arr=["mobilenet_v1","rfcn","ssd-mobilenet","ssd-resnet34","faster_rcnn"]
required_model_sourcedir=$(echo "${model_sourcedir_arr[@]}" | grep -wq "${model_name}" &&  echo "Yes" || echo "No")
if [[ ${required_model_sourcedir} == "Yes" ]]; then
  model_sourcedir="${WORKSPACE}/tf_models"
fi
test_precision=${precision}
if [ ${model_name} == "ssd-mobilenet" ] ; then
    test_precision='fp32'
fi

echo "---------------------------------------------------------"
echo 'Running with parameters:'
echo "    model_name:                    ${model_name}"
echo "    precision:                     ${precision}"
echo "    accuracy:                      ${accuracy}"
echo "    batch_size:                    ${batch_size}"
echo "    in_data_location:              ${in_data_location}"
echo "    model_checkpoint:              ${model_checkpoint}"
echo "    in_graph:                      ${in_graph}"
echo "    model_arguments:               ${model_arguments}"
echo "    summary_log:                   ${summary_log}"
echo "    log_dir:                       ${log_dir}"
echo "    model_sourcedir:               ${model_sourcedir}"
echo "======================================="
# get cpu information for multi-instance
nsockets=$( lscpu | grep 'Socket(s)' | cut -d: -f2 | xargs echo -n)
ncores_per_socket=${ncores_per_socket:=$( lscpu | grep 'Core(s) per socket' | cut -d: -f2 | xargs echo -n)}
total_cores=$((nsockets * ncores_per_socket))
# set intel BKC
export KMP_BLOCKTIME=1
export KMP_AFFINITY=granularity=fine,verbose,compact,1,0
export TF_MKL_OPTIMIZE_PRIMITIVE_MEMUSE=false

run_inference(){
  default_cmd="python launch_benchmark.py \
          --model-name ${model_name} \
          --precision ${test_precision} \
          --mode inference \
          --framework tensorflow \
          --verbose \
          --batch-size ${batch_size}"

  if [ ${accuracy} == "true" ] ; then
      benchmark_arg="--accuracy-only "
  else
      benchmark_arg="--benchmark-only "
  fi

  if [[ ${in_data_location} == *null* ]]; then
      data_location_arg=""
  else
      data_location_arg="--data-location ${in_data_location} "
  fi

  if [[ ${in_graph} == *null* ]]; then
      in_graph_arg=""
  else
      in_graph_arg="--in-graph ${in_graph} "
  fi

  if [[ ${model_checkpoint} == *null* ]]; then
      checkpoint_arg=""
  else
      checkpoint_arg="--checkpoint ${model_checkpoint} "
  fi

  if [[ ${model_sourcedir} == *null* ]]; then
      model_source_dir_arg=""
  else
      model_source_dir_arg="--model-source-dir ${model_sourcedir} "
  fi

  if [[ ${model_arguments} == *null* ]]; then
      additional_arg=""
  else
      additional_arg="-- ${model_arguments} "
  fi

  # run for multi-instance benchmark
  if [[ ${log_dir} == *latency* ]]; then
      ncores_per_instance=4
      inter_op=1
      intra_op=${ncores_per_instance}
  else
      ncores_per_instance=${ncores_per_socket}
      inter_op=1
      intra_op=${ncores_per_instance}
  fi

  accuracy_inter_intra=""
  # set ssd-mobilebet inter & intra
  if [[ ${model_name} == "ssd-mobilenet" ]];then
      accuracy_inter_intra="--num-inter-threads 1 \
                            --num-intra-threads ${intra_op}"
  fi


  if [[ ${log_dir} == *accuracy* ]]; then
      logfile=${log_dir}.log
      # orginaze the cmd
      cmd="${default_cmd} \
          ${data_location_arg} \
          ${in_graph_arg} \
          ${model_source_dir_arg} \
          ${benchmark_arg} \
          ${accuracy_inter_intra} \
          ${additional_arg} >> ${logfile} 2>&1"

      export OMP_NUM_THREADS=${ncores_per_socket}
      echo "RUNCMD: $cmd " >& ${logfile}
      echo "Batch Size: ${batch_size}" >> ${logfile}
      eval $cmd >> ${logfile}

  else
      # orginaze the cmd

      cmd="${default_cmd} \
          ${data_location_arg} \
          ${checkpoint_arg} \
          ${in_graph_arg} \
          ${model_source_dir_arg} \
          ${benchmark_arg} \
          --num-inter-threads ${inter_op} \
          --num-intra-threads ${intra_op} \
          ${additional_arg} "


      # run multi-instance benchmark
      export OMP_NUM_THREADS=${ncores_per_instance}
      for((j=0;$j<${total_cores};j=$(($j + ${ncores_per_instance}))));
      do
         numactl -l -C "$j-$((j + ncores_per_instance -1)),$((j + total_cores))-$((j + total_cores + ncores_per_instance- 1))" \
         ${cmd} 2>&1|tee ${log_dir}_${total_cores}_${ncores_per_instance}_${j}.log &
      done

      wait

  fi

}

# run inference
run_inference

# log collect
case ${model_name} in

  "mobilenet_v1")
      echo "model is ${model_name}"
      if [[ ${log_dir} == *latency* ]]; then
          latency=$(grep "steps = 50" ${log_dir}_*.log | awk -F ' ' '{print $4}' | awk 'BEGIN{sum=0}{sum+=(1000/$1)}END{printf("%.4f\n",sum/NR)}')
      elif [[ ${log_dir} == *throughput* ]]; then
          throughput=$(grep "steps = 50" ${log_dir}_*.log | awk -F ' ' '{print $4}' | awk 'BEGIN{sum=0}{sum+=$1}END{printf("%.4f\n",sum)}')
      else
          accuracy=$(grep '(Top1 accuracy, Top5 accuracy) =' ${log_dir}.log |tail -1 |sed -e 's/.*(//;s/,.*//')
      fi
      ;;
  "faster_rcnn"|"rfcn")
      echo "model is ${model_name}"
      if [[ ${log_dir} == *latency* ]]; then
          latency=$(grep 'Avg. Duration per Step:' ${log_dir}_*.log |sed "s/.*://" |awk 'BEGIN{sum=0}{sum+=$1}END{printf("%.4f\n",1000*(sum/NR))}')
      elif [[ ${log_dir} == *throughput* ]]; then
          throughput=$(grep 'Avg. Duration per Step:' ${log_dir}_*.log |sed "s/.*://" |awk 'BEGIN{sum=0}{sum+=(1/$1)}END{printf("%.4f\n",sum)}')
      else
          accuracy=$(grep 'Average Precision' ${log_dir}.log |head -1 |sed -e 's/.*= //')
      fi
      ;;
  "inceptionv3" )
      echo "model is ${model_name}"
      if [[ ${log_dir} == *latency* ]]; then
          if [[ ${test_precision} == "fp32" ]]; then
              latency=$(grep 'Latency:' ${log_dir}_*.log |sed "s/.*: //;s/ms//" | awk 'BEGIN{sum=0}{sum+=$1}END{printf("%.4f\n",sum/NR)}')
          elif [[ ${test_precision} == "int8" ]]; then
              latency=$(grep 'Average throughput for batch size' ${log_dir}_*.log |sed -e "s/.*://;s/[^0-9.]//g" |awk 'BEGIN{sum=0}{sum+=(1000/$1)}END{printf("%.4f\n",sum/NR)}')
          fi
      elif [[ ${log_dir} == *throughput* ]]; then
          if [[ ${test_precision} == "fp32" ]]; then
              throughput=$(grep 'Throughput:' ${log_dir}_*.log |sed "s/.*: //;s/images\/sec//" |awk 'BEGIN{sum=0}{sum+=$1}END{printf("%.4f\n",sum)}')
          elif [[ ${test_precision} == "int8" ]]; then
              throughput=$(grep 'Average throughput for batch size' ${log_dir}_*.log |sed -e "s/.*://;s/[^0-9.]//g" |awk 'BEGIN{sum=0}{sum+=$1}END{printf("%.4f\n",sum)}')
          fi
      else
          accuracy=$(grep 'Top1 accuracy, Top5 accuracy' ${log_dir}.log |tail -1 |sed -e 's/.*(//;s/,.*//')
      fi
      ;;
  "resnet50"|"resnet50v1_5"|"resnet101")
      echo "model is ${model_name}"
      if [[ ${log_dir} == *latency* ]]; then
          latency=$(grep 'Latency:'  ${log_dir}_*.log |sed 's/.*: //;s/ms//' | awk 'BEGIN{sum=0}{sum+=$1}END{printf("%.4f\n",sum/NR)}')
      elif [[ ${log_dir} == *throughput* ]]; then
          throughput=$(grep 'Throughput:'  ${log_dir}_*.log |sed 's/.*: //;s/images\/sec//'  | awk 'BEGIN{sum=0}{sum+=$1}END{printf("%.4f\n",sum)}')
      else
          accuracy=$(grep 'Top1 accuracy, Top5 accuracy' ${log_dir}.log |tail -1 |sed -e 's/.*(//;s/,.*//')
      fi
      ;;
  "ssd-mobilenet"|"ssd-resnet34")
      echo "model is ${model_name}"
      if [[ ${log_dir} == *latency* ]]; then
          latency=$(grep 'Time spent per BATCH: ' ${log_dir}_*.log  |sed "s/.*: *//;s/[^0-9.]//g"| awk 'BEGIN{sum=0}{sum+=$1}END{printf("%.4f\n",sum/NR)}')
      elif [[ ${log_dir} == *throughput* ]]; then
          throughput=$(grep 'Total samples/sec: ' ${log_dir}_*.log |sed "s/.*: *//;s/samples\/s//" | awk 'BEGIN{sum=0}{sum+=$1}END{printf("%.4f\n",sum)}')
      else
          if [[ ${model_name} == "ssd-resnet34" ]]; then
              accuracy=$(grep 'Current AP:' ${log_dir}.log |sed 's/.*://;s/[^0-9.]//g')
          else
              accuracy=$(grep 'Average Precision' ${log_dir}.log |head -1 |sed -e 's/.*= //')
          fi
      fi
      ;;
      *)
      echo "${model_name} is not recognized"
      ;;
esac

# log written
if [[ $precision == "fp32" ]]; then
  log_precision="FP32"
elif [[ $precision == "int8" ]]; then
  log_precision="INT8"
fi

if [[ ${log_dir} == *latency* ]]; then
    echo "TensorFlow;CLX8280;${log_precision};${model};Inference;Latency;1;${latency};${BUILD_URL}artifact/${model}" |tee -a ${summary_log}
elif [[ ${log_dir} == *throughput* ]]; then
    echo "TensorFlow;CLX8280;${log_precision};${model};Inference;Throughput;${batch_size};${throughput};${BUILD_URL}artifact/${model}" |tee -a ${summary_log}
else
    echo "TensorFlow;CLX8280;${log_precision};${model};Inference;Accuracy;${batch_size};${accuracy};${BUILD_URL}artifact/${model}" |tee -a ${summary_log}
fi


