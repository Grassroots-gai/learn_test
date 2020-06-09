#!/bin/bash

# proxy
cat << EOF > /etc/environment
http_proxy=http://child-prc.intel.com:913/
https_proxy=https://child-prc.intel.com:913/
no_proxy="192.169.*.*"
EOF

# proxy for apt
cp _etc_apt_apt.conf.d_proxy.conf \
   /etc/apt/apt.conf.d/proxy.conf

# hosts
cp _etc_hosts /etc/hosts

# ssh
mkdir -p /root/.ssh
cp _id_rsa     /root/.ssh/id_rsa
cp _id_rsa.pub /root/.ssh/id_rsa.pub
cat /root/.ssh/id_rsa.pub > /root/.ssh/authorized_keys
chmod 600 /root/.ssh/authorized_keys
