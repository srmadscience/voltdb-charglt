/*
 * Copyright (C) 2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package org.voltdb.chargingdemo;


import java.io.IOException;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import org.voltdb.chargingdemo.callbacks.AddCreditCallback;
import org.voltdb.chargingdemo.callbacks.ComplainOnErrorCallback;
import org.voltdb.chargingdemo.callbacks.ReportQuotaUsageCallback;
import org.voltdb.chargingdemo.callbacks.UserKVState;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.ProcCallException;
import org.voltdb.voltutil.stats.SafeHistogramCache;

import com.google.gson.Gson;

import chargingdemoprocs.ExtraUserData;
import chargingdemoprocs.ReferenceData;

/**
 * This is an abstract class that contains the actual logic of the demo code.
 */
public abstract class BaseChargingDemo {

    public static final long GENERIC_QUERY_USER_ID = 42;
    public static final int HISTOGRAM_SIZE_MS = 1000000;

    public static final String REPORT_QUOTA_USAGE = "ReportQuotaUsage";
    public static final String KV_PUT = "KV_PUT";
    public static final String KV_GET = "KV_GET";

    public static SafeHistogramCache shc = SafeHistogramCache.getInstance();

    public static final String UNABLE_TO_MEET_REQUESTED_TPS = "UNABLE_TO_MEET_REQUESTED_TPS";
    public static final String EXTRA_MS = "EXTRA_MS";


    /**
     * Print a formatted message.
     *
     * @param message
     */
    public static void msg(String message) {

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        String strDate = sdfDate.format(now);
        System.out.println(strDate + ":" + message);

    }

    /**
     * Connect to VoltDB using a comma delimited hostname list.
     *
     * @param commaDelimitedHostnames
     * @return
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    protected static Client connectVoltDB(String commaDelimitedHostnames) throws Exception {
        Client client = null;
        ClientConfig config = null;

        try {
            msg("Logging into VoltDB");

            config = new ClientConfig(); // "admin", "idontknow");
            //config.setTopologyChangeAware(true);
            //config.setReconnectOnConnectionLoss(true);
            //config.setHeavyweight(true);

            client = ClientFactory.createClient(config);

            String[] hostnameArray = commaDelimitedHostnames.split(",");

            for (String element : hostnameArray) {
                msg("Connect to " + element + "...");
                try {
                    client.createConnection(element);
                } catch (Exception e) {
                    msg(e.getMessage());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("VoltDB connection failed.." + e.getMessage(), e);
        }

        return client;

    }

    /**
     * Convenience method to generate a JSON payload.
     *
     * @param length
     * @return
     */
    protected static String getExtraUserDataAsJsonString(int length, Gson gson, Random r) {

        ExtraUserData eud = new ExtraUserData();

        eud.loyaltySchemeName = "HelperCard";
        eud.loyaltySchemeNumber = getNewLoyaltyCardNumber(r);

        StringBuffer ourText = new StringBuffer();

        for (int i = 0; i < length / 2; i++) {
            ourText.append(Integer.toHexString(r.nextInt(256)));
        }

        eud.mysteriousHexPayload = ourText.toString();

        return gson.toJson(eud);
    }

    /**
     *
     * Delete all users in a range at tpMs per second
     *
     * @param minId
     * @param maxId
     * @param tpMs
     * @param mainClient
     * @throws InterruptedException
     * @throws IOException
     * @throws NoConnectionsException
     */
    protected static void deleteAllUsers(int minId, int maxId, int tpMs, Client mainClient)
            throws InterruptedException, IOException, NoConnectionsException {

        msg("Deleting users from " + minId + " to " + maxId);

        final long startMsDelete = System.currentTimeMillis();
        long currentMs = System.currentTimeMillis();

        // To make sure we do things at a consistent rate (tpMs) we
        // track how many transactions we've queued this ms and sleep if
        // we've reached our limit.
        int tpThisMs = 0;

        // So we iterate through all our users...
        for (int i = minId; i <= maxId; i++) {

            if (tpThisMs++ > tpMs) {

                // but sleep if we're moving too fast...
                while (currentMs == System.currentTimeMillis()) {
                    Thread.sleep(0, 50000);
                }

                currentMs = System.currentTimeMillis();
                tpThisMs = 0;
            }

            // Put a request to delete a user into the queue.
            ComplainOnErrorCallback deleteUserCallback = new ComplainOnErrorCallback();
            mainClient.callProcedure(deleteUserCallback, "DelUser", i);

            if (i % 100000 == 1) {
                msg("Deleted " + i + " users...");
            }

        }

        // Because we've put messages into the clients queue we
        // need to wait for them to be processed.
        msg("All " + (maxId - minId + 1) + " entries in queue, waiting for it to drain...");
        mainClient.drain();

        final long entriesPerMs = (maxId - minId + 1) / (System.currentTimeMillis() - startMsDelete);
        msg("Deleted " + entriesPerMs + " users per ms...");
    }

