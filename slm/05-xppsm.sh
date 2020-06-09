#!/bin/bash

tar -C /tmp/ -xf _xppsm-2.2.0-centos7.4.tar
yum install -y /tmp/xppsm-2.2.0/centos7.4/packages/x86_64/core/*
rm -rf /tmp/xppsm-2.2.0/

hwloc-dump-hwdata | grep "Cluster Mode"
hwloc-dump-hwdata | grep "Memory Mode"
