#!/bin/sh

DIR=`dirname $0`
cd $DIR
PID=`cat x2ora.pid`
kill $PID
TS=`date "+%Y%m%d-%H%M%S"`
nohup ./gradlew run > logs/$TS.log  &
ln -sf $TS.log logs/latest.log
pid=$!
echo $pid > x2ora.pid
tail -f logs/latest.log

