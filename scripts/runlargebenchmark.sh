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
TC=$5
DURATION=300

if 	
	[ "$MX" = "" -o "$ST" = "" -o "$INC" = "" -o "$USERCOUNT" = "" -o "$TC" = "" ]
then
	echo Usage: $0 start_tps max_tps increment usercount threadcount

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
DT=`date '+%Y%m%d_%H%M%S'`

while
	[ "${CT}" -le "${MX}" ]
do

	echo "Starting a $DURATION second run  of $TC threads, each at ${CT} Transactions Per Millisecond"

	T=1

	while 
		[ "$T" -le "$TC" ]
	do

       	EACH_TPS=`expr ${CT} / ${TC}`
		echo Starting thread $T at $EACH_TPS KTPS...
		echo `date` java ${JVMOPTS}  -jar ChargingDemoTransactions.jar `cat $HOME/.vdbhostnames`  ${USERCOUNT} ${EACH_TPS} $DURATION 60 >> $HOME/logs/activity.log
		java ${JVMOPTS}  -jar ChargingDemoTransactions.jar `cat $HOME/.vdbhostnames`  ${USERCOUNT} ${EACH_TPS} $DURATION 60 > $HOME/logs/${DT}_charging_`uname -n`_${CT}_${T}.lst &
		T=`expr $T + 1`
		sleep 1

	done

	echo Waiting for threads to finish...

	wait 

        grep GREPABLE $HOME/logs/${DT}_charging_`uname -n`_${CT}_1.lst

        FAILED_FILE=/tmp/$$.tmp
        touch ${FAILED_FILE}
        cat $HOME/logs/${DT}_charging_`uname -n`_${CT}_1.lst | grep UNABLE_TO_MEET_REQUESTED_TPS >> ${FAILED_FILE}


        if
                [ -s "${FAILED_FILE}" ]
        then
                rm ${FAILED_FILE}
                echo FAILED
                exit 1
        fi

        rm ${FAILED_FILE}

	sleep 15

	CT=`expr $CT + ${INC}`

done


exit 0
