// Groovy scripts SSD-MobileNet

model = 'SSD-MobileNet'

def call() {
    if(!fileExists("${WORKSPACE}/tf_models")) {
        checkout([
                $class                           : 'GitSCM',
                branches                         : [[name: "20da786b078c85af57a4c88904f7889139739ab0"]],
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
                git checkout 20da786b078c85af57a4c88904f7889139739ab0                
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
                pip install Cython
                pip install pycocotools
                pip install matplotlib
                pip install pillow
                
                set -x
                cp /tf_dataset/sh_data/output/coco_val.record ${WORKSPACE}/
                if [ "$Test_fp32" == "True" ];
				then
                    # latency
                    log="benchmark-clx8280-${model}-inference-fp32-latency-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --data-location ${WORKSPACE}/coco_val.record \
                        --in-graph /tf_dataset/pre-trained-models/SSDMobilenet/fp32/frozen_inference_graph.pb \
                        --model-source-dir ${WORKSPACE}/tf_models \
                        --model-name ssd-mobilenet \
                        --framework tensorflow \
                        --precision fp32 \
                        --mode inference \
                        --socket-id 0 \
                        --num-intra-threads 28 \
                        --num-inter-threads 1 \
                        --batch-size 1 \
                        --benchmark-only \
                        > ${benchmark_log_file} 2>&1
                    latency=$(grep 'Time spent per BATCH: ' ${benchmark_log_file} |sed "s/[^0-9.]//g")
                    echo "TensorFlow;CLX8280;FP32;${model};Inference;Latency;1;${latency};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    throughput=$(grep 'Total samples/sec: ' ${benchmark_log_file} |sed "s/[^0-9.]//g")
                    echo "TensorFlow;CLX8280;FP32;${model};Inference;Throughput;1;${throughput};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    # accuracy
                    log="benchmark-clx8280-${model}-inference-fp32-accuracy-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --in-graph /tf_dataset/pre-trained-models/SSDMobilenet/fp32/frozen_inference_graph.pb \
                        --model-source-dir ${WORKSPACE}/tf_models \
                        --model-name ssd-mobilenet \
                        --framework tensorflow \
                        --precision fp32 \
                        --mode inference \
                        --accuracy-only \
                        --num-intra-threads 28 \
                        --num-inter-threads 1 \
                        --data-location ${WORKSPACE}/coco_val.record \
                        > ${benchmark_log_file} 2>&1
                    accuracy=$(grep 'Average Precision' ${benchmark_log_file} |head -1 |sed -e 's/.*= //')
                    echo "TensorFlow;CLX8280;FP32;${model};Inference;Accuracy;1;${accuracy};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                fi
                
                # tensorflow-intelai-models modify input layer
                cd ${WORKSPACE}/tensorflow-intelai-models/models/object_detection/tensorflow/ssd-mobilenet/inference/int8
                sed -i "s;input_layer = 'Preprocessor/subpart2';input_layer = 'image_tensor';" infer_detections.py
                
                if [ "$Test_int8" == "True" ];
                then
                    # generate int8 inference
                    # quantization
                    cd ${WORKSPACE}/tensorflow-intelai-tools/api
                    
                    quantization_log_file="${WORKSPACE}/quantization-clx8280-${model}-$(date +%s).log"
                    python ${WORKSPACE}/tensorflow-intelai-tools/api/quantize_model.py \
                        --model ssd_mobilenet \
                        --in_graph /tf_dataset/pre-trained-models/SSDMobilenet/fp32/frozen_inference_graph.pb \
                        --out_graph ${WORKSPACE}/ssd-mobilenet-quantize-${HOSTNAME}.pb \
                        --data_location ${WORKSPACE}/coco_val.record \
                        --models_zoo_location ${WORKSPACE}/tensorflow-intelai-models/ \
                        --models_source_dir ${WORKSPACE}/tf_models \
                        > ${quantization_log_file} 2>&1
                    
                    # latency
                    cd ${WORKSPACE}/tensorflow-intelai-models/benchmarks
                    
                    log="benchmark-clx8280-${model}-inference-int8-latency-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --data-location ${WORKSPACE}/coco_val.record \
                        --in-graph ${WORKSPACE}/ssd-mobilenet-quantize-${HOSTNAME}.pb \
                        --model-source-dir ${WORKSPACE}/tf_models \
                        --model-name ssd-mobilenet \
                        --framework tensorflow \
                        --precision fp32 \
                        --mode inference \
                        --socket-id 0 \
                        --batch-size 1 \
                        --num-intra-threads 28 \
                        --num-inter-threads 1 \
                        --benchmark-only \
                        > ${benchmark_log_file} 2>&1
                    latency=$(grep 'Time spent per BATCH: ' ${benchmark_log_file} |sed "s/[^0-9.]//g")
                    echo "TensorFlow;CLX8280;INT8;${model};Inference;Latency;1;${latency};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    throughput=$(grep 'Total samples/sec: ' ${benchmark_log_file} |sed "s/[^0-9.]//g")
                    echo "TensorFlow;CLX8280;INT8;${model};Inference;Throughput;1;${throughput};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                    
                    # accuracy
                    log="benchmark-clx8280-${model}-inference-int8-accuracy-$(date +%s).log"
                    benchmark_log_file="${WORKSPACE}/${log}"
                    python launch_benchmark.py \
                        --data-location ${WORKSPACE}/coco_val.record \
                        --in-graph ${WORKSPACE}/ssd-mobilenet-quantize-${HOSTNAME}.pb \
                        --model-source-dir ${WORKSPACE}/tf_models \
                        --model-name ssd-mobilenet \
                        --framework tensorflow \
                        --precision fp32 \
                        --mode inference \
                        --socket-id 0 \
                        --batch-size 1 \
                        --num-intra-threads 28 \
                        --num-inter-threads 1 \
                        --accuracy-only \
                        > ${benchmark_log_file} 2>&1
                    accuracy=$(grep 'Average Precision' ${benchmark_log_file} |head -1 |sed -e 's/.*= //')
                    echo "TensorFlow;CLX8280;INT8;${model};Inference;Accuracy;1;${accuracy};${BUILD_URL}artifact/${log}" |tee -a ${summary_log}
                fi
            '''
        }
    }
}

return this;