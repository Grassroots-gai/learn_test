#!/bin/bash

### GNMT Training
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
    model='gnmt'
    mode='training'
    precision='fp32'
    batch_size=1
    conda_env_name='tensorflow'
    model_src_dir='/home/limengfx/limengfx/tensorflow/intel-models/benchmarks'
    data_location='/home/limengfx/limengfx/tensorflow/intel-models/benchmarks/wmt16'

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
    #
    export KMP_BLOCKTIME=1
    export KMP_AFFINITY=granularity=fine,verbose,compact,1,0 

    # mpi
    source ${HOME}/intel/compilers_and_libraries/linux/mpi/intel64/bin/mpivars.sh
    
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
            if(i % cpi == 0) {
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

    for(( i=0; i<1; i++ ))
    do
        real_cores_per_instance=$(echo ${cpu_array[i]} |awk -F, '{print NF}')
        log_file="${log_dir}/rcpi${real_cores_per_instance}_ins${i}.log"

        printf " python launch_benchmark.py \
                  --benchmark-only \
                  --framework tensorflow \
                  --model-name gnmt \
                  --mode training \
                  --precision fp32 \
                  --num-processes=2 \
                  --num-processes-per-node=1 \
                  --num-train-steps=300 \
                  --num_units=1024 \
                  --dropout=0.2 \
                  --src=de \
                  --tgt=en \
                  --vocab_prefix=vocab.bpe.32000 \
                  --train_prefix=train.tok.clean.bpe.32000 \
                  --dev_prefix=newstest2013.tok.bpe.32000 \
                  --test_prefix=newstest2015.tok.bpe.32000 \
                  --batch-size=${batch_size} \
                  --hparams_path=nmt/standard_hparams/wmt16_gnmt_4_layer_multi_instances.json \
                  --data-location=${data_location} \
                  --num-cores=${real_cores_per_instance} \
                  --num-intra-threads=$[ ${real_cores_per_instance} - 1 ] \
                  --num-inter-threads=1 \
                  --verbose \
        > ${log_file} 2>&1 & " |tee -a ${excute_cmd_file}
    done

    echo -e "\n wait" >> ${excute_cmd_file}
    echo -e "\n\n\n batch_size: $batch_size, cores_per_instance: $cores_per_instance, instance: ${instance} is Running"

    sleep 3
    
    # clean checkpoint
    sudo rm -rf /tmp/* && rm -rf common/tensorflow/logs/
    sudo sh -c 'echo 3 > /proc/sys/vm/drop_caches'
    
    source ${excute_cmd_file}
}

# collect logs
function collect_logs {
    latency=$(grep '^  step 300' ${log_dir}/rcpi${cores_per_instance}* |sed -e 's/.*time//;s/wps.*//;s/s//g' |awk  '
    BEGIN {
        sum = 0;
        i = 0;
    }
    {
        sum = sum + $1 / 100 * 1000;
        i++;
    }
    END {
        sum = sum / i;
        printf("%.3f", sum);
    }')

    throughput=$(grep '^  step 300' ${log_dir}/rcpi* |sed -e 's/.*wps//;s/ppl.*//'  |awk '
    BEGIN {
        sum = 0;
    }
    {
        if($2 == 1000) {
            sum = sum + $1 * 1000
        }else {
            sum = sum + $1;
        }
    }
    END {
        printf("%.2f", sum);
    }')
    echo "tensorflow,${model},${mode},${precision},${batch_size},${instance},${cores_per_instance},${latency},${throughput}" |tee -a ${WORKSPACE}/summary_tests.log
}

main "$@"
