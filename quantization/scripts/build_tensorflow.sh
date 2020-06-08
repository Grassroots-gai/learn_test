#!/bin/bash

for var in "$@"
do 
    case $var in
        --tensorflow_src_dir=*)
            tensorflow_src_dir=$(echo $var |cut -f2 -d=)
        ;;
        --conda_env_name=*)
            conda_env_name=$(echo $var |cut -f2 -d=)
        ;;
        *)
            echo "Error: No such parameter: ${var}"
            exit 1
        ;;
    esac
done

#
cd ${tensorflow_src_dir}
git remote -v
git branch
git show |head -5

# proxy
export ftp_proxy=http://child-prc.intel.com:913
export http_proxy=http://child-prc.intel.com:913
export https_proxy=http://child-prc.intel.com:913

# gcc6.3
export PATH=${HOME}/tools/gcc6_3_0/bin:$PATH
export LD_LIBRARY_PATH=${HOME}/tools/gcc6_3_0/lib64:$LD_LIBRARY_PATH
gcc -v

# conda3
# conda_env_name="tf$(date '+%s')"
conda remove --all -y -n ${conda_env_name}
conda create python=3.7 -y -n ${conda_env_name}
conda activate ${conda_env_name}
python -V

# bazel26
export PATH=${HOME}/tools/bazel0_26_1/bin:$PATH
bazel version

# pre install tensorflow
pip install tensorflow

# configure
bazel clean --expunge --async
expect ${HOME}/tensorflow/tensorflow_config_cpu

# build
bazel build --config=mkl --cxxopt=-D_GLIBCXX_USE_CXX11_ABI=0 --copt=-O3 --copt=-mmmx \
    --copt=-msse --copt=-msse2 --copt=-msse3 --copt=-mssse3 --copt=-msse4.1 \
    --copt=-msse4.2 --copt=-mpopcnt --copt=-mavx --copt=-maes --copt=-mpclmul \
    --copt=-mfsgsbase --copt=-mrdrnd --copt=-mfma --copt=-mbmi --copt=-mbmi2 \
    --copt=-mf16c --copt=-mmovbe --copt=-mavx2 -c opt //tensorflow/tools/pip_package:build_pip_package

# build tools
bazel build tensorflow/tools/graph_transforms:transform_graph
bazel build tensorflow/tools/graph_transforms:summarize_graph

# wheel
rm -rf tmp_whl_dir && mkdir tmp_whl_dir
./bazel-bin/tensorflow/tools/pip_package/build_pip_package tmp_whl_dir

# install tf
pip install --force-reinstall --no-cache-dir tmp_whl_dir/tensorflow*.whl
