#!/bin/bash

/opt/intel/vtune_amplifier_xe/sepdk/src/rmmod-sep
/opt/intel/vtune_amplifier_xe/sepdk/src/insmod-sep

cp _etc_rc.d_init.d_sep4_1 /etc/rc.d/init.d/sep4_1
