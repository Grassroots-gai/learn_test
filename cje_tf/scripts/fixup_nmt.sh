#!/bin/bash
set -x
#cd ./nmt/nmt/


#sed -e 's/help=\"Number of workers (inference only).\")/help=\"Number of workers (inference only).\")\n  parser.add_argument(\"--num_inter_threads\", type=int, default=0, help=\"number of inter_op_parallelism_threads\")\n  parser.add_argument(\"--num_intra_threads\", type=int, default=0, help=\"number of intra_op_parallelism_threads\")/' \
#    -e 's/random_seed=flags.random_seed,/random_seed=flags.random_seed,\n      override_loaded_hparams=flags.override_loaded_hparams,\n      num_intra_threads=FLAGS.num_intra_threads,\n      num_inter_threads=FLAGS.num_inter_threads,/' ./nmt.py  > ./nmt_tmp.py
#mv ./nmt_tmp.py ./nmt.py

#sed -e 's/log_device_placement=log_device_placement)/log_device_placement=log_device_placement)\n  config_proto.inter_op_parallelism_threads = hparams.num_inter_threads\n  config_proto.intra_op_parallelism_threads = hparams.num_intra_threads/' ./train.py > ./train_tmp.py
#mv ./train_tmp.py ./train.py

cp -R /mnt/nrvlab_300G_work01/cuixiaom/Nmt/Nmt_9c6a6ef/nmt  .
