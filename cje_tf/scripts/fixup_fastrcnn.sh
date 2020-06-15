#!/bin/bash
set -x

WORKDIR=`pwd`
TENSORFLOW_MODELS_DIR=${WORKDIR}/tensorflow-models
COCOAPI_DIR=${WORKDIR}/cocoapi

# make sure the dependencies are installed
# sudo pip install Cython
# sudo pip install pillow
# sudo pip install lxml
# sudo pip install jupyter
# sudo pip install matplotlib
# sudo pip install slim

# COCO API installation
# make sure the cocoapi repo is checked out: git clone https://github.com/cocodataset/cocoapi.git
cd $COCOAPI_DIR/PythonAPI
make
cp -r pycocotools ${TENSORFLOW_MODELS_DIR}/research/

# protobuf compilation, use protoc 3.3
export PATH=/opt/tensorflow/protoc-3.3.0-linux-x86_64/bin:$PATH
cd $TENSORFLOW_MODELS_DIR/research
protoc object_detection/protos/*.proto --python_out=.

# add libraries to PYTHONPATH
#export PYTHONPATH=$PYTHONPATH:`pwd`:`pwd`/slim

#sed 's/DEFAULT_CHECKPOINT = \(.*\)/DEFAULT_CHECKPOINT = "/mnt/aipg_tensorflow_shared/coco-data"/' ./run_tf_benchmark.py > ./run_tf_benchmark_tmp.py
#mv ./run_tf_benchmark_tmp.py ./run_tf_benchmark.py
#chmod 755 ./run_tf_benchmark.py

cd ${TENSORFLOW_MODELS_DIR}/research/
sed 's/self.args.checkpoint + self.args.config/self.args.config/' ./model_init.py > ./model_init.py.tmp
cp model_init.py.tmp model_init.py 

if [ -z ${DATASET_LOCATION} ] ; then
    DATASET_LOCATION="/tf_dataset"
fi

DATASET_DIR="${DATASET_LOCATION}/dataset"
CJE_TF_SCRIPTS_DIR=${WORKDIR}/cje-tf/scripts
cd $CJE_TF_SCRIPTS_DIR 
sed "s|\/dataset|${DATASET_DIR}|" fastrcnn_pipeline.config > fastrcnn_pipeline.config.tmp
cp fastrcnn_pipeline.config.tmp fastrcnn_pipeline.config

sed "s|\/dataset|${DATASET_DIR}|" fastrcnn_resnet50_pets.config > fastrcnn_resnet50_pets.config.tmp
cp fastrcnn_resnet50_pets.config.tmp fastrcnn_resnet50_pets.config
