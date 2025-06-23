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
import org.voltdb.types.TimestampType;

public class UpsertUser extends VoltProcedure {

    // @formatter:off

	public static final SQLStmt getUser = new SQLStmt("SELECT userid FROM user_table WHERE userid = ?;");

	public static final SQLStmt getTxn = new SQLStmt("SELECT txn_time FROM user_recent_transactions "
			+ "WHERE userid = ? AND user_txn_id = ?;");

	public static final SQLStmt addTxn = new SQLStmt("INSERT INTO user_recent_transactions "
			+ "(userid, user_txn_id, txn_time, approved_amount,spent_amount,purpose) VALUES (?,?,NOW,?,?,?);");

	public static final SQLStmt insertUser = new SQLStmt(
			"INSERT INTO user_table (userid, user_json_object,user_last_seen) "
					+ "VALUES (?,?,?);");

	public static final SQLStmt reportAddcreditEvent = new SQLStmt(
			"INSERT INTO user_financial_events (userid,amount,user_txn_id,message) VALUES (?,?,?,?);");

	// @formatter:on

    public VoltTable[] run(long userId, long addBalance, String json, String purpose, TimestampType lastSeen,
            String txnId) throws VoltAbortException {

        long currentBalance = 0;

        voltQueueSQL(getUser, userId);
        voltQueueSQL(getTxn, userId, txnId);

        VoltTable[] results = voltExecuteSQL();

        if (results[1].advanceRow()) {

            this.setAppStatusCode(ReferenceData.STATUS_TXN_ALREADY_HAPPENED);
            this.setAppStatusString(
                    "Event already happened at " + results[1].getTimestampAsTimestamp("txn_time").toString());

        } else {

            voltQueueSQL(addTxn, userId, txnId, 0, addBalance, "Upsert user");

            if (!results[0].advanceRow()) {

                final String status = "Created user " + userId + " with opening credit of " + addBalance;
                voltQueueSQL(insertUser, userId, json, lastSeen);
                voltQueueSQL(reportAddcreditEvent, userId, addBalance, txnId, "user created");
                this.setAppStatusCode(ReferenceData.STATUS_OK);
                this.setAppStatusString(status);

            } else {

                final String status = "Updated user " + userId + " - added credit of " + addBalance + "; balance now "
                        + currentBalance;

                voltQueueSQL(reportAddcreditEvent, userId, addBalance, txnId, "user upserted");
                this.setAppStatusCode(ReferenceData.STATUS_OK);
                this.setAppStatusString(status);

            }

        }

        return voltExecuteSQL(true);
    }
}
