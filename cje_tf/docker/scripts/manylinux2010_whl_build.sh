#!/bin/bash
# This script will build the whls inside the specified container environment
# Input: 
#   Expects properly cloned tensorflow sources to be mounted and located at /tensorflow
#   It assumes that the container has been built with TensorFlow manylinux2010
#     Dockerfile at https://github.com/tensorflow/tensorflow/tensorflow/tools/ci_build/Dockerfile.rbe.ubuntu16.04-manylinux2010
#   It also assume that it is being run inside the container; i.e. before running 
#     this script, the container must be built and running.
# Output: 
#   the manylinux2010 container will be at /host
# See manylinux2010_container_build.sh to build the containers
set -e
function usage (){
  echo "Unable to find what I'm looking for. I need:"
  echo "  1. tensorflow sources at /tensorflow or \$TF_SRC_DIR,"
  echo "  2. dev-toolsets at /dt8 and /dt7,"
  echo "  3. a dev-toolset version of either 7 or 8"
  echo ""
  echo "You can also set the following env vars:"
  echo "  TARGET_PLATFORM=sandybridge"
  echo "  WHL_DIR=/tmp/whl"
  echo "  BUILD_V1=y"
  echo "Usage: TF_SRC_DIR=[tensorflow source dir] DEV_TOOLSET_VER=[dev-toolset version 7 or 8] $0"
  exit 1
}

# Check our environment
TENSORFLOW_REPO="${TENSORFLOW_REPO:-https://github.com/tensorflow/tensorflow.git}"
TF_SRC_DIR="${TF_SRC_DIR:-/tensorflow}"
TENSORFLOW_BRANCH="${TENSORFLOW_BRANCH:-master}"
DEV_TOOLSET_VER="${DEV_TOOLSET_VER:-8}"
PYTHON_VERSION="${PYTHON_VERSION:-3.5}"
BAZEL_VERSION=${BAZEL_VERSION:-"0.29.1"}
SET_BUILD_ENV_SCRIPT="${TF_SRC_DIR}/tensorflow/tools/ci_build/linux/mkl/set-build-env.py"
TARGET_PLATFORM="${TARGET_PLATFORM:-sandybridge}"
WHL_DIR="${WHL_DIR:-/tmp/whl}"
TMP_WHL="/tmp/whl"
TAG_PREFIX="v"

echo "TF_SRC_DIR=${TF_SRC_DIR}"
echo "DEV_TOOLSET_VER=${DEV_TOOLSET_VER}"
gcc --version
# bazel 0.26.1 doesn't allow --version parameter
# bazel --version
python${PYTHON_VERSION} --version
echo "SET_BUILD_ENV_SCRIPT=${SET_BUILD_ENV_SCRIPT}"
echo "TARGET_PLATFORM=${TARGET_PLATFORM}"
echo "WHL_DIR=${WHL_DIR}"

BUILD_V2=""
if [[ "${BUILD_V1}" == "y" ]]; then
  BUILD_V2="--disable-v2"
  echo "Disabling v2; building TensorFlow 1.x"
fi

# Switch to the specified Dev-Toolset environment to pick up a modern version of gcc
# Set the Path
DT="/dt${DEV_TOOLSET_VER}/usr/bin"
echo "Setting PATH to ${PATH}"
export PATH=$DT:$PATH
export AR=$DT/gcc-ar
export CC=$DT/gcc
export CPP=$DT/cpp
export CXX=$DT/g++
export GCC=$CC
export GCC_AR=$AR
export GCC_NM=$DT/gcc-nm
export GCC_RANLIB=$DT/gcc-ranlib

