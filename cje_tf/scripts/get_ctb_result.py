# generate checkTimeBombs html result

import os
import sys
import json
import getopt
from datetime import datetime

try:
  opts, args = getopt.getopt(sys.argv[1:],"hu:b:i:o:",["tensorflow=","branch=","input="])
except getopt.GetoptError:
  print('use -h for help')
  sys.exit(2)
for opt, arg in opts:
  if opt == '-h':
    print('for example: get_ctb_html.py -u tensorflow.url -b branch -i input.json')
    sys.exit()
  elif opt in ("-u", "--tensorflow"):
    tensorflow_repo = arg
  elif opt in ("-b", "--branch"):
    tensorflow_branch = arg
  elif opt in ("-i", "--input"):
    jsonfile = arg

current_date = datetime.now().strftime("%F")
# jsonfile = "tensorflow-check-time-bombs.json"
detailpath = "detail.html"
newpath = "new.html"

# tensorflow_repo = "https://github.com/tensorflow/tensorflow"
# tensorflow_branch = "master"

with open(newpath, 'w') as f:
    f.write("<table class=\"features-table\" style=\"width: 60%;margin: 0 auto 0 0;\"><tr><th>Scan Date</th><th>Date</th><th>File Name</th><th>Line Num</th></tr>")
with open(detailpath, 'w') as f:
    f.write("<table class=\"features-table\" style=\"width: 60%;margin: 0 auto 0 0;\"><tr><th>Scan Date</th><th>Date</th><th>File Name</th><th>Line Num</th></tr>")

if os.path.isfile(jsonfile):
    with open(jsonfile, 'r') as f:
        datastore = json.load(f)

for file in datastore:

    scan_date = file["scantime"]
    linenum = file["linenum"]
    date = file["date"]
    filename = file["filename"]
    linkurl = tensorflow_repo + "/blob/" + tensorflow_branch + "/" + filename + '#L' + linenum

    if scan_date >= current_date:
        with open(newpath, 'a+') as f:
            f.write("<tr><td>{}</td><td>{}</td><td>{}</td><td><a href={}>{}</a></td></tr>\n".format(scan_date,date,filename,linkurl,linenum))

    if scan_date <= current_date:
        with open(detailpath, 'a+') as f:
            f.write("<tr><td>{}</td><td>{}</td><td>{}</td><td><a href={}>{}</a></td></tr>\n".format(scan_date,date,filename,linkurl,linenum))

with open(newpath, 'a+') as f:
    f.write("</table>")

with open(detailpath, 'a+') as f:
    f.write("</table>")
