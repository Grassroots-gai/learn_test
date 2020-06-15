#!/usr/bin/env bash
# Install pip packages using the version of pip passed in

if [[ $# -ne 1 ]]; then
  echo "num params = $#"
  echo "Usage: $0 <pip suffix (e.g. 3.6)>"
  exit 1
fi

PIP="pip${1}"

${PIP} --no-cache-dir install --upgrade \
            pip \
            auditwheel \
            Pillow \
            future \
            h5py \
            jupyter \
            keras_applications \
            keras_preprocessing \
            mock \
            pandas \
            scipy \
            setuptools \
            sklearn \
            wheel==0.34.1
