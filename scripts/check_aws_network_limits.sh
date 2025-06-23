#!/bin/sh

#
#  Copyright (C) 2025 Volt Active Data Inc.
# 
#  Use of this source code is governed by an MIT
#  license that can be found in the LICENSE file or at
#  https://opensource.org/licenses/MIT.
# 

INALLOW=`ethtool -S ens5 | grep bw_in_allowance_exceeded | awk -F: '{ print $2 }'`
OUTALLOW=`ethtool -S ens5 | grep bw_out_allowance_exceeded | awk -F: '{ print $2 }'`


if 
	[ "$INALLOW" -gt 0 ]
then
	echo WARNING: AWS IS THROTTLING INPUT TRAFFIC - ${INALLOW}
fi

if 
	[ "$OUTALLOW" -gt 0 ]
then
	echo WARNING: AWS IS THROTTLING OUTPUT TRAFFIC - ${OUTALLOW}
fi

