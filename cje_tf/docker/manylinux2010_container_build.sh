#!/bin/bash

function fail()
{
  echo "ERROR: ${1}"
  echo "Exiting..."
  exit 1
}

TENSORFLOW_REPO="${TENSORFLOW_REPO:-https://github.com/Intel-tensorflow/tensorflow.git}"
TENSORFLOW_BRANCH="${TENSORFLOW_BRANCH:-v2.1.0}"
DOCKERFILE_DIR="tensorflow/tools/ci_build"
DOCKERFILE_NAME="Dockerfile.rbe.ubuntu16.04-manylinux2010"
BUILD_V1="${BUILD_V1:-n}"
PYTHON_VERSIONS="${PYTHON_VERSIONS:-3.5,3.6,3.7}"
BAZEL_VERSION=${BAZEL_VERSION:-"0.29.1"}
HTTP_PROXY="http://proxy-us.intel.com:911"
HTTPS_PROXY="http://proxy-us.intel.com:911"
CONTAINER_URI="${CONTAINER_URI:-amr-registry.caas.intel.com/aipg-tf/manylinux2010}"
DEFAULT_CONTAINER_TAG=$(echo "${TENSORFLOW_BRANCH}" | sed 's/v//')-devel-mkl
CONTAINER_TAG="${CONTAINER_TAG:-${DEFAULT_CONTAINER_TAG}}"
FULL_CONTAINER_TAG="${CONTAINER_URI}:${CONTAINER_TAG}"
BUILD_WHLS="${BUILD_WHLS:-1}"
WHL_DIR="/host"

# Process the Python versions array
IFS=',' read -ra PYTHON_VERSIONS <<< ${PYTHON_VERSIONS}

echo "TENSORFLOW_DIR ${TENSORFLOW_DIR}"
echo "DOCKERFILE_DIR ${DOCKERFILE_DIR}"
echo "FULL_CONTAINER_TAG ${FULL_CONTAINER_TAG}"
echo "PYTHON_VERSIONS = ${PYTHON_VERSIONS[@]}"

dr='docker run --disable-content-trust --hostname="crobiso1-container" -e https_proxy -e http_proxy -e HTTPS_PROXY -e HTTP_PROXY -e no_proxy -e NO_PROXY'

# Build the container
for py in ${PYTHON_VERSIONS[@]}; do
  echo "Building manylinux2010 container for python${py}"
  docker build --disable-content-trust --build-arg PYTHON_VERSION=$py \
    --build-arg HTTPS_PROXY=${HTTP_PROXY} --build-arg https_proxy=${HTTP_PROXY} \
    --build-arg HTTP_PROXY=${HTTP_PROXY} --build-arg http_proxy=${HTTP_PROXY} \
    --build-arg FTP_PROXY=${HTTP_PROXY} --build-arg ftp_proxy=${HTTP_PROXY} \
    --tag ${FULL_CONTAINER_TAG}-py${py} -f ${DOCKERFILE_NAME} .
done

if [[ "${BUILD_WHLS}" == "1" ]]; then
  # BUild the whl
  for py in ${PYTHON_VERSIONS[@]}; do 
      echo "Building TF for python${py} in ${FULL_CONTAINER_TAG}-py${py}"
      # ${dr} -e BUILD_V1="${BUILD_V1}" -e WHL_DIR="${WHL_DIR}" -e TENSORFLOW_REPO="${TENSORFLOW_REPO}" -e TENSORFLOW_BRANCH="${TENSORFLOW_BRANCH}" -e PYTHON="python${py}" -e PIP="pip${py}" -e BAZEL_VERSION="${BAZEL_VERSION}" --mount src=$(pwd),dst=/host,type=bind ${docker_addr} /host/manylinux2010_whl_build.sh 2>&1 | tee log
      ${dr} -e BUILD_V1="${BUILD_V1}" -e WHL_DIR="${WHL_DIR}" -e TENSORFLOW_REPO="${TENSORFLOW_REPO}" -e TENSORFLOW_BRANCH="${TENSORFLOW_BRANCH}" -e PYTHON_VERSION="${py}" -e BAZEL_VERSION="${BAZEL_VERSION}" --mount src=$(pwd),dst=/host,type=bind ${FULL_CONTAINER_TAG}-py${py} /host/manylinux2010_whl_build.sh 2>&1 | tee log
  done
fi
