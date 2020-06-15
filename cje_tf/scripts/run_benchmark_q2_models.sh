#!/bin/bash
# run_benchmark.sh --model=[deepSpeech/inception_v4/inception_resnet_v2/fastrcnn/gnmt/rfcn/transformerLanguage/transformerSpeech/wideDeep/InceptionV3/Resnet50] --mode=[training/inference] --single_socket=[True|False]
set -x

if [ -z ${DATASET_LOCATION} ] ; then
    DATASET_LOCATION="/tf_dataset"
fi

DATASET_DIR="$DATASET_LOCATION/dataset"
CHECKPOINT_DIR="$DATASET_LOCATION/pre-trained-models"

HOST=`uname -n | awk -F'.' '{print $1}'`
WORKDIR=`pwd`

SLIM_MODEL_DIR=${WORKDIR}/tensorflow-slim-models
FASTRCNN_DIR=${WORKDIR}/tensorflow-models/research
GNMT_DIR=${WORKDIR}/tensorflow-NMT
SQUEEZE_NET_DIR=${WORKDIR}/tensorflow-SqueezeNet
YOLO_V2_DIR=${WORKDIR}/tensorflow-YoloV2
RFCN_DIR=${WORKDIR}/tensorflow-RFCN
TRANSFORMER_SPEECH_DIR=${WORKDIR}/tensorflow-TransformerSpeech
CJE_TF_SCRIPTS_DIR=${WORKDIR}/cje-tf/scripts
WAVENET_DIR=${WORKDIR}/tensorflow-regular-wavenet
WAVENET_MAGENTA_DIR=${WORKDIR}/tensorflow-WaveNet
WIDE_DEEP_DIR=${WORKDIR}/wideDeep
TRANSFORMER_LANGUAGE_DIR=${WORKDIR}/tensorflow-TransformerLanguage
DEEP_SPEECH_DIR=${WORKDIR}/tensorflow-DeepSpeech
DIR_UNet=${WORKDIR}/tensorflow-UNet
DIR_DRAW=${WORKDIR}/tensorflow-DRAW
DIR_A3C=${WORKDIR}/tensorflow-A3C
DIR_3DGAN=${WORKDIR}/tensorflow-3DGAN
DIR_DCGAN=${WORKDIR}/tensorflow-DCGAN
DIR_INFERENCE=${WORKDIR}/tensorflow-inference/fp32_models_benchmark
DIR_TENSORFLOW=${WORKDIR}/private-tensorflow

if [ $# != "3" ]; then
    echo 'ERROR:'
    echo "Expected 1 parameter got $#"
    printf 'Please use following parameters:
    --model=<model to run>
    --mode=training|inference
    --single_socket=true|false
    ' 
    echo 'All parameters are obligatory.'
    exit 1
fi      

PATTERN='[-a-zA-Z0-9_]*='
for i in "$@"
do      
    case $i in
        --model=*)
            echo $i
            MODEL=`echo $i | sed "s/${PATTERN}//"`;;
        --mode=*)
            MODE=`echo $i | sed "s/${PATTERN}//"`;;
        --single_socket=*)
            SINGLE_SOCKET=`echo $i | sed "s/${PATTERN}//"`;;
        *)
            echo "Parameter $i not recognized."; exit 1;;
    esac
done

LOGFILE_THROUGHPUT=${WORKDIR}/benchmark_${MODEL}_${MODE}_${HOST}_throughput.log
LOGFILE_LATENCY=${WORKDIR}/benchmark_${MODEL}_${MODE}_${HOST}_latency.log

echo ${LOGFILE_THROUGHPUT}
echo ${LOGFILE_LATENCY}

FASTRCNN_BATCHSIZE=1
RFCN_BATCHSIZE=1

export PATH="/nfs/fm/disks/aipg_tensorflow_tools/gcc6.3/bin/:/nfs/fm/disks/aipg_tensorflow_tools/bazel/bin:$PATH"
export LD_LIBRARY_PATH="/nfs/fm/disks/aipg_tensorflow_tools/gcc6.3/lib64:$LD_LIBRARY_PATH"