    /**
     * Create userCount users at tpMs per second.
     *
     * @param userCount
     * @param tpMs
     * @param ourJson
     * @param initialCredit
     * @param mainClient
     * @throws InterruptedException
     * @throws IOException
     * @throws NoConnectionsException
     */
    protected static void upsertAllUsers(int userCount, int tpMs, String ourJson, int initialCredit, Client mainClient)
            throws InterruptedException, IOException, NoConnectionsException {

        final long startMsUpsert = System.currentTimeMillis();

        long currentMs = System.currentTimeMillis();
        int tpThisMs = 0;
        Random r = new Random();

        for (int i = 0; i < userCount; i++) {

            if (tpThisMs++ > tpMs) {

                while (currentMs == System.currentTimeMillis()) {
                    Thread.sleep(0, 50000);
                }

                currentMs = System.currentTimeMillis();
                tpThisMs = 0;
            }

            ComplainOnErrorCallback upsertUserCallback = new ComplainOnErrorCallback();

            mainClient.callProcedure(upsertUserCallback, "UpsertUser", i, r.nextInt(initialCredit), ourJson, "Created",
                    new Date(startMsUpsert), "Create_" + i);

            if (i % 100000 == 1) {
                msg("Upserted " + i + " users...");

            }

        }

        msg("All " + userCount + " entries in queue, waiting for it to drain...");
        mainClient.drain();

        long entriesPerMS = userCount / (System.currentTimeMillis() - startMsUpsert);
        msg("Upserted " + entriesPerMS + " users per ms...");
    }

    /**
     * Convenience method to query a user a general stats and log the results.
     *
     * @param mainClient
     * @param queryUserId
     * @throws IOException
     * @throws NoConnectionsException
     * @throws ProcCallException
     */
    protected static void queryUserAndStats(Client mainClient, long queryUserId)
            throws IOException, NoConnectionsException, ProcCallException {

        // Query user #queryUserId...
        msg("Query user #" + queryUserId + "...");
        ClientResponse userResponse = mainClient.callProcedure("GetUser", queryUserId);

        for (int i = 0; i < userResponse.getResults().length; i++) {
            msg(System.lineSeparator() + userResponse.getResults()[i].toFormattedString());
        }

        msg("Show amount of credit currently reserved for products...");
        ClientResponse allocResponse = mainClient.callProcedure("ShowCurrentAllocations__promBL");

        for (int i = 0; i < allocResponse.getResults().length; i++) {
            msg(System.lineSeparator() + allocResponse.getResults()[i].toFormattedString());
        }
    }

    /**
     *
     * Convenience method to query all users who have a specific loyalty card id
     *
     * @param mainClient
     * @param cardId
     * @throws IOException
     * @throws NoConnectionsException
     * @throws ProcCallException
     */
    protected static void queryLoyaltyCard(Client mainClient, long cardId)
            throws IOException, NoConnectionsException, ProcCallException {

        // Query user #queryUserId...
        msg("Query card #" + cardId + "...");
        ClientResponse userResponse = mainClient.callProcedure("FindByLoyaltyCard", cardId);

        for (int i = 0; i < userResponse.getResults().length; i++) {
            msg(System.lineSeparator() + userResponse.getResults()[i].toFormattedString());
        }

    }

