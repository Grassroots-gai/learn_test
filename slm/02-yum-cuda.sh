#!/bin/bash

# yum mirror
cp _etc_yum.repos.d_cuda.repo /etc/yum.repos.d/cuda.repo

# cuda and cudnn
yum install -y cuda

export http_proxy=
curl -O http://192.169.10.1/repo/cuda/cudnn-8.0-linux-x64-v6.0.tgz
tar -C /usr/local -xf cudnn-8.0-linux-x64-v6.0.tgz

