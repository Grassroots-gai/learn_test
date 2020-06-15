#!/bin/bash
set -x

if [ $# != "2" ]; then
    echo 'ERROR:'
    echo "Expected 2 parameter got $#"
    printf 'Please use following parameters:
    --runLog=<name of the build log file>
    --runType=<type of run mklml/mkldnn/eigen>
    '
    exit 1
fi

PATTERN='[-a-zA-Z0-9_]*='
for i in "$@"
do
    case $i in
        --runLog=*)
            runLog=`echo $i | sed "s/${PATTERN}//"`;;
        --runType=*)
            runType=`echo $i | sed "s/${PATTERN}//"`;;
        *)
            echo "Parameter $i not recognized."; exit 1;;
    esac
done

if [ "$runType" == "mklml" ]; then
    fileToUpdate="/mnt/aipg_tensorflow_shared/validation/tf_master_validation/ml.failures"
elif [ "$runType" == "mkldnn" ]; then
    fileToUpdate="/mnt/aipg_tensorflow_shared/validation/tf_master_validation/dnn.failures"
else
    fileToUpdate="/mnt/aipg_tensorflow_shared/validation/tf_master_validation/eigen.failures"
fi
  
grep "were skipped"  $runLog > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "update file: $fileToUpdate"
    fgrep FAILED $runLog  | sed 's/[ ][ ]*.*//' > $fileToUpdate 
fi

