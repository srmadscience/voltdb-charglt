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

public class DelUser extends VoltProcedure {

    // @formatter:off

	public static final SQLStmt delUser = new SQLStmt("DELETE FROM user_table WHERE userid = ?;");
	public static final SQLStmt delUserUsage = new SQLStmt("DELETE FROM user_usage_table WHERE userid = ?;");
	public static final SQLStmt delBalance = new SQLStmt("DELETE FROM user_balance WHERE userid = ?;");
	public static final SQLStmt delTxns = new SQLStmt("DELETE FROM user_recent_transactions WHERE userid = ?;");

	// @formatter:on

    /**
     * Deletes all information we have about a user.
     *
     * @param userId
     * @return
     * @throws VoltAbortException
     */
    public VoltTable[] run(long userId) throws VoltAbortException {

        voltQueueSQL(delUser, userId);
        voltQueueSQL(delUserUsage, userId);
        voltQueueSQL(delBalance, userId);
        voltQueueSQL(delTxns, userId);

        return voltExecuteSQL(true);
    }
}
