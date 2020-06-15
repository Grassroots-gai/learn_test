#!/bin/bash
set -x

WORKDIR=`pwd`
TENSORFLOW_TRANSFORMERSPEECH_DIR=${WORKDIR}/tensorflow-TransformerSpeech

# Using Pre-trained model for inference
# Repository contains pre-trained model named speechrecog_librispeech_trainedmodel.tgz
cd $TENSORFLOW_TRANSFORMERSPEECH_DIR/tensor2tensor
tar -zxf speechrecog_librispeech_trainedmodel.tgz
