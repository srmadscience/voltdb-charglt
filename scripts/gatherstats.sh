#!/bin/sh
#
#  Copyright (C) 2025 Volt Active Data Inc.
# 
#  Use of this source code is governed by an MIT
#  license that can be found in the LICENSE file or at
#  https://opensource.org/licenses/MIT.
# 

ITYPE=`curl -s http://169.254.169.254/latest/meta-data/instance-type`
KFACTOR=`cat $HOME/voltwrangler_params.dat | awk '{ print $2 }'`
CMDLOGGING=`cat $HOME/voltwrangler_params.dat | awk '{ print $3 }'`
DEMONAME=`cat $HOME/voltwrangler_params.dat | awk '{ print $5 }'`
SPH=`cat $HOME/voltwrangler_params.dat | awk '{ print $7 }'`
NODECOUNT=`cat $HOME/voltwrangler_params.dat | awk '{ print $10 }'`

TNAME=$1
AMI=$2
echo -n AMI:TESTNAME:INSTANCE:KFACTOR:CMDLOGGING:DEMONAME:SPH:NODECOUNT:FILE:DATE:TIME_MINS:TIME_SS:GREP:TARGET_TPMS:ACTUAL_TPS:
for i in RQU KV_GET KV_PUT 
do
	for j in AVG 50 99 99.9 99.99 99.999 MAX MAX_FREQ
do
	echo -n ${i}_${j}:
done
done
echo ""

grep GREPABLE $3 | sed '1,$s/^/'${AMI}:${TNAME}:${ITYPE}:${KFACTOR}:${CMDLOGGING}:${DEMONAME}:${SPH}:${NODECOUNT}:$3:'/g'

