#!/bin/bash -x

if [ -z ${WORKSPACE} ] ; then
    WORKSPACE=`pwd`
fi
if [ -z ${DATASET_LOCATION} ] ; then
    DATASET_LOCATION="/tf_dataset"
fi
if [ -z ${TARGET_PLATFORM} ] ; then
    TARGET_PLATFORM="avx2"
fi
if [ -z ${TENSORFLOW_BRANCH} ] ; then
    TENSORFLOW_BRANCH="master"
fi
if [ -z ${SINGLE_SOCKET} ] ; then
    SINGLE_SOCKET=true
fi
if [ -z ${SERVERNAME} ] ; then
    SERVERNAME=`uname -n | awk -F'.' '{print $1}'`
fi
if [ -z ${DOCKER_IMAGE} ] ; then
    DOCKER_IMAGE="amr-registry.caas.intel.com/aipg-tf/qa:nightly-master-avx2-devel-mkl"
fi

if [ -z ${DATA_TYPE} ] ; then
    DATA_TYPE="fp32"
fi

echo 'Running with parameters:'
echo "    WORKSPACE: ${WORKSPACE}"
echo "    DATASET_LOCATION: ${DATASET_LOCATION}"
echo '    Mounted volumes:'
echo "        WORKSPACE mounted on: /workspace"
echo "        DATASET_LOCATION mounted on: ${DATASET_LOCATION}"
echo "    TARGET_PLATFORM: ${TARGET_PLATFORM}"
echo "    TENSORFLOW_BRANCH: ${TENSORFLOW_BRANCH}"
echo "    SINGLE_SOCKET: ${SINGLE_SOCKET}"
echo "    SERVERNAME: ${SERVERNAME}"
echo "    MODELS: ${MODELS}"
echo "    MODES: ${MODES}"
echo "    DATA_TYPE: ${DATA_TYPE}"

echo "---------------------------------------------------------" 
echo 'Running with parameters:'
echo "    model_name:                    ${model_name}"
echo "    precision:                     ${precision}" 
echo "    mode:                          ${mode}"
echo "    accuracy:                      ${accuracy}"
echo "    framework:                     ${framework}"
echo "    batch_size:                    ${batch_size}"
echo "    socket_id:                     ${socket_id}"
echo "    verbose:                       ${verbose}"
echo "    in_docker_image:               ${in_docker_image}"
echo "    in_data_location:              ${in_data_location}"
echo "    in_data_location_accuracy:     ${in_data_location_accuracy}"
echo "    in_data_location_int8:         ${in_data_location_int8}"
echo "    in_graph:                      ${in_graph}"
echo "    https_proxy:                   ${https_proxy}"
echo "    http_proxy:                    ${http_proxy}"
echo "    logfile:                       ${logfile}" 
echo "    model_sourcedir:               ${model_sourcedir}"
echo "    model_checkpoint:              ${model_checkpoint}"
echo "    model_arguments:               ${model_arguments}"
echo "    model_arguments_accuracy:      ${model_arguments_accuracy}"
echo "    model_arguments_fp32:          ${model_arguments_fp32}"
echo "    model_arguments_int8:          ${model_arguments_int8}"
echo "    model_num_inter_threads:       ${model_num_inter_threads}"
echo "    model_num_intra_threads:       ${model_num_intra_threads}"
echo "    model_data_num_inter_threads:  ${model_data_num_inter_threads}"
echo "    model_data_num_intra_threads:  ${model_data_num_intra_threads}"

WORKDIR=`pwd`
PRETRAINED_MODELS_DIR=${DATASET_LOCATION}/pre-trained-models
INTEL_MODELS_BENCHMARKS_DIR=${WORKDIR}/intel-models/benchmarks/
TENSORFLOW_MODELS_SOURCE_DIR=${WORKDIR}/${model_sourcedir}/
MASKRCNN_DIR=${WORKDIR}/Mask-RCNN
TENSORFLOW_MODELS_NCF_SOURCE_DIR=${WORKDIR}/models-ncf
TENSORFLOW_MODELS_RFCN_SOURCE_DIR=${WORKDIR}/models-rfcn
TRANSFORMER_LANGUAGE_DIR=${WORKDIR}/tensor2tensor
TRANSFORMER_LANGUAGE_TMP_CHECKPOINT_DIR=${TRANSFORMER_LANGUAGE_DIR}/tmpCheckpoint

