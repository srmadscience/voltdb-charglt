/*
 * Copyright (C) 2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package chargingdemoprocs;


import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class ReportQuotaUsage extends VoltProcedure {

    // @formatter:off

	public static final SQLStmt getUser = new SQLStmt(
			"SELECT userid FROM user_table WHERE userid = ?;");

    public static final SQLStmt removeOldestTransaction = new SQLStmt("DELETE "
              + "FROM user_recent_transactions "
              + "WHERE userid = ? "
              + "AND txn_time < DATEADD(MINUTE, -1,NOW) "
              + "ORDER BY userid, txn_time, user_txn_id LIMIT 2;");

    public static final SQLStmt getTxn = new SQLStmt("SELECT txn_time FROM user_recent_transactions "
            + "WHERE userid = ? AND user_txn_id = ?;");

	public static final SQLStmt getUserBalance = new SQLStmt("SELECT balance, CAST(? AS BIGINT) sessionid FROM user_balance WHERE userid = ?;");

	public static final SQLStmt getCurrentlyAllocated = new SQLStmt(
			"select nvl(sum(allocated_amount),0)  allocated_amount from user_usage_table where userid = ?;");

	public static final SQLStmt addTxn = new SQLStmt("INSERT INTO user_recent_transactions "
			+ "(userid, user_txn_id, txn_time, approved_amount,spent_amount,purpose,sessionid) VALUES (?,?,NOW,?,?,?,?);");

	public static final SQLStmt delOldUsage = new SQLStmt(
			"DELETE FROM user_usage_table WHERE userid = ? AND sessionid = ?;");

	public static final SQLStmt reportFinancialEvent = new SQLStmt(
			"INSERT INTO user_financial_events (userid,amount,user_txn_id,message) VALUES (?,?,?,?);");

	public static final SQLStmt createAllocation = new SQLStmt("INSERT INTO user_usage_table "
			+ "(userid, allocated_amount,sessionid, lastdate) VALUES (?,?,?,NOW);");


	// @formatter:on

    /**
     * @param userId         - Identifies a user
     * @param unitsUsed      - How many units of credit were used. Initially this
     *                       will be zero, as we start by reserving credit.
     * @param unitsWanted    - How many units of credit the user is looking for.
     *                       Some or all of this may be granted.
     * @param inputSessionId - a Unique ID for a session. A negative number means a
     *                       new session.
     * @param txnId          - A unique ID for the network call. This is needed so
     *                       we can tell if a transaction completed, but didn't get
     *                       back to the client.
     * @return
     * @throws VoltAbortException
     */
    public VoltTable[] run(long userId, int unitsUsed, int unitsWanted, long inputSessionId, String txnId)
            throws VoltAbortException {

        // Set session ID if needed.
        long sessionId = inputSessionId;

        if (sessionId == Long.MIN_VALUE) {
            sessionId = this.getUniqueId();
        }

        // See if this user is real or this transaction has already happened.
        // Get rid of old transaction records.
        voltQueueSQL(getUser, userId);
        voltQueueSQL(getTxn, userId, txnId);
        voltQueueSQL(removeOldestTransaction, userId);

        VoltTable[] results1 = voltExecuteSQL();
        VoltTable userTable = results1[0];
        VoltTable sameTxnTable = results1[1];

        // Sanity check: Does this user exist?
        if (!userTable.advanceRow()) {
            throw new VoltAbortException("User " + userId + " does not exist");
        }

        // Sanity Check: Is this a re-send of a transaction we've already done?
        if (sameTxnTable.advanceRow()) {
            this.setAppStatusCode(ReferenceData.STATUS_TXN_ALREADY_HAPPENED);
            this.setAppStatusString(
                    "Event already happened at " + results1[1].getTimestampAsTimestamp("txn_time").toString());
            return voltExecuteSQL(true);
        }

        long amountSpent = unitsUsed * -1;
        String decision = "Spent " + amountSpent;

        // Delete old usage record
        voltQueueSQL(delOldUsage, userId, sessionId);
        voltQueueSQL(getUserBalance, sessionId, userId);
        voltQueueSQL(getCurrentlyAllocated, userId);

        // The first time we're called we won't have spent anything, we'll be reserving
        // credit.
        if (amountSpent != 0) {
            voltQueueSQL(reportFinancialEvent, userId, amountSpent, txnId, decision);
        }

        if (unitsWanted == 0) {
            voltQueueSQL(addTxn, userId, txnId, 0, amountSpent, decision, sessionId);
            voltQueueSQL(getUserBalance, sessionId, userId);
            voltQueueSQL(getCurrentlyAllocated, userId);

            this.setAppStatusCode(ReferenceData.STATUS_OK);
            return voltExecuteSQL(true);
        }

        VoltTable[] results2 = voltExecuteSQL();

        VoltTable userBalance = results2[1];
        VoltTable allocated = results2[2];

        // Calculate how much money is actually available...

        userBalance.advanceRow();
        long availableCredit = userBalance.getLong("balance");

        if (allocated.advanceRow()) {
            availableCredit = availableCredit - allocated.getLong("allocated_amount");
        }

        long amountApproved = 0;

        if (availableCredit < 0) {

            decision = decision + "; Negative balance: " + availableCredit;
            this.setAppStatusCode(ReferenceData.STATUS_NO_MONEY);

        } else if (unitsWanted > availableCredit) {

            amountApproved = availableCredit;
            decision = decision + "; Allocated " + availableCredit + " units of " + unitsWanted + " asked for";
            this.setAppStatusCode(ReferenceData.STATUS_SOME_UNITS_ALLOCATED);

        } else {

            amountApproved = unitsWanted;
            decision = decision + "; Allocated " + unitsWanted;
            this.setAppStatusCode(ReferenceData.STATUS_ALL_UNITS_ALLOCATED);

        }

        voltQueueSQL(createAllocation, userId, amountApproved, sessionId);

        this.setAppStatusString(decision);
        // Note that transaction is now 'official'

        voltQueueSQL(addTxn, userId, txnId, amountApproved, amountSpent, decision, sessionId);
        voltQueueSQL(getUserBalance, sessionId, userId);
        voltQueueSQL(getCurrentlyAllocated, userId);

        return voltExecuteSQL();

    }

}
