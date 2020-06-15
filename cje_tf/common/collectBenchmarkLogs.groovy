// Dynamic function collectBenchmarkLogs(models, modes, single_socket, full_validation)
//
// Parameters:
//
//     models             to collect logs for the list of models separated by comma 
//
//     modes              run modes: training and/or inference separated by comma
//
//     single_socket      whether the run is single socket or not 
//
//     full_validation    whether the run is benchmark or convergence
//
//     run_Q1models       whether the run is for Q1 models
//
//     run_Q2models       whether the run is for Q2 models
//
// Returns: nothing
//
// External dependencies: None
// Q1 models: 
//    Q1_MODELS="resnet50 inception3 vgg16 ds2 SSDvgg16 mnist resnet32cifar10 cifar10 dcgan"
// Q2 models:
//    Q2MODELS="inception_v4 inception_resnet_v2 SqueezeNet YoloV2 fastrcnn gnmt rfcn transformerLanguage transformerSpeech WaveNet wideDeep WaveNet_Magenta deepSpeech mobilenet_v1"

def call(String models, \
         String modes, \
         Boolean single_socket, \
         Boolean full_validation, \
         Boolean run_Q1models, \
         Boolean run_Q2models) {

    echo "---------------------------------------------------------"
    echo "------------  running collectBenchnmarkLog  -------------"
    echo "---------------------------------------------------------"

    echo "DBG models: $models"
    echo "DBG modes: $modes"
    echo "DBG run_Q1models: $run_Q1models"
    echo "DBG run_Q2models: $run_Q2models"

    SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                          returnStdout: true).trim()

    models=models.split(',')
    modes=modes.split(',')

    models.each { model ->
        echo "model is ${model}"
        modes.each { mode ->
            echo "mode is ${mode}"
            withEnv(["current_model=$model","current_mode=$mode", "single_socket=${single_socket}", "full_validation=${full_validation}", "servername=${SERVERNAME}", "run_q1=${run_Q1models}", "run_q2=${run_Q2models}"]) {
                
                sh '''#!/bin/bash -x
                # Q1 models
                Q1_MODELS="resnet50 inception3 vgg16 ds2 SSDvgg16 mnist resnet32cifar10 cifar10 dcgan"
                # Q2 models
                Q2MODELS="inception_v4 inception_resnet_v2 SqueezeNet YoloV2 fastrcnn gnmt rfcn transformerLanguage transformerSpeech WaveNet wideDeep WaveNet_Magenta deepSpeech mobilenet_v1"
                # Checking the model we want to run is a Q1 model or Q2 model
                echo $Q1_MODELS | grep -w -F -q $current_model
                if [ $run_q1 == "true" ]; then
                    echo "$current_model is a Q1 model"
                    if [ -f "$WORKSPACE/benchmark_${current_model}_${current_mode}_${servername}.log" ] || 
                       [ -f "$WORKSPACE/benchmark_${current_model}_${current_mode}_${servername}_accuracy.log" ] ||
                       [ -f "$WORKSPACE/benchmark_${current_model}_${current_mode}_${servername}_latency.log" ] ||
                       [ -f "$WORKSPACE/benchmark_${current_model}_${current_mode}_${servername}_throughput.log" ]; then
                        chmod 775 $WORKSPACE/cje-tf/scripts/collect_logs.sh
                        $WORKSPACE/cje-tf/scripts/collect_logs.sh --model=${current_model} --mode=${current_mode} --fullvalidation=${full_validation} --single_socket=${single_socket}
                    fi
                fi
                
                if [ $run_q2 == "true" ]; then
                    echo "$current_model is a Q2 model"
                    chmod 775 $WORKSPACE/cje-tf/scripts/collect_logs_q2_models.sh
                    $WORKSPACE/cje-tf/scripts/collect_logs_q2_models.sh --model=${current_model} --mode=${current_mode} --fullvalidation=${full_validation} --single_socket=${single_socket}
                  
                fi

                if [ $? -eq 0 ] ; then
                    echo "running model ${current_model} ${current_mode} success"
                    RESULT="SUCCESS"
                else
                    echo "running model ${current_model} ${current_mode} fail"
                    RESULT="FAILURE"
                fi
                '''
            }
        }
    }
    echo "done running collectBenchmarkLog ......."
    stash allowEmpty: true, includes: "*.log", name: "logfile"
}

return this;
