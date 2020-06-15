// Dynamic function post2AIBTdashboard(channel, buildStatus, msg)
//
// Parameters:
//
//     runtype       posting data type, e.g. tfdo-inference, or tfdo-accuracy
//
//     framework     framework name, use tensorflow-do for now
//
//     frontend      framework name, use tensorflow-do for now
//
//     target        target server: e.g. default(test server), or production 
//
//     logdir        the models log directory 
//
//     logtypes      the types of the log, e.g. latency, throughput, or accuracy
//    
//     models        the list of network model names that needs to be posted to dashboard
//
//     datatype      the datatypes that needs to be  posted to dashboard, e.g. float32, int8
//
// Returns: nothing
//
// External dependencies: None

def call(String runtype, \
         String framework, \
         String frontend, \
         String target = 'default', \
         String logdir,\
         List<String> logtypes, \
         List<String> models, \
         String datatype) {

    stage('Post to AIBT dashboard') {

        CJE_TF_COMMON_DIR="$WORKSPACE/cje-tf/common"
        def post2AIBTdashboard = load("${CJE_TF_COMMON_DIR}/post2AIBTdashboard.groovy")
        SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                          returnStdout: true).trim()

        for (model in models) {
            for ( logtype in logtypes ) {

                // log files generated from model-zoo exists under the WORKSPACE/<model> directory and follow the format: 
                // benchmark_<model>_inference_<fp32/int8>_<accuracy/latency/throughput>_<py2/py3>_servername.log
                if (fileExists("${WORKSPACE}/${model}")) {
                    if ( datatype == 'float32' ) {
                        model_zoo_logfile = sh (script:"find ${WORKSPACE}/${model} -name benchmark_${model}_inference_fp32_${logtype}* || true", 
                                                returnStdout: true).trim()
                    }
                    else {
                        model_zoo_logfile = sh (script:"find ${WORKSPACE}/${model} -name benchmark_${model}_inference_${datatype}_${logtype}* || true", 
                                                returnStdout: true).trim()
                    }
                    println "model_zoo_logfile is $model_zoo_logfile"

                    logfiles=model_zoo_logfile.split(' ')
                    logfiles.each { logfile -> 
                        echo "logfile is $logfile" 
                        if ( fileExists(logfile) ) {
                            println "DEBUG: $logfile exist=yes, use model_zoo_logfile: $logfile"
                            logfile = model_zoo_logfile 
                            post2AIBTdashboard(runtype, framework, frontend, target, logdir, logfile, model, datatype)
                        }  else {
                            println "DEBUG: $logfile exist=no"
                        }
                    }
                }

                // if not running from model-zoo then logs are generated under the WORKSPACE dir
                else {
                    if ( model == 'DenseNet' || model == '3DUNet' || model == 'MaskRCNN' ) {
                        println "model is $model"
                        logfile = "${logdir}/Q2_models_${model}_${logtype}.log"
                    }
                    else if ( model == 'vgg16' ) {
                        println "model is vgg16 or ssd_vgg16"
                        logfile = "${logdir}/benchmark_${model}_inference_${SERVERNAME}.log"
                    }
                    else {
                        println "model is $model"
                        logfile = "${logdir}/benchmark_${model}_inference_${SERVERNAME}_${logtype}.log"
                    }
                    println "logfile is $logfile"
                    // post only if logfile exists
                    if (fileExists(logfile)) {
                        post2AIBTdashboard(runtype, framework, frontend, target, logdir, logfile, model, datatype)
                    }

                }

            }
        }

    }
}

return this;
