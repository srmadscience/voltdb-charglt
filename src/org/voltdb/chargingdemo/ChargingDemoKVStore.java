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

public class ChargingDemoKVStore extends BaseChargingDemo {


    /**
     * @param args
     */
    public static void main(String[] args) {

        msg("Parameters:" + Arrays.toString(args));

        if (args.length != 7) {
            msg("Usage: hostnames recordcount tpms durationseconds queryseconds jsonsize deltaProportion");
            System.exit(1);
        }

        // Comma delimited list of hosts...
        String hostlist = args[0];

        // How many users
        int userCount = Integer.parseInt(args[1]);

        // Target transactions per millisecond.
        int tpMs = Integer.parseInt(args[2]);

        // Runtime for TRANSACTIONS in seconds.
        int durationSeconds = Integer.parseInt(args[3]);

        // How often we do global queries...
        int globalQueryFreqSeconds = Integer.parseInt(args[4]);

        // How often we do global queries...
        int jsonsize = Integer.parseInt(args[5]);

        int deltaProportion = Integer.parseInt(args[6]);
        
        // Extra delay for testing really slow hardware
        int extraMs = getExtraMsIfSet();
 
        try {
            // A VoltDB Client object maintains multiple connections to all the
            // servers in the cluster.
            Client mainClient = connectVoltDB(hostlist);

            unlockAllRecords(mainClient);
            boolean ok = runKVBenchmark(userCount, tpMs, durationSeconds, globalQueryFreqSeconds, jsonsize, mainClient,
                    deltaProportion, extraMs);

            msg("Closing connection...");
            mainClient.close();

            if (ok) {
                System.exit(0);
            }

            msg(UNABLE_TO_MEET_REQUESTED_TPS);
            System.exit(1);

        } catch (Exception e) {
            msg(e.getMessage());
        }

    }

  

}
