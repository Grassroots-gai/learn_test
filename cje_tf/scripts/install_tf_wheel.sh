#!/bin/bash
set -x

p_version=$(python -V 2>&1)
case "$p_version" in
    *2.7*)
        PIP="pip"
        ;;
    *3.4*)
        PIP="python3.4 -m pip"
        ;;
    *3.5*)
        PIP="python3.5 -m pip"
        ;;
    *3.6*)
        PIP="python3.6 -m pip"
        ;;
    *3.7*)
        PIP="python3.7 -m pip"
        ;;
    *)
        echo "Error: unknown"
        ;;
esac
echo $PIP

wheel=$(ls /workspace/build/*.whl)
${PIP} install --upgrade $wheel 
apt-get clean; apt-get update -y; apt-get install numactl -y || yum clean all; yum update -y; yum install numactl -y
