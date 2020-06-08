// Groovy scripts ResNet-101

model = 'ResNet-101'

def call() {

    // generate fp32 inference
    dir("${WORKSPACE}/tensorflow-intelai-models/benchmarks") {
        withEnv(["model=${model}","summary_log=${summary_log}","tensorflow_version=${tensorflow_version}","Test_fp32=${Test_fp32}","Test_int8=${Test_int8}"]) {
            sh '''#!/bin/bash
        
                # gcc 
                export PATH=${HOME}/gcc6.3/bin/:$PATH
                export LD_LIBRARY_PATH=${HOME}/gcc6.3/lib64:$LD_LIBRARY_PATH
                gcc -v
                
                # python
                set -x
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
                        --framework tensorflow \
                        --precision fp32 \
                        --mode inference \
                        --model-name resnet101 \
                        --batch-size 1 \
                        --in-graph /tf_dataset/pre-trained-models/resnet101/fp32/optimized_graph.pb \
                        --socket-id 0 \
                        > ${benchmark_log_file} 2>&1
                    latency=$(grep 'Latency:' ${benchmark_log_file} |sed 's/[^0-9.]//g')
                    echo "TensorFlow;CLX8280;FP32;${model};Inference;Latency;1;${latency};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    # throughput
                    log="benchmark-clx8280-${model}-inference-fp32-throughput-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --framework tensorflow \
                        --precision fp32 \
                        --mode inference \
                        --model-name resnet101 \
                        --batch-size 128 \
                        --in-graph /tf_dataset/pre-trained-models/resnet101/fp32/optimized_graph.pb \
                        --socket-id 0 \
                        > ${benchmark_log_file} 2>&1
                    throughput=$(grep 'Throughput:' ${benchmark_log_file} |sed 's/[^0-9.]//g')
                    echo "TensorFlow;CLX8280;FP32;${model};Inference;Throughput;128;${throughput};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    # accuracy
                    log="benchmark-clx8280-${model}-inference-fp32-accuracy-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --framework tensorflow \
                        --precision fp32 \
                        --mode inference \
                        --model-name resnet101 \
                        --batch-size 100 \
                        --in-graph /tf_dataset/pre-trained-models/resnet101/fp32/optimized_graph.pb \
                        --data-location /tf_dataset/dataset/TF_Imagenet_FullData \
                        --accuracy-only \
                        --socket-id 0 \
                        > ${benchmark_log_file} 2>&1
                    accuracy=$(grep 'Top1 accuracy, Top5 accuracy' ${benchmark_log_file} |tail -1 |sed -e 's/.*(//;s/,.*//')
                    echo "TensorFlow;CLX8280;FP32;${model};Inference;Accuracy;100;${accuracy};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                fi
                
                
                if [ "$Test_int8" == "True" ];
                then
                
                    # generate int8 inference
                    # quantization
                    cd ${WORKSPACE}/tensorflow-intelai-tools/api
                    
                    quantization_log_file="${WORKSPACE}/quantization-clx8280-${model}-$(date +%s).log"
                    python ${WORKSPACE}/tensorflow-intelai-tools/api/quantize_model.py \
                        --model resnet101 \
                        --in_graph /tf_dataset/pre-trained-models/resnet101/fp32/optimized_graph.pb \
                        --out_graph ${WORKSPACE}/resnet101-quantize-${HOSTNAME}.pb \
                        --data_location /tf_dataset/dataset/TF_Imagenet_FullData \
                        --models_zoo_location ${WORKSPACE}/tensorflow-intelai-models/ \
                        > ${quantization_log_file} 2>&1
                    
                    # latency
                    cd ${WORKSPACE}/tensorflow-intelai-models/benchmarks
                    log="benchmark-clx8280-${model}-inference-int8-latency-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --model-name resnet101 \
                        --precision int8 \
                        --mode inference \
                        --framework tensorflow \
                        --benchmark-only \
                        --batch-size 1 \
                        --socket-id 0 \
                        --in-graph=${WORKSPACE}/resnet101-quantize-${HOSTNAME}.pb \
                        -- warmup_steps=50 steps=500 \
                        > ${benchmark_log_file} 2>&1
                    latency=$(grep 'Latency:' ${benchmark_log_file} |sed 's/[^0-9.]//g')
                    echo "TensorFlow;CLX8280;INT8;${model};Inference;Latency;1;${latency};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    # throughput
                    log="benchmark-clx8280-${model}-inference-int8-throughput-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --model-name resnet101 \
                        --precision int8 \
                        --mode inference \
                        --framework tensorflow \
                        --benchmark-only \
                        --batch-size 128 \
                        --socket-id 0 \
                        --in-graph=${WORKSPACE}/resnet101-quantize-${HOSTNAME}.pb \
                        -- warmup_steps=50 steps=500 \
                        > ${benchmark_log_file} 2>&1
                    throughput=$(grep 'Throughput:' ${benchmark_log_file} |sed 's/[^0-9.]//g')
                    echo "TensorFlow;CLX8280;INT8;${model};Inference;Throughput;128;${throughput};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    # accuracy
                    log="benchmark-clx8280-${model}-inference-int8-accuracy-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --model-name resnet101 \
                        --precision int8 \
                        --mode inference \
                        --framework tensorflow \
                        --accuracy-only \
                        --batch-size 100 \
                        --data-location /tf_dataset/dataset/TF_Imagenet_FullData \
                        --in-graph=${WORKSPACE}/resnet101-quantize-${HOSTNAME}.pb \
                        > ${benchmark_log_file} 2>&1
                    accuracy=$(grep 'Top1 accuracy, Top5 accuracy' ${benchmark_log_file} |tail -1 |sed -e 's/.*(//;s/,.*//')
                    echo "TensorFlow;CLX8280;INT8;${model};Inference;Accuracy;100;${accuracy};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                fi
            '''
        }
    }
}

return this;