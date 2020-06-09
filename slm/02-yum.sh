#!/bin/bash

# yum proxy
#if ! grep proxy /etc/yum.conf > /dev/null ; then
#	echo "proxy=_none_" >> /etc/yum.conf
#fi

#scp epel and KEY
#scp root@mlt-ace:/etc/yum.repos.d/epel.repo /etc/yum.repos.d/
#scp root@mlt-ace:/etc/pki/rpm-gpg/RPM-GPG-KEY-EPEL-7 /etc/pki/rpm-gpg/

# yum mirror
if ! grep "mlt-ace" /etc/yum.repos.d/CentOS-Base.repo > /dev/null ; then
	cp /etc/yum.repos.d/CentOS-Base.repo /etc/yum.repos.d/CentOS-Base.repo.ori
	cp _etc_yum.repos.d_CentOS-Base.repo /etc/yum.repos.d/CentOS-Base.repo
	yum install -y epel-release
fi

# yum epel mirror
if ! grep "mlt-ace" /etc/yum.repos.d/epel.repo > /dev/null ; then
	cp /etc/yum.repos.d/epel.repo /etc/yum.repos.d/epel.repo.ori
	cp _etc_yum.repos.d_epel.repo /etc/yum.repos.d/epel.repo
fi

# yum install package
## basic
yum install -y htop tmux wget bc

## dev tool
yum install -y cmake cmake3
yum groupinstall -y "Development Tools"
yum groupinstall -y "X Window System"

## python
yum install -y python-devel numpy python2-pip
yum install -y python34 python34-devel python34-numpy python34-pip
yum install -y python-virtualenv python-wheel

## caffe
yum install -y protobuf-devel leveldb-devel snappy-devel opencv-devel boost-devel hdf5-devel gflags-devel glog-devel lmdb-devel

## network
yum install -y iperf iperf3 qperf

## 
yum install -y lm_sensors ipmitool

yum install -y sox-devel

yum install -y java java-1.8.0-openjdk-devel

yum install -y scl-utils