single_socket_arg=""
if [ ${SINGLE_SOCKET} == "true" ]; then
    single_socket_arg="-s"
fi

if [ ${MODEL} == "inception_v4" ] || [ ${MODEL} == "inception_resnet_v2" ]; then

    echo "model is ${MODEL}"

    cd ${SLIM_MODEL_DIR}

    if [ ${MODE} == "inference" ] ; then

        # run batch size 128 for throughput

        cmd="python run_tf_benchmark.py -b=128 -m=${MODEL} $single_socket_arg -f -v -d=${DATASET_DIR}/TF_Imagenet_FullData -c=${CHECKPOINT_DIR}/${MODEL}/fp32/ >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 128" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

        # run batch size 1 for latency
        cmd="python run_tf_benchmark.py -b=1 -m=${MODEL} $single_socket_arg -f -v -d=${DATASET_DIR}/TF_Imagenet_FullData -c=${CHECKPOINT_DIR}/${MODEL}/fp32/ >> ${LOGFILE_LATENCY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
        echo "Batch Size: 1" >> ${LOGFILE_LATENCY}
        eval $cmd >> ${LOGFILE_LATENCY}

    else 
        echo "skipping ${MODEL} training for now"
        exit 0
    fi

    if [ $? -eq 0 ] ; then 
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi
elif [ ${MODEL} == "inceptionv3" ] || [ ${MODEL} == "resnet50" ]; then

    echo "model is ${MODEL}"

    cd ${DIR_INFERENCE}

    if [ ${MODE} == "inference" ] ; then

        # run batch size 128 for throughput

        cmd="python run_tf_benchmark.py -b=128 -m=${MODEL} $single_socket_arg -f -v -g=${CHECKPOINT_DIR}/${MODEL}/fp32/freezed_${MODEL}.pb >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 128" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

        # run batch size 1 for latency
        cmd="python run_tf_benchmark.py -b=1 -m=${MODEL} $single_socket_arg -f -v -g=${CHECKPOINT_DIR}/${MODEL}/fp32/freezed_${MODEL}.pb >> ${LOGFILE_LATENCY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
        echo "Batch Size: 1" >> ${LOGFILE_LATENCY}
        eval $cmd >> ${LOGFILE_LATENCY}

    else
        echo "skipping ${MODEL} training for now"
        exit 0
    fi

    if [ $? -eq 0 ] ; then
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi
elif [ ${MODEL} == "mobilenet_v1" ] ; then

    echo "model is ${MODEL}"
    cd ${SLIM_MODEL_DIR}

    if [ ${MODE} == "inference" ] ; then

        # run batch size 100 for throughput
        cmd="python run_tf_benchmark.py -b=100 -m=${MODEL} $single_socket_arg -f -v -d=${DATASET_DIR}/TF_Imagenet_FullData -c=${CHECKPOINT_DIR}/${MODEL}/fp32 >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 100" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

        # run batch size 1 for latency
        cmd="python run_tf_benchmark.py -b=1 -m=${MODEL} $single_socket_arg -f -v -d=${DATASET_DIR}/TF_Imagenet_FullData -c=${CHECKPOINT_DIR}/${MODEL}/fp32 >> ${LOGFILE_LATENCY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
        echo "Batch Size: 1" >> ${LOGFILE_LATENCY}
        eval $cmd >> ${LOGFILE_LATENCY}
    else
        echo "skipping ${MODEL} training for now"
        exit 0
    fi

    if [ $? -eq 0 ] ; then
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

