#!/bin/bash

export PATH=${HOME}/miniconda3/bin/:$PATH
pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple

if [ $(conda info -e | grep tf-${tensorflow_version} | wc -l) != 0 ]; then
    source activate tf-${tensorflow_version}
else
    conda create python=3.6.9 -y -n tf-${tensorflow_version}
    source activate tf-${tensorflow_version}
fi

pip install ${HOME}/suyue/${tensorflow_version}/*.whl
python -V