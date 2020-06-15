#!/usr/bin/python
import sys
import os
import re

# by default log file is unit_tests.log unless otherwise passed in
total = len(sys.argv)
if (total == 1):
  logfile = "unit_tests.log"
else:
  logfile = str(sys.argv[1])

def checkTestLog():
  error = ""
  foundError = False
  #with open('unit_tests.log', 'r') as f:
  with open(logfile, 'r') as f:
    for line in f:
      if "ERROR:" in line and ("not created" in line or "not all outputs were created" in line):  continue
      if "ERROR: Couldn't start the build." in line: break
      if foundError and ("FAIL:" in line or "Building complete" in line): break
      if "ERROR:" in line:
        foundError = True
        temp = re.findall(r"'(.*?)'", line)[0]
        if '/tensorflow/' in temp:  temp = temp.split('/tensorflow/')[1] # display
      if foundError:  error += line
  if not error:
    return sys.exit(0)
  print "==============================================================="
  print temp, "unit test failed to build."
  print "==============================================================="
  #print error.rstrip()
  return sys.exit(1)

def main():
  checkTestLog()

if __name__ == "__main__":
  main()