    /**
     *
     * Run a key value store benchmark for userCount users at tpMs transactions per
     * millisecond and with deltaProportion records sending the entire record.
     *
     * @param userCount
     * @param tpMs
     * @param durationSeconds
     * @param globalQueryFreqSeconds
     * @param jsonsize
     * @param mainClient
     * @param deltaProportion
     * @param extraMs
     * @return true if >=90% of requested throughput was achieved.
     * @throws InterruptedException
     * @throws IOException
     * @throws NoConnectionsException
     * @throws ProcCallException
     */
    protected static boolean runKVBenchmark(int userCount, int tpMs, int durationSeconds, int globalQueryFreqSeconds,
            int jsonsize, Client mainClient, int deltaProportion, int extraMs)
            throws InterruptedException, IOException, NoConnectionsException, ProcCallException {

        long lastGlobalQueryMs = 0;

        UserKVState[] userState = new UserKVState[userCount];

        Random r = new Random();

        Gson gson = new Gson();

        for (int i = 0; i < userCount; i++) {
            userState[i] = new UserKVState(i, shc);
        }

        final long startMsRun = System.currentTimeMillis();
        long currentMs = System.currentTimeMillis();
        int tpThisMs = 0;

        final long endtimeMs = System.currentTimeMillis() + (durationSeconds * 1000);

        // How many transactions we've done...
        int tranCount = 0;
        int inFlightCount = 0;
        int lockCount = 0;
        int contestedLockCount = 0;
        int fullUpdate = 0;
        int deltaUpdate = 0;

        while (endtimeMs > System.currentTimeMillis()) {

            if (tpThisMs++ > tpMs) {

                while (currentMs == System.currentTimeMillis()) {
                    Thread.sleep(0, 50000);

                }
                
                sleepExtraMSIfNeeded(extraMs);
                
                currentMs = System.currentTimeMillis();
                tpThisMs = 0;
            }

            // Find session to do a transaction for...
            int oursession = r.nextInt(userCount);

            // See if session already has an active transaction and avoid
            // it if it does.

            if (userState[oursession].isTxInFlight()) {

                inFlightCount++;

            } else if (userState[oursession].getUserStatus() == UserKVState.STATUS_LOCKED_BY_SOMEONE_ELSE) {

                if (userState[oursession].getOtherLockTimeMs() + ReferenceData.LOCK_TIMEOUT_MS < System
                        .currentTimeMillis()) {

                    userState[oursession].startTran();
                    userState[oursession].setStatus(UserKVState.STATUS_TRYING_TO_LOCK);
                    mainClient.callProcedure(userState[oursession], "GetAndLockUser", oursession);
                    lockCount++;

                } else {
                    contestedLockCount++;
                }

            } else if (userState[oursession].getUserStatus() == UserKVState.STATUS_UNLOCKED) {

                userState[oursession].startTran();
                userState[oursession].setStatus(UserKVState.STATUS_TRYING_TO_LOCK);
                mainClient.callProcedure(userState[oursession], "GetAndLockUser", oursession);
                lockCount++;

            } else if (userState[oursession].getUserStatus() == UserKVState.STATUS_LOCKED) {

                userState[oursession].startTran();
                userState[oursession].setStatus(UserKVState.STATUS_UPDATING);

                if (deltaProportion > r.nextInt(101)) {
                    deltaUpdate++;
                    // Instead of sending entire JSON object across wire ask app to update loyalty
                    // number. For
                    // large values stored as JSON this can have a dramatic effect on network
                    // bandwidth
                    mainClient.callProcedure(userState[oursession], "UpdateLockedUser", oursession,
                            userState[oursession].getLockId(), getNewLoyaltyCardNumber(r),
                            ExtraUserData.NEW_LOYALTY_NUMBER);
                } else {
                    fullUpdate++;
                    mainClient.callProcedure(userState[oursession], "UpdateLockedUser", oursession,
                            userState[oursession].getLockId(), getExtraUserDataAsJsonString(jsonsize, gson, r), null);
                }

            }

            tranCount++;

            if (tranCount % 100000 == 1) {
                msg("Transaction " + tranCount);
            }

        }

        // See if we need to do global queries...
        if (lastGlobalQueryMs + (globalQueryFreqSeconds * 1000) < System.currentTimeMillis()) {
            lastGlobalQueryMs = System.currentTimeMillis();

            queryUserAndStats(mainClient, GENERIC_QUERY_USER_ID);

        }

        msg(tranCount + " transactions done...");
        msg("All entries in queue, waiting for it to drain...");
        mainClient.drain();
        msg("Queue drained...");

        long transactionsPerMs = tranCount / (System.currentTimeMillis() - startMsRun);
        msg("processed " + transactionsPerMs + " entries per ms while doing transactions...");

        long lockFailCount = 0;
        for (int i = 0; i < userCount; i++) {
            lockFailCount += userState[i].getLockedBySomeoneElseCount();
        }

        msg(inFlightCount + " events where a tx was in flight were observed");
        msg(lockCount + " lock attempts");
        msg(contestedLockCount + " contested lock attempts");
        msg(lockFailCount + " lock attempt failures");
        msg(fullUpdate + " full updates");
        msg(deltaUpdate + " delta updates");

        double tps = tranCount;
        tps = tps / (System.currentTimeMillis() - startMsRun);
        tps = tps * 1000;

        reportRunLatencyStats(tpMs, tps);

        // Declare victory if we got >= 90% of requested TPS...
        if (tps / (tpMs * 1000) > .9) {
            return true;

        }

        return false;
    }

