#!/bin/bash

IP=`cat /etc/hostname | tr -cd "[0-9]" | sed 's/^0*//'`


# 1G
if ! ip addr | grep "192.169.10.${IP}/24"; then
	#interface="enp4s0"
	#interface="enp61s0f1" #skx
	interface="eno2" #skx2
	#interface="enp4s0" #knm
	echo "===== Config ${interface} ====="

#	sed -i 's/BOOTPROTO/#BOOTPROTO/' /etc/sysconfig/network-scripts/ifcfg-${interface}

#cat << EOF >> /etc/sysconfig/network-scripts/ifcfg-${interface}
#
#BOOTPROTO=none
#IPADDR=192.169.10.${IP}
#NETMASK=255.255.255.0
#EOF

cat << EOF > /etc/sysconfig/network-scripts/ifcfg-${interface}
NAME=${interface}
DEVICE=${interface}
ONBOOT=yes
NETBOOT=yes
IPV6INIT=yes
TYPE=Ethernet
BOOTPROTO=none
IPADDR=192.169.10.${IP}
NETMASK=255.255.255.0
EOF

	ifdown ${interface}
	ifup ${interface}
fi


# 10G
if ! ip addr | grep "192.169.20.${IP}/24"; then
	#interface="eno3"
	interface="enp134s0f0" #skx
	#interface="enp4s0" #knm
	echo "===== Config ${interface} ====="

cat << EOF > /etc/sysconfig/network-scripts/ifcfg-${interface}
NAME=${interface}
DEVICE=${interface}
ONBOOT="yes"
NETBOOT=yes
IPV6INIT=yes
TYPE=Ethernet
BOOTPROTO=none
IPADDR=192.169.20.${IP}
NETMASK=255.255.255.0
EOF
#	sed -i 's/BOOTPROTO/#BOOTPROTO/' /etc/sysconfig/network-scripts/ifcfg-${interface}
#	sed -i 's/ONBOOT/#ONBOOT/' /etc/sysconfig/network-scripts/ifcfg-${interface}

#cat << EOF >> /etc/sysconfig/network-scripts/ifcfg-${interface}

#BOOTPROTO=none
#IPADDR=192.169.20.${IP}
#NETMASK=255.255.255.0
#ONBOOT="yes"
#EOF


	ifdown ${interface}
	ifup ${interface}
fi



# OPA
if lspci | grep -i omni-path > /dev/null ; then
	echo "Install OPA Driver"	

	yum install -y libibmad libibverbs librdmacm libibcm perftest rdma infinipath-psm expat elfutils-libelf-devel libstdc++-devel gcc-gfortran atlas tcl expect tcsh sysfsutils pciutils opensm-devel opensm-libs bc libhfi1 libhfi1-static libibumad-devel libuuid-devel papi libfabric-devel openssl-devel

	#tar -C /tmp/ -xf _IntelOPA-Basic.RHEL73-x86_64.10.3.1.0.22.tgz
	#pushd /tmp/IntelOPA-Basic.RHEL73-x86_64.10.3.1.0.22
	#tar -C /tmp/ -xf _IntelOPA-Basic.RHEL73-x86_64.10.4.2.0.7.tgz
	#pushd /tmp/IntelOPA-Basic.RHEL73-x86_64.10.4.2.0.7
        tar -C /tmp/ -xf _IntelOPA-Basic.RHEL75-x86_64.10.8.0.0.204.tgz
        pushd /tmp/IntelOPA-Basic.RHEL75-x86_64.10.8.0.0.204
	./INSTALL -n
	popd
	#rm -rf /tmp/IntelOPA-Basic.RHEL73-x86_64.10.3.1.0.22
	rm -rf /tmp/IntelOPA-Basic.RHEL75-x86_64.10.8.0.0.204

cat << EOF >> /etc/sysconfig/network-scripts/ifcfg-ib0
DEVICE=ib0
BOOTPROTO=static
IPADDR=192.169.30.${IP}
BROADCAST=192.169.30.255
NETWORK=192.169.30.0
NETMASK=255.255.255.0
ONBOOT=yes
CONNECTED_MODE=yes
MTU=65520
NM_CONTROLLED=no
EOF

	#tar -C /tmp -xf _OFI-for-IntelOPA-RHEL7_3-x86_64.10.4.2.0.7-110.tar.gz
	#yum install -y /tmp/OFI-for-IntelOPA-RHEL7_3-x86_64.10.4.2.0.7-110/*
	#rm -rf /tmp/OFI-for-IntelOPA-RHEL7_3-x86_64.10.4.2.0.7-110/
	
fi
