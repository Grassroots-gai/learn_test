#!/bin/bash

#scp root@192.169.10.1:/root/scripts/_RHEL_7.5_lustre_client.tgz ./
#install the lustre client for CentOS 7.5
yum -y install libyaml
tar -xvf _RHEL_7.5_lustre_client.tgz
cd _RHEL7.5.lustre-clinet-2.10.4.1/
rpm -ivh kmod-lustre-client-2.10.4-1.el7.x86_64.rpm
rpm -ivh lustre-client-2.10.4-1.el7.x86_64.rpm

#mount
mkdir /lustre
echo "options lnet networks=o2ib0(ib0)" > /etc/modprobe.d/lustre.conf
echo "192.169.30.2@o2ib:/lustre       /lustre lustre  defaults,_netdev,localflock        0 0" >> /etc/fstab
mount -a
ls /lustre
