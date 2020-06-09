#!/bin/bash

#scp root@192.169.10.1:/root/scripts/_RHEL_7.5_lustre_client.tgz ./
#install the lustre client for CentOS 7.5
pushd CLUSTER_BUILD/LUSTRE_UBUNTU_1804
dpkg -i lustre-client-modules-4.15.0-45-generic_2.12.2-1_amd64.deb
dpkg -i lustre-iokit_2.12.2-1_amd64.deb
dpkg -i lustre-client-utils_2.12.2-1_amd64.deb
apt --fix-broken install -y
popd

#mount
IPSUBNET="192.169.20"
linkname=$(for linkname in $(echo $(ip link show | grep "^[0-9]" | cut -d':' -f 2)); do ip address show $linkname | grep -q "${IPSUBNET}" && echo $linkname; done)
mkdir /lustre
echo "options lnet networks=tcp($(echo $linkname))" > /etc/modprobe.d/lustre.conf
echo "192.169.20.2@tcp:/lustrefs     /lustre lustre  defaults,_netdev,localflock        0 0" >> /etc/fstab
mount -a
ls /lustre/dataset
