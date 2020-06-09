#!/bin/bash

# yum mirror
cp _etc_yum.repos.d_docker-ce.repo /etc/yum.repos.d/docker-ce.repo

# dependency
yum install -y container-selinux-2.95-2.el7_6.noarch.rpm

# docker-ce
yum install -y docker-ce

mkdir -p /etc/systemd/system/docker.service.d/
cp _etc_systemd_system_docker.service.d_proxy.conf /etc/systemd/system/docker.service.d/proxy.conf

systemctl start docker
