#!/bin/sh

#
#  Copyright (C) 2025 Volt Active Data Inc.
# 
#  Use of this source code is governed by an MIT
#  license that can be found in the LICENSE file or at
#  https://opensource.org/licenses/MIT.
# 

BNAME=$1
AMI=$2

if
	[ "$BNAME" = "" ]
then
	echo Usage: nohup sh $0 filename AMI \& 
	exit 1
fi

if
	[ "$AMI" = "" ]
then
	echo Usage: nohup sh $0 filename  AMI \& 
	exit 2
fi

sh check_aws_network_limits.sh
sh -x runlargebenchmark.sh 90 2000 10 4000000 10  > ${BNAME}_oltp.lst
sh gatherstats.sh c6i.12xlarge_5  ${AMI} ${BNAME}_oltp.lst > ${BNAME}_oltp.txt
sh check_aws_network_limits.sh
sleep 60
sh -x runlargekvbenchmark.sh 90 2000 10 4000000 100 50 10  > ${BNAME}_kv.lst
sh gatherstats.sh c6i.12xlarge_5  ${AMI} ${BNAME}_kv.lst > ${BNAME}_kv.txt
sh check_aws_network_limits.sh


