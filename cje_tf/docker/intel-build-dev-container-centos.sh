#!/usr/bin/env bash
# Copyright 2016 The TensorFlow Authors. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ==============================================================================
# Build a whl and container with Intel(R) MKL support
# Usage: build-dev-container.sh

set -e

# Helper function to traverse directories up until given file is found.
function upsearch () {
  test / == "$PWD" && return || \
      test -e "$1" && echo "$PWD" && return || \
      cd .. && upsearch "$1"
}

db="docker build --disable-content-trust --build-arg https_proxy=${http_proxy} --build-arg http_proxy=${http_proxy} --build-arg HTTP_PROXY=${HTTP_PROXY} --build-arg HTTPS_PROXY=${HTTPS_PROXY}"

dr='docker run --disable-content-trust --hostname="crobiso1-container" -e https_proxy -e http_proxy -e HTTPS_PROXY -e HTTP_PROXY -e no_proxy -e NO_PROXY'

TF_BUILD_VERSION=${TF_BUILD_VERSION:-"v1.13.0"}
BAZEL_VERSION=${BAZEL_VERSION:-"0.24.0"}
IS_PY37_SUPPORTED=${IS_PY37_SUPPORTED:-"1"}
TF_BUILD_REPO=${TF_BUILD_REPO:-"https://github.com/tensorflow/tensorflow.git"}

CENTOS_DOCKER_TAG=$(echo "${TF_BUILD_VERSION}" | sed 's/v//')-devel-mkl

WHL_DIR="/host"
PWD=`pwd`
echo "PWD=$PWD"

if [[ "${TF_BAZEL_BUILD_OPTIONS}" == "" ]]; then
  echo "TF_BAZEL_BUILD_OPTIONS must be defined. Use the set_xxx_build scripts"
  exit 1
fi

# Let's do 3.7 first because we're in a hurry to use it
cd Dockerfiles
# Python3.7 is only supported for TensorFlow v1.13.1 and newer
if [[ "${IS_PY37_SUPPORTED}" == "1" ]]; then
    ${db} -t amr-registry.caas.intel.com/aipg-tf/dev/centos7:${CENTOS_DOCKER_TAG}-py3.7 --build-arg PYTHON=python3.7 --build-arg PIP=pip3.7 -f Dockerfile.devel-mkl-centos .
    cd "../scripts"
    ${dr} -e TF_BUILD_REPO="${TF_BUILD_REPO}" -e TF_BAZEL_BUILD_OPTIONS="${TF_BAZEL_BUILD_OPTIONS}" -e TF_OPTIONAL_BAZEL_BUILD_OPTIONS="${TF_OPTIONAL_BAZEL_BUILD_OPTIONS}" -e WHL_DIR="${WHL_DIR}" -e TF_BUILD_VERSION="${TF_BUILD_VERSION}" -e BAZEL_VERSION="${BAZEL_VERSION}" -e PYTHON="python3.7" -e PIP="pip3.7" --mount src=$(pwd),dst=/host,type=bind amr-registry.caas.intel.com/aipg-tf/dev/centos7:${CENTOS_DOCKER_TAG}-py3.7 /host/build-tensorflow-whl.sh 2>&1 | tee log
    cd "../Dockerfiles"
fi

${db} -t amr-registry.caas.intel.com/aipg-tf/dev/centos7:${CENTOS_DOCKER_TAG} -f Dockerfile.devel-mkl-centos .
${db} -t amr-registry.caas.intel.com/aipg-tf/dev/centos7:${CENTOS_DOCKER_TAG}-py3 --build-arg PYTHON=python3 --build-arg PIP=pip3 -f Dockerfile.devel-mkl-centos .
${db} -t amr-registry.caas.intel.com/aipg-tf/dev/centos7:${CENTOS_DOCKER_TAG}-py3.4 --build-arg PYTHON=python3.4 --build-arg PIP=pip3.4 -f Dockerfile.devel-mkl-centos .
${db} -t amr-registry.caas.intel.com/aipg-tf/dev/centos7:${CENTOS_DOCKER_TAG}-py3.5 --build-arg PYTHON=python3.5 --build-arg PIP=pip3.5 -f Dockerfile.devel-mkl-centos .
${db} -t amr-registry.caas.intel.com/aipg-tf/dev/centos7:${CENTOS_DOCKER_TAG}-py3.6 --build-arg PYTHON=python3.6 --build-arg PIP=pip3.6 -f Dockerfile.devel-mkl-centos .

