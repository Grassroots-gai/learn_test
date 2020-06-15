#!/usr/bin/env bash
# Copyright 2019 The TensorFlow Authors. All Rights Reserved.
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

DEBUG=1
DOCKER_BINARY="docker"

# Create tmp directory for Docker build
TMP_DIR=$(pwd)
echo ""
echo "Docker build will occur in temporary directory: ${TMP_DIR}"

# Helper function to traverse directories up until given file is found.
function upsearch () {
  test / == "$PWD" && return || \
      test -e "$1" && echo "$PWD" && return || \
      cd .. && upsearch "$1"
}

function debug()
{
  if [[ ${DEBUG} == 1 ]] ; then
    echo $1
  fi
}

function die()
{
  echo $1
  exit 1
}

# Set up WORKSPACE.
WORKSPACE="${WORKSPACE:-$(upsearch WORKSPACE)}"

ROOT_CONTAINER=${ROOT_CONTAINER:-tensorflow/tensorflow}
TF_ROOT_CONTAINER_TAG=${ROOT_CONTAINER_TAG:-latest}
FINAL_IMAGE_NAME=${TF_DOCKER_BUILD_IMAGE_NAME:-intel-mkl/tensorflow}
TF_DOCKER_BUILD_VERSION=${TF_DOCKER_BUILD_VERSION:-nightly}
BUILD_AVX_CONTAINERS=${BUILD_AVX_CONTAINERS:-no}
BUILD_AVX2_CONTAINERS=${BUILD_AVX2_CONTAINERS:-no}
BUILD_SKX_CONTAINERS=${BUILD_SKX_CONTAINERS:-no}
BUILD_CLX_CONTAINERS=${BUILD_CLX_CONTAINERS:-no}
BUILD_JUPYTER_CONTAINERS=${BUILD_JUPYTER_CONTAINERS:-no}
CONTAINER_PORT=${TF_DOCKER_BUILD_PORT:-8888}
TF_DOCKER_BUILD_WHL_AVX_PY2=${TF_DOCKER_BUILD_WHL_AVX_PY2}
TF_DOCKER_BUILD_WHL_AVX_PY3=${TF_DOCKER_BUILD_WHL_AVX_PY3}
TF_DOCKER_BUILD_WHL_AVX2_PY2=${TF_DOCKER_BUILD_WHL_AVX2_PY2}
TF_DOCKER_BUILD_WHL_AVX2_PY3=${TF_DOCKER_BUILD_WHL_AVX2_PY3}

debug "TF_ROOT_CONTAINER_TAG=${TF_ROOT_CONTAINER_TAG}"
debug "ROOT_CONTAINER=${ROOT_CONTAINER}"
debug "TF_BUILD_VERSION=${TF_BUILD_VERSION}"
debug "FINAL_IMAGE_NAME=${FINAL_IMAGE_NAME}"
debug "TF_DOCKER_BUILD_VERSION=${TF_DOCKER_BUILD_VERSION}"
debug "BUILD_AVX_CONTAINERS=${BUILD_AVX_CONTAINERS}"
debug "BUILD_AVX2_CONTAINERS=${BUILD_AVX2_CONTAINERS}"
debug "BUILD_SKX_CONTAINERS=${BUILD_SKX_CONTAINERS}"
debug "BUILD_CLX_CONTAINERS=${BUILD_CLX_CONTAINERS}"
debug "TMP_DIR=${TMP_DIR}"
debug "TF_DOCKER_BUILD_WHL_AVX_PY2=${TF_DOCKER_BUILD_WHL_AVX_PY2}"
debug "TF_DOCKER_BUILD_WHL_AVX_PY3=${TF_DOCKER_BUILD_WHL_AVX_PY3}"
debug "TF_DOCKER_BUILD_WHL_AVX2_PY2=${TF_DOCKER_BUILD_WHL_AVX2_PY2}"
debug "TF_DOCKER_BUILD_WHL_AVX2_PY3=${TF_DOCKER_BUILD_WHL_AVX2_PY3}"

function copy_whl_for_build()
{
    if [[ "$#" != "1" ]]; then
        die "Usage: ${FUNCNAME} <TF_DOCKER_BUILD_CENTRAL_WHL>"
    fi
   
    PIP_WHL="${TF_DOCKER_BUILD_CENTRAL_WHL}"
    if [[ -f "${PIP_WHL}" ]]; then
        die "ERROR: Cannot locate the specified pip whl file ${PIP_WHL}"
    fi
    echo "Specified PIP whl file is at: ${PIP_WHL}"

    # Copy the pip file to tmp directory
    cp "${PIP_WHL}" "${TMP_DIR}/" || \
    die "ERROR: Failed to copy wheel file: ${PIP_WHL}"

    # Use string replacement to put the correct file name into the Dockerfile
    PIP_WHL=$(basename "${PIP_WHL}")
        
    TF_DOCKER_BUILD_ARGS+=("--build-arg TF_WHL_URL=${PIP_WHL}" )
    
}

