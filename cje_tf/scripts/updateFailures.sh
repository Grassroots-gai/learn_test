# Update failures files for Unit Tests
# Parameters: buildLog (optional), runLog, runType
# If runType is mklml, update ml.failures
# If runType is mkldnn, update dnn.failures
# Oterwise, update dnn.failures
# This update should only happen for nightly jobs, and the failures files are
# under /mnt/aipg_tensorflow_shared/validation/logs

#!/bin/bash
set -x

buildLog="dummy"
runLog="dummy"
runType="mkldnn"


if [ $# -lt "2" ]; then
    echo 'ERROR:'
    echo "Expected 2 or 3 parameter got $#"
    printf 'Please use following parameters:
    --buildLog=<name of the build log file> (optional)
    --runLog=<name of the test log file>
    --runType=<type of run mklml/mkldnn/eigen>
    '
    exit 1
fi

PATTERN='[-a-zA-Z0-9_]*='

for i in "$@"
do
    case $i in
        --buildLog=*)
            buildLog=`echo $i | sed "s/${PATTERN}//"`;;
        --runLog=*)
            runLog=`echo $i | sed "s/${PATTERN}//"`;;
        --runType=*)
            runType=`echo $i | sed "s/${PATTERN}//"`;;
        *)
            echo "Parameter $i not recognized."; exit 1;;
    esac
done

if [ "$runType" == "mklml" ]; then
  fileToUpdate="/mnt/aipg_tensorflow_shared/validation/logs/ml.failures"
elif [ "$runType" == "mkldnn" ]; then
  fileToUpdate="/mnt/aipg_tensorflow_shared/validation/logs/dnn.failures"
else
  fileToUpdate="/mnt/aipg_tensorflow_shared/validation/logs/eigen.failures"
fi


if [ "$buildLog" != "dummy" ]; then
  grep "were skipped"  $buildLog > /dev/null 2>&1
  if [ $? -ne 0 ]; then
    grep "were skipped"  $runLog > /dev/null 2>&1
    if [ $? -ne 0 ]; then
      echo "update file: $fileToUpdate"
      #fgrep FAILED $runLog  | sed 's/[ ][ ]*.*//' > file.tmp 
      fgrep FAILED $runLog  | sed 's/[ ][ ]*.*//' > $fileToUpdate 
    fi
  fi
else
  grep "were skipped"  $runLog > /dev/null 2>&1
  if [ $? -ne 0 ]; then
    echo "update file: $fileToUpdate"
    #fgrep FAILED $runLog  | sed 's/[ ][ ]*.*//' > file.tmp 
    fgrep FAILED $runLog  | sed 's/[ ][ ]*.*//' > $fileToUpdate 
  fi
fi

