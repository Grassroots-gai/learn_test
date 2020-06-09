#!/bin/bash

yum install -y _syscfg/syscfg-V14.0-B15.i386.rpm

/usr/bin/syscfg/syscfg -s /tmp/syscfg.ini

if grep Phi /proc/cpuinfo > /dev/null ; then
	grep "Snoop Response Hold Off" /tmp/syscfg.ini
	grep "Boot performance mode" /tmp/syscfg.ini
	grep "Turbo Mode" /tmp/syscfg.ini
	grep "Package C State limit" /tmp/syscfg.ini
else
	grep "Hyper-Threading Tech" /tmp/syscfg.ini
fi
