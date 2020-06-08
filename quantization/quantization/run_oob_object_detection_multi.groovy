// Groovy scripts for object_detection oob models of multi-instance inference

//quantize pb
def quantize(String model_name){
    dir("${WORKSPACE}/tensorflow-intelai-tools/api") {
        withEnv(["tensorflow_version=${tensorflow_version}","model_name=${model_name}"]){
            sh '''#!/bin/bash
            export PATH=${HOME}/miniconda3/bin/:$PATH
            source activate tf-${tensorflow_version}
            
            # setup env
            cp -r ${WORKSPACE}/dlft_oob_performance  ${WORKSPACE}/tensorflow-intelai-tools/api/
            export PYTHONPATH=${WORKSPACE}/dlft_oob_performance/tensorflow/models/research:${WORKSPACE}/dlft_oob_performance/tensorflow/models:$PYTHONPATH
            
            TF_RECORD_FILES=/tf_dataset/dataset/pre_ci/coco_val.record
            PB_DIR_FP32=/tf_dataset/pre-train-model-oob/object_detection/${model_name}/frozen_inference_graph.pb
            PB_DIR_INT8=${WORKSPACE}/${model_name}_quantization.pb
            
            # quantization
            quantization_log_file="${WORKSPACE}/quantization-clx8280-${model_name}-$(date +%s).log"
            cmd="python ${WORKSPACE}/tensorflow-intelai-tools/api/quantize_model_oob_obj.py \
            --model ${model_name} \
            --model_location $PB_DIR_FP32  \
            --data_location $TF_RECORD_FILES \
            --out_graph $PB_DIR_INT8 \
            >> ${quantization_log_file} 2>&1"
            echo "RUNCMD: $cmd " >& ${quantization_log_file}
            eval $cmd >> ${quantization_log_file}
            
            echo "end of quantization"
            '''
        }
    }
}

def run_inference(String model_name,String precision,String mode){
    dir("${WORKSPACE}/dlft_oob_performance") {
        withEnv(["summary_log=${summary_log}",
                 "tensorflow_version=${tensorflow_version}",
                 "model_name=${model_name}",
                 "precision=${precision}",
                 "mode=${mode}"]) {
            sh '''#!/bin/bash            
                # python
                export PATH=${HOME}/miniconda3/bin/:$PATH
                source activate tf-${tensorflow_version}
                export PYTHONPATH=${WORKSPACE}/dlft_oob_performance/tensorflow/models/research:${WORKSPACE}/dlft_oob_performance/tensorflow/models:$PYTHONPATH
                
                echo "-----------------------run OOB object-detection inference--------------------------"
                cd ${WORKSPACE}/validation-tensorflow
                chmod a+x quantization/scripts/run_obj_models.sh
                ${WORKSPACE}/validation-tensorflow/quantization/scripts/run_obj_models.sh
            '''
        }
    }
}

def call() {
    if(!fileExists("${WORKSPACE}/dlft_oob_performance")){
        checkout([
                $class: 'GitSCM',
                browser: [$class: 'AssemblaWeb', repoUrl: ''],
                doGenerateSubmoduleConfigurations: false,
                extensions: [
                        [$class: 'RelativeTargetDirectory',relativeTargetDir: "dlft_oob_performance"],
                        [$class: 'CloneOption', timeout: 60],
                ],
                submoduleCfg: [],
                userRemoteConfigs: [
                        [credentialsId: "${teamforge_credential}",
                         url: " https://gitlab.devtools.intel.com/daisyden/dlft_oob_performance.git"]
                ]
        ])
        sh '''#!/bin/bash
            # download submodule
            cd ${WORKSPACE}/dlft_oob_performance 
            git submodule update --init --recursive
        '''
    }else{

        sh '''#!/bin/bash
            # checkout and clean models
            cd ${WORKSPACE}/dlft_oob_performance/tensorflow/models 
            git reset --hard
            git clean -df
            git checkout r1.13.0
        '''
    }

    dir ("${WORKSPACE}") {

        withEnv(["tensorflow_version=${tensorflow_version}"]) {
            sh '''#!/bin/bash
                # build conda env
                export PATH=${HOME}/miniconda3/bin/:$PATH
                source ${WORKSPACE}/validation-tensorflow/quantization/scripts/install_tf_wheel.sh 
                source activate tf-${tensorflow_version}
                
                # setup env
                copy_to_dir=${WORKSPACE}/dlft_oob_performance/tensorflow/models/research/object_detection/inference/
                cp -r ${WORKSPACE}/dlft_oob_performance/scripts/object_detection/inference/*  ${copy_to_dir}
            '''
        }

        performance_array=performance.split(',')
        data_type_arry=data_type.split(',')

        echo "performance_array = ${performance_array}"
        echo "data_type_arry = ${data_type_arry}"

        data_type_arry.each { precision ->
            echo "precision is ${precision}"

            if(precision == "int8"){
                quantize("${model}")
            }

            performance_array.each{ mode ->
                echo "mode is ${mode}"
                if(mode != "accuracy"){
                    run_inference("${model}", "$precision", "${mode}")
                }
            }
        }
    }
}

return this;