// Groovy scripts for classification oob models of multi-instance inference

//quantize pb
def quantize(String model_name){
    dir("${WORKSPACE}/tensorflow-intelai-tools/api") {
        withEnv(["tensorflow_version=${tensorflow_version}","model_name=${model_name}"]){
            sh '''#!/bin/bash
            export PATH=${HOME}/miniconda3/bin/:$PATH
            source activate tf-${tensorflow_version}
            cp -r ${WORKSPACE}/dlft_oob_performance ${WORKSPACE}/tensorflow-intelai-tools/api/
            
            quantization_log_file="${WORKSPACE}/quantization-clx8280-${model_name}-$(date +%s).log"
            cmd="python ${WORKSPACE}/tensorflow-intelai-tools/api/quantize_model_oob.py \
                --model ${model_name} \
                --model_location /tf_dataset/pre-train-model-slim/pbfile/frozen_pb/frozen_${model_name}.pb \
                --data_location /tf_dataset/dataset/TF_mini_imagenet \
                --out_graph ${WORKSPACE}/${model_name}_quantization.pb \
                > ${quantization_log_file} 2>&1"
            echo "RUNCMD: $cmd " >& ${quantization_log_file}
            
            starttime=`date +'%Y-%m-%d %H:%M:%S'`
            eval $cmd >> ${quantization_log_file}
            endtime=`date +'%Y-%m-%d %H:%M:%S'`
            start_seconds=$(date --date="$starttime" +%s);
            end_seconds=$(date --date="$endtime" +%s);           
            echo "quantization time spend: "$((end_seconds-start_seconds))"s "
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
                export PATH=${HOME}/miniconda3/bin/:$PATH
                source activate tf-${tensorflow_version}

                echo "-----------------------run OOB image-classification inference--------------------------"
                cd ${WORKSPACE}/validation-tensorflow
                chmod a+x quantization/scripts/run_slim_models.sh
                ${WORKSPACE}/validation-tensorflow/quantization/scripts/run_slim_models.sh
            '''
        }
    }
}

def call() {
    if (!fileExists("${WORKSPACE}/dlft_oob_performance")) {
        checkout([
                $class                           : 'GitSCM',
                browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                doGenerateSubmoduleConfigurations: false,
                extensions                       : [
                        [$class: 'RelativeTargetDirectory', relativeTargetDir: "dlft_oob_performance"],
                        [$class: 'CloneOption', timeout: 60],
                ],
                submoduleCfg                     : [],
                userRemoteConfigs                : [
                        [credentialsId: "${teamforge_credential}",
                         url          : " https://gitlab.devtools.intel.com/daisyden/dlft_oob_performance.git"]
                ]
        ])
        sh '''#!/bin/bash
            # download submodule
            cd ${WORKSPACE}/dlft_oob_performance 
            git submodule update --init --recursive
        '''
    } else {
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
                # python ${WORKSPACE}/dlft_oob_performance/tensorflow/models/research/slim/setup.py install
                
                # cp running scripts
                cd ${WORKSPACE}/validation-tensorflow/quantization/scripts/slim-scripts/
                slimpath="${WORKSPACE}/dlft_oob_performance/tensorflow/models/research/slim/"
                cp eval_image_classifier_optimize.py datasets2.py preprocessing2.py ${slimpath}
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
                run_inference("${model}", "$precision", "${mode}")
            }
        }
    }
}

return this;