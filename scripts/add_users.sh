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
TPMS=$2
MAX_CREDIT=$3

if 
        [ "$USERCOUNT" = "" -o "$TPMS" = "" -o "$MAX_CREDIT" = "" ]
then
        echo Usage: $0 usercount tpms max_credit

        exit 1
fi

cd
mkdir logs 2> /dev/null

cd voltdb-charglt/jars

echo `date` java ${JVMOPTS} -jar CreateChargingDemoData.jar `cat $HOME/.vdbhostnames`  $USERCOUNT $TPMS $MAX_CREDIT >> $HOME/logs/activity.log
java ${JVMOPTS} -jar CreateChargingDemoData.jar `cat $HOME/.vdbhostnames`  $USERCOUNT $TPMS $MAX_CREDIT