elif [ ${MODEL} == "deepSpeech" ] ; then

    # this model only supported with batch size 1 for inference
    echo "model is deepSpeech"
    cd ${DEEP_SPEECH_DIR}

    if [ ${MODE} == "inference" ] ; then

        # run with batch size 1 for both latency and throughput
        cmd="python run_tf_benchmark.py $single_socket_arg -b 1 -f -c ${CHECKPOINT_DIR}/${MODEL}/fp32/checkpoint_from_fully_trained_model -d ${DATASET_DIR}/${MODEL}/librispeech/LibriSpeech/dev-clean-wav/5694-64038-0019.wav >> ${LOGFILE_LATENCY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
        echo "Batch Size: 1" >> ${LOGFILE_LATENCY}
        eval $cmd >> ${LOGFILE_LATENCY}
        #cp ${LOGFILE_LATENCY} ${LOGFILE_THROUGHPUT}

    else
        echo "skipping deep speech training for now"
        exit 0

    fi

    if [ $? -eq 0 ] ; then
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

elif [ ${MODEL} == "fastrcnn" ] ; then

    # this model can only run with batch size 1
    echo "model is fastrcnn"
    cd ${FASTRCNN_DIR}
    export PYTHONPATH=$PYTHONPATH:`pwd`:`pwd`/slim

    # run with batch size 1 for both throughput and latency
    if [ ${MODE} == "inference" ] ; then        
        
        cmd="python run_tf_benchmark.py $single_socket_arg -f -c ${CHECKPOINT_DIR}/${MODEL}/fp32/checkpoint/faster_rcnn_resnet50_coco_2018_01_28/ -g ${CJE_TF_SCRIPTS_DIR}/fastrcnn_pipeline.config  >> ${LOGFILE_LATENCY} 2>&1"
        echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
        echo "Batch Size: ${FASTRCNN_BATCHSIZE}" >> ${LOGFILE_LATENCY}
        eval $cmd >> ${LOGFILE_LATENCY}
        #cp ${LOGFILE_LATENCY} ${LOGFILE_THROUGHPUT}

    else 

        cmd="python run_tf_benchmark.py -c ${CHECKPOINT_DIR}/${MODEL}/fp32/checkpoint/ -g ${CJE_TF_SCRIPTS_DIR}/fastrcnn_resnet50_pets.config >> ${LOGFILE_THROUGHPUT} 2>&1"
        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: ${FASTRCNN_BATCHSIZE}" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

    fi

    if [ $? -eq 0 ] ; then 
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

