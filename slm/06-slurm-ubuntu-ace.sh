#!/bin/bash

# yum install -y munge
# munge
echo " ===== Installation of MUNGE ====="
apt install -y munge libmunge-dev libmunge2
#chown -R root:root /etc/munge /var/{lib,log,run}/munge
#chmod -Rf 700 /etc/munge
#chmod -Rf 711 /var/lib/munge
#chmod -Rf 700 /var/log/munge
#chmod -Rf 0755 /var/run/munge
echo " Start MUNGED "
cp _etc_munge_munge.key /etc/munge/munge.key
systemctl restart munge
systemctl enable munge

# slurm
apt install -y mysql-server
#yum install -y readline-devel perl-Switch perl-ExtUtils-MakeMaker pam-devel

# Because in ubuntu service will auto start after installation. Stop it before
# we change configuration files
apt install -y slurm-wlm
apt install -y libpam-slurm libslurm-dev libslurm-perl
systemctl status slurmd
systemctl stop slurmd
systemctl stop slurmctld
systemctl disable slurmctld

ps auxf | grep -i slurm

#mkdir -p /var/spool/slurmd
ls /var/spool/slurmd || mkdir -p /var/spool/slurm/
mkdir -p /etc/slurm-llnl
ls -l /etc | grep slurm
# change own of slurm config
chown -R root:root /etc/slurm-llnl
ls -l /etc/slurm-llnl


cp _etc_slurm_slurm.conf.ubuntu /etc/slurm-llnl/slurm.conf
cp _etc_slurm_slurm.epilog.clean.ubuntu /etc/slurm-llnl/slurm.epilog.clean

cat /lib/systemd/system/slurmd.service

# 
sed -i "s/SlurmdPidFile=.*/SlurmdPidFile=\/var\/run\/slurmd.pid/" /etc/slurm-llnl/slurm.conf
sed -i "s/^PIDFile=.*/PIDFile=\/var\/run\/slurmd\.pid/" /etc/systemd/system/multi-user.target.wants/slurmd.service
systemctl daemon-reload

systemctl start slurmd
systemctl enable slurmd


# PAM: disable login
cat << EOF >> /etc/pam.d/slurm
account  required  pam_unix.so
account  required  pam_slurm.so
auth     required  pam_localuser.so
session  required  pam_limits.so
EOF

sed -i 's/nis //' /etc/pam.d/system-auth

echo "account    required     pam_slurm.so" >> /etc/pam.d/sshd

