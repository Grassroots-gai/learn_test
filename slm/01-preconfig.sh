#!/bin/bash

# proxy
cat << EOF > /etc/profile.d/proxy.sh
export http_proxy=http://child-prc.intel.com:913
export https_proxy=http://child-prc.intel.com:913
export no_proxy="192.169.*.*"
EOF

# hosts
cp _etc_hosts /etc/hosts

# ssh
mkdir -p /root/.ssh
cp _id_rsa     /root/.ssh/id_rsa
cp _id_rsa.pub /root/.ssh/id_rsa.pub
cat /root/.ssh/id_rsa.pub > /root/.ssh/authorized_keys
chmod 600 /root/.ssh/authorized_keys
