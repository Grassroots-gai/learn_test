// Groovy scripts SSD-ResNet34

model = 'SSD-ResNet34'

def call() {
    if(!fileExists("${WORKSPACE}/tf_models")) {
        checkout([
                $class                           : 'GitSCM',
                branches                         : [[name: "f505cecde2d8ebf6fe15f40fb8bc350b2b1ed5dc"]],
                browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                doGenerateSubmoduleConfigurations: false,
                extensions                       : [
                        [$class: 'RelativeTargetDirectory', relativeTargetDir: "tf_models"],
                        [$class: 'CloneOption', timeout: 60, depth: 1]
                ],
                submoduleCfg                     : [],
                userRemoteConfigs                : [
                        [credentialsId: "${teamforge_credential}",
                         url          : "https://github.com/tensorflow/models.git"]
                ]
        ])
    }else {
        dir("${WORKSPACE}/tf_models") {
            sh '''#!/bin/bash
                set -x
                echo "pwd of tf_models--->"
                pwd
                git reset --hard
                git clean -df
                git checkout f505cecde2d8ebf6fe15f40fb8bc350b2b1ed5dc                
            '''
        }
    }
    
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
                pip install --force-reinstall numpy==1.17.4 
                if [ "$Test_fp32" == "True" ];
				then      
                    # latency
                    log="benchmark-clx8280-${model}-inference-fp32-latency-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --in-graph /tf_dataset/pre-trained-models/ssd-resnet34/fp32/ssd_resnet34_fp32_bs1_pretrained_model.pb \
                        --model-source-dir ${WORKSPACE}/tf_models \
                        --model-name ssd-resnet34 \
                        --framework tensorflow \
                        --precision fp32 \
                        --mode inference \
                        --socket-id 0 \
                        --batch-size 1 \
                        --benchmark-only \
                        > ${benchmark_log_file} 2>&1
                    latency=$(grep 'Time spent per BATCH:' ${benchmark_log_file} |sed 's/[^0-9.]//g')
                    echo "TensorFlow;CLX8280;FP32;${model};Inference;Latency;1;${latency};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    throughput=$(grep 'Total samples/sec:' ${benchmark_log_file} |sed 's/[^0-9.]//g')
                    echo "TensorFlow;CLX8280;FP32;${model};Inference;Throughput;1;${throughput};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    # accuracy
                    log="benchmark-clx8280-${model}-inference-fp32-accuracy-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --data-location /tf_dataset/sh_data/ssd-resnet34 \
                        --in-graph /tf_dataset/pre-trained-models/ssd-resnet34/fp32/ssd_resnet34_fp32_bs1_pretrained_model.pb \
                        --model-source-dir ${WORKSPACE}/tf_models \
                        --model-name ssd-resnet34 \
                        --framework tensorflow \
                        --precision fp32 \
                        --mode inference \
                        --socket-id 0 \
                        --batch-size=1 \
                        --accuracy-only \
                        > ${benchmark_log_file} 2>&1
                    accuracy=$(grep 'Current AP:' ${benchmark_log_file} |sed 's/[^0-9.]//g')
                    echo "TensorFlow;CLX8280;FP32;${model};Inference;Accuracy;1;${accuracy};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                fi
                
                if [ "$Test_int8" == "True" ];
                then
                    # generate int8 inference
                    # quantization
                    cd ${WORKSPACE}/tensorflow-intelai-tools/api
                    
                    quantization_log_file="${WORKSPACE}/quantization-clx8280-${model}-$(date +%s).log"
                    python ${WORKSPACE}/tensorflow-intelai-tools/api/quantize_model.py \
                        --model ssd_resnet34 \
                        --in_graph /tf_dataset/pre-trained-models/ssd-resnet34/fp32/ssd_resnet34_fp32_bs1_pretrained_model.pb \
                        --out_graph ${WORKSPACE}/ssd-resnet34-quantize-${HOSTNAME}.pb \
                        --data_location /tf_dataset/dataset/ssd-resnet34 \
                        --models_zoo_location ${WORKSPACE}/tensorflow-intelai-models/ \
                        --models_source_dir ${WORKSPACE}/tf_models \
                        > ${quantization_log_file} 2>&1
                    
                    # latency
                    cd ${WORKSPACE}/tensorflow-intelai-models/benchmarks
                    
                    log="benchmark-clx8280-${model}-inference-int8-latency-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --in-graph ${WORKSPACE}/ssd-resnet34-quantize-${HOSTNAME}.pb \
                        --model-source-dir ${WORKSPACE}/tf_models \
                        --model-name ssd-resnet34 \
                        --framework tensorflow \
                        --precision int8 \
                        --mode inference \
                        --socket-id 0 \
                        --batch-size=1 \
                        --benchmark-only \
                        > ${benchmark_log_file} 2>&1
                    latency=$(grep 'Time spent per BATCH:' ${benchmark_log_file} |sed 's/[^0-9.]//g')
                    echo "TensorFlow;CLX8280;INT8;${model};Inference;Latency;1;${latency};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    throughput=$(grep 'Total samples/sec:' ${benchmark_log_file} |sed 's/[^0-9.]//g')
                    echo "TensorFlow;CLX8280;INT8;${model};Inference;Throughput;1;${throughput};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    # accuracy
                    log="benchmark-clx8280-${model}-inference-int8-accuracy-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --data-location /tf_dataset/sh_data/ssd-resnet34 \
                        --in-graph ${WORKSPACE}/ssd-resnet34-quantize-${HOSTNAME}.pb \
                        --model-source-dir ${WORKSPACE}/tf_models \
                        --model-name ssd-resnet34 \
                        --framework tensorflow \
                        --precision int8 \
                        --mode inference \
                        --socket-id 0 \
                        --batch-size=1 \
                        --accuracy-only \
                        > ${benchmark_log_file} 2>&1
                    accuracy=$(grep 'Current AP:' ${benchmark_log_file} |sed 's/[^0-9.]//g')
                    echo "TensorFlow;CLX8280;INT8;${model};Inference;Accuracy;1;${accuracy};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                fi
            '''
        }
    }
}

return this;