// Groovy scripts R-FCN

model = 'R-FCN'

def call() {

    if(!fileExists("${WORKSPACE}/tf_models")) {
        checkout([
                $class                           : 'GitSCM',
                branches                         : [[name: "master"]],
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
        dir("${WORKSPACE}/tf_models"){
            sh '''#!/bin/bash
                set -x
                echo "pwd of tf_models--->"
                pwd
                git reset --hard
                git clean -df
                git checkout 6c21084503b27a9ab118e1db25f79957d5ef540b 
                git apply ${WORKSPACE}/tensorflow-intelai-models/models/object_detection/tensorflow/rfcn/inference/tf-2.0.patch               
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
                pip install -r ${WORKSPACE}/tensorflow-intelai-models/benchmarks/object_detection/tensorflow/rfcn/requirements.txt
                
                set -x
                if [ "$Test_fp32" == "True" ];
				then
                    # latency
                    log="benchmark-clx8280-${model}-inference-fp32-latency-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --model-name rfcn \
                        --mode inference \
                        --precision fp32 \
                        --framework tensorflow \
                        --batch-size 1 \
                        --model-source-dir ${WORKSPACE}/tf_models \
                        --in-graph /tf_dataset/pre-trained-models/rfcn/fp32/rfcn_resnet101_coco_2018_01_28/frozen_inference_graph.pb \
                        --benchmark-only \
                        --data-location /tf_dataset/sh_data/COCO2017/val2017 \
                        --verbose \
                        -- number_of_steps=500 \
                        > ${benchmark_log_file} 2>&1
                    latency=$(grep 'Avg. Duration per Step:' ${benchmark_log_file} |tail -1 |sed "s/.*://" |awk '{printf("%.3f", 1000 * $1)}')
                    echo "TensorFlow;CLX8280;FP32;${model};Inference;Latency;1;${latency};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    throughput=$(grep 'Avg. Duration per Step:' ${benchmark_log_file} |tail -1 |sed "s/.*://" |awk '{printf("%.3f", 1 / $1)}')
                    echo "TensorFlow;CLX8280;FP32;${model};Inference;Throughput;1;${throughput};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    # accuracy
                    log="benchmark-clx8280-${model}-inference-fp32-accuracy-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --model-name rfcn \
                        --mode inference \
                        --precision fp32 \
                        --framework tensorflow \
                        --model-source-dir ${WORKSPACE}/tf_models \
                        --data-location /tf_dataset/sh_data/rfcn/coco_val.record \
                        --in-graph /tf_dataset/pre-trained-models/rfcn/fp32/rfcn_resnet101_coco_2018_01_28/frozen_inference_graph.pb  \
                        --accuracy-only \
                        --socket-id 0 \
                        --batch-size 1 \
                        -- split="accuracy_message" \
                        > ${benchmark_log_file} 2>&1
                    accuracy=$(grep 'Average Precision' ${benchmark_log_file} |head -1 |sed -e 's/.*= //')
                    echo "TensorFlow;CLX8280;FP32;${model};Inference;Accuracy;1;${accuracy};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                fi
                
                if [ "$Test_int8" == "True" ];
                then
                    # generate int8 inference
                    # quantization
                    cd ${WORKSPACE}/tensorflow-intelai-tools/api
                    
                    quantization_log_file="${WORKSPACE}/quantization-clx8280-${model}-$(date +%s).log"
                    python quantize_model.py \
                        --model rfcn \
                        --in_graph /tf_dataset/pre-trained-models/rfcn/fp32/rfcn_resnet101_coco_2018_01_28/frozen_inference_graph.pb \
                        --out_graph ${WORKSPACE}/rfcn-quantize-${HOSTNAME}.pb \
                        --data_location /tf_dataset/sh_data/rfcn/coco_val.record \
                        --models_zoo_location ${WORKSPACE}/tensorflow-intelai-models/ \
                        --models_source_dir ${WORKSPACE}/tf_models \
                        > ${quantization_log_file} 2>&1
                    
                    # latency
                    cd ${WORKSPACE}/tensorflow-intelai-models/benchmarks
                    
                    log="benchmark-clx8280-${model}-inference-int8-latency-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --model-name rfcn \
                        --mode inference \
                        --precision int8 \
                        --framework tensorflow \
                        --model-source-dir ${WORKSPACE}/tf_models \
                        --in-graph ${WORKSPACE}/rfcn-quantize-${HOSTNAME}.pb \
                        --benchmark-only \
                        --data-location /tf_dataset/sh_data/COCO2017/val2017 \
                        --verbose \
                        -- number_of_steps=500 \
                        > ${benchmark_log_file} 2>&1
                    latency=$(grep 'Avg. Duration per Step:' ${benchmark_log_file} |tail -1 |sed "s/.*://" |awk '{printf("%.3f", 1000 * $1)}')
                    echo "TensorFlow;CLX8280;INT8;${model};Inference;Latency;1;${latency};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    throughput=$(grep 'Avg. Duration per Step:' ${benchmark_log_file} |tail -1 |sed "s/.*://" |awk '{printf("%.3f", 1 / $1)}')
                    echo "TensorFlow;CLX8280;INT8;${model};Inference;Throughput;1;${throughput};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    # accuracy
                    log="benchmark-clx8280-${model}-inference-int8-accuracy-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --model-name rfcn \
                        --mode inference \
                        --precision int8 \
                        --framework tensorflow \
                        --model-source-dir ${WORKSPACE}/tf_models \
                        --data-location /tf_dataset/sh_data/rfcn/coco_val.record \
                        --in-graph ${WORKSPACE}/rfcn-quantize-${HOSTNAME}.pb \
                        --accuracy-only \
                        --socket-id 0 \
                        -- split="accuracy_message" \
                        > ${benchmark_log_file} 2>&1
                    accuracy=$(grep 'Average Precision' ${benchmark_log_file} |head -1 |sed -e 's/.*= //')
                    echo "TensorFlow;CLX8280;INT8;${model};Inference;Accuracy;1;${accuracy};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                fi
            '''
        }
    }
}

return this;