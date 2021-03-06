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

# Helper function to traverse directories up until given file is found.
function upsearch () {
  test / == "$PWD" && return || \
      test -e "$1" && echo "$PWD" && return || \
      cd .. && upsearch "$1"
}

# Set up WORKSPACE.
WORKSPACE="${WORKSPACE:-$(upsearch WORKSPACE)}"

ROOT_CONTAINER_TAG=${ROOT_CONTAINER_TAG:-latest}
TF_DOCKER_BUILD_DEVEL_BRANCH=${TF_DOCKER_BUILD_DEVEL_BRANCH:-master}
TF_DOCKER_BUILD_IMAGE_NAME=${TF_DOCKER_BUILD_IMAGE_NAME:-intel-mkl/tensorflow}
TF_DOCKER_BUILD_VERSION=${TF_DOCKER_BUILD_VERSION:-nightly}
BUILD_AVX_CONTAINERS=${BUILD_AVX_CONTAINERS:-no}
BUILD_AVX2_CONTAINERS=${BUILD_AVX2_CONTAINERS:-no}


echo "TF_DOCKER_BUILD_DEVEL_BRANCH=${TF_DOCKER_BUILD_DEVEL_BRANCH}"
echo "TF_DOCKER_BUILD_IMAGE_NAME=${TF_DOCKER_BUILD_IMAGE_NAME}"
echo "TF_DOCKER_BUILD_VERSION=${TF_DOCKER_BUILD_VERSION}"
echo "BUILD_AVX_CONTAINERS=${BUILD_AVX_CONTAINERS}"
echo "BUILD_AVX2_CONTAINERS=${BUILD_AVX2_CONTAINERS}"

# Build containers for AVX
if [[ ${BUILD_AVX_CONTAINERS} == "yes" ]]; then
	# Include the instructions for sandybridge and later, but tune for ivybridge
	TF_BAZEL_BUILD_OPTIONS="--config=mkl --copt=-march=sandybridge --copt=-mtune=ivybridge --copt=-O3 --cxxopt=-D_GLIBCXX_USE_CXX11_ABI=0"

	# build the python 2 container and whl
	TF_DOCKER_BUILD_TYPE="MKL" \
	  TF_DOCKER_BUILD_IS_DEVEL="YES" \
	  TF_DOCKER_BUILD_DEVEL_BRANCH="${TF_DOCKER_BUILD_DEVEL_BRANCH}" \
	  TF_DOCKER_BUILD_IMAGE_NAME="${TF_DOCKER_BUILD_IMAGE_NAME}" \
	  TF_DOCKER_BUILD_VERSION="${TF_DOCKER_BUILD_VERSION}" \
	  TF_BAZEL_BUILD_OPTIONS="${TF_BAZEL_BUILD_OPTIONS}" \
	  ROOT_CONTAINER_TAG="${ROOT_CONTAINER_TAG}" \
	  ${WORKSPACE}/tensorflow/tensorflow/tools/docker/intel_parameterized_docker_build.sh

	# build the python 3 container and whl
	TF_DOCKER_BUILD_TYPE="MKL" \
	  TF_DOCKER_BUILD_IS_DEVEL="YES" \
	  TF_DOCKER_BUILD_DEVEL_BRANCH="${TF_DOCKER_BUILD_DEVEL_BRANCH}" \
	  TF_DOCKER_BUILD_IMAGE_NAME="${TF_DOCKER_BUILD_IMAGE_NAME}" \
	  TF_DOCKER_BUILD_VERSION="${TF_DOCKER_BUILD_VERSION}" \
	  TF_DOCKER_BUILD_PYTHON_VERSION="PYTHON3" \
	  TF_BAZEL_BUILD_OPTIONS="${TF_BAZEL_BUILD_OPTIONS}" \
	  ROOT_CONTAINER_TAG="${ROOT_CONTAINER_TAG}" \
	  ${WORKSPACE}/tensorflow/tensorflow/tools/docker/intel_parameterized_docker_build.sh
fi

# Build containers for AVX2
if [[ ${BUILD_AVX2_CONTAINERS} == "yes" ]]; then
	TF_BAZEL_BUILD_OPTIONS="--config=mkl --copt=-march=haswell --copt=-mtune=broadwell --copt=-O3 --cxxopt=-D_GLIBCXX_USE_CXX11_ABI=0"

	# build the python 2 container and whl
	TF_DOCKER_BUILD_TYPE="MKL" \
	  TF_DOCKER_BUILD_IS_DEVEL="YES" \
	  TF_DOCKER_BUILD_DEVEL_BRANCH="${TF_DOCKER_BUILD_DEVEL_BRANCH}" \
	  TF_DOCKER_BUILD_IMAGE_NAME="${TF_DOCKER_BUILD_IMAGE_NAME}" \
	  TF_DOCKER_BUILD_VERSION="${TF_DOCKER_BUILD_VERSION}-avx2" \
	  TF_BAZEL_BUILD_OPTIONS="${TF_BAZEL_BUILD_OPTIONS}" \
	  ROOT_CONTAINER_TAG="${ROOT_CONTAINER_TAG}" \
	  ${WORKSPACE}/tensorflow/tensorflow/tools/docker/intel_parameterized_docker_build.sh

	# build the python 3 container and whl
	TF_DOCKER_BUILD_TYPE="MKL" \
	  TF_DOCKER_BUILD_IS_DEVEL="YES" \
	  TF_DOCKER_BUILD_DEVEL_BRANCH="${TF_DOCKER_BUILD_DEVEL_BRANCH}" \
	  TF_DOCKER_BUILD_IMAGE_NAME="${TF_DOCKER_BUILD_IMAGE_NAME}" \
	  TF_DOCKER_BUILD_VERSION="${TF_DOCKER_BUILD_VERSION}-avx2" \
	  TF_DOCKER_BUILD_PYTHON_VERSION="PYTHON3" \
	  TF_BAZEL_BUILD_OPTIONS="${TF_BAZEL_BUILD_OPTIONS}" \
	  ROOT_CONTAINER_TAG="${ROOT_CONTAINER_TAG}" \
	  ${WORKSPACE}/tensorflow/tensorflow/tools/docker/intel_parameterized_docker_build.sh
fi