function build_container()
{
  if [[ $# -lt 2 ]]; then
    die "Usage: build_container <TEMP_IMAGE_NAME> <TF_DOCKER_BUILD_ARGS>."
  fi
  
  TEMP_IMAGE_NAME=${1}
  debug "TEMP_IMAGE_NAME=${TEMP_IMAGE_NAME}"
  shift
  TF_DOCKER_BUILD_ARGS=("${@}")

  # Add the proxy info build args
  TF_DOCKER_BUILD_ARGS+=("--build-arg http_proxy=${http_proxy}")
  TF_DOCKER_BUILD_ARGS+=("--build-arg https_proxy=${https_proxy}")
  TF_DOCKER_BUILD_ARGS+=("--build-arg socks_proxy=${socks_proxy}")
  TF_DOCKER_BUILD_ARGS+=("--build-arg no_proxy=${no_proxy}")
  TF_DOCKER_BUILD_ARGS+=("--build-arg HTTP_PROXY=${http_proxy}")
  TF_DOCKER_BUILD_ARGS+=("--build-arg SOCKS_PROXY=${socks_proxy}")
  TF_DOCKER_BUILD_ARGS+=("--build-arg NO_PROXY=${no_proxy}")
  

  # Perform docker build
  debug "Building docker image with image name and tag: ${TEMP_IMAGE_NAME}"
  CMD="${DOCKER_BINARY} build ${TF_DOCKER_BUILD_ARGS[@]} --no-cache --pull -t ${TEMP_IMAGE_NAME} -f Dockerfile.mkl ${TMP_DIR}"
  cd Dockerfiles
  debug "CMD=${CMD}"
  ${CMD}

  if [[ $? == "0" ]]; then
    debug "${DOCKER_BINARY} build of ${TEMP_IMAGE_NAME} succeeded"
  else
    die "FAIL: ${DOCKER_BINARY} build of ${TEMP_IMAGE_NAME} failed"
  fi
}

function test_container()
{
  if [[ "$#" != "1" ]]; then
    die "Usage: ${FUNCNAME} <TEMP_IMAGE_NAME>"
  fi

  TEMP_IMAGE_NAME=${1}

  # Make sure that there is no other containers of the same image running
  if "${DOCKER_BINARY}" ps | grep -q "${TEMP_IMAGE_NAME}"; then
    die "ERROR: It appears that there are docker containers of the image "\
  "${TEMP_IMAGE_NAME} running. Please stop them before proceeding"
  fi

  # Start a docker container from the newly-built docker image
  DOCKER_RUN_LOG="${TMP_DIR}/docker_run.log"
  debug "  Log file is at: ${DOCKER_RUN_LOG}"

  debug "Running docker container from image ${TEMP_IMAGE_NAME}..."
  RUN_CMD="${DOCKER_BINARY} run --rm -d -p ${CONTAINER_PORT}:${CONTAINER_PORT} ${TEMP_IMAGE_NAME} tail -f /dev/null 2>&1 > ${DOCKER_RUN_LOG}"
  cd Dockerfiles
  debug "RUN_CMD=${RUN_CMD}"
  ${RUN_CMD}
  
  # Get the container ID
  CONTAINER_ID=""
  while [[ -z ${CONTAINER_ID} ]]; do
    sleep 1
    debug "Polling for container ID..."
    CONTAINER_ID=$("${DOCKER_BINARY}" ps | grep "${TEMP_IMAGE_NAME}" | awk '{print $1}')
  done

  debug "ID of the running docker container: ${CONTAINER_ID}"

  debug "Performing basic sanity checks on the running container..."
  TEST_CMD=$(${DOCKER_BINARY} exec ${CONTAINER_ID} bash -c "${PYTHON} -c 'from tensorflow.python import pywrap_tensorflow; print(pywrap_tensorflow.IsMklEnabled())'")
  debug "Running test command: ${TEST_CMD}"
  if [ "${TEST_CMD}" = "True" ] ; then
      echo "PASS: MKL enabled test in ${TEMP_IMAGE_NAME}"
  else
      die "FAIL: MKL enabled test in ${TEMP_IMAGE_NAME}"
  fi

  # Stop the running docker container
  sleep 1
  "${DOCKER_BINARY}" stop --time=0 ${CONTAINER_ID}
}

function tag_container()
{
  # Apply the final image name and tag
  TEMP_IMAGE_NAME="${1}"
  FINAL_IMG="${2}"

  DOCKER_VER=$("${DOCKER_BINARY}" version | grep Version | head -1 | awk '{print $NF}')
  if [[ -z "${DOCKER_VER}" ]]; then
    die "ERROR: Failed to determine ${DOCKER_BINARY} version"
  fi
  DOCKER_MAJOR_VER=$(echo "${DOCKER_VER}" | cut -d. -f 1)
  DOCKER_MINOR_VER=$(echo "${DOCKER_VER}" | cut -d. -f 2)

  FORCE_TAG=""
  if [[ "${DOCKER_MAJOR_VER}" -le 1 ]] && \
    [[ "${DOCKER_MINOR_VER}" -le 9 ]]; then
    FORCE_TAG="--force"
  fi

  "${DOCKER_BINARY}" tag ${FORCE_TAG} "${TEMP_IMAGE_NAME}" "${FINAL_IMG}" || \
      die "Failed to tag intermediate docker image ${TEMP_IMAGE_NAME} as ${FINAL_IMG}"

  debug "Successfully tagged docker image: ${FINAL_IMG}"
}

PYTHON_VERSIONS=("python3")
PLATFORMS=()
if [[ ${BUILD_AVX_CONTAINERS} == "yes" ]]; then
  PLATFORMS+=("sandybridge")
fi

if [[ ${BUILD_AVX2_CONTAINERS} == "yes" ]]; then
  PLATFORMS+=("haswell")
fi

if [[ ${BUILD_SKX_CONTAINERS} == "yes" ]]; then
  PLATFORMS+=("skylake")
fi

if [[ ${BUILD_CLX_CONTAINERS} == "yes" ]]; then
  PLATFORMS+=("icelake")
fi

for PLATFORM in "${PLATFORMS[@]}"
do
  for PYTHON in "${PYTHON_VERSIONS[@]}"
  do
    # Clear the build args array
    FINAL_TAG="${TF_DOCKER_BUILD_VERSION}"
    ROOT_CONTAINER_TAG="${TF_ROOT_CONTAINER_TAG}"

      if [[ ${PLATFORM} == "sandybridge" ]]; then
        FINAL_TAG="${FINAL_TAG}"
        if [[ "${PYTHON}" == "python3" ]]; then
           WHL_LOCATION=${TF_DOCKER_BUILD_WHL_AVX_PY3}
           # ${TF_DOCKER_BUILD_WHL_AVX_PY3}
        else
           WHL_LOCATION=${TF_DOCKER_BUILD_WHL_AVX_PY2}
           #copy_whl_for_build ${TF_DOCKER_BUILD_WHL_AVX_PY2}
        fi 
      fi

      if [[ ${PLATFORM} == "haswell" ]]; then
        FINAL_TAG="${FINAL_TAG}-avx2"
        if [[ "${PYTHON}" == "python3" ]]; then
           WHL_LOCATION=${TF_DOCKER_BUILD_WHL_AVX2_PY3}
           #copy_whl_for_build ${TF_DOCKER_BUILD_WHL_AVX2_PY3}
        else
           WHL_LOCATION=${TF_DOCKER_BUILD_WHL_AVX2_PY2}
           #copy_whl_for_build ${TF_DOCKER_BUILD_WHL_AVX2_PY2}
        fi
        
      fi

      if [[ ${PLATFORM} == "skylake" ]]; then
        FINAL_TAG="${FINAL_TAG}-avx512"
      fi

      if [[ ${PLATFORM} == "icelake" ]]; then
        FINAL_TAG="${FINAL_TAG}-avx512-VNNI"
      fi

      # Add -mkl to the image tag
      FINAL_TAG="${FINAL_TAG}-mkl"

      if [[ "${PYTHON}" == "python3" ]]; then
      	TF_DOCKER_BUILD_ARGS+=("--build-arg PYTHON=python3")	      
        TF_DOCKER_BUILD_ARGS+=("--build-arg PIP=pip3")
        FINAL_TAG="${FINAL_TAG}-py3"        
        ROOT_CONTAINER_TAG="${ROOT_CONTAINER_TAG}-py3"
      fi

      if [[ ${BUILD_JUPYTER_CONTAINERS} == "yes" ]]; then
        ROOT_CONTAINER_TAG="${ROOT_CONTAINER_TAG}-jupyter"
        FINAL_TAG="${FINAL_TAG}-jupyter"
      fi

      TF_DOCKER_BUILD_ARGS+=("--build-arg ROOT_CONTAINER_TAG=${ROOT_CONTAINER_TAG}")
      TF_DOCKER_BUILD_ARGS+=("--build-arg ROOT_CONTAINER=${ROOT_CONTAINER}")

      # Intermediate image name with tag      
      TEMP_IMAGE_NAME="${USER}/tensorflow:${FINAL_TAG}"
      echo "TEMP_IMAGE_NAME ${TEMP_IMAGE_NAME}"
      TF_DOCKER_BUILD_ARGS+=("--build-arg TF_WHL_URL=${WHL_LOCATION}")
      build_container "${TEMP_IMAGE_NAME}" "${TF_DOCKER_BUILD_ARGS[@]}"
      test_container "${TEMP_IMAGE_NAME}"
      tag_container "${TEMP_IMAGE_NAME}" "${FINAL_IMAGE_NAME}:${FINAL_TAG}"

  done
done
