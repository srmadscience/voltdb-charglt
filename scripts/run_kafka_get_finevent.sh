#!/bin/sh

#
#  Copyright (C) 2025 Volt Active Data Inc.
# 
#  Use of this source code is governed by an MIT
#  license that can be found in the LICENSE file or at
#  https://opensource.org/licenses/MIT.
# 

KPORT=9092
KHOSTS=`cat $HOME/.vdbhostnames | sed '1,$s/,/:'${KPORT}',/g'`:${KPORT}

if 
	[ "$1" = "START" ]
then
	${HOME}/bin/kafka_2.13-2.6.0/bin/kafka-console-consumer.sh  --from-beginning  --bootstrap-server ${KHOSTS} --topic USER_FINANCIAL_EVENTS
else
	${HOME}/bin/kafka_2.13-2.6.0/bin/kafka-console-consumer.sh  --bootstrap-server ${KHOSTS} --topic USER_FINANCIAL_EVENTS
fi

