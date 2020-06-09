#!/bin/bash

# yum install -y munge
# munge
echo " ===== Installation of MUNGE ====="
yum install -y bzip2-devel openssl-devel zlib-devel
pushd CLUSTER_BUILD/MUNGE/RPMS >& /dev/null
  rpm -ivh munge*
popd >& /dev/null
#chown -R root:root /etc/munge /var/{lib,log,run}/munge
chmod -Rf 700 /etc/munge
chmod -Rf 711 /var/lib/munge
chmod -Rf 700 /var/log/munge
chmod -Rf 0755 /var/run/munge
echo " Start MUNGED "
cp _etc_munge_munge.key /etc/munge/munge.key
systemctl start munge
systemctl enable munge

# slurm
yum install -y readline-devel perl-Switch perl-ExtUtils-MakeMaker pam-devel
pushd CLUSTER_BUILD/SLURM/RPMS/ >& /dev/null
  yum install -y mysql
  yum install -y mysql-devel
  yum localinstall -y ~/scripts/CLUSTER_BUILD/MYSQL/RPMS/mysql57-community-release-el7-11.noarch.rpm
  yum install -y mysql-community-server
  rpm -ivh slurm-18.08.4-1.el7.x86_64.rpm
  rpm -ivh slurm-perlapi*
  rpm -ivh slurm-contrib*
  rpm -ivh slurm-devel*
  rpm -ivh slurm-libpmi*
  rpm -ivh slurm-openlava*
  rpm -ivh slurm-pam_slurm*
  rpm -ivh slurm-slurmd*
  rpm -ivh slurm-torque*
popd >& /dev/null

mkdir -p /var/spool/slurm
mkdir -p /etc/slurm

cp _etc_slurm_slurm.conf /etc/slurm/slurm.conf
cp _etc_slurm_slurm.epilog.clean /etc/slurm/slurm.epilog.clean

# disable login
cat << EOF >> /etc/pam.d/slurm
account  required  pam_unix.so
account  required  pam_slurm.so
auth     required  pam_localuser.so
session  required  pam_limits.so
EOF

sed -i 's/nis //' /etc/pam.d/system-auth

echo "account    required     pam_slurm.so" >> /etc/pam.d/sshd

systemctl start slurmd
systemctl enable slurmd

