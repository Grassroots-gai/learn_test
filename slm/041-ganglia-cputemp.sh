#!/bin/bash

cp _usr_lib64_ganglia_python_modules_cpu_temp.py /usr/lib64/ganglia/python_modules/cpu_temp.py
cp _etc_ganglia_conf.d_cpu_temp.pyconf /etc/ganglia/conf.d/cpu_temp.pyconf

systemctl restart gmond