    /**
     * Used when we need to really slow down below 1 tx per ms.. 
     * @param extraMs an arbitrary extra delay.
     */
    private static void sleepExtraMSIfNeeded(int extraMs) {
        if (extraMs > 0) {
            try {
                Thread.sleep(extraMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
    }

    /**
     * Convenience method to remove unneeded records storing old allotments of
     * credit.
     *
     * @param mainClient
     * @throws IOException
     * @throws NoConnectionsException
     * @throws ProcCallException
     */
    protected static void clearUnfinishedTransactions(Client mainClient)
            throws IOException, NoConnectionsException, ProcCallException {

        msg("Clearing unfinished transactions from prior runs...");

        mainClient.callProcedure("@AdHoc", "DELETE FROM user_usage_table;");
        msg("...done");

    }

    /**
     *
     * Convenience method to clear outstaning locks between runs
     *
     * @param mainClient
     * @throws IOException
     * @throws NoConnectionsException
     * @throws ProcCallException
     */
    protected static void unlockAllRecords(Client mainClient)
            throws IOException, NoConnectionsException, ProcCallException {

        msg("Clearing locked sessions from prior runs...");

        mainClient.callProcedure("@AdHoc",
                "UPDATE user_table SET user_softlock_sessionid = null, user_softlock_expiry = null WHERE user_softlock_sessionid IS NOT NULL;");
        msg("...done");

    }

    /**
     *
     * Run a transaction benchmark for userCount users at tpMs per ms.
     *
     * @param userCount              number of users
     * @param tpMs                   transactions per milliseconds
     * @param durationSeconds
     * @param globalQueryFreqSeconds how often we check on global stats and a single
     *                               user
     * @param mainClient
     * @param extraMS
     * @return true if within 90% of targeted TPS
     * @throws InterruptedException
     * @throws IOException
     * @throws NoConnectionsException
     * @throws ProcCallException
     */
    protected static boolean runTransactionBenchmark(int userCount, int tpMs, int durationSeconds,
            int globalQueryFreqSeconds, Client mainClient, int extraMs)
            throws InterruptedException, IOException, NoConnectionsException, ProcCallException {

        // Used to track changes and be unique when we are running multiple threads
        final long pid = getPid();

        Random r = new Random();

        UserTransactionState[] users = new UserTransactionState[userCount];

        msg("Creating client records for " + users.length + " users");
        for (int i = 0; i < users.length; i++) {
            // We don't know a users credit till we've spoken to the server, so
            // we make an optimistic assumption...
            users[i] = new UserTransactionState(i, Long.MAX_VALUE);
        }

        final long startMsRun = System.currentTimeMillis();
        long currentMs = System.currentTimeMillis();
        int tpThisMs = 0;

        final long endtimeMs = System.currentTimeMillis() + (durationSeconds * 1000);

        // How many transactions we've done...
        long tranCount = 0;
        long inFlightCount = 0;
        long addCreditCount = 0;
        long reportUsageCount = 0;
        long lastGlobalQueryMs = System.currentTimeMillis();

        msg("starting...");

        while (endtimeMs > System.currentTimeMillis()) {

            if (tpThisMs++ > tpMs) {

                while (currentMs == System.currentTimeMillis()) {
                    Thread.sleep(0, 50000);

                }
                
                sleepExtraMSIfNeeded(extraMs);

                currentMs = System.currentTimeMillis();
                tpThisMs = 0;
            }

            int randomuser = r.nextInt(userCount);

            if (users[randomuser].isTxInFlight()) {
                inFlightCount++;
            } else {

                users[randomuser].startTran();

                if (users[randomuser].spendableBalance < 1000) {

                    addCreditCount++;

                    final long extraCredit = r.nextInt(1000) + 1000;

                    AddCreditCallback addCreditCallback = new AddCreditCallback(users[randomuser]);

                    mainClient.callProcedure(addCreditCallback, "AddCredit", randomuser, extraCredit,
                            "AddCreditOnShortage_" + pid + "_" + addCreditCount + "_" + System.currentTimeMillis());

                } else {

                    reportUsageCount++;

                    ReportQuotaUsageCallback reportUsageCallback = new ReportQuotaUsageCallback(users[randomuser], shc);

                    long unitsUsed = (int) (users[randomuser].currentlyReserved * 0.9);
                    long unitsWanted = r.nextInt(100);

                    mainClient.callProcedure(reportUsageCallback, "ReportQuotaUsage", randomuser, unitsUsed,
                            unitsWanted, users[randomuser].sessionId,
                            "ReportQuotaUsage_" + pid + "_" + reportUsageCount + "_" + System.currentTimeMillis());

                }
            }

            if (tranCount++ % 100000 == 0) {
                msg("On transaction #" + tranCount);
            }

            // See if we need to do global queries...
            if (lastGlobalQueryMs + (globalQueryFreqSeconds * 1000) < System.currentTimeMillis()) {
                lastGlobalQueryMs = System.currentTimeMillis();

                queryUserAndStats(mainClient, GENERIC_QUERY_USER_ID);

            }

        }

        msg("finished adding transactions to queue");
        mainClient.drain();
        msg("Queue drained");

        long elapsedTimeMs = System.currentTimeMillis() - startMsRun;
        msg("Processed " + tranCount + " transactions in " + elapsedTimeMs + " milliseconds");

        double tps = tranCount;
        tps = tps / (elapsedTimeMs / 1000);

        msg("TPS = " + tps);

        msg("Add Credit calls = " + addCreditCount);
        msg("Report Usage calls = " + reportUsageCount);
        msg("Skipped because transaction was in flight = " + inFlightCount);

        reportRunLatencyStats(tpMs, tps);

        // Declare victory if we got >= 90% of requested TPS...
        if (tps / (tpMs * 1000) > .9) {
            return true;

        }

        return false;
    }

    /**
     * Turn latency stats into a grepable string
     *
     * @param tpMs target transactions per millisecond
     * @param tps  observed TPS
     */
    private static void reportRunLatencyStats(int tpMs, double tps) {
        StringBuffer oneLineSummary = new StringBuffer("GREPABLE SUMMARY:");

        oneLineSummary.append(tpMs);
        oneLineSummary.append(':');

        oneLineSummary.append(tps);
        oneLineSummary.append(':');

        SafeHistogramCache.getProcPercentiles(shc, oneLineSummary, REPORT_QUOTA_USAGE);

        SafeHistogramCache.getProcPercentiles(shc, oneLineSummary, KV_PUT);

        SafeHistogramCache.getProcPercentiles(shc, oneLineSummary, KV_GET);

        msg(oneLineSummary.toString());

        msg(shc.toString());
    }

    /**
     * Get Linux process ID - used for pseudo unique ids
     * 
     * @return Linux process ID
     */
    private static long getPid() {
        return ProcessHandle.current().pid();
    }

    /**
     * Return a loyalty card number
     *
     * @param r
     * @return a random loyalty card number between 0 and 1 million
     */
    private static long getNewLoyaltyCardNumber(Random r) {
        return System.currentTimeMillis() % 1000000;
    }
    
    /**
     * get EXTRA_MS env variable if set
     * @return extraMs
     */
    public static int getExtraMsIfSet() {
        
        int extraMs = 0;

        String extraMsEnv = System.getenv(EXTRA_MS);
        
        if (extraMsEnv != null && extraMsEnv.length() > 0) {
            msg("EXTRA_MS is '" + extraMsEnv + "'" );
            extraMs = Integer.parseInt(extraMsEnv);
        }
        
        return extraMs;
    }

}
