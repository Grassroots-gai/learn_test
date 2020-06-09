#!/bin/bash
set -x

# uninstall opa driver
tar xf _IntelOPA-Basic.RHEL74-x86_64.10.6.0.0.134.tgz
pushd IntelOPA-Basic.RHEL74-x86_64.10.6.0.0.134 > /dev/null
./INSTALL -u
popd

# update yum repo
cp _etc_yum.repos.d_CentOS-Base.repo /etc/yum.repos.d/CentOS-Base.repo
cp _etc_yum.repos.d_epel.repo /etc/yum.repos.d/epel.repo

sed -i 's/^proxy=.*//' /etc/yum.conf

# upgrade system
yum update -y

# update fstab
sed -i 's?192\.168\.10\.2?192\.168\.10\.1?' /etc/fstab

# update sep driver
cp _etc_rc.d_init.d_sep4_1 /etc/rc.d/init.d/sep4_1
cp -P _etc_rc.d_S99sep4_1 /etc/rc.d/rc2.d/S99sep4_1
cp -P _etc_rc.d_S99sep4_1 /etc/rc.d/rc3.d/S99sep4_1
cp -P _etc_rc.d_S99sep4_1 /etc/rc.d/rc4.d/S99sep4_1
cp -P _etc_rc.d_S99sep4_1 /etc/rc.d/rc5.d/S99sep4_1

#reboot
