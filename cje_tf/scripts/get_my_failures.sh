#set -x
if [ $# -lt 1 ] 
  then
  echo "Usage : $0 <log file> [all/ml/dnn/eigen]"
  exit 1
fi
if ! [ -e  $1 ] 
  then
  echo "Cannot open $1 "
  exit 1
fi
choice=all
if [ $# -gt 1 ] 
then
  if [ $2 = "all" ]
  then
     choice="all"
  elif [ $2 = "eigen" ]
  then
     choice="eigen"
  elif [ $2 = "ml" ]
  then
     choice="ml"
  elif [ $2 = "dnn" ]
  then
     choice="dnn"
  fi
fi
#echo "If the script hangs, make sure no_proxy env variable is set to intel.com"

#wget http://mlt-bdw0.sc.intel.com/tf_nightly_results/nightly_failed.txt -O /tmp/currfail.txt.$$ > /dev/null 2>&1 
if [ "$choice" != "all" ] 
then
  cp /mnt/aipg_tensorflow_shared/validation/logs/$choice.failures /tmp/currfail.txt.$$ 

  if [ $? -ne 0 ]
  then
    echo "Cannot find error file $choice.failures"
    exit 1
  fi
fi

fgrep "were skipped" $1 > /dev/null 2>&1
if [ $? -eq 0 ]
then
  echo "It looks like some tests were skipped, please check your logs and fix this first"
  exit 1
fi
fgrep FAILED $1 | sed "s/[ ][ ]*FAILED.*//" > /tmp/myfailed.txt.$$
fgrep "TIMEOUT " $1 | sed "s/[ ][ ]*TIMEOUT.*//"  >> /tmp/myfailed.txt.$$
if [ "$choice" = "all" ]
  then
     cat /tmp/myfailed.txt.$$ > /tmp/newfailed.txt.$$
  else
     fgrep -v -f /tmp/currfail.txt.$$ /tmp/myfailed.txt.$$ > /tmp/newfailed.txt.$$
fi

rm -f logs.tar.gz
logdirs=/tmp/logs.$$ 
logsummary=$logdirs/logs.txt
mkdir -p $logdirs
echo "new failures                      |||         Path to log file"  >> $logsummary
echo "===================================================================" >> $logsummary
echo >> $logsummary
echo >> $logsummary
x=0
while read line
do
  x=`expr $x + 1`
  echo -n $x $line >> $logsummary
  #test_name=`echo $line | sed 's/\//_/g'`
  #echo -n $x $test_name
  logfile_name_all=`grep "$line .*(see" $1 | sed 's/.*(see[ ]*\(.*\))/ \1/'`
  for logfile_name in $logfile_name_all
    do
      #echo $logfile_name
      new_logfile=`echo $logfile_name | sed -e "s/.*testlogs/testlogs/" | sed -e "s/:/\//g"`
      new_logdir=`echo $new_logfile | sed -e 's/\(.*\)\/[^/]*/\1/'`
      echo "  " $new_logfile >> $logsummary
      mkdir -p $logdirs/$new_logdir
      [ -e $logfile_name ] && [ -d  $logdirs/$new_logdir ] && cp $logfile_name $logdirs/$new_logdir
    done
done < /tmp/newfailed.txt.$$
rm -f logs.tar.gz
tar -czf logs.tar.gz -C $logdirs .
echo 
echo -n There were $x 
[ $choice != "all" ] && echo -n " new"
echo " failures, see logs.tar.gz for the log files."
rm -f /tmp/myfailed.txt.$$ /tmp/newfailed.txt.$$ /tmp/currfail.txt.$$ 
rm -rf /tmp/logs.$$