run_benchmark() {

    if [ ${model_name} == "ncf" ]; then
      cd ${TENSORFLOW_MODELS_NCF_SOURCE_DIR}/intel-models/benchmarks
    else
      cd ${INTEL_MODELS_BENCHMARKS_DIR}
    fi

    default_cmd="python launch_benchmark.py \
          --model-name ${model_name} \
          --precision ${precision} \
          --mode ${mode} \
          --framework ${framework} \
          --batch-size ${batch_size} \
          --socket-id ${socket_id} \
          --docker-image ${in_docker_image} "

    additional_arg="-- https_proxy=${https_proxy} \
                       http_proxy=${http_proxy} \
                       DEBIAN_FRONTEND=noninteractive"

    if [ ${accuracy} == "true" ] ; then
        benchmark_arg="--accuracy-only "
    else
        benchmark_arg="--benchmark-only "
    fi

    if [[ ${model_arguments} == *null* ]]; then
        :
    else
        additional_arg="${additional_arg} ${model_arguments} "
    fi

    if [[ ${in_data_location} == *null* ]]; then
        data_location_arg=""
    else
        data_location_arg="--data-location ${in_data_location} "
    fi

    if [[ ${in_graph} == *null* ]]; then
        in_graph_arg=""
    else
        in_graph_arg="--in-graph ${in_graph} "
    fi

    if [[ ${model_checkpoint} == *null* ]]; then
        checkpoint_arg=""
    else
        if [ ${model_name} == "transformer_language" ] ; then
            checkpoint_arg="--checkpoint ${TRANSFORMER_LANGUAGE_TMP_CHECKPOINT_DIR} "
        else
            checkpoint_arg="--checkpoint ${model_checkpoint} "
        fi
    fi

    if [[ ${model_sourcedir} == *null* ]]; then
        model_source_dir_arg=""
    else
        model_source_dir_arg="--model-source-dir ${TENSORFLOW_MODELS_SOURCE_DIR} "
    fi

    if [[ ${model_num_inter_threads} == *null* ]]; then
        model_num_inter_threads=""
    else
        model_num_inter_threads="--num-inter-threads ${model_num_inter_threads} "
    fi

    if [[ ${model_num_intra_threads} == *null* ]]; then
        model_num_intra_threads=""
    else
        model_num_intra_threads="--num-intra-threads ${model_num_intra_threads} "
    fi

    if [[ ${model_data_num_inter_threads} == *null* ]]; then
        model_data_num_inter_threads=""
    else
        model_data_num_inter_threads="--data-num-inter-threads ${model_data_num_inter_threads} "
    fi

    if [[ ${model_data_num_intra_threads} == *null* ]]; then
        model_data_num_intra_threads=""
    else
        model_data_num_intra_threads="--data-num-intra-threads ${model_data_num_intra_threads} "
    fi

    if [ ${verbose} == "true" ] ; then
        verbose_arg="--verbose "
    else
        verbose_arg=""

    fi

    # accuracy
    if [ ${accuracy} == "true" ] ; then

        if [ ${model_name} == "mobilenet_v1" ] || [ ${model_name} == "inception_resnet_v2" ] ; then
             
            cmd="${default_cmd} \
                ${data_location_arg} \
                ${in_graph_arg} \
                ${model_source_dir_arg} \
                ${benchmark_arg} \
                ${verbose_arg} \
                ${additional_arg} >> ${logfile} 2>&1"

        elif [ ${model_name} == "faster_rcnn" ]; then

            if [ ${precision} == "fp32" ]; then

                cmd="${default_cmd} \
                    ${data_location_arg} \
                    ${in_graph_arg} \
                    ${model_source_dir_arg} \
                    ${benchmark_arg} \
                    ${verbose_arg} \
                    ${additional_arg} >> ${logfile} 2>&1"

            else

                data_location_arg="--data-location ${in_data_location_accuracy} "

                cmd="${default_cmd} \
                    ${data_location_arg} \
                    ${in_graph_arg} \
                    ${model_source_dir_arg} \
                    ${benchmark_arg} \
                    ${verbose_arg} \
                    ${additional_arg} >> ${logfile} 2>&1"
            fi

        elif [ ${model_name} == "rfcn" ]; then

            data_location_arg="--data-location ${in_data_location_accuracy} "

            cmd="${default_cmd} \
                ${data_location_arg} \
                ${in_graph_arg} \
                ${model_source_dir_arg} \
                ${benchmark_arg} \
                ${verbose_arg} \
                ${additional_arg}  \
                ${model_arguments_accuracy} >> ${logfile} 2>&1"

        else
            cmd="${default_cmd} \
                ${data_location_arg} \
                ${checkpoint_arg} \
                ${in_graph_arg} \
                ${model_source_dir_arg} \
                ${benchmark_arg} \
                ${verbose_arg} \
                ${additional_arg} >> ${logfile} 2>&1"
       fi

    # latency / throughput
    else
        
        if [ ${model_name} == "mobilenet_v1" ]; then

            cmd="${default_cmd} \
               ${data_location_arg} \
               ${checkpoint_arg} \
               ${model_source_dir_arg} \
               ${benchmark_arg} \
               ${verbose_arg} \
               ${additional_arg} >> ${logfile} 2>&1"

        elif [ ${model_name} == "faster_rcnn" ]; then

            if [ ${precision} == "int8" ]; then

                data_location_arg="--data-location ${in_data_location_int8} "

                cmd="${default_cmd} \
                   ${data_location_arg} \
                   ${model_source_dir_arg} \
                   ${in_graph_arg} \
                   ${benchmark_arg} \
                   ${verbose_arg} \
                   ${additional_arg} \
                   ${model_arguments_int8} >> ${logfile} 2>&1"
            else

                cmd="${default_cmd} \
                   ${data_location_arg} \
                   ${checkpoint_arg} \
                   ${model_source_dir_arg} \
                   ${benchmark_arg} \
                   ${verbose_arg} \
                   ${additional_arg} \
                   ${model_arguments_fp32} >> ${logfile} 2>&1"

            fi

        elif [ ${model_name} == "inception_resnet_v2" ]; then

            cmd="${default_cmd} \
               ${in_graph_arg} \
               ${benchmark_arg} \
               ${verbose_arg} \
               ${additional_arg} >> ${logfile} 2>&1"

        elif [ ${model_name} == "rfcn" ]; then

            if [ ${precision} == "int8" ]; then       

                data_location_arg="--data-location ${in_data_location_int8} "

                cmd="${default_cmd} \
                   ${data_location_arg} \
                   ${model_source_dir_arg} \
                   ${in_graph_arg} \
                   ${benchmark_arg} \
                   ${verbose_arg} \
                   ${additional_arg}  \
                   ${model_arguments_int8} >> ${logfile} 2>&1"
            else

                cmd="${default_cmd} \
                   ${data_location_arg} \
                   ${model_source_dir_arg} \
                   ${checkpoint_arg} \
                   ${benchmark_arg} \
                   ${verbose_arg} \
                   ${additional_arg}  \
                   ${model_arguments_fp32} >> ${logfile} 2>&1"
            fi

        elif [ ${model_name} == "ssd-mobilenet" ]; then

            if [ ${precision} == "int8" ]; then       

                data_location_arg="--data-location ${in_data_location_int8} "
            fi

            cmd="${default_cmd} \
               ${data_location_arg} \
               ${model_source_dir_arg} \
               ${in_graph_arg} \
               ${benchmark_arg} \
               ${verbose_arg} \
               ${additional_arg} >> ${logfile} 2>&1"

        elif [ ${model_name} == "wide_deep_large_ds" ]; then

            cmd="${default_cmd} \
               ${data_location_arg} \
               ${model_source_dir_arg} \
               ${in_graph_arg} \
               ${benchmark_arg} \
               ${verbose_arg} \
               ${additional_arg} >> ${logfile} 2>&1"

        else
            cmd="${default_cmd} \
               ${data_location_arg} \
               ${checkpoint_arg} \
               ${in_graph_arg} \
               ${model_source_dir_arg} \
               ${benchmark_arg} \
               ${verbose_arg} \
               ${model_num_inter_threads} \
               ${model_num_intra_threads} \
               ${model_data_num_inter_threads} \
               ${model_data_num_intra_threads} \
               ${additional_arg} >> ${logfile} 2>&1"
        fi

    fi

     echo "RUNCMD: $cmd " >& ${logfile}
     echo "Batch Size: ${batch_size}" >> ${logfile}
     eval $cmd >> ${logfile}

}
                           
