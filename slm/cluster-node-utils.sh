#!/bin/bash

function stop_service() {
    echo ""
    # slurm 
    systemctl stop slurmd
    # nis
    systemctl stop ypbind
    # nfs
    systemctl stop 
}
