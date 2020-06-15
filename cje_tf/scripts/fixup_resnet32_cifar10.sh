#!/bin/bash
# To run Resnet50 with cifar10 benchmark testing:
# 1. # git clone https://github.com/tensorflow/models.git
# 2. # cd models/official/resnet
# 3. use following command to download Cifar10 dataset 
#      # python cifar10_download_and_extract.py --data_dir=. 
#    - above command generates cifar-10-batches-bin folder under your current directory 
#    - by default if you don't specify the --data_dir, the data will be downloaded under /tmp 
# 4. update ./cifar10_main.py
# change the line: parser.add_argument('--train_epochs', type=int, default=250, help='The number of epochs to train.')
#         to have: default=11,  
#    - this is about the number of epoches we start to see convergence
# change the line: parser.add_argument('--epochs_per_eval', type=int, default=10, help='The number of epochs to run in between evaluations.')
#         to have: default=1,  
#    - this is about the number of epoches to run in between evaluation 
# 5. # rm /tmp/cifar10_model 
#    - this prevents it training from last run
# 6. # export OMP_NUM_THREADS=10
# 7. to run the test:
#    # python cifar10_main.py --data_dir=.
# 8. The last step will output an accuracy, if it is below 0.80, report it is not converging.
#    Get the *last* "train_accuracy" before this kind of line.
#        {'loss': 1.3893298, 'global_step': 1564, 'accuracy': 0.6218}

set -x
cd ./models/official/resnet
# for jenkin nightly benchmark testing, I've already downloaded the data under /dataset
# python cifar10_download_and_extract.py --data_dir=. 
#sed -e "s/'--train_epochs', type=int, default=250/'--train_epochs', type=int, default=11/" -e "s/'--epochs_per_eval', type=int, default=10/'--epochs_per_eval', type=int, default=1/" ./cifar10_main.py > ./cifar10_main_tmp.py
sed -e "s/train_epochs=182/train_epochs=11/" -e "s/epochs_between_evals=10/epochs_between_evals=1/" ./cifar10_main.py > ./cifar10_main_tmp.py
mv ./cifar10_main_tmp.py ./cifar10_main.py
chmod 755 ./cifar10_main.py


