#!/bin/bash
if [[ `hostname` ==  mlt2-clx0* ]];then
        IP=`awk -F "clx" '{print $NF}' /etc/hostname | sed 's/^0//'`
else
        IP=`awk -F "clx" '{print $NF}' /etc/hostname`
fi
# 1G
if ! ip addr | grep "192.169.10.${IP}/24"; then
	interface="eno2"
	echo "===== Config ${interface} ====="
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
	interface="enp0s20f0u5"
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
	ifdown ${interface}
	ifup ${interface}
fi
