#!/bin/bash
set -x
cd ./tensorflow-SSD-Training/

if [ -z ${DATASET_LOCATION} ] ; then
    DATASET_LOCATION="/tf_dataset"
fi
sed 's/self.max_number_of_steps = 30000/self.max_number_of_steps = 500/' ./train_model.py > ./train_model_tmp.py
mv ./train_model_tmp.py ./train_model.py
sed "s|\/mnt\/aipg_tensorflow_shared\/voc-data|${DATASET_LOCATION}\/dataset\/SSDvgg16\/voc|" preparedata.py  > preparedata_tmp.py
cp preparedata_tmp.py preparedata.py
