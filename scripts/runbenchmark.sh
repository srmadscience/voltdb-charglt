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
MX=$2
INC=$3
USERCOUNT=$4
DURATION=600

if 	
	[ "$MX" = "" -o "$ST" = "" -o "$INC" = "" -o "$USERCOUNT" = "" ]
then
	echo Usage: $0 start_tps max_tps increment usercount

	exit 1
fi

cd
mkdir logs 2> /dev/null

cd voltdb-charglt/jars 

# silently kill off any copy that is currently running...
kill -9 `ps -deaf | grep ChargingDemoKVStore.jar  | grep -v grep | awk '{ print $2 }'` 2> /dev/null
kill -9 `ps -deaf | grep ChargingDemoTransactions.jar  | grep -v grep | awk '{ print $2 }'` 2> /dev/null

sleep 2 

CT=${ST}

while
	[ "${CT}" -le "${MX}" ]
do

	DT=`date '+%Y%m%d_%H%M%S'`
	echo "Starting a $DURATION second run at ${CT} Transactions Per Millisecond"
	echo `date` java ${JVMOPTS}  -jar ChargingDemoTransactions.jar `cat $HOME/.vdbhostnames`  ${USERCOUNT} ${CT} ${DURATION} 60 >> $HOME/logs/activity.log
	java ${JVMOPTS}  -jar ChargingDemoTransactions.jar `cat $HOME/.vdbhostnames`  ${USERCOUNT} ${CT} ${DURATION} 60 
	if 
		[ "$?" = "1" ]
	then
		break;
	fi

	CT=`expr $CT + ${INC}`
done


exit 0
