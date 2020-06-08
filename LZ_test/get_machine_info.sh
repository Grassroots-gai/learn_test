#!/bin/bash

if [[ -f /usr/bin/lsb_release ]]; then
    OS=$(cat /etc/issue |awk -F ' ' '{printf $1" "} {printf $2}')
else
    OS=$(cat /etc/issue |sed -n '1p' | awk -F ' ' '{printf $1" "$2}' )
fi
echo -e "OS:${OS}"

OS_version=$(uname -m)
echo -e "OS_version:${OS_version}"

kernel_version=$(uname -r)
echo -e "Kernel_version:${kernel_version}"

CPU=$(grep 'model name' /proc/cpuinfo |uniq |awk -F : '{print $2}' |sed 's/^[ \t]*//g' |sed 's/ \+/ /g')
echo -e "CPU model:${CPU}"

CPU_Freq=$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor)
echo -e "CPU freq:${CPU_Freq}"

Counts=$(grep 'physical id' /proc/cpuinfo |sort |uniq |wc -l)
echo -e "Total of physical CPU:${Counts}"

Cores=$(grep 'cpu cores' /proc/cpuinfo |uniq |awk -F : '{print $2}' |sed 's/^[ \t]*//g')
echo -e "Number of CPU cores:${Cores}"

PROCESSOR=$(grep 'processor' /proc/cpuinfo |sort |uniq |wc -l)
echo -e "Number of logical CPUs:${PROCESSOR}"

Mode=$(getconf LONG_BIT)
echo -e "Present Mode Of CPU:${Mode}"

Total=$(cat /proc/meminfo |grep 'MemTotal' |awk -F : '{print $2}' |sed 's/^[ \t]*//g')
echo -e "Total Memory:${Total}"
