#!/usr/bin/env bash

set -xe

SUPPORTED_PY_VERS="2.7 3.6 3.7" # Python versions we currently support
PY_VERS=${PY_VERS:-"2.7,3.6,3.7"}   # Version provided by user
TF_WHLS_VER=${TF_WHLS_VER:-"v1.14.0"}   # TensorFlow version
# where tensorflow wheels are and the recipes to build the conda packages
TF_WHLS_DIR=${TF_WHLS_DIR:-/tmp}
# where output of this script goes
TF_CONDA_DIR=${TF_CONDA_DIR:-/tmp}
CHANNEL=${CHANNEL:-"intel"}

# only eval `tag` if it isn't set
# we do it this way so we can run this script inside conda_pkgs dir if we want
# rather than copying data from there to where this lives
if [ -z ${TAG+x} ]; then
    TAG=$(git describe --tags --always --dirty)
fi

# check if all versions are valid
# should be a list of strings at this point
for PY_VER in ${PY_VERS}; do
    if [[ ! $SUPPORTED_PY_VERS =~ (^|[[:space:]])$PY_VER($|[[:space:]]) ]]; then
        echo "Python version ${PY_VER} is not supported"
        exit 1
    fi;
done

# Build the image as follows
docker build \
    --build-arg HTTP_PROXY=${HTTP_PROXY} \
    --build-arg HTTPS_PROXY=${HTTPS_PROXY} \
    --build-arg http_proxy=${http_proxy} \
    --build-arg https_proxy=${https_proxy} \
    --build-arg TF_WHLS_VER=${TF_WHLS_VER} \
    --build-arg TF_WHLS_DIR=${TF_WHLS_DIR} \
    --build-arg CHANNEL=${CHANNEL} \
    --build-arg PY_VERS="${PY_VERS}" \
    . -t conda-pkg-build:${TAG}

# Copy the file to local file system
mkdir -p ${TF_CONDA_DIR}
docker run --name temp-conda-pkg-build conda-pkg-build:${TAG} /bin/true
docker cp temp-conda-pkg-build:/tmp/${TF_WHLS_VER} ${TF_CONDA_DIR}
#docker rm temp-conda-pkg-build
#docker rmi conda-pkg-build:${TAG}
echo "All new packages are stored at: ${TF_CONDA_DIR}"

# Upload the files to permanent storage
# scp -rp ${COND_PKGS_DIR} FILE_SERVER
set +xe
