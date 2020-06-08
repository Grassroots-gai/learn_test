// Groovy scripts bert_tencent

model = 'bert_tencent'

def call() {

    // generate inference
    dir("${WORKSPACE}/tensorflow-intelai-models/benchmarks") {
        withEnv(["model=${model}","summary_log=${summary_log}","tensorflow_version=${tensorflow_version}"]) {
            sh '''#!/bin/bash
                FP32_PB_DIR=/tf_dataset/pre-trained-models/bert_tencent/optimized_fp32_bert.pb
                INT8_PB_DIR=${WORKSPACE}/${model}-quantize-${HOSTNAME}.pb
                # gcc 
                export PATH=${HOME}/gcc6.3/bin/:$PATH
                export LD_LIBRARY_PATH=${HOME}/gcc6.3/lib64:$LD_LIBRARY_PATH
                gcc -v
                
                # python
                export PATH=${HOME}/miniconda3/bin/:$PATH
                pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple
                source ${WORKSPACE}/tensorflow-validation/quantization/scripts/install_tf_wheel.sh 
                source activate tf-${tensorflow_version}
                pip list
                python -V
                pip install -r ${WORKSPACE}/tensorflow-intelai-models/requirements-test.txt
                
                set -x
                if [ "$Test_fp32" == "True" ];
				then
                    # latency
                    log="benchmark-clx8280-${model}-inference-fp32-latency-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                    --model-name bert \
                    --precision fp32 \
                    --mode inference \
                    --framework tensorflow \
                    --benchmark-only \
                    --in-graph ${FP32_PB_DIR} \
                    --batch-size 1 \
                    --socket-id 0 \
                    --num-cores 28 \
                    --num-intra-threads 28 \
                    --num-inter-threads 1 > ${benchmark_log_file} 2>&1
                    latency=$(grep "Avg Latency time of 1000 times " ${benchmark_log_file} | awk -F'= ' '{print $2}' | awk -F' ' '{print $1}')
                    echo "TensorFlow;CLX8280;FP32;${model};Inference;Latency;1;${latency};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    throughput=$(echo | awk -v latency=${latency} '{ result= 1000 / latency; printf("%.2f", result) }')
                    echo "TensorFlow;CLX8280;FP32;${model};Inference;Throughput;1;${throughput};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                fi
                
                if [ "$Test_int8" == "True" ];
                then
                    # generate int8 inference
                    # quantization
                    # cd ${WORKSPACE}/tensorflow-intelai-tools/api
                    cp -r ${WORKSPACE}/tensorflow-intelai-models ${WORKSPACE}/tensorflow-intelai-tools/api/
                    
                    quantization_log_file="${WORKSPACE}/quantization-clx8280-${model}-$(date +%s).log"
                    python ${WORKSPACE}/tensorflow-intelai-tools/api/quantize_bert.py \
                        --model bert_tencent \
                        --model_location ${FP32_PB_DIR} \
                        --out_graph ${INT8_PB_DIR} > ${quantization_log_file} 2>&1
                    
                    # latency
                    cd ${WORKSPACE}/tensorflow-intelai-models/benchmarks
                    
                    log="benchmark-clx8280-${model}-inference-int8-latency-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                    --model-name bert \
                    --precision int8 \
                    --mode inference \
                    --framework tensorflow \
                    --benchmark-only \
                    --in-graph ${INT8_PB_DIR} \
                    --batch-size 1 \
                    --socket-id 0 \
                    --num-cores 28 \
                    --num-intra-threads 28 \
                    --num-inter-threads 1 > ${benchmark_log_file} 2>&1
                    latency=$(grep "Avg Latency time of 1000 times " ${benchmark_log_file} | awk -F'= ' '{print $2}' | awk -F' ' '{print $1}')
                    echo "TensorFlow;CLX8280;INT8;${model};Inference;Latency;1;${latency};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    throughput=$(echo | awk -v latency=${latency} '{ result= 1000 / latency; printf("%.2f", result) }')
                    echo "TensorFlow;CLX8280;INT8;${model};Inference;Throughput;1;${throughput};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                fi
            '''
        }
    }
}

return this;