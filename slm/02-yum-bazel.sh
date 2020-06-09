#!/bin/bash

# yum mirror
cp _etc_yum.repos.d_vbatts-bazel-epel-7.repo /etc/yum.repos.d/vbatts-bazel-epel-7.repo

# bazel
yum install -y bazel

