#!/bin/sh

#
#  Copyright (C) 2025 Volt Active Data Inc.
# 
#  Use of this source code is governed by an MIT
#  license that can be found in the LICENSE file or at
#  https://opensource.org/licenses/MIT.
# 

. $HOME/.profile

ST=$1
USERCOUNT=$2
DURATION=1200

if 	
	[ "$ST" = "" -o "$USERCOUNT" = "" ]
then
	echo Usage: $0 tps usercount

	exit 1
fi

cd
mkdir logs 2> /dev/null

cd voltdb-charglt/jars 

# silently kill off any copy that is currently running...
kill -9 `ps -deaf | grep ChargingDemoKVStore.jar  | grep -v grep | awk '{ print $2 }'` 2> /dev/null
kill -9 `ps -deaf | grep ChargingDemoTransactions.jar  | grep -v grep | awk '{ print $2 }'` 2> /dev/null

sleep 2 

DT=`date '+%Y%m%d_%H%M'`


echo "Starting a $DURATION second run at ${ST} Transactions Per Millisecond"
echo `date` java ${JVMOPTS}  -jar ChargingDemoTransactions.jar `cat $HOME/.vdbhostnames`  ${USERCOUNT} ${ST} $DURATION 60 >> $HOME/logs/activity.log
java ${JVMOPTS}  -jar ChargingDemoTransactions.jar `cat $HOME/.vdbhostnames`  ${USERCOUNT} ${ST} $DURATION 60 | tee -a $HOME/logs/${DT}_charging_`uname -n`_${ST}.lst 

exit 0
