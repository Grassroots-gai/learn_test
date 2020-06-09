#!/bin/bash

IP=`cat /etc/hostname | tr -cd "[0-9]" | sed 's/^0*//'`

cat << EOF > /etc/sysconfig/network-scripts/ifcfg-enp0s20f0u5
NAME=enp0s20f0u5
DEVICE=enp0s20f0u5
ONBOOT=yes
NETBOOT=yes
BOOTPROTO=dhcp
TYPE=Ethernet
EOF

ifdown enp0s20f0u5
ifup enp0s20f0u5
