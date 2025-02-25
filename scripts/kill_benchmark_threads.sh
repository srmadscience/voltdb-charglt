#!/usr/bin/bash
#
#  Copyright (C) 2025 Volt Active Data Inc.
# 
#  Use of this source code is governed by an MIT
#  license that can be found in the LICENSE file or at
#  https://opensource.org/licenses/MIT.
# 

# Kill running benchmark threads

 

. $HOME/.profile

 

# Get the first PID

PID=$(ps -deaf | grep ChargingDemoTransactions.jar  | grep -v grep | awk '{ print $2 }')

 

function kill_threads () {

until [ -z $PID ]

    do

        echo "Killing $PID"

        kill -9 $PID

        sleep 2

        PID=$(ps -deaf | grep ChargingDemoTransactions.jar  | grep -v grep | awk '{ print $2 }')

        echo $PID

  done

}

 

kill_threads
