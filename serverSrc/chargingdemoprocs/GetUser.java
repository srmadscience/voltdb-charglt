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

public class GetUser extends VoltProcedure {

    // @formatter:off

	public static final SQLStmt getUser = new SQLStmt("SELECT * FROM user_table WHERE userid = ?;");
	public static final SQLStmt getUserUsage = new SQLStmt(
			"SELECT * FROM user_usage_table WHERE userid = ? ORDER BY sessionid;");
	public static final SQLStmt getUserBalance = new SQLStmt("SELECT * FROM user_balance WHERE userid = ?;");
	public static final SQLStmt getAllTxn = new SQLStmt("SELECT * FROM user_recent_transactions "
			+ "WHERE userid = ? ORDER BY txn_time, user_txn_id;");

	// @formatter:on

    /**
     * Gets all the information we have about a user.
     *
     * @param userId
     * @return
     * @throws VoltAbortException
     */
    public VoltTable[] run(long userId) throws VoltAbortException {

        voltQueueSQL(getUser, userId);
        voltQueueSQL(getUserUsage, userId);
        voltQueueSQL(getUserBalance, userId);
        voltQueueSQL(getAllTxn, userId);

        return voltExecuteSQL(true);

    }
}
