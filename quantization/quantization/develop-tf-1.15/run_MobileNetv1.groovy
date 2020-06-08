// Groovy scripts MobileNetv1

model = 'MobileNetv1'

def call() {

    if(!fileExists("${WORKSPACE}/tf_models")) {
        checkout([
                $class                           : 'GitSCM',
                branches                         : [[name: "*/master"]],
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
                git checkout master                
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
                
                set -x
                
                if [ "$Test_fp32" == "True" ];
				then
                    # latency
                    log="benchmark-clx8280-${model}-inference-fp32-latency-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --precision fp32 \
                        --model-name mobilenet_v1 \
                        --mode inference \
                        --framework tensorflow \
                        --model-source-dir ${WORKSPACE}/tf_models  \
                        --batch-size 1 \
                        --socket-id 0 \
                        --checkpoint /tf_dataset/pre-trained-models/mobilenet_v1/fp32/ \
                        > ${benchmark_log_file} 2>&1
                    latency=$(grep 'Latency ms/step' ${benchmark_log_file} |sed 's/[^0-9.]//g')
                    echo "TensorFlow;CLX8280;FP32;${model};Inference;Latency;1;${latency};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
    
                    # throughput
                    log="benchmark-clx8280-${model}-inference-fp32-throughput-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --precision fp32 \
                        --model-name mobilenet_v1 \
                        --mode inference \
                        --framework tensorflow \
                        --model-source-dir ${WORKSPACE}/tf_models  \
                        --batch-size 128 \
                        --socket-id 0 \
                        --checkpoint /tf_dataset/pre-trained-models/mobilenet_v1/fp32/ \
                        > ${benchmark_log_file} 2>&1
                    throughput=$(grep 'Total images/sec' ${benchmark_log_file} |sed 's/[^0-9.]//g')
                    echo "TensorFlow;CLX8280;FP32;${model};Inference;Throughput;128;${throughput};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    # accuracy
                    log="benchmark-clx8280-${model}-inference-fp32-accuracy-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --precision fp32 \
                        --model-name mobilenet_v1 \
                        --mode inference \
                        --framework tensorflow \
                        --model-source-dir ${WORKSPACE}/tf_models  \
                        --batch-size 100 \
                        --accuracy-only \
                        --data-location /tf_dataset/dataset/TF_Imagenet_FullData \
                        --in-graph /tf_dataset/pre-trained-models/mobilenet_v1/fp32/mobilenet_v1_1.0_224_frozen.pb \
                        > ${benchmark_log_file} 2>&1
                    accuracy=$(grep '(Top1 accuracy, Top5 accuracy) =' ${benchmark_log_file} |tail -1 |sed -e 's/.*(//;s/,.*//')
                    echo "TensorFlow;CLX8280;FP32;${model};Inference;Accuracy;100;${accuracy};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                fi
                
                
                if [ "$Test_int8" == "True" ];
                then
                    # generate int8 inference
                    # quantization
                    cd ${WORKSPACE}/tensorflow-intelai-tools/api
                    
                    quantization_log_file="${WORKSPACE}/quantization-clx8280-${model}-$(date +%s).log"
                    python ${WORKSPACE}/tensorflow-intelai-tools/api/quantize_model.py \
                        --model mobilenet_v1 \
                        --in_graph /tf_dataset/pre-trained-models/mobilenet_v1/fp32/mobilenet_v1_1.0_224_frozen.pb \
                        --out_graph ${WORKSPACE}/mobilenet_v1-quantize-${HOSTNAME}.pb \
                        --data_location /tf_dataset/dataset/TF_Imagenet_FullData \
                        --models_zoo_location ${WORKSPACE}/tensorflow-intelai-models/ \
                        > ${quantization_log_file} 2>&1
                    
                    # latency
                    cd ${WORKSPACE}/tensorflow-intelai-models/benchmarks
                    log="benchmark-clx8280-${model}-inference-int8-latency-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --model-name mobilenet_v1 \
                        --precision int8 \
                        --mode inference \
                        --framework tensorflow \
                        --benchmark-only \
                        --batch-size 1 \
                        --socket-id 0 \
                        --in-graph ${WORKSPACE}/mobilenet_v1-quantize-${HOSTNAME}.pb \
                        -- input_height=224 input_width=224 warmup_steps=100 steps=500 \
                        input_layer="input" output_layer="MobilenetV1/Predictions/Reshape_1"\
                        > ${benchmark_log_file} 2>&1
                    latency=$(grep 'images/sec' ${benchmark_log_file} |tail -1 |sed -e 's/.*, //;s/ .*//' |awk '{printf("%.3f",1000/$1)}')
                    echo "TensorFlow;CLX8280;INT8;${model};Inference;Latency;1;${latency};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    # throughput
                    log="benchmark-clx8280-${model}-inference-int8-throughput-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --model-name mobilenet_v1 \
                        --precision int8 \
                        --mode inference \
                        --framework tensorflow \
                        --benchmark-only \
                        --batch-size 240 \
                        --socket-id 0 \
                        --in-graph ${WORKSPACE}/mobilenet_v1-quantize-${HOSTNAME}.pb \
                        -- input_height=224 input_width=224 warmup_steps=100 steps=500 \
                        input_layer="input" output_layer="MobilenetV1/Predictions/Reshape_1" \
                        > ${benchmark_log_file} 2>&1
                    throughput=$(grep 'images/sec' ${benchmark_log_file} |tail -1 |sed -e 's/.*, //;s/ .*//')
                    echo "TensorFlow;CLX8280;INT8;${model};Inference;Throughput;240;${throughput};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    # accuracy
                    log="benchmark-clx8280-${model}-inference-int8-accuracy-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --model-name mobilenet_v1 \
                        --precision int8 \
                        --mode inference \
                        --framework tensorflow \
                        --accuracy-only \
                        --batch-size 100 \
                        --socket-id 0 \
                        --in-graph ${WORKSPACE}/mobilenet_v1-quantize-${HOSTNAME}.pb \
                        --data-location /tf_dataset/dataset/TF_Imagenet_FullData  -- input_height=224 input_width=224 \
                        input_layer="input" output_layer="MobilenetV1/Predictions/Reshape_1" \
                        > ${benchmark_log_file} 2>&1
                    accuracy=$(grep 'Top1 accuracy, Top5 accuracy' ${benchmark_log_file} |tail -1 |sed -e 's/.*(//;s/,.*//')
                    echo "TensorFlow;CLX8280;INT8;${model};Inference;Accuracy;100;${accuracy};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                fi
            '''
        }
    }
}

return this;