#!/bin/bash
# run_benchmark.sh --model=[resnet50/inception3/vgg16/ds2/SSDvgg16/nmt/mnist/resnet32cifar10/alexnet/googlenet/cifar10/dcgan/gnmt] --mode=[training/inference] --single_socket=[true|false]
set -x

if [ -z ${DATASET_LOCATION} ] ; then
    DATASET_LOCATION="/tf_dataset"
fi

DATASET_DIR="$DATASET_LOCATION/dataset"
CHECKPOINT_DIR="$DATASET_LOCATION/pre-trained-models"

HOST=`uname -n | awk -F'.' '{print $1}'`
WORKDIR=`pwd`
source ${WORKDIR}/venv/bin/activate
echo ${WORKDIR}
DS2DIR=${WORKDIR}/deepSpeech2/src
echo ${DS2DIR}
SSDVGG16INFDIR=${WORKDIR}/tensorflow-SSD-Inference
SSDVGG16TRDIR=${WORKDIR}/tensorflow-SSD-Training
NMTDIR=${WORKDIR}/tensorflow-NMT
echo ${NMTDIR}
if [ -d ${WORKDIR}/private-tensorflow ]; then
    MNISTDIR=${WORKDIR}/private-tensorflow/tensorflow/examples/tutorials/mnist
else 
    MNISTDIR=${WORKDIR}/tensorflow/tensorflow/examples/tutorials/mnist
fi
echo ${MNISTDIR}
MODELSDIR=${WORKDIR}/models/
echo ${MODELSDIR}
RN32CIFAR10DIR=${WORKDIR}/models/official/resnet
echo ${RN32CIFAR10DIR}
CIFAR10DIR=${WORKDIR}/models/tutorials/image/cifar10
echo ${CIFAR10DIR}
DCGANDIR=${WORKDIR}/dcgan-tf-benchmark
echo ${DCGANDIR}
GNMTDIR=${WORKDIR}/tensorflow-NMT
echo ${GNMTDIR}

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

LOGFILE=${WORKDIR}/benchmark_${MODEL}_${MODE}_${HOST}.log
CIFAR10EVALLOGFILE=${WORKDIR}/benchmark_${MODEL}_${MODE}_eval_${HOST}.log

THROUGHPUT_LOG=${WORKDIR}/benchmark_${MODEL}_${MODE}_${HOST}_throughput.log
LATENCY_LOG=${WORKDIR}/benchmark_${MODEL}_${MODE}_${HOST}_latency.log
ACCURACY_LOG=${WORKDIR}/benchmark_${MODEL}_${MODE}_${HOST}_accuracy.log

echo ${LOGFILE}
echo ${THROUGHPUT_LOG}
echo ${LATENCY_LOG}
echo ${ACCURACY_LOG}

export PATH="/nfs/fm/disks/aipg_tensorflow_tools/gcc6.3/bin/:/nfs/fm/disks/aipg_tensorflow_tools/bazel/bin:$PATH"
export LD_LIBRARY_PATH="/nfs/fm/disks/aipg_tensorflow_tools/gcc6.3/lib64:$LD_LIBRARY_PATH"
                
if [ ${MODEL} == "cifar10" ] ; then
    echo "model is cifar10"
    rm -rf /tmp/cifar10_train
    rm -rf /tmp/cifar10_eval
    cd ${CIFAR10DIR}

    if [ ${MODE} == "training" ] ; then 
        cmd="numactl -m 1 python cifar10_train.py --data_dir=${DATASET_DIR}/resnet32-cifar10 --max_steps=1000"

        echo "RUNCMD: $cmd " >& ${LOGFILE}
        eval $cmd >> ${LOGFILE}

        cmd="numactl -m 1 python cifar10_eval.py --data_dir=${DATASET_DIR}/resnet32-cifar10 --run_once=true"
        echo "RUNCMD: $cmd " >& ${CIFAR10EVALLOGFILE}
        eval $cmd >> ${CIFAR10EVALLOGFILE}

    else 
        echo "skipping cifar10 inference for now"
        exit 0
    fi

    if [ $? -eq 0 ] ; then 
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

