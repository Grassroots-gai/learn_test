#!/bin/bash

#scp root@192.169.10.1:/root/scripts/_RHEL_7.5_lustre_client.tgz ./
#install the lustre client for CentOS 7.5
tar -xvf _RHEL_7.6_lustre_client.tgz
rpm -ivh kmod-lustre-client-2.10.8-1.el7.x86_64.rpm
yum install -y libyaml
rpm -ivh lustre-client-2.10.8-1.el7.x86_64.rpm


#mount
mkdir /lustre
echo "options lnet networks=tcp(enp0s20f0u5)" > /etc/modprobe.d/lustre.conf
echo "192.169.20.2@tcp:/lustrefs     /lustre lustre  defaults,_netdev,localflock        0 0" >> /etc/fstab
mount -a
ls /lustre/dataset
