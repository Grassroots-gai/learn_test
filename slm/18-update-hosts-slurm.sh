#!/bin/bash

#bash 00-rsync.sh
bash 15-update-hosts.sh
cat /etc/hosts | grep mlt-clx199
bash 16-update-slurm.sh
cat /etc/slurm/slurm.conf | grep mlt-clx
systemctl restart slurmd

