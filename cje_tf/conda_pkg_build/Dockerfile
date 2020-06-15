#This Dockerfile just for build tensorflow conda package.

FROM centos:7
MAINTAINER chuanqi.wang@intel.com

ARG TF_WHLS_VER
ARG TF_WHLS_DIR=/tmp
ARG CHANNEL=intel
ARG PY_VERS="2.7 3.6 3.7"

COPY ./${TF_WHLS_DIR} /tmp/${TF_WHLS_VER}
COPY ./recipes/${TF_WHLS_VER} /tmp/${TF_WHLS_VER}

# Install some basic package for base image.
RUN rpm -iUvh https://dl.fedoraproject.org/pub/epel/7/x86_64/Packages/e/epel-release-7-12.noarch.rpm && \
    yum upgrade -y && \
    yum install -y \
        bzip2 \
        numactl \
        redhat-rpm-config \
        tar \
        findutils \
        gcc-c++ \
        cmake \
        git \
        vim \
        wget \
        ssh \
        openssh.x86_64 \
        openssh-server.x86_64 \
        openssh-clients.x86_64 \
        initscripts  \
        net-tools \
        ufw \
        iptables \
        gcc-gfortran && \
    yum clean all

WORKDIR /root/

# Install miniconda and conda build package.
RUN wget --no-check-certificate -c https://repo.continuum.io/miniconda/Miniconda2-latest-Linux-x86_64.sh && \
    bash Miniconda2-latest-Linux-x86_64.sh -b && \
    echo "export PATH=/root/miniconda2/bin:$PATH" >> /root/.bashrc && \
    source /root/.bashrc && \
    conda config --add channels intel && \
    conda install conda-build conda-verify anaconda-client -y

WORKDIR /tmp/${TF_WHLS_VER}

# Build the tensorflow-base conda package
RUN source /root/.bashrc && \
    conda-build --python 37 -c ${CHANNEL} tensorflow-base/meta.yaml && \
    cp /root/miniconda2/conda-bld/linux-64/*.tar.bz2 tensorflow-base/ && \
    conda clean --all -y
 
# Build the conda packages and store them in conda folder for each version
RUN \
    source /root/.bashrc && \
    for PY_VER in ${PY_VERS}; do \
        export TF_WHL_DIR=$(find -name "*${PY_VER}*"); \
        export CONDA_PY=$(echo ${PY_VER} | tr -d '.'); \
        export TF_WHL=$(find /tmp/${TF_WHLS_VER}/${TF_WHL_DIR}/ -name "*tensorflow*.whl"); \
        conda-build --python ${CONDA_PY} -c ${CHANNEL} ${TF_WHL_DIR}/meta.yaml; \
        cp /root/miniconda2/conda-bld/linux-64/*.tar.bz2 ${TF_WHL_DIR}/; \
    done && \
    conda clean --all -y

