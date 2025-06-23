#!/bin/sh

#
#  Copyright (C) 2025 Volt Active Data Inc.
# 
#  Use of this source code is governed by an MIT
#  license that can be found in the LICENSE file or at
#  https://opensource.org/licenses/MIT.
# 

. $HOME/.profile

TPS=200

cd
mkdir logs 2> /dev/null

cd voltdb-charglt
cd jars
echo `date` java  ${JVMOPTS}  -jar DeleteChargingDemoData.jar  `cat $HOME/.vdbhostnames`  $TPS >> $HOME/logs/activity.log
java  ${JVMOPTS}  -jar DeleteChargingDemoData.jar  `cat $HOME/.vdbhostnames`  $TPS