LN="ln -sf"
OLD_BIN="/usr/bin"
${LN} ${DT}/gcc-ar ${OLD_BIN}/gcc-ar
${LN} ${DT}/gcc ${OLD_BIN}/gcc
${LN} ${DT}/cpp ${OLD_BIN}/cpp
${LN} ${DT}/g++ ${OLD_BIN}/g++
${LN} ${DT}/c++ ${OLD_BIN}/c++
${LN} ${DT}/gcc-ar ${OLD_BIN}/gcc-ar
${LN} ${DT}/gcc-nm ${OLD_BIN}/gcc-nm
${LN} ${DT}/gcc-ranlib ${OLD_BIN}/gcc-ranlib
${LN} ${DT}/gcov ${OLD_BIN}/gcov
${LN} ${DT}/gcov-tool ${OLD_BIN}/gcov-tool

ls -la /usr/bin/g*

env

echo "Retrieving TF ${TENSORFLOW_BRANCH} in ${TF_SRC_DIR}"
cd /
rm -rf ${TF_SRC_DIR}
if [[ ${TENSORFLOW_BRANCH} == ${TAG_PREFIX}1* ]] || [[ "${TENSORFLOW_BRANCH}" == ${TAG_PREFIX}2* ]]; then
    echo "Fetching tags..."
        git clone --depth=1 ${TENSORFLOW_REPO} ${TF_SRC_DIR}
        cd ${TF_SRC_DIR}
        git fetch --tags
        git checkout ${TENSORFLOW_BRANCH}
else
        git clone --depth=1 --branch=${TENSORFLOW_BRANCH} ${TENSORFLOW_REPO} ${TF_SRC_DIR}
        cd ${TF_SRC_DIR}
fi

# Install Bazel
wget --quiet https://github.com/bazelbuild/bazel/releases/download/$BAZEL_VERSION/bazel-$BAZEL_VERSION-installer-linux-x86_64.sh
chmod +x bazel-$BAZEL_VERSION-installer-linux-x86_64.sh && \
    ./bazel-$BAZEL_VERSION-installer-linux-x86_64.sh && \
    rm -rf bazel-$BAZEL_VERSION-installer-linux-x86_64.sh

# Use python3 to configure
echo "building Tensorflow sources at ${TF_SRC_DIR}"
cd ${TF_SRC_DIR}
# Hack the configure script to allow bazel 0.26.1
echo "hacking the configure script to allow bazel 0.26.1"
sed s/0.27.1/0.26.1/g -i configure.py
yes "" | python${PYTHON_VERSION} configure.py

# Use the set-build-env.py to set the bazel build command
python${PYTHON_VERSION} ${SET_BUILD_ENV_SCRIPT} -p ${TARGET_PLATFORM} -f /root/.mkl.bazelrc \
    ${BUILD_V2} --secure-build

cat <<EOF > /root/.bazelrc
import /root/.mkl.bazelrc
EOF

echo "-----------> bazel build options <---------------------"
cat /root/.mkl.bazelrc
echo "\n"
echo "--------------------------------------------------------"

#Build TF
BAZEL_LINKLIBS=-l%:libstdc++.a bazel --bazelrc=/root/.bazelrc build -c opt \
   tensorflow/tools/pip_package:build_pip_package    

echo "Building whl at ${WHL_DIR}"
bazel-bin/tensorflow/tools/pip_package/build_pip_package "${WHL_DIR}"

# make version without '.' and then check if we have a wheel that matches that
PY_VER=${PYTHON_VERSION//[.]/}
echo "PY_VER = ${PY_VER}"

echo "--------------------------------------------------------"
ls ${WHL_DIR}
echo "--------------------------------------------------------"

# Don't need to install the whl
if [[ "${PY_VER}" != "3" ]]; then
    echo "Installing the whl to ensure sanity"
    wheel=`find ${WHL_DIR} -name "*${PY_VER}*" -and -name "*.whl"`
    echo "wheel is ${wheel}"
    echo "Installing whl ${wheel}"
    pip${PYTHON_VERSION} --no-cache-dir install --upgrade "${wheel}"
fi

#audit the wheel
echo "Auditing the wheel..."
echo "Auditing wheel ${wheel}"
auditwheel repair "${wheel}" --plat manylinux2010_x86_64 --wheel-dir "${WHL_DIR}"    

echo "DONE."
