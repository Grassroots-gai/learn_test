#/bin/bash
#set -x
echo Process id is $$
testdir=`pwd`
scriptsloc=/mnt/aipg_tensorflow_shared/validation/scripts
cd "$testdir"
mkdir -p runlogs
secsinday=`expr 60 "*" 60 "*" 24`
#echo $secsinday
today=`date "+%b %d %Y"`
runtoday="$today 6:00:00 PM"
#echo $timetoruntoday 
timetoruntoday=`date -d "$runtoday" +%s`
now=`date +%s`
timetonextrun=`expr $timetoruntoday - $now`
#echo $timetonextrun
[ $timetonextrun -le 0 ] && timetonextrun=`expr $secsinday + $timetonextrun`
#echo $timetonextrun
nexrun=`expr $now + $timetonextrun`
echo next run will be at ` date -d @$nexrun`
echo "+++++++++++++++++++++++++++++++++++++++++++++++++++" 
while true
do
  sleep $timetonextrun
  logifle=runlogs/`date +%Y_%m_%d`.log 
  echo -n Starting job: `date` " "
  start=`date +%s`
  $scriptsloc/update-repos.sh > $logifle 2>&1
  end=`date +%s` 
  echo End job: `date`
  timetonextrun=`expr $secsinday - $end + $start`
  #echo $timetonextrun
done