elif [ ${MODEL} == "gnmt" ] ; then

    echo "model is gnmt"
    cd ${GNMT_DIR}

    if [ ${MODE} == "inference" ] ; then

        # run batch size 1 for latency 
        #cmd="python run_tf_benchmark.py -b=1 $single_socket_arg -f -v -d=/dataset/q2models/NMT/wmt16 -c=/dataset/q2models/NMT/nervana_gnmt_model_8_layer/translate.ckpt-340000  >> ${LOGFILE_LATENCY} 2>&1"
        #cmd="python run_tf_benchmark.py -b=1 $single_socket_arg -f -v -d=/dataset/q2models/NMT/wmt16  >> ${LOGFILE_LATENCY} 2>&1"
        #cmd="python run_tf_benchmark.py -b=1 $single_socket_arg -f -v -d=/dataset/q2models/NMT/wmt16 -IF /dataset/q2models/NMT/wmt16/newstest2015_50w.de -RF /dataset/q2models/NMT/wmt16/newstest2015_50w.en  >> ${LOGFILE_LATENCY} 2>&1"
        cmd="python run_tf_benchmark.py -b=1 $single_socket_arg -f -v -d=${DATASET_DIR}/${MODEL}/wmt16 -c=${CHECKPOINT_DIR}/${MODEL}/fp32/nervana_gnmt_model_4_layer/translate.ckpt -IF ${DATASET_DIR}/${MODEL}/wmt16/newstest2015.tok.bpe.32000.de -RF ${DATASET_DIR}/${MODEL}/wmt16/newstest2015.tok.bpe.32000.en --num_inter_threads=1 --num_intra_threads=26 >> ${LOGFILE_LATENCY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
        echo "Batch Size: 1" >> ${LOGFILE_LATENCY}
        eval $cmd >> ${LOGFILE_LATENCY}

        # run batch size 32 for throughput 
        #cmd="python run_tf_benchmark.py -b=32 $single_socket_arg -f -v -d=${DATASET_DIR}/${MODEL}/wmt16 -c=/dataset/q2models/NMT/nervana_gnmt_model_8_layer/translate.ckpt-340000 >> ${LOGFILE_THROUGHPUT} 2>&1"
        #cmd="python run_tf_benchmark.py -b=32 $single_socket_arg -f -v -d=/dataset/q2models/NMT/wmt16  >> ${LOGFILE_THROUGHPUT} 2>&1"
        cmd="python run_tf_benchmark.py -b=32 $single_socket_arg -f -v -d=${DATASET_DIR}/${MODEL}/wmt16 -c=${CHECKPOINT_DIR}/${MODEL}/fp32/nervana_gnmt_model_4_layer/translate.ckpt -IF ${DATASET_DIR}/${MODEL}/wmt16/newstest2015.tok.bpe.32000.de -RF ${DATASET_DIR}/${MODEL}/wmt16/newstest2015.tok.bpe.32000.en --num_inter_threads=1 --num_intra_threads=26  >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 32" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

    else

        echo "skipping gnmt training for now"
        exit 0
        #cmd="python run_tf_benchmark.py -v -d=/dataset/q2models/NMT/wmt16 >> ${LOGFILE_THROUGHPUT} 2>&1"
        #echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        #echo "Batch Size: 128" >> ${LOGFILE_THROUGHPUT}
        #eval $cmd >> ${LOGFILE_THROUGHPUT}

    fi

    if [ $? -eq 0 ] ; then
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

elif [ ${MODEL} == "rfcn" ] ; then

    # this model can only run with batch size 1
    echo "model is RFCN"
    cd ${RFCN_DIR}

    if [ ${MODE} == "inference" ] ; then

        # run with batch size 1 for both latency and throughput
        cmd="python run_tf_benchmark.py $single_socket_arg -f -v -g ${CJE_TF_SCRIPTS_DIR}/rfcn_pipeline.config -c ${CHECKPOINT_DIR}/${MODEL}/fp32/rfcn_resnet101_coco_2018_01_28   >> ${LOGFILE_LATENCY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
        echo "Batch Size: ${RFCN_BATCHSIZE}" >> ${LOGFILE_LATENCY}
        eval $cmd >> ${LOGFILE_LATENCY}
        #cp ${LOGFILE_LATENCY} ${LOGFILE_THROUGHPUT}

    else
        echo "skipping RFCN training for now"
        exit 0
    fi

    if [ $? -eq 0 ] ; then
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

elif [ ${MODEL} == "SqueezeNet" ] ; then

    echo "model is SqueezeNet"
    cd ${SQUEEZE_NET_DIR}

    if [ ${MODE} == "inference" ] ; then

        # run batch size 64 for throughput
        cmd="python run_tf_benchmark.py -b=64 $single_socket_arg -f -v -d=${DATASET_DIR}/slim_models/TF_Imagenet_FullData -c=ckpt >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 64" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

        # run batch size 1 for latency
        cmd="python run_tf_benchmark.py -b=1 $single_socket_arg -f -v -d=${DATASET_DIR}/slim_models/TF_Imagenet_FullData -c=ckpt >> ${LOGFILE_LATENCY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
        echo "Batch Size: 1" >> ${LOGFILE_LATENCY}
        eval $cmd >> ${LOGFILE_LATENCY}

    else
        echo "skipping SqueezeNet training for now"
        exit 0
    fi

    if [ $? -eq 0 ] ; then
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