#create the three whls and install into the containers
# whls will be found in the current directory
#to step into the containter , remove the call to /host/build-tensorflow-whl.sh
cd "../scripts"
${dr} -e TF_BUILD_REPO="${TF_BUILD_REPO}" -e TF_BAZEL_BUILD_OPTIONS="${TF_BAZEL_BUILD_OPTIONS}" -e TF_OPTIONAL_BAZEL_BUILD_OPTIONS="${TF_OPTIONAL_BAZEL_BUILD_OPTIONS}" -e WHL_DIR="${WHL_DIR}" -e TF_BUILD_VERSION="${TF_BUILD_VERSION}" -e BAZEL_VERSION="${BAZEL_VERSION}" --mount src=$(pwd),dst=/host,type=bind amr-registry.caas.intel.com/aipg-tf/dev/centos7:${CENTOS_DOCKER_TAG} /host/build-tensorflow-whl.sh 2>&1 | tee log
${dr} -e TF_BUILD_REPO="${TF_BUILD_REPO}" -e TF_BAZEL_BUILD_OPTIONS="${TF_BAZEL_BUILD_OPTIONS}" -e TF_OPTIONAL_BAZEL_BUILD_OPTIONS="${TF_OPTIONAL_BAZEL_BUILD_OPTIONS}" -e WHL_DIR="${WHL_DIR}" -e TF_BUILD_VERSION="${TF_BUILD_VERSION}" -e BAZEL_VERSION="${BAZEL_VERSION}" -e PYTHON="python3" -e PIP="pip3" --mount src=$(pwd),dst=/host,type=bind amr-registry.caas.intel.com/aipg-tf/dev/centos7:${CENTOS_DOCKER_TAG}-py3 /host/build-tensorflow-whl.sh 2>&1 | tee log
${dr} -e TF_BUILD_REPO="${TF_BUILD_REPO}" -e TF_BAZEL_BUILD_OPTIONS="${TF_BAZEL_BUILD_OPTIONS}" -e TF_OPTIONAL_BAZEL_BUILD_OPTIONS="${TF_OPTIONAL_BAZEL_BUILD_OPTIONS}" -e WHL_DIR="${WHL_DIR}" -e TF_BUILD_VERSION="${TF_BUILD_VERSION}" -e BAZEL_VERSION="${BAZEL_VERSION}" -e PYTHON="python3.4" -e PIP="pip3.4" --mount src=$(pwd),dst=/host,type=bind amr-registry.caas.intel.com/aipg-tf/dev/centos7:${CENTOS_DOCKER_TAG}-py3.4 /host/build-tensorflow-whl.sh 2>&1 | tee log
${dr} -e TF_BUILD_REPO="${TF_BUILD_REPO}" -e TF_BAZEL_BUILD_OPTIONS="${TF_BAZEL_BUILD_OPTIONS}" -e TF_OPTIONAL_BAZEL_BUILD_OPTIONS="${TF_OPTIONAL_BAZEL_BUILD_OPTIONS}" -e WHL_DIR="${WHL_DIR}" -e TF_BUILD_VERSION="${TF_BUILD_VERSION}" -e BAZEL_VERSION="${BAZEL_VERSION}" -e PYTHON="python3.5" -e PIP="pip3.5" --mount src=$(pwd),dst=/host,type=bind amr-registry.caas.intel.com/aipg-tf/dev/centos7:${CENTOS_DOCKER_TAG}-py3.5 /host/build-tensorflow-whl.sh 2>&1 | tee log
${dr} -e TF_BUILD_REPO="${TF_BUILD_REPO}" -e TF_BAZEL_BUILD_OPTIONS="${TF_BAZEL_BUILD_OPTIONS}" -e TF_OPTIONAL_BAZEL_BUILD_OPTIONS="${TF_OPTIONAL_BAZEL_BUILD_OPTIONS}" -e WHL_DIR="${WHL_DIR}" -e TF_BUILD_VERSION="${TF_BUILD_VERSION}" -e BAZEL_VERSION="${BAZEL_VERSION}" -e PYTHON="python3.6" -e PIP="pip3.6" --mount src=$(pwd),dst=/host,type=bind amr-registry.caas.intel.com/aipg-tf/dev/centos7:${CENTOS_DOCKER_TAG}-py3.6 /host/build-tensorflow-whl.sh 2>&1 | tee log

