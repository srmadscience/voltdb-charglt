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
JSONSIZE=$5
DELTAPROP=$6
TC=$7

DURATION=120

if 	
	[ "$ST" = "" -o "$MX" = "" -o "$INC" = "" -o "$USERCOUNT" = "" -o "$JSONSIZE" = "" -o "$DELTAPROP" = "" -o "$TC" = "" ]
then
	echo Usage: $0 start_tps end_tps inc_tps usercount blobsize percent_of_changes threadcount

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

	EACH_TPS=`expr ${CT} / ${TC}`

	T=1

        AWSNETBAD=`sh ../scripts/check_aws_network_limits.sh`
	echo AWSNETBAD=${AWSNETBAD}

	while 
		[ "$T" -le "$TC" ]
	do

		echo Starting thread $T at ${EACH_TPS}  KTPS...
		echo `date` java ${JVMOPTS}  -jar ChargingDemoKVStore.jar  `cat $HOME/.vdbhostnames`  ${USERCOUNT} ${EACH_TPS} $DURATION 60 $JSONSIZE $DELTAPROP >> $HOME/logs/activity.log
		java ${JVMOPTS}  -jar ChargingDemoKVStore.jar  `cat $HOME/.vdbhostnames`  ${USERCOUNT} ${EACH_TPS} $DURATION 60 $JSONSIZE $DELTAPROP > $HOME/logs/${DT}_kv_`uname -n`_${CT}_${T}.lst  & 
		T=`expr $T + 1`
		sleep 1

	done

	echo Waiting for threads to finish...

	wait 
	grep GREPABLE $HOME/logs/${DT}_kv_`uname -n`_${CT}_1.lst 

	FAILED_FILE=/tmp/$$.tmp
	touch ${FAILED_FILE}
	cat $HOME/logs/${DT}_kv_`uname -n`_${CT}_1.lst | grep UNABLE_TO_MEET_REQUESTED_TPS >> ${FAILED_FILE}


        if
                [ -s "${FAILED_FILE}" ]
        then
		rm ${FAILED_FILE}
                echo FAILED
                exit 1
        fi
  
	rm ${FAILED_FILE}

   	OLDAWSNETBAD=${AWSNETBAD}
        AWSNETBAD=`sh ../scripts/check_aws_network_limits.sh`
	echo AWSNETBAD=${AWSNETBAD}

        if
                [ "${OLDAWSNETBAD}" != "${AWSNETBAD}"  ]
        then
                echo FAILED_BANDWIDTH ${AWSNETBAD}
                #exit 2
        fi


	sleep 15

	CT=`expr $CT + ${INC}`

done