elif [ ${MODEL} == "transformerLanguage" ] ; then

    echo "model is transformerLanguage"
    cd ${TRANSFORMER_LANGUAGE_DIR}
    # temproarily workaround 02/08/2019
    # when running inference the model creates a temporarily directory "decode" under the inference checkpoint directory,
    # this conflicts with our Jenkin dataset/checkpoint directory policy, which is read-only
    # workaround to copy the checkpoint directory to a writtable directory before the run
    mkdir tmpCheckpoint
    TMP_CHECKPOINT_DIR=${TRANSFORMER_LANGUAGE_DIR}/tmpCheckpoint
    cp ${CHECKPOINT_DIR}/${MODEL}/fp32/checkpoint ${TMP_CHECKPOINT_DIR} 
    cp ${CHECKPOINT_DIR}/${MODEL}/fp32/model.ckpt-250000* ${TMP_CHECKPOINT_DIR} 

    if [ ${MODE} == "inference" ] ; then

        # run batch size 1 for latency 
        cmd="python run_tf_benchmark.py -b=1 -f $single_socket_arg -v -c ${TMP_CHECKPOINT_DIR} -IF ${CHECKPOINT_DIR}/${MODEL}/fp32/newstest2015.en -RF ${CHECKPOINT_DIR}/${MODEL}/fp32/newstest2015.de -d ${DATASET_DIR}/${MODEL} >> ${LOGFILE_LATENCY} 2>&1"
        #cmd="python run_tf_benchmark.py -b=1 -f $single_socket_arg -v -c ${CHECKPOINT_DIR}/${MODEL}/fp32 -IF ${CHECKPOINT_DIR}/${MODEL}/fp32/newstest2015.en -RF ${CHECKPOINT_DIR}/${MODEL}/fp32/newstest2015.de -d ${DATASET_DIR}/${MODEL} >> ${LOGFILE_LATENCY} 2>&1"
 
        echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
        echo "Batch Size: 1" >> ${LOGFILE_LATENCY}
        eval $cmd >> ${LOGFILE_LATENCY}

        # run batch size 32 for throughput 
        cmd="python run_tf_benchmark.py -b=32 -f $single_socket_arg -v -c ${TMP_CHECKPOINT_DIR} -IF ${CHECKPOINT_DIR}/${MODEL}/fp32/newstest2015.en -RF ${CHECKPOINT_DIR}/${MODEL}/fp32/newstest2015.de -d ${DATASET_DIR}/${MODEL}>> ${LOGFILE_THROUGHPUT} 2>&1"
        #cmd="python run_tf_benchmark.py -b=32 -f $single_socket_arg -v -c ${CHECKPOINT_DIR}/${MODEL}/fp32 -IF ${CHECKPOINT_DIR}/${MODEL}/fp32/newstest2015.en -RF 

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 32" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

    else

        echo "skipping RFCN training for now"
        exit 0

    fi

    if [ $? -eq 0 ] ; then
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

elif [ ${MODEL} == "transformerSpeech" ] ; then

    echo "model is transformerSpeech"
    cd ${TRANSFORMER_SPEECH_DIR}

    if [ ${MODE} == "inference" ] ; then

        # run batch size 1 for latency 
        # ??? checkpoint
        cmd="python run_tf_benchmark.py -b 1 -f $single_socket_arg -M transformer -H transformer_librispeech -P librispeech_clean_small -d ${DATASET_DIR}/${MODEL} -c ./speechrecog_librispeech_trainedmodel -N 10 >> ${LOGFILE_LATENCY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
        echo "Batch Size: 1" >> ${LOGFILE_LATENCY}
        eval $cmd >> ${LOGFILE_LATENCY}

        # run batch size 128 for throughput
        cmd="python run_tf_benchmark.py -b 128 -f $single_socket_arg -M transformer -H transformer_librispeech -P librispeech_clean_small -d ${DATASET_DIR}/${MODEL} -c ./speechrecog_librispeech_trainedmodel -N 10 >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 128" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

    else

        echo "skipping ${MODEL} training for now"
        exit 0

        #cmd="python run_tf_benchmark.py -M transformer -H transformer_librispeech -P librispeech_clean_small -d ${DATASET_DIR}/${MODEL} -c ./output >> ${LOGFILE_THROUGHPUT} 2>&1"
        #echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        #echo "Batch Size: 6M" >> ${LOGFILE_THROUGHPUT}
        #eval $cmd >> ${LOGFILE_THROUGHPUT}

    fi

    if [ $? -eq 0 ] ; then
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

