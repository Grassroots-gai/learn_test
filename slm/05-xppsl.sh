#!/bin/bash

tar -C /tmp/ -xf _xppsl-1.5.2-centos7.3.tar
yum install -y /tmp/xppsl-1.5.2/centos7.3/x86_64/*
rm -rf /tmp/xppsl-1.5.2/

hwloc-dump-hwdata | grep "Cluster Mode"
hwloc-dump-hwdata | grep "Memory Mode"
