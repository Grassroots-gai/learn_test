#!/bin/bash
set -xe

TF_ROOT_DIR="${TF_ROOT_DIR:-/tensorflow}"
TF_BUILD_VERSION="${TF_BUILD_VERSION:-master}"
BAZEL_VERSION=${BAZEL_VERSION:-"0.24.1"}
PYTHON="${PYTHON:-python}"
PIP=${PIP:-"pip"}
WHL_DIR="${WHL_DIR:-/tmp/pip}"
TAG_PREFIX="v"
TF_BUILD_REPO=${TF_BUILD_REPO:-"https://github.com/tensorflow/tensorflow.git"}
TF_BAZEL_BUILD_OPTIONS=${TF_BAZEL_BUILD_OPTIONS:-"--config=mkl --copt=-march=sandybridge --copt=-mtune=ivybridge --copt=-O3 --cxxopt=-D_GLIBCXX_USE_CXX11_ABI=0"}
TF_OPTIONAL_BAZEL_BUILD_OPTIONS=${TF_OPTIONAL_BAZEL_BUILD_OPTIONS:-''}
BUILD_DIR="${TF_ROOT_DIR}/tensorflow"

echo "Building TF ${TF_BUILD_VERSION} in ${TF_ROOT_DIR} with ${TF_BAZEL_BUILD_OPTIONS} ${TF_OPTIONAL_BAZEL_BUILD_OPTIONS}"
cd /
rm -rf ${TF_ROOT_DIR}
mkdir -p ${TF_ROOT_DIR}
cd ${TF_ROOT_DIR}
if [[ ${TF_BUILD_VERSION} == ${TAG_PREFIX}1* ]] || [[ "${TF_BUILD_VERSION}" == ${TAG_PREFIX}2* ]]; then
    echo "Fetching tags..."
        git clone --depth=1 ${TF_BUILD_REPO} ${BUILD_DIR}
        cd ${BUILD_DIR}
        git fetch --tags
        git checkout ${TF_BUILD_VERSION}
else
        git clone --depth=1 --branch=${TF_BUILD_VERSION} ${TF_BUILD_REPO} ${BUILD_DIR}
        cd ${BUILD_DIR}
fi

wget --quiet https://github.com/bazelbuild/bazel/releases/download/$BAZEL_VERSION/bazel-$BAZEL_VERSION-installer-linux-x86_64.sh
chmod +x bazel-$BAZEL_VERSION-installer-linux-x86_64.sh && \
    ./bazel-$BAZEL_VERSION-installer-linux-x86_64.sh && \
    rm -rf bazel-$BAZEL_VERSION-installer-linux-x86_64.sh

echo "Configuring..."
yes "" | ${PYTHON} configure.py

echo "Building TensorFlow with these options ${TF_BAZEL_BUILD_OPTIONS}"
bazel build -c opt \
        ${TF_BAZEL_BUILD_OPTIONS} \
        ${TF_OPTIONAL_BAZEL_BUILD_OPTIONS} \
        tensorflow/tools/pip_package:build_pip_package

echo "Building whl at ${WHL_DIR}"
bazel-bin/tensorflow/tools/pip_package/build_pip_package "${WHL_DIR}"

echo "Installing whl found at ${WHL_DIR}"
${PIP} --no-cache-dir install --upgrade "${WHL_DIR}"/tensorflow-*.whl

echo "DONE!"
