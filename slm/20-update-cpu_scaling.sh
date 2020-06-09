#!/bin/bash

if ! cat /etc/rc.local | grep "cpupower"; then
    chmod +x /etc/rc.d/rc.local
    echo cpupower frequency-set -g performance >> /etc/rc.local
    source /etc/rc.local
    cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
fi

