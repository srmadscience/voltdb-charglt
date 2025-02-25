/*
 * Copyright (C) 2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package chargingdemoprocs;

import java.util.Date;


import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;
import org.voltdb.types.TimestampType;

public class AddCredit extends VoltProcedure {

    // @formatter:off

	public static final SQLStmt getUser = new SQLStmt(
			"SELECT userid FROM user_table WHERE userid = ?;");

	public static final SQLStmt getTxn = new SQLStmt("SELECT txn_time FROM user_recent_transactions "
			+ "WHERE userid = ? AND user_txn_id = ?;");

	public static final SQLStmt addTxn = new SQLStmt("INSERT INTO user_recent_transactions "
			+ "(userid, user_txn_id, txn_time, approved_amount,spent_amount,purpose) VALUES (?,?,NOW,?,?,?);");

	public static final SQLStmt reportFinancialEvent = new SQLStmt("INSERT INTO user_financial_events "
			+ "(userid,amount,user_txn_id,message) VALUES (?,?,?,?);");

	public static final SQLStmt getUserBalance = new SQLStmt("SELECT balance FROM user_balance WHERE userid = ?;");

	public static final SQLStmt getCurrrentlyAllocated = new SQLStmt(
			"select nvl(sum(allocated_amount),0)  allocated_amount from user_usage_table where userid = ?;");

    public static final SQLStmt getOldestTxn = new SQLStmt("SELECT user_txn_id, txn_time "
            + "FROM user_recent_transactions "
            + "WHERE userid = ? "
            + "ORDER BY txn_time,userid,user_txn_id LIMIT 1;");

   public static final SQLStmt deleteOldTxn = new SQLStmt("DELETE FROM user_recent_transactions "
            + "WHERE userid = ? AND user_txn_id = ?;");


	// @formatter:on

    private static final long FIVE_MINUTES_IN_MS = 1000 * 60 * 5;

    /**
     * A VoltDB stored procedure to add credit to a user in the chargingdemo demo.
     * It checks that the user exists and also makes sure that this transaction
     * hasn't already happened.
     *
     * @param userId
     * @param extraCredit
     * @param txnId
     * @return Balance and Credit info
     * @throws VoltAbortException
     */
    public VoltTable[] run(long userId, long extraCredit, String txnId) throws VoltAbortException {

        // See if we know about this user and transaction...
        voltQueueSQL(getUser, userId);
        voltQueueSQL(getTxn, userId, txnId);
        voltQueueSQL(getOldestTxn, userId);

        VoltTable[] userAndTxn = voltExecuteSQL();

        // Sanity Check: Is this a real user?
        if (!userAndTxn[0].advanceRow()) {
            throw new VoltAbortException("User ID " + userId + " does not exist");
        }

        // Sanity Check: Has this transaction already happened?
        if (userAndTxn[1].advanceRow()) {

            this.setAppStatusCode(ReferenceData.STATUS_TXN_ALREADY_HAPPENED);
            this.setAppStatusString(
                    "Event already happened at " + userAndTxn[1].getTimestampAsTimestamp("txn_time").toString());
            voltQueueSQL(reportFinancialEvent, userId, extraCredit, txnId, "Credit already added");

        } else {

            // Report credit add...
            this.setAppStatusCode(ReferenceData.STATUS_CREDIT_ADDED);
            this.setAppStatusString(extraCredit + " added by Txn " + txnId);

            // Insert a row into the stream for each user's financial events.
            // The view user_balances can then calculate actual credit
            voltQueueSQL(addTxn, userId, txnId, 0, extraCredit, "Add Credit");
            voltQueueSQL(reportFinancialEvent, userId, extraCredit, txnId, "Added " + extraCredit);
        }

        // Delete oldest record if old enough
        if (userAndTxn[2].advanceRow()) {
            TimestampType oldestTxn = userAndTxn[2].getTimestampAsTimestamp("txn_time");

            if (oldestTxn.asExactJavaDate().before(new Date(getTransactionTime().getTime() - FIVE_MINUTES_IN_MS))) {
                String oldestTxnId = userAndTxn[2].getString("user_txn_id");
                voltQueueSQL(deleteOldTxn, userId, oldestTxnId);
            }
        }

        voltQueueSQL(getUserBalance, userId);
        voltQueueSQL(getCurrrentlyAllocated, userId);

        return voltExecuteSQL(true);
    }
}