elif [ ${MODEL} == "dcgan" ] ; then
    echo "model is DCGAN"
    cd ${DCGANDIR}

    ln -s ${DATASET_DIR}/dcgan/data ${DCGANDIR}/data
    if [ ${MODE} == "training" ] ; then 
        cmd="python ./run_dcgan.py"

        echo "RUNCMD: $cmd " >& ${LOGFILE}
        eval $cmd >> ${LOGFILE}

    else 
        echo "skipping DCGAN inference for now"
        exit 0
    fi

    if [ $? -eq 0 ] ; then 
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

elif [ ${MODEL} == "ds2" ] ; then
    echo "model is ds2"
    cd ${DS2DIR}
    if [ ${MODE} == "training" ] ; then

        ./train.sh  2>&1 | tee ${LOGFILE} 
        if [ $? -eq 0 ] ; then 
            echo "success"
            STATUS="SUCCESS"
        else
            echo "failure"
            STATUS="FAILURE"
        fi
    else
        echo "skipping ds2 inference for now"
        exit 0
    fi 

elif [ ${MODEL} == "SSDvgg16" ] ; then
    echo "model is SSDvgg16"

    if [ ${MODE} == "training" ] ; then 
        cd ${SSDVGG16TRDIR}

        mkdir -p ${WORKDIR}/data/voc 
        ln -s ${DATASET_DIR}/SSDvgg16/voc/tfrecords ${WORKDIR}/data/voc
        
        cmd="python ${SSDVGG16TRDIR}/train_model.py >> ${LOGFILE} 2>&1"
        echo "RUNCMD: $cmd" >& ${LOGFILE}
        eval $cmd >> ${LOGFILE}
    else
        cd ${SSDVGG16INFDIR}
        # assuming this is inference mode

        # Model owner: Sheng Fu
        # Benchmark interface is the same as int8.  You just need to change the graph. 
        # run batch size 224 for accuracy
        cmd="python eval_int8_model_inference.py -g ${CHECKPOINT_DIR}/ssd-vgg16/fp32/freezed_dilate_ssd.pb -a -d ${DATASET_DIR}/SSDvgg16 -b 224 >> ${ACCURACY_LOG} 2>&1"

        echo "RUNCMD: $cmd " >& ${ACCURACY_LOG}
        echo "Batch Size: 224" >> ${ACCURACY_LOG}
        eval $cmd >> ${ACCURACY_LOG}

        # run batch size 224 for throughput
        cmd="python eval_int8_model_inference.py -g ${CHECKPOINT_DIR}/ssd-vgg16/fp32/freezed_dilate_ssd.pb -k -s -b 224 >> ${THROUGHPUT_LOG} 2>&1"

        echo "RUNCMD: $cmd " >& ${THROUGHPUT_LOG}
        echo "Batch Size: 224" >> ${THROUGHPUT_LOG}
        eval $cmd >> ${THROUGHPUT_LOG}

        # run batch size 1 for latency
        cmd="python eval_int8_model_inference.py -g ${CHECKPOINT_DIR}/ssd-vgg16/fp32/freezed_dilate_ssd.pb -k -s -b 1 >> ${LATENCY_LOG} 2>&1"

        echo "RUNCMD: $cmd " >& ${LATENCY_LOG}
        echo "Batch Size: 1" >> ${LATENCY_LOG}
        eval $cmd >> ${LATENCY_LOG}
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
    cd ${GNMTDIR}
    export OMP_NUM_THREADS=28
    TRAINING_DATA_DIR=/mnt/nrvlab_300G_work01/cuixiaom/Data/Nmt/Ger-Eng/Training/wmt16
    INFERENCE_DATA_DIR=/mnt/nrvlab_300G_work01/cuixiaom/Data/Nmt/Ger-Eng/Inference/CheckpointNmt/nervana_gnmt_model_8_layer
    OUTPUT_DIR=./output1

    if [ ${MODE} == "training" ] ; then 
        cmd="python -m nmt.nmt \
            --src=de --tgt=en \
            --hparams_path=nmt/standard_hparams/wmt16_gnmt_8_layer.json \
            --out_dir=$OUTPUT_DIR \
            --vocab_prefix=$TRAINING_DATA_DIR/vocab.bpe.32000 \
            --train_prefix=$TRAINING_DATA_DIR/train.tok.clean.bpe.32000 \
            --dev_prefix=$TRAINING_DATA_DIR/newstest2013.tok.bpe.32000 \
            --test_prefix=$TRAINING_DATA_DIR/newstest2015.tok.bpe.32000 \
            --num_train_steps=1000 \
            --num_inter_threads=4 \
            --num_intra_threads=28  >> ${LOGFILE} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE}
        eval $cmd >> ${LOGFILE}

    else 
        export KMP_BLOCKTIME=0
        export KMP_AFFINITY=granularity=fine,compact,1,0
        export OMP_NUM_THREADS=28
        cores=27
        cmd="numactl --physcpubind=0-$cores --membind=0 python -m nmt.nmt \
            --src=de --tgt=en \
            --ckpt=$INFERENCE_DATA_DIR/translate.ckpt-3000 \
            --hparams_path=nmt/submit_hparams/wmt16_gnmt_8_layer.json \
            --out_dir=$OUTPUT_DIR \
            --vocab_prefix=$TRAINING_DATA_DIR/vocab.bpe.32000 \
            --inference_input_file=$TRAINING_DATA_DIR/newstest2014.tok.bpe.32000.de \
            --inference_output_file=$OUTPUT_DIR/output_infer \
            --inference_ref_file=$TRAINING_DATA_DIR/newstest2014.tok.bpe.32000.en \
            --num_inter_threads=2 \
            --num_intra_threads=28 >> ${LOGFILE} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE}
        eval $cmd >> ${LOGFILE}
    fi

    if [ $? -eq 0 ] ; then 
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

