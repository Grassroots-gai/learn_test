#!/bin/bash

# Precondition: 
#  01-preconfig.sh should be executed.
#  server must be configured.
SERVER=192.169.10.1

# nfs
echo "===== Install NFS ====="
yum install -y nfs-utils

# new pxe installation has no /home mount point.
# also there is no swap mount point.
# so we only append NFS mount point.

cat << EOF >> /etc/fstab
192.169.10.3:/home /home nfs sync,vers=4.0,auto 0 0
192.169.10.1:/opt  /opt  nfs sync,vers=4.0,auto 0 0
EOF
# replace the 2 following lines in /etc/fstab
#192.169.10.3:/home /home nfs rsize=32768,wsize=32768,async,auto 0 0
#192.169.10.1:/opt  /opt  nfs rsize=32768,wsize=32768,async,auto 0 0

umount /home
mount -a


# ntp
echo "===== Install NTP ====="
yum -y install ntp

mv _etc_ntp.conf /etc/ntp.conf

systemctl stop chronyd
systemctl disable chronyd
systemctl enable ntpd
systemctl start ntpd
ntpq -p
#date +%Y%m%d -s "$(ssh $SERVER date +%Y%m%d)"
#date +%T -s "$(ssh $SERVER date +%T)"
#hwclock -w


# nis
echo "===== Install NIS ====="
yum -y install ypbind rpcbind

ypdomainname mlt-network1
echo "NISDOMAIN=mlt-network1" >> /etc/sysconfig/network

authconfig --enablenis --nisdomain=mlt-network1 --nisserver=192.169.10.1 --update

systemctl start rpcbind ypbind 
systemctl enable rpcbind ypbind 

# numa
echo "===== Install numactl ====="
yum -y install numactl

# sep
echo "===== Install SEP Driver ====="
cp _etc_rc.d_init.d_sep4_1 /etc/rc.d/init.d/sep4_1
#cp -P _etc_rc.d_S99sep4_1 /etc/rc.d/rc2.d/S99sep4_1
#cp -P _etc_rc.d_S99sep4_1 /etc/rc.d/rc3.d/S99sep4_1
#cp -P _etc_rc.d_S99sep4_1 /etc/rc.d/rc4.d/S99sep4_1
#cp -P _etc_rc.d_S99sep4_1 /etc/rc.d/rc5.d/S99sep4_1


# ganglia
#echo "===== Install Ganglia ====="
#yum install -y ganglia-gmond ganglia-gmond-python

#cp _etc_ganglia_gmond.conf /etc/ganglia/gmond.conf

#systemctl start gmond
#systemctl enable gmond

