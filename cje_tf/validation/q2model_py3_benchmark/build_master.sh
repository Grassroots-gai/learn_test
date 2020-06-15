#!/usr/bin/env bash


# Helper function to traverse directories up until given file is found.
function upsearch () {
  test / == "$PWD" && return || \
      test -e "$1" && echo "$PWD" && return || \
      cd .. && upsearch "$1"
}

# Set up WORKSPACE.
WS="${WS:-$(upsearch WORKSPACE)}"
TF_DOCKER_BUILD_DEVEL_BRANCH="master"
TF_DOCKER_BUILD_IMAGE_NAME="jenkin-job/private-tensorflow"
TF_DOCKER_BUILD_VERSION="nightly"

#TF_DOCKER_BUILD_ARGS="--build-arg https_proxy=https://proxy-us.intel.com:912 http_proxy=http://proxy-us.intel.com:911 no_proxy=localhost,127.0.0.1,intel.com,.intel.com,10.0.0.0/8,172.16.0.0/16,192.168.0.0/16"
#https_proxy="https://proxy-us.intel.com:912"
#http_proxy="http://proxy-us.intel.com:911"



# build the python 3 container and whl
TF_DOCKER_BUILD_TYPE="MKL" \
  TF_DOCKER_BUILD_IS_DEVEL="YES" \
  TF_DOCKER_BUILD_DEVEL_BRANCH="${TF_DOCKER_BUILD_DEVEL_BRANCH}" \
  TF_DOCKER_BUILD_IMAGE_NAME="${TF_DOCKER_BUILD_IMAGE_NAME}" \
  TF_DOCKER_BUILD_VERSION="${TF_DOCKER_BUILD_VERSION}" \
  TF_DOCKER_BUILD_PYTHON_VERSION="PYTHON3" \
  ${WS}/tensorflow/tools/docker/parameterized_docker_build.sh
