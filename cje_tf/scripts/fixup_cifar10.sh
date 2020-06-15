#!/bin/bash
set -x
cd ./models/tutorials/image/cifar10
# 1. update ./cifar10_train.py
sed -e 's/log_device_placement))/log_device_placement, inter_op_parallelism_threads=4))/' \
    -e 's/cifar10.maybe_download_and_extract()/import os\n  os.environ[\"KMP_BLOCKTIME\"] = \"1\"\n  os.environ[\"KMP_SETTINGS\"] = \"1\"\n  os.environ[\"KMP_AFFINITY\"]= \"granularity=fine,compact,1,0\"\n  os.environ[\"OMP_NUM_THREADS\"]= \"34\"\n  os.environ[\"MKL_NUM_THREADS\"]= \"50\"\n  cifar10.maybe_download_and_extract()/'  ./cifar10_train.py > ./cifar10_train_tmp.py
mv ./cifar10_train_tmp.py ./cifar10_train.py
chmod 755 ./cifar10_train.py

# 2. update ./cifar10_eval.py
sed -e "s/'eval_interval_secs', 60 \* 5/'eval_interval_secs', 30/" \
    -e 's/cifar10.maybe_download_and_extract()/import os\n  os.environ[\"KMP_BLOCKTIME\"] = \"1\"\n  os.environ[\"KMP_SETTINGS\"] = \"1\"\n  os.environ[\"KMP_AFFINITY\"]= \"granularity=fine,compact,1,0\"\n  os.environ[\"OMP_NUM_THREADS\"]= \"34\"\n  os.environ[\"MKL_NUM_THREADS\"]= \"50\"\n  cifar10.maybe_download_and_extract()/'  ./cifar10_eval.py > ./cifar10_eval_tmp.py
mv ./cifar10_eval_tmp.py ./cifar10_eval.py
chmod 755 ./cifar10_eval.py
