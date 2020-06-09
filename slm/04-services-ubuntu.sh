#!/bin/bash

# Precondition: 
#  01-preconfig.sh should be executed.
#  server must be configured.
SERVER=192.169.10.1

# firewall
systemctl stop ufw
systemctl disable ufw


# nfs
echo "===== Install NFS ====="
echo "Remove local user, and their home dir."
if ! mount | grep -q 192.*home; then
  mv /home /home_old
  mkdir -p /home
  sed -i "s/home/home_old/" /etc/passwd
else
  echo Home already mounted.
fi

# For new installed server, just enumarate the first 5 users.
#for i in 1000 1001 1002 1003 1004;
#do
#    getent passwd $i;
#    if [ "$?" == "0" ]; then
#        deluser --remove-home $(getent passwd $i | cut -d':' -f1)
#    fi
#done

apt update

apt install -y nfs-common

# pxe installation is not prepared completely.
# swap mount point needs remove.
# and append NFS mount point.

sed -i "/swap/d" /etc/fstab

cat << EOF >> /etc/fstab
192.169.10.3:/home /home nfs sync,vers=4.0,auto 0 0
192.169.10.1:/opt  /opt  nfs sync,vers=4.0,auto 0 0
EOF

umount /home || true
mount -a


# ntp
echo "===== Install NTP ====="
apt install -y ntpdate
ntpdate mlt-blue
timedatectl set-ntp off
apt install -y ntp

systemctl stop ntp
cat << EOF_NTP >> /etc/ntp.conf
server mlt-blue prefer iburst
EOF_NTP

# Zone correcting
rm -f /etc/localtime
ln -s /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

systemctl restart ntp

ntpq -p
date +%Y%m%d -s "$(ssh -i ~/.ssh/id_rsa $SERVER date +%Y%m%d)"
date +%T -s "$(ssh -i ~/.ssh/id_rsa $SERVER date +%T)"
hwclock -w


## nis
echo "===== Install NIS ====="
#yum -y install ypbind rpcbind
apt install nis

systemctl stop ypbind # ypbind must stop to config
ypdomainname mlt-network1
echo "mlt-network1" > /etc/defaultdomain
cp _etc_nsswitch.conf /etc/nsswitch.conf

echo "domain mlt-network1 server 192.169.10.1" \
    >> /etc/yp.conf
echo "session optional pam_mkhomedir.so skel=/etc/skel umask=077" \
    >> /etc/pam.d/common-session

systemctl start rpcbind nis
systemctl enable rpcbind nis

# numa
echo "===== Install numactl ====="
apt install -y numactl

# sep
echo "===== Install SEP Driver ====="
cp _etc_rc.d_init.d_sep4_1 /etc/init.d/sep4_1
update-rc.d sep4_1 defaults
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
