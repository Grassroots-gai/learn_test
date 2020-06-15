#!/usr/bin/python
import subprocess as sp
import sys
import os
import shutil

# runType: mkldnn/mklml/eigen/eigen_<customized>
runType = str(sys.argv[1])

# runJob: nightly/pr
if ( len(sys.argv) == 2):
    # if not specify, default to pr job
    runJob = "pr"
else :
    runJob = str(sys.argv[2])

print runType
print runJob

#if ( runType == "mkldnn" ):
#  failureFile = "/mnt/aipg_tensorflow_shared/validation/logs/dnn.failures"
#elif ( runType == "mklml" ):
#  failureFile = "/mnt/aipg_tensorflow_shared/validation/logs/ml.failures"
#else:
#  failureFile = "/mnt/aipg_tensorflow_shared/validation/logs/eigen.failures"
# 12/26/2017 always compare to eigen failure doesn't matter if it's mklml or mkldnn
if ( runJob == "nightly" ):
    # 01/09/2018 if it's nightly run, always compare with eigen.failures
    failureFiles = ["/mnt/aipg_tensorflow_shared/validation/logs/eigen.failures"]
    print failureFiles 
    
else:
    # PR job needs to compare with ml.failures or dnn.failures
    if ( runType == "mkldnn" ):
      failureFiles = ["/mnt/aipg_tensorflow_shared/validation/logs/eigen.failures", "/mnt/aipg_tensorflow_shared/validation/logs/dnn.failures"]
      print failureFiles
    elif ( runType == "mklml" ):
      failureFiles = ["/mnt/aipg_tensorflow_shared/validation/logs/eigen.failures", "/mnt/aipg_tensorflow_shared/validation/logs/ml.failures"]
      print failureFiles
    elif (runType.startswith("eigen_") and runType.endswith(".failures")):
      filename="/mnt/aipg_tensorflow_shared/validation/logs/" + runType
      failureFiles = [filename]
      print failureFiles

def deleteKnownFailures():
  #try: sp.check_output("wget http://mlt-bdw0.sc.intel.com/tf_nightly_results/eigen.failures > /dev/null 2>&1", shell=True)
  #try: sp.check_output("/opt/tensorflow/validation/eigen.failures > /dev/null 2>&1", shell=True)
  #except sp.CalledProcessError: sys.exit("Unable to fetch known failures from /opt/tensorflow/validation.")
  #with open('nightly_failed.txt', 'r') as f:
  for failureFile in failureFiles:
    print failureFile
    with open(failureFile, 'r') as f:
      for line in f:
        if "//" in line:  line = line.split("//")[1]
        if ":" in line: 
          line = line.split(":")
          line = "./" + line[0] + "/" + line[1].rstrip() + "/"
          if os.path.exists(line): shutil.rmtree(line)
       

def main():
  deleteKnownFailures()


if __name__ == "__main__":
  main()