elif [ ${MODEL} == "nmt" ] ; then
    echo "model is nmt"
    cd ${NMTDIR}
    export KMP_BLOCKTIME=0
    export KMP_AFFINITY=granularity=fine,compact,1,0
    export OMP_NUM_THREADS=28
    TRAINING_DATA_DIR=/mnt/nrvlab_300G_work01/cuixiaom/Data/Nmt/Eng-Vie/Training/IWSLT-ENG-VIE
    INFERENCE_DATA_DIR=/mnt/nrvlab_300G_work01/cuixiaom/Data/Nmt/Eng-Vie/Inference/Checkpoint/WMT-ENG-VIE/envi_model_1
    OUTPUT_DIR=./output

    if [ ${MODE} == "training" ] ; then 
        cmd="python -m nmt.nmt --src=vi --tgt=en --vocab_prefix=$TRAINING_DATA_DIR/vocab --train_prefix=$TRAINING_DATA_DIR/train --dev_prefix=$TRAINING_DATA_DIR/tst2012 --test_prefix=$TRAINING_DATA_DIR/tst2013 --out_dir=$OUTPUT_DIR --num_train_steps=12000 --steps_per_stats=100 --num_layers=2 --num_units=128 --dropout=0.2 --metrics=bleu --num_inter_threads=2 --num_intra_threads=44  >> ${LOGFILE} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE}
        eval $cmd >> ${LOGFILE}
    else 
        
        cmd="numactl --physcpubind=0-27 --membind=0 python -m nmt.nmt \
             --src=en --tgt=vi \
             --ckpt=$INFERENCE_DATA_DIR/translate.ckpt \
             --hparams_path=nmt/standard_hparams/iwslt15.json \
             --out_dir=$OUTPUT_DIR \
             --vocab_prefix=$TRAINING_DATA_DIR/vocab \
             --inference_input_file=$TRAINING_DATA_DIR/tst2013.en \
             --inference_output_file=$OUTPUT_DIR/translated_file \
             --inference_ref_file=$TRAINING_DATA_DIR/tst2013.vi \
             --num_inter_threads=1 \
             --num_intra_threads=28  >> ${LOGFILE} 2>&1" 
        echo "RUNCMD: $cmd " >& ${LOGFILE}
        eval $cmd >> ${LOGFILE}
    fi

    if [ $? -eq 0 ] ; then 
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

