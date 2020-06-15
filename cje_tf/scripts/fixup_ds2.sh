#!/bin/bash
set -x
cd ./deepSpeech2/src/
# 1. update ./deepSpeech2/src/train.sh
# change the following lines:
# change the line: dummy=False -> dummy=True
# change the line: --max_steps 40000 -> --max_steps 100
#python deepSpeech_train.py --batch_size 32 --no-shuffle --max_steps 40000 --num_rnn_layers 7 --num_hidden 1760 --num_filters 32 --initial_lr 1e-6 --train_dir $model_dir --data_dir $data_dir --debug ${debug} --nchw ${nchw} --engine ${engine} --dummy ${dummy}
#to
#python deepSpeech_train.py --batch_size 32 --no-shuffle --max_steps 100 --num_rnn_layers 7 --num_hidden 1760 --num_filters 32 --initial_lr 1e-6 --train_dir $model_dir --data_dir $data_dir --debug ${debug} --nchw ${nchw} --engine ${engine} --dummy ${dummy}

sed -e 's/dummy=False/dummy=True/' -e 's/max_steps \([0-9]*\)/max_steps 100/' -e 's/batch_size \([0-9]*\)/batch_size 256/' ./train.sh  > ./train_tmp.sh

sed -e 's/python/cmd=\"&/' \
    -e 's/\${dummy}/&\" \necho \"RUNCMD: \$cmd\"  \neval \$cmd \n/' ./train_tmp.sh > ./train_tmp1.sh 

mv ./train_tmp1.sh ./train.sh
chmod 755 ./train.sh

# 2. update ./deepSpeech2/src/setenvs.py
# change the block start with class arglist:
# and change the line: platform = 'knl' -> platform = 'bdw'
sed 's/    platform = '\''knl'\''/    platform = '\''bdw'\''/' ./setenvs.py  > ./setenvs_tmp.py
mv ./setenvs_tmp.py ./setenvs.py
chmod 755 ./setenvs.py

# 3. update  ./deepSpeech2/src/deepSpeech_train.py
# change the line: ARGS.intra_op = 8 under
# if args.platform == 'bdw':
# ARGS.intra_op = 20
sed 's/ARGS.intra_op = \([0-9]*\)/ARGS.intra_op = 20/' ./deepSpeech_train.py > ./deepSpeech_train_tmp.py
mv ./deepSpeech_train_tmp.py ./deepSpeech_train.py
chmod 755 ./deepSpeech_train.py

