#!/bin/bash
set -x

p_version=$(python -V 2>&1)
case "$p_version" in
    *2.7*)
        PIP="pip"
        ;;
    *3.5*)
        PIP="python3.5 -m pip"
        ;;
    *3.6*)
        PIP="python3.6 -m pip"
        ;;
    *)
        echo "Error: unknown"
        ;;
esac
echo $PIP

${PIP} install --upgrade tf-estimator-nightly
${PIP} install future>=0.17.1
