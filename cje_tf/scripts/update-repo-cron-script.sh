#!/bin/bash
#set -x
#echo Process id is $$
#testdir=`pwd`
scriptsloc=/mnt/aipg_tensorflow_shared/validation/scripts
#cd "$testdir"
#mkdir -p runlogs
#secsinday=`expr 60 "*" 60 "*" 24`
##echo $secsinday
#today=`date "+%b %d %Y"`
#runtoday="$today 6:00:00 PM"
##echo $timetoruntoday 
#timetoruntoday=`date -d "$runtoday" +%s`
#now=`date +%s`
#timetonextrun=`expr $timetoruntoday - $now`
##echo $timetonextrun
#[ $timetonextrun -le 0 ] && timetonextrun=`expr $secsinday + $timetonextrun`
#echo $timetonextrun
#nexrun=`expr $now + $timetonextrun`
#echo next run will be at ` date -d @$nexrun`
#echo "+++++++++++++++++++++++++++++++++++++++++++++++++++" 
#while true
#do
#  sleep $timetonextrun
eval $(ssh-agent -s)
ssh-add ~/.ssh/id_rsa
  logfile="${scriptsloc}/runlogs/$(date +%Y_%m_%d).log"
  echo "Starting job: $(date)"
  echo "Logfile=$logfile"
  ${scriptsloc}/update-repos.sh 2>&1 | tee ${logfile}
  echo End job: $(date)

#  timetonextrun=`expr $secsinday - $end + $start`
#  #echo $timetonextrun
#done
exit 0