elif [ ${MODEL} == "YoloV2" ] ; then

    echo "model is YoloV2"
    cd ${YOLO_V2_DIR}

    if [ ${MODE} == "inference" ] ; then

        # run batch size 8 for throughput
        cmd="python run_tf_benchmark.py -b=8 $single_socket_arg -f -v -d=${DATASET_DIR}/${MODEL}/JPEGImages/ --pbLoad ${CHECKPOINT_DIR}/${MODEL}/fp32/yolov2-voc.pb --metaLoad built_graph/yolov2-voc.meta >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 8" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

        # run batch size 1 for latency
        cmd="python run_tf_benchmark.py -b=1 $single_socket_arg -f -v -d=${DATASET_DIR}/${MODEL}/JPEGImages/ --pbLoad ${CHECKPOINT_DIR}/${MODEL}/fp32/yolov2-voc.pb --metaLoad built_graph/yolov2-voc.meta >> ${LOGFILE_LATENCY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
        echo "Batch Size: 1" >> ${LOGFILE_LATENCY}
        eval $cmd >> ${LOGFILE_LATENCY}

    else
        echo "skipping YoloV2 training for now"
        exit 0
    fi

    if [ $? -eq 0 ] ; then
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi
elif [ ${MODEL} == "WaveNet" ] ; then

    echo "model is WaveNet"
    cd ${WAVENET_DIR}

    if [ ${MODE} == "inference" ] ; then

        # run batch size 1 for both throughput and latency
        cmd="python run_tf_benchmark.py -n 1 -s -f -v -c=${CHECKPOINT_DIR}/${MODEL}/fp32/model.ckpt-99 --sample=8510 >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 1" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}
    else
        echo "skipping Wavenet training for now"
        exit 0
    fi

    if [ $? -eq 0 ] ; then
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

elif [ ${MODEL} == "wideDeep" ] ; then

    echo "model is wideDeep"
    cd ${WIDE_DEEP_DIR}

    if [ ${MODE} == "inference" ] ; then

        # run batch size 1024 for throughput 
        cmd="python ./official/wide_deep/run_tf_benchmark.py -f -s -d=${DATASET_DIR}/${MODEL} -c=${CHECKPOINT_DIR}/${MODEL}/fp32 -v -b=1024 >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 1024" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}

        # run batch size 1 for latency 
        cmd="python ./official/wide_deep/run_tf_benchmark.py -f -s -d=${DATASET_DIR}/${MODEL} -c=${CHECKPOINT_DIR}/${MODEL}/fp32 -v -b=1 >> ${LOGFILE_LATENCY} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_LATENCY}
        echo "Batch Size: 1" >> ${LOGFILE_LATENCY}
        eval $cmd >> ${LOGFILE_LATENCY}

    else

        echo "skipping wideDeep training for now"
        exit 0

    fi

    if [ $? -eq 0 ] ; then
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi
elif [ ${MODEL} == "WaveNet_Magenta" ] ; then

    echo "model is ${MODEL}"
    cd ${WAVENET_MAGENTA_DIR}

    if [ ${MODE} == "inference" ] ; then

        # install magenta
        ${WAVENET_MAGENTA_DIR}/magenta/tools/build.sh

        mkdir -p ${WAVENET_MAGENTA_DIR}/wavenet_magenta_save

        # run batch size 1 for both throughput and latency
        cmd="python run_tf_benchmark.py -n 16 -s -f --c=${CHECKPOINT_DIR}/${MODEL}/fp32/model.ckpt-200000 -d=${DATASET_DIR}/${MODEL} --data-save-path=${WAVENET_MAGENTA_DIR}/wavenet_magenta_save --sample=10510 >> ${LOGFILE_THROUGHPUT} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        echo "Batch Size: 1" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}
    else
        echo "skipping Wavenet training for now"
        exit 0
    fi

    if [ $? -eq 0 ] ; then
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

