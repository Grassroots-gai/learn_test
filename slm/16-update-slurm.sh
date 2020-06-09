#!/bin/bash

cp /root/scripts/_etc_slurm_slurm_node.conf /etc/slurm/slurm.conf
systemctl restart slurmd

