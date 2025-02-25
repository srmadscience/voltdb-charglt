/*
 * Copyright (C) 2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package org.voltdb.chargingdemo;


import java.util.Arrays;

import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;

public class DeleteChargingDemoData extends BaseChargingDemo {

    /**
     * @param args
     */
    public static void main(String[] args) {

        msg("Parameters:" + Arrays.toString(args));

        if (args.length != 2) {
            msg("Usage: hostnames tpms");
            System.exit(1);
        }

        // Comma delimited list of hosts...
        String hostlist = args[0];

        // Target transactions per millisecond.
        int tpMs = Integer.parseInt(args[1]);

        try {
            // A VoltDB Client object maintains multiple connections to all the
            // servers in the cluster.
            Client mainClient = connectVoltDB(hostlist);

            ClientResponse cr = mainClient.callProcedure("@AdHoc",
                    "SELECT min(userid) min_userid, max(userid) max_userid FROM user_table;");

            if (cr.getResults()[0].advanceRow()) {

                int minId = (int) cr.getResults()[0].getLong("min_userid");
                int maxId = (int) cr.getResults()[0].getLong("max_userid");

                if (cr.getResults()[0].wasNull()) {
                    msg("no users found");
                } else {
                    deleteAllUsers(minId, maxId, tpMs, mainClient);
                }

            }

            msg("Closing connection...");
            mainClient.close();

        } catch (Exception e) {
            msg(e.getMessage());
        }

    }

}
