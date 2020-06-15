#!/bin/bash

# Get path to TensorFlow files
pushd .
cd /
tf_path=$(python -c "import tensorflow as tf; import os; print(os.path.join(os.path.dirname(tf.__file__), 'python'))")

# Virtual TensorFlow pip package path as described here:
# https://github.com/tensorflow/tensorflow/commit/f1ffa0225ae19870c1473b1d70faf0ceaeea9862
tf_core_path=$(python -c "import tensorflow_core as tf_core; import os; print(os.path.join(os.path.dirname(tf_core.__file__), 'python'))")

# Check if tf_path or tf_core_path exist
if [ -d ${tf_path} ]; then
  cd ${tf_path}
elif [ -d ${tf_core_path} ]; then
  cd ${tf_core_path}
else
  echo "Neither ${tf_path} nor ${tf_core_path} exits"
  popd
  return 1
fi

# Look for S3FileSystem symbols
if [ $(nm -g _pywrap_tensorflow_internal.so | grep S3FileSystem | head -5 | wc -l) -le 0 ]
then
    echo "FAILED: No S3 support found"
else
    echo "PASS: S3 support found"
fi
popd