case ${model_name} in
    
    "faster_rcnn")
        echo "model is faster_rcnn"
        # temporarily workaround
        cd ${WORKDIR}/models/research/object_detection
        chmod 777 metrics
        cd "metrics"
        chmod 777 offline_eval_map_corloc.py
        sed -i.bak 162s/eval_input_config/eval_input_configs/ offline_eval_map_corloc.py
        sed -i.bak 91s/input_config/input_config[0]/ offline_eval_map_corloc.py
        sed -i.bak 92s/input_config/input_config[0]/ offline_eval_map_corloc.py
        sed -i.bak 95s/input_config/input_config[0]/ offline_eval_map_corloc.py

        if [[ ${logfile} == *throughput* ]]; then
            :
        else
            run_benchmark
        fi
        ;;
    "inception_resnet_v2")
        echo "model is inception_resnet_v2" 
        run_benchmark
        ;;
    "inceptionv3")
        echo "model is inceptionv3" 
        run_benchmark
        ;;
    "inceptionv4")
        echo "model is inceptionv4" 
        run_benchmark
        ;;
    "maskrcnn")
        echo "model is maskrcnn"
        #cd ${MASKRCNN_DIR}
        cp ${DATASET_LOCATION}/dataset/MaskRCNN/mask_rcnn_coco.h5 ${MASKRCNN_DIR}
        # no accuracy run, only run once with batch size 1 for both latency and throughput
        if [[ ${logfile} == *throughput* ]]; then
            run_benchmark
        fi
        ;;
    "mobilenet_v1")
        echo "model is mobilenet_v1" 
        run_benchmark 
        ;;
    "ncf")
        echo "model is ncf" 
        run_benchmark
        ;;
    "resnet50")
        echo "model is resnet50" 
        run_benchmark
        ;;
    "resnet50v1_5")
        echo "model is resnet50v1_5" 
        run_benchmark
        ;;
    "resnet101")
        echo "model is resnet101" 
        run_benchmark
        ;;
    "rfcn")
        echo "model is rfcn"
        # temporarily workaround
        #cd ${WORKDIR}/models/research/object_detection
        cd ${TENSORFLOW_MODELS_RFCN_SOURCE_DIR}/research/object_detection
        chmod 777 metrics
        cd "metrics"
        chmod 777 offline_eval_map_corloc.py
        sed -i.bak 162s/eval_input_config/eval_input_configs/ offline_eval_map_corloc.py
        sed -i.bak 91s/input_config/input_config[0]/ offline_eval_map_corloc.py
        sed -i.bak 92s/input_config/input_config[0]/ offline_eval_map_corloc.py
        sed -i.bak 95s/input_config/input_config[0]/ offline_eval_map_corloc.py

        # only run once with batch size 1 to get both latency and throughput
        if [[ ${logfile} == *throughput* ]]; then
            :
        else 
            run_benchmark
        fi
        ;;
    "ssd-mobilenet")
        echo "model is ssd-mobilenet"
        # only run once with batch size 1 to get both latency and throughput
        if [[ ${logfile} == *latency* ]]; then
            :
        else
            run_benchmark
        fi
        ;;
    "ssd_vgg16")
        echo "model is ssd_vgg16"
        # only run once with batch size 1 to get both latency and throughput
        if [[ ${logfile} == *latency* ]]; then
            :
        else
            run_benchmark
        fi
        ;;
    "transformer_language")
        echo "model is transformer_language"
        # workaround
        # when running inference the model creates a temporarily directory "decode" under the inference checkpoint directory,
        # this conflicts with our Jenkin dataset/checkpoint directory policy, which is read-only
        # workaround to copy the checkpoint directory to a writtable directory before the run
        if [ ! -d ${TRANSFORMER_LANGUAGE_TMP_CHECKPOINT_DIR} ]; then
            mkdir -p ${TRANSFORMER_LANGUAGE_TMP_CHECKPOINT_DIR}
            cp ${PRETRAINED_MODELS_DIR}/transformerLanguage/fp32/checkpoint ${TRANSFORMER_LANGUAGE_TMP_CHECKPOINT_DIR}
            cp ${PRETRAINED_MODELS_DIR}/transformerLanguage/fp32/model.ckpt-250000* ${TRANSFORMER_LANGUAGE_TMP_CHECKPOINT_DIR}
            cp ${PRETRAINED_MODELS_DIR}/transformerLanguage/fp32/newstest2015.de ${TRANSFORMER_LANGUAGE_TMP_CHECKPOINT_DIR}
            cp ${PRETRAINED_MODELS_DIR}/transformerLanguage/fp32/newstest2015.en ${TRANSFORMER_LANGUAGE_TMP_CHECKPOINT_DIR}
        fi

        # not running accuracy
        if [[ ${logfile} == *accuracy* ]]; then
            :
        else
            run_benchmark
        fi
        ;;
    "wide_deep")
        echo "model is wide_deep"
        # not running accuracy
        if [[ ${logfile} == *accuracy* ]]; then
            :
        else
            run_benchmark
        fi
        ;;
    "wide_deep_large_ds")
        echo "model is wide_deep_large_ds"
        run_benchmark
        ;;
    *)
        echo "model is not recognized" 
        ;;
esac
