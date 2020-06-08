#!/bin/bash

### ResNet-50 v1.0 & v1.5
function main {
    fetch_cpu_info
    init_params "$@"
    set_environment
    
    cd ${model_src_dir}
    git remote -v
    git branch
    git show |head -5

    generate_core
    collect_logs
}

# init params
function init_params {
    model='resnet50'
    mode='inference'
    precision='fp32'
    batch_size=1
    conda_env_name='tensorflow'
    model_src_dir='/home/limengfx/limengfx/tensorflow/models/benchmarks'
    in_graph='/home/limengfx/limengfx/tensorflow/pb_models/resnet50/resnet50_fp32_pretrained_model.pb'

    for var in "$@"
    do 
        case $var in
            --model=*)
                model=$(echo $var |cut -f2 -d=)
            ;;
            --mode=*)
                mode=$(echo $var |cut -f2 -d=)
            ;;
            --precision=*)
                precision=$(echo $var |cut -f2 -d=)
            ;;
            --batch_size=*)
                batch_size=$(echo $var |cut -f2 -d=)
            ;;
            --numa_nodes_use=*)
                numa_nodes_use=$(echo $var |cut -f2 -d=)
            ;;
            --cores_per_instance=*)
                cores_per_instance=$(echo $var |cut -f2 -d=)
            ;;
            --conda_env_name=*)
                conda_env_name=$(echo $var |cut -f2 -d=)
            ;;
            --model_src_dir=*)
                model_src_dir=$(echo $var |cut -f2 -d=)
            ;;
            --in_graph=*)
                in_graph=$(echo $var |cut -f2 -d=)
            ;;
            --cores_per_node=*)
                cores_per_node=$(echo $var |cut -f2 -d=)
            ;;
            --data_location=*)
                data_location=$(echo $var |cut -f2 -d=)
            ;;
            --tf_models_dir=*)
                tf_models_dir=$(echo $var |cut -f2 -d=)
            ;;
            *)
                echo "Error: No such parameter: ${var}"
                exit 1
            ;;
        esac
    done
}

# environment
function set_environment {
    export KMP_AFFINITY=granularity=fine,noduplicates,compact,1,0

    # proxy
    export ftp_proxy=http://child-prc.intel.com:913
    export http_proxy=http://child-prc.intel.com:913
    export https_proxy=http://child-prc.intel.com:913

    # gcc6.3
    # export PATH=${HOME}/tools/gcc6_3_0/bin:$PATH
    # export LD_LIBRARY_PATH=${HOME}/tools/gcc6_3_0/lib64:$LD_LIBRARY_PATH
    gcc -v

    # conda3 python3
    export PATH="/home/limengfx/tools/anaconda3/bin:$PATH"
    source activate ${conda_env_name}
    python -V
}

# cpu info
function fetch_cpu_info {
    hostname
    cat /etc/os-release
    cat /proc/sys/kernel/numa_balancing
    cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
    lscpu 
    free -h
    numactl -H
    sockets_num=$(lscpu |grep 'Socket(s):' |sed 's/[^0-9]//g')
    cores_per_socket=$(lscpu |grep 'Core(s) per socket:' |sed 's/[^0-9]//g')
    phsical_cores_num=$( echo "${sockets_num} * ${cores_per_socket}" |bc )
    numa_nodes_num=$(lscpu |grep 'NUMA node(s):' |sed 's/[^0-9]//g')
    cores_per_node=$( echo "${phsical_cores_num} / ${numa_nodes_num}" |bc )
    cores_per_instance=${cores_per_node}
    numa_nodes_use='1'
}

# run
function generate_core {
    # cpu array
    cpu_array=($(numactl -H |grep "node [0-9]* cpus:" |sed "s/.*node [0-9]* cpus: *//" |\
    head -${numa_nodes_use} |cut -f1-${cores_per_node} -d' ' |awk -v cpi=${cores_per_instance} -v cpn=${cores_per_node} '{
        for( i=1; i<=NF; i++ ) {
            if(i % cpi == 0 || i % cpn == 0) {
                print $i","
            }else {
                printf $i","
            }
        }
    }' |sed "s/,$//"))
    instance=${#cpu_array[@]}

    # set run command
    log_dir="${WORKSPACE}/tensorflow-${model}-${mode}-${precision}-bs${batch_size}-cpi${cores_per_instance}-ins${instance}-nnu${numa_nodes_use}-$(date +'%s')"
    mkdir -p ${log_dir}

    excute_cmd_file="${log_dir}/tensorflow-run-$(date +'%s').sh"
    rm -f ${excute_cmd_file}

    for(( i=0; i<instance; i++ ))
    do
        real_cores_per_instance=$(echo ${cpu_array[i]} |awk -F, '{print NF}')
        log_file="${log_dir}/rcpi${real_cores_per_instance}_ins${i}.log"
        
        printf " GNMT_SPECIFY_ITERS=100 OMP_NUM_THREADS=${real_cores_per_instance} numactl --localalloc --physcpubind ${cpu_array[i]} \
                python ./benchmarks/launch_benchmark.py \
                --model-name ${model} --framework tensorflow --precision ${precision} \
                --mode ${mode} --batch-size ${batch_size} --num-cores ${real_cores_per_instance} \
                --num-intra-threads ${real_cores_per_instance} --num-inter-threads 1  \
                --data-location ${data_location} --in-graph ${in_graph} \
            > ${log_file} 2>&1 & " |tee -a ${excute_cmd_file}
            
            
    done

    echo -e "\n wait" >> ${excute_cmd_file}
    echo -e "\n\n\n batch_size: $batch_size, cores_per_instance: $cores_per_instance, instance: ${instance} is Running"

    sleep 3
    source ${excute_cmd_file}
}

# collect logs
function collect_logs {
    latency=$(grep 'Average Prediction Latency:' ${log_dir}/rcpi${cores_per_instance}* |sed -e 's/.*log//;s/[^0-9.]//g;s/.$//' |awk  '
    BEGIN {
        sum = 0;
        i = 0;
    }
    {
        sum = sum + $1;
        i++;
    }
    END {
        sum = sum / i;
        printf("%.3f", sum);
    }')

    throughput=$(grep 'Overall Throughput :' ${log_dir}/rcpi* |sed -e 's/.*log//;s/[^0-9.]//g;s/.$//' |awk '
    BEGIN {
        sum = 0;
    }
    {
        sum = sum + $1;
    }
    END {
        printf("%.2f", sum);
    }')
    echo "tensorflow,${model},${mode},${precision},${batch_size},${instance},${cores_per_instance},${latency},${throughput}" |tee -a ${WORKSPACE}/summary_tests.log
}

main "$@"
