#!/bin/sh

#
#  Copyright (C) 2025 Volt Active Data Inc.
# 
#  Use of this source code is governed by an MIT
#  license that can be found in the LICENSE file or at
#  https://opensource.org/licenses/MIT.
# 

. $HOME/.profile

USERCOUNT=$1
ST=$2
DURATION=$3
MAXCREDIT=$4
KPORT=9092

if 	
	[ "$ST" = "" -o "$USERCOUNT" = "" -o "$DURATION" = "" -o "$MAXCREDIT" = "" ]
then
	echo Usage: $0 usercount tps duration max_credit

	exit 1
fi

cd
mkdir logs 2> /dev/null

cd voltdb-charglt/jars 


DT=`date '+%Y%m%d_%H%M'`

KHOSTS=`cat $HOME/.vdbhostnames | sed '1,$s/,/:'${KPORT}',/g'`:${KPORT}

echo "Starting a $DURATION second run at ${ST} Transactions Per Second"
echo `date` java ${JVMOPTS}  -jar KafkaCreditDemo.jar ${KHOSTS} ${USERCOUNT} ${ST} $DURATION $MAXCREDIT  >> $HOME/logs/activity.log
java ${JVMOPTS}  -jar KafkaCreditDemo.jar ${KHOSTS} ${USERCOUNT} ${ST} $DURATION $MAXCREDIT | tee -a $HOME/logs/${DT}_kafka__`uname -n`_${ST}.lst
sleep 2 

exit 0
