#!/bin/bash
set -x
cd ./SSD-Tensorflow/

mkdir log
mkdir tfrecords
mkdir tfrecords_test

DATASET_DIR=/mnt/nrvlab_300G_work01/karenwu/VOC2007/VOCdevkit/VOC2007/
#DATASET_DIR=./VOC2007/VOCdevkit/VOC2007/
TFRECORDS_TEST_OUTPUT_DIR=./tfrecords_test
TFRECORDS_OUTPUT_DIR=./tfrecords

python tf_convert_data.py --dataset_name=pascalvoc --dataset_dir=${DATASET_DIR} --output_name=voc_2007_test --output_dir=${TFRECORDS_TEST_OUTPUT_DIR}
python tf_convert_data.py --dataset_name=pascalvoc --dataset_dir=${DATASET_DIR} --output_name=voc_2007_train --output_dir=${TFRECORDS_OUTPUT_DIR}

sed 's/import os/from __future__ import print_function\nimport os/' ./tf_utils.py > ./tf_utils_tmp.py
mv ./tf_utils_tmp.py ./tf_utils.py

cp /nfs/site/home/karenwu/script/train_ssd_network.py .
cp /nfs/site/home/karenwu/script/inference_ssd_network.py .