elif [ ${MODEL} == "mnist" ] ; then
    echo "model is mnist"
    cd ${MNISTDIR}

    if [ ${MODE} == "training" ] ; then 
        cmd="numactl -m 1 -- python mnist.py >> ${LOGFILE} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE}
        eval $cmd >> ${LOGFILE}
    else 
        echo "skipping mnist inference for now"
        exit 0
    fi

    if [ $? -eq 0 ] ; then 
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

elif [ ${MODEL} == "resnet32cifar10" ] ; then
    echo "model is resnet32cifar10"
    cd ${RN32CIFAR10DIR}

    # workaround https://jira.devtools.intel.com/browse/TFDO-2863
    sed -e "s/train_distribute=distribution_strategy,//" ./resnet_run_loop.py > ./resnet_run_loop_tmp.py
    mv ./resnet_run_loop_tmp.py ./resnet_run_loop.py
    chmod 755 ./resnet_run_loop.py

    if [ ${MODE} == "training" ] ; then 
        rm -rf /tmp/cifar10_model
        export OMP_NUM_THREADS=10
        export PYTHONPATH=$PYTHONPATH:${MODELSDIR}
        cmd="stdbuf -oL python cifar10_main.py --data_dir=${DATASET_DIR}/resnet32-cifar10/cifar-10-batches-bin >> ${LOGFILE} 2>&1"

        echo "RUNCMD: $cmd " >& ${LOGFILE}
        eval $cmd >> ${LOGFILE}
    else 
        echo "skipping resnet32 cifar10 inference for now"
        exit 0
    fi

    if [ $? -eq 0 ] ; then 
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

elif [ ${MODEL} == "vgg16" ] || [ ${MODEL} == "alexnet" ] || [ ${MODEL} == "googlenet" ]  ; then

    if [ ${MODE} == "training" ] ; then 
        cmd="python ${WORKDIR}/private-tensorflow-benchmarks/scripts/tf_cnn_benchmarks/run_single_node_benchmark.py --model ${MODEL} --num_batches 800 --num_warmup_batches 200 --cpu skl --batch_size 128 --data_format NCHW --num_intra_threads 56 --num_inter_threads 1 >> ${LOGFILE} 2>&1"
        echo "RUNCMD: $cmd" >& ${LOGFILE}
        eval $cmd >> ${LOGFILE}
    else 
        cmd="python ${WORKDIR}/private-tensorflow-benchmarks/scripts/tf_cnn_benchmarks/run_single_node_benchmark.py --model ${MODEL} --num_batches 800 --num_warmup_batches 200 --cpu skl --batch_size 128 --data_format NCHW --num_intra_threads 56 --num_inter_threads 1 --forward_only=True >> ${LOGFILE} 2>&1"
        echo "RUNCMD: $cmd"  >& ${LOGFILE}
        eval $cmd >> ${LOGFILE}
    fi

    if [ $? -eq 0 ] ; then 
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

elif [ ${MODEL} == "resnet50" ] || [ ${MODEL} == "inception3" ] ; then
    # MODEL resnet50 or inception3
    if [ ${MODE} == "training" ] ; then 
        cmd="python ${WORKDIR}/private-tensorflow-benchmarks/scripts/tf_cnn_benchmarks/run_single_node_benchmark.py --model ${MODEL} --num_batches 800 --num_warmup_batches 200 --cpu skl >> ${LOGFILE} 2>&1"
        echo "RUNCMD: $cmd" >& ${LOGFILE}
        eval $cmd >> ${LOGFILE}
    else
        cmd="python ${WORKDIR}/private-tensorflow-benchmarks/scripts/tf_cnn_benchmarks/run_single_node_benchmark.py --model ${MODEL} --num_batches 800 --num_warmup_batches 200 --cpu skl --forward_only=True --single_socket=${SINGLE_SOCKET} >> ${LOGFILE} 2>&1"
        echo "RUNCMD: $cmd" >& ${LOGFILE}
        eval $cmd >> ${LOGFILE}
    fi

    if [ $? -eq 0 ] ; then 
        echo "success"
        STATUS="SUCCESS"
    else
        echo "failure"
        STATUS="FAILURE"
    fi

else
    echo "unrecognized model"
fi
                
if [ $STATUS == "SUCCESS" ]; then 
    exit 0
else
    exit 1
fi


