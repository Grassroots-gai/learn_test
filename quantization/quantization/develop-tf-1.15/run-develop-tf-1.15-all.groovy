@NonCPS
def jsonParse(def json) {
    new groovy.json.JsonSlurperClassic().parseText(json)
}

// checkout official models
def tf_models_checkout(model_source_branch){
    if( model_source_branch != null ) {
        if(!fileExists("${WORKSPACE}/tf_models")) {
            checkout([
                    $class                           : 'GitSCM',
                    branches                         : [[name: model_source_branch ]],
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
                withEnv(["current_branch=${model_source_branch}"]) {
                    sh '''#!/bin/bash
                    set -x
                    echo "branch of tf_models--->"
                    git reset --hard
                    git clean -df
                    git checkout ${current_branch}
                    git branch
                    git rev-parse HEAD                
                    '''
                }
            }
        }
    }
}

//run inference
def run_inference(String model_name,String precision,Boolean accuracy,String batch_size,String in_data_location,String in_graph,String log_dir,String checkpoint,String model_arg){
    dir("${WORKSPACE}/tensorflow-intelai-models/benchmarks") {
        withEnv(["summary_log=${summary_log}",
                 "tensorflow_version=${tensorflow_version}",
                 "intel_models_branch=${models_branch}",
                 "model=${model}",
                 "model_name=${model_name}",
                 "precision=${precision}",
                 "accuracy=${accuracy}",
                 "batch_size=${batch_size}",
                 "in_data_location=${in_data_location}",
                 "model_checkpoint=${checkpoint}",
                 "in_graph=${in_graph}",
                 "model_arguments=${model_arg}",
                 "log_dir=${log_dir}"
        ]) {
            sh '''#!/bin/bash
                set -x
                
                # python
                export PATH=${HOME}/miniconda3/bin/:$PATH
                source activate tf-${tensorflow_version}
                set +x
                
                echo "-----------------------run inference--------------------------"
                chmod 775 ${WORKSPACE}/validation-tensorflow/quantization/${intel_models_branch}/run_inference_intel-models_tf1.15.sh
                ${WORKSPACE}/validation-tensorflow/quantization/${intel_models_branch}/run_inference_intel-models_tf1.15.sh
            '''
        }
    }
}

//quantize pb
def quantize(String model_name, String in_graph, String out_graph, String data_location){
    dir("${WORKSPACE}/tensorflow-intelai-tools/api") {
        withEnv(["tensorflow_version=${tensorflow_version}","model=${model}","model_name=${model_name}","in_graph=${in_graph}","out_graph=${out_graph}","data_location=${data_location}"]){
            sh '''#!/bin/bash 
            export PATH=${HOME}/miniconda3/bin/:$PATH
            source activate tf-${tensorflow_version}
            
            # rm benchmarks for unclear /tmp clean
            if [[ ${model_name} == "ssd-resnet34" ]]; then
              rm -rf /tmp/benchmarks
            fi
            
            if [[ ${model_name} == "ssd-mobilenet" ]]; then
               cp /tf_dataset/sh_data/ssd_mobilenet/v1.5/coco_val.record ${WORKSPACE}/
               data_location="${WORKSPACE}/coco_val.record"
            fi
            
            # rm benchmarks for incomplete /tmp clean
            if [[ ${model_name} == "ssd-resnet34" ]]; then
              rm -rf /tmp/benchmarks
            fi
            
            if [[ ${data_location} == "/tf_dataset/dataset/TF_Imagenet_FullData" ]]; then
                data_location="/tf_dataset/dataset/TF_mini_imagenet"
            fi
            
            quantization_log_file="${WORKSPACE}/quantization-clx8280-${model}-$(date +%s).log"
            python quantize_model.py \
                --model ${model_name} \
                --in_graph ${in_graph} \
                --out_graph ${out_graph} \
                --data_location ${data_location} \
                --models_zoo_location ${WORKSPACE}/tensorflow-intelai-models/ \
                --models_source_dir ${WORKSPACE}/tf_models \
                > ${quantization_log_file} 2>&1
            
            echo "end of quantization"
            '''
        }
    }
}

def call() {
    def modelconf =  jsonParse(readFile("./validation-tensorflow/quantization/${models_branch}/${models_branch}-config.json"))
    model_name = modelconf."${model}"."model_name"
    checkpoint = modelconf."${model}"."checkpoint_fp32"
    in_graph = modelconf."${model}"."in_graph_fp32"
    model_arg_fp32 = modelconf."${model}"."model_arg_fp32"
    model_arg_int8 = modelconf."${model}"."model_arg_int8"
    model_arg_accuracy = modelconf."${model}"."model_arg_accuracy"
    data_location = modelconf."${model}"."data_location"
    batchsize_throuthput = modelconf."${model}"."batchsize_throuthput"
    batchsize_accuracy = modelconf."${model}"."batchsize_accuracy"
    model_source_branch = modelconf."${model}"."model_source_branch"
    data_location_accuracy = modelconf."${model}"."data_location_accuracy"

    //checkout tensorflow models
    tf_models_checkout(model_source_branch)

    dir ("${WORKSPACE}") {

        withEnv(["tensorflow_version=${tensorflow_version}"]) {
            sh '''#!/bin/bash
                # build conda env
                export PATH=${HOME}/miniconda3/bin/:$PATH
                pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple
                source ${WORKSPACE}/validation-tensorflow/quantization/scripts/install_tf_wheel.sh 
                source activate tf-${tensorflow_version}
                pip install -r ${WORKSPACE}/tensorflow-intelai-models/requirements-test.txt
            '''
        }

        performance_array=performance.split(',')
        data_type_arry=data_type.split(',')

        echo "performance_array = ${performance_array}"
        echo "data_type_arry = ${data_type_arry}"

        data_type_arry.each { precision ->
            echo "precision is ${precision}"

            if(precision=='int8'){
                quantize_graph="${WORKSPACE}/${model_name}-quantize.pb"
                quantize("${model_name}","${in_graph}","${quantize_graph}","${data_location_accuracy}")
                model_arg="${model_arg_int8}"
                in_graph="${quantize_graph}"
            }else{
                model_arg="${model_arg_fp32}"
            }

            // accuracy
            if ('accuracy' in performance_array) {
                run_inference("${model_name}", "$precision", true, "${batchsize_accuracy}", "${data_location_accuracy}", "${in_graph}", "${model}_inference_${precision}_accuracy", "${checkpoint}", "${model_arg_accuracy}")
            }

            // latency
            if ('latency' in performance_array) {
                run_inference("${model_name}", "$precision", false, "1", "${data_location}", "${in_graph}", "${model}_inference_${precision}_latency", "${checkpoint}","${model_arg}")
            }

            // throughput
            if ('throughput' in performance_array) {
                run_inference("${model_name}", "$precision", false, "${batchsize_throuthput}", "${data_location}", "${in_graph}", "${model}_inference_${precision}_throughput", "${checkpoint}", "${model_arg}")
            }
        }
    }
}

return this;