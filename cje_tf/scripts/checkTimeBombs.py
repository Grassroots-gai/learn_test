#!/usr/bin/python
import json
import sys
#import commands
import getopt
import os
import datetime

#this scipts is to scan the source to get if we have new time bombs

datefile = "/tensorflow/python/compat/compat.py"
pyversion = sys.version_info.major
today = datetime.date.today()
yesterday = today + datetime.timedelta(-1)
tomorrow = today + datetime.timedelta(1)
input_file = "NA"
output_file = str(today) + ".json"
src = "NA"
diff_file = "diff.xml"



try:
  opts, args = getopt.getopt(sys.argv[1:],"hi:o:s:d:",["ifile=","ofile=","src=","dfile="])
except getopt.GetoptError:
  print('use -h for help')
  sys.exit(2)
for opt, arg in opts:
  if opt == '-h':
    print('this tool is to check time bombs in tensorflow code')
    print(' ')
    print('python checkTimeBombs.py -i <inputfile> -o <outputfile> -s <src> -d <difffile>')
    print(' ')
    print('for example: checkTimeBombs.py -i input.log -o output.log -s ./tensorflow -d diff.xml')
    print('input:  the input file is the previous log you want to compare to see if new bombs detected')
    print('output: the output file is the log file generated in this scan')
    print('src:    the src is the location of tensorflow src you want to scan')
    print('difffile: the diff file is the difference between input file and output file, the default is diff.xml')
    sys.exit()
  elif opt in ("-i", "--ifile"):
    input_file = arg
  elif opt in ("-o", "--ofile"):
    output_file = arg
  elif opt in ("-s", "--src"):
    src = arg
  elif opt in ("-d", "--dfile"):
    diff_file = arg

print('input_file: {0}'.format(input_file))
print('output_file: {0}'.format(output_file))
print('src: {0}'.format(src))
print('diff_file: {0}'.format(diff_file))

if src == "NA":
  print("no src location provided, please see -h")
  exit(3)


#function to get indetation of given string
def getIndent(line_str):
  line = line_str.replace("\t", "  ")
  return len(line)-len(line.lstrip())

#grep to get which file has called the compat.forward_compatible
command="grep \"compat.forward_compatible(\" -nR  {}".format(src)
if pyversion == 3:
  import subprocess
  status, outputs = subprocess.getstatusoutput(command)
else:
  import commands
  status, outputs = commands.getstatusoutput(command)
if status != 0:
  print("the grep command seems to be error, the command is: {0}".format(command))
  exit(2)
output = outputs.split("\n")


#begin to get needed information in each file that has call the compat.forward_compatible
instances = []
for i in range(len(output)):
  contents = output[i].split(":")
  if len(contents) <3:
    continue
  file_name = contents[0]
  #filter the compat.py and compat_test.py
  if "compat.py" in file_name or "compat_test.py" in file_name:
    continue
  line_num = contents[1]
  content = contents[2].strip()
  date = content[content.find("(")+1:content.find(")")].replace(", ", "-").replace("year=", "").replace("month=", "").replace("day=", "")
  instance = {}
  instance["filename_path"] = src
  instance["filename"] = file_name.replace(src+"/", "").replace(src, "")
  instance["linenum"] = line_num
  instance["date"] = date 
  with open(file_name) as x:
    reverse_lines = [line.rstrip('\n') for line in x.readlines()[0:int(line_num)]]
  find_func = 0
  find_class = 0
  function_name = "NA"
  class_name = "NA"
  for line in reverse_lines[::-1]:
    if find_func == 0 and "def " in line:
      function_name = line.strip()
      find_func = 1
    elif find_class == 0 and "class " in line and line.find("class") == 0:
      class_name = line.strip()
      find_class = 1
    if find_func == 1 and find_class == 1:
      break 
  with open(file_name) as l:
    lines = [line.rstrip('\n') for line in l.readlines()[int(line_num)-1:]]
  indent = getIndent(lines[0])
  block = [lines[0].strip()]
  for line in lines[1::]:
    if (getIndent(line) == indent and "else" not in line and "elif" not in line) or getIndent(line) < indent:
      break
    else:
      block.append(line.strip())
  instance["block"] = block  
  instance["funcname"] = function_name
  instance["classname"] = class_name 
  instance["scantime"] = str(today)
  instances.append(instance)

#if give the reference file, we will check if new items found, otherwise we will just generate output file without compare
with open(diff_file, 'w') as f:  
  f.write("<table><tr><td value=\"Scan Date\"/><td value=\"Date\"/><td value=\"File Name\"/><td value=\"Line Num\"/></tr>")
new_num = 0
if input_file == "NA":
  print("no refernece file provided, will just generate the output file")
else:  
  with open(input_file) as i:
    ref = json.load(i)
  for n in range(len(instances)):
    new = instances[n]
    find_tag = 0
    for o in range(len(ref)):
      old = ref[o]
      if new["filename"] != old["filename"] or new["date"] != old["date"] or new["funcname"] != old["funcname"] or new["classname"] != old["classname"]:
        continue
      else:
        if pyversion == 3:
          import operator
          if operator.eq(new["block"],old["block"]):
            find_tag = 1 
            new["scantime"] = old["scantime"]
            break
        else:
          if cmp(new["block"],old["block"]) == 0:
            find_tag = 1
            new["scantime"] = old["scantime"]
            break 
    if find_tag != 1:
      print("new item found")
      linkurl = "https://github.com/tensorflow/tensorflow/blob/master/" + new["filename"] + "#L" + new["linenum"]            
      with open(diff_file, "a+") as f:
        f.write("<tr><td value=\"{}\"/><td value=\"{}\"/><td value=\"{}\"/><td value=\"{}\" href=\"{}\"/></tr>\n".format(new["scantime"],new["date"],new["filename"],new["linenum"],linkurl))
      new_num -= 1

  with open(diff_file, 'a+') as f:
    f.write("</table>")

#save to output 
json_file = json.dumps(instances)

with open(output_file, "w") as fout:
  fout.write(json_file)


#check compat.py to see if the date in this file is today, tomorrow or yesterday, will return 1 if not meet.
with open(src + datefile, "r") as df:
  lines = [line.rstrip('\n') for line in df.readlines()]
for line in lines:
  if "_FORWARD_COMPATIBILITY_HORIZON =" in line and getIndent(line) == 0:
    year, month, day = line[line.find("(")+1:line.find(")")].split(", ")
    if int(month) < 10:
      monthstr = "0" + str(month)
    else:
      monthstr = str(month)
    if int(day) < 10:
      daystr = "0" + str(day)
    else:
      daystr = str(day)
    timestr = str(year) + "-" + monthstr + "-" + daystr
    if timestr != str(yesterday) and timestr != str(tomorrow) and timestr != str(today):
      print("Abnormal_FORWARD_COMPATIBILITY_HORIZON!")
      exit(1)
     

exit(new_num)