# Shanghai OOB Models #######
elif [ ${MODEL} == "UNet" ] || [ ${MODEL} == "DRAW" ] || [ ${MODEL} == "A3C" ] || [ ${MODEL} == "3DGAN" ]; then
    
    echo "model is ${MODEL}"
    cd $(eval "echo \${DIR_${MODEL}}")    

    if [ ${MODE} == "inference" ] ; then
        
        # inference cmd
        cmd="python run_tf_benchmark.py --inference-only ${single_socket_arg} "
        
        case "${MODEL}" in 
            "UNet")
                cmd+=" --checkpoint ${CHECKPOINT_DIR}/${MODEL}/fp32/unet_trained/model.cpkt "
                fps_default_bs=1
            ;;
            "DRAW")
                cmd+=" --checkpoint ./drawmodel.ckpt "
                fps_default_bs=100
            ;;
            "A3C")
                cmd+=" --checkpoint ${CHECKPOINT_DIR}/${MODEL}/fp32/checkpoints "
                fps_default_bs=1
            ;;
            "3DGAN")
                pip install scikit-learn==0.19
                cmd+=" --data-location ${DATASET_DIR}/${MODEL}/Ele_v1_1_2.h5 "
                fps_default_bs=128
            ;;
            *)
                echo "The model: No such ${MODEL} in OOB models!"
            ;;
        esac
    
        # batch size 1 for latency
        latency_cmd=" ${cmd} --batch-size 1 >> ${LOGFILE_LATENCY} 2>&1 "
        echo "RUNCMD: ${latency_cmd} " >& ${LOGFILE_LATENCY}
        # echo "Batch Size: 1" >> ${LOGFILE_LATENCY}
        eval ${latency_cmd} >> ${LOGFILE_LATENCY}
        
        # batch size default for throughput
        fps_cmd=" ${cmd} --batch-size ${fps_default_bs} >> ${LOGFILE_THROUGHPUT} 2>&1"        
        echo "RUNCMD: ${fps_cmd} " >& ${LOGFILE_THROUGHPUT}
        # echo "Batch Size: ${fps_default_bs}" >> ${LOGFILE_THROUGHPUT}
        eval ${fps_cmd} >> ${LOGFILE_THROUGHPUT}
    else
        echo "skipping ${MODEL} training for now"
        exit 0
    fi
    
    if [ $? -eq 0 ] ; then
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi
    
elif [ ${MODEL} == "DCGAN" ]; then  ### only throughput
    
    echo "model is ${MODEL}"
    cd $(eval "echo \${DIR_${MODEL}}")    

    if [ ${MODE} == "inference" ] ; then
        
        # run batch size 100 for throughput
        cmd="python run_tf_benchmark.py  --inference-only ${single_socket_arg} --batch-size 100 \
                           --checkpoint ${CHECKPOINT_DIR}/${MODEL}/fp32/unconditional \
                           --data-location ${DATASET_DIR}/${MODEL}/cifar10 \
                           >> ${LOGFILE_THROUGHPUT} 2>&1"
        
        echo "RUNCMD: $cmd " >& ${LOGFILE_THROUGHPUT}
        # echo "Batch Size: 100" >> ${LOGFILE_THROUGHPUT}
        eval $cmd >> ${LOGFILE_THROUGHPUT}
    else
        echo "skipping ${MODEL} training for now"
        exit 0
    fi
    
    if [ $? -eq 0 ] ; then
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

else
    echo "model is not recognized"
    STATUS="FAILURE"
fi

                
if [ $STATUS == "SUCCESS" ]; then 
    exit 0
else
    exit 1
fi
