/*
 * Copyright (C) 2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package org.voltdb.chargingdemo;


/**
 * Class to keep track of how many transactions a given user has. It also keeps
 * track of whether a transaction is in progress and when it started.
 *
 */
public class UserTransactionState {

    /**
     * ID of user.
     */
    public int id = 0;

    /**
     * Id of user session, or Long.MIN_VALUE if unknown.
     */
    public long sessionId = Long.MIN_VALUE;

    /**
     * When a transaction started, or zero if there isn't one.
     */
    public long txStartMs = 0;

    /**
     * Balance. Long.MAX_VALUE means we don't know...
     */
    public long spendableBalance = 0;

    /**
     * Currently reserved balance...
     */
    public long currentlyReserved = 0;

    /**
     * Create a record for a user.
     *
     * @param id
     * @param spendableBalance Long.MAX_VALUE means we don't know...
     */
    public UserTransactionState(int id, long spendableBalance) {
        this.id = id;
        this.spendableBalance = spendableBalance;
    }

    /**
     * Report start of transaction.
     */
    public void startTran() {

        txStartMs = System.currentTimeMillis();
    }

    /**
     * @return the txInFlight
     */
    public boolean isTxInFlight() {

        if (txStartMs > 0) {
            return true;
        }

        return false;
    }

    /**
     * We measure latency by comparing when this call happens to when startTran was
     * called.
     *
     * @param productId
     * @param sessionid
     * @param statusByte
     */
    public void reportEndTransaction(long sessionid, byte statusByte, long spendableBalance) {

        txStartMs = 0;
        this.sessionId = sessionid;
        this.spendableBalance = spendableBalance;

    }

    public void endTran() {
        txStartMs = 0;

    }

}
