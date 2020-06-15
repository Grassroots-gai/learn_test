#!/bin/bash
set -x
cd ./tensorflow-NMT/nmt/standard_hparams

sed 's/\"num_train_steps\": 340000/\"num_train_steps\": 1000/' ./wmt16_gnmt_8_layer.json > ./wmt16_gnmt_8_layer_tmp.json
mv ./wmt16_gnmt_8_layer_tmp.json ./wmt16_gnmt_8_layer.json
