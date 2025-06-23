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

public class GetAndLockUser extends VoltProcedure {

  // @formatter:off

    public static final SQLStmt getUser = new SQLStmt("SELECT * FROM user_table WHERE userid = ?;");

    public static final SQLStmt getAllTxn = new SQLStmt("SELECT user_txn_id, txn_time "
        + "FROM user_recent_transactions "
        + "WHERE userid = ? ORDER BY txn_time, user_txn_id;");

	public static final SQLStmt getUserUsage = new SQLStmt(
			"SELECT * FROM user_usage_table WHERE userid = ? ORDER BY sessionid;");

    public static final SQLStmt upsertUserLock = new SQLStmt("UPDATE user_table "
        + "SET user_softlock_sessionid = ? "
        + "   ,user_softlock_expiry = DATEADD(MILLISECOND,?,?) "
        + "WHERE userid = ?;");

    // @formatter:on

    /**
     * Gets all the information we have about a user, while adding an expiring
     * timestamp and an internally generated lock id that is used to do updates.
     *
     * @param userId
     * @return lockid (accessibe via ClientStatus.getAppStatusString())
     * @throws VoltAbortException
     */
    public VoltTable[] run(long userId) throws VoltAbortException {

        voltQueueSQL(getUser, userId);

        VoltTable[] userRecord = voltExecuteSQL();

        // Sanity check: Does this user exist?
        if (!userRecord[0].advanceRow()) {
            throw new VoltAbortException("User " + userId + " does not exist");
        }

        final TimestampType currentTimestamp = new TimestampType(this.getTransactionTime());
        final TimestampType lockingSessionExpiryTimestamp = userRecord[0]
                .getTimestampAsTimestamp("user_softlock_expiry");

        // If somebody has locked this session and the lock hasn't expired complain...
        if (lockingSessionExpiryTimestamp != null && lockingSessionExpiryTimestamp.compareTo(currentTimestamp) > 0) {

            final long lockingSessionId = userRecord[0].getLong("user_softlock_sessionid");
            this.setAppStatusCode(ReferenceData.STATUS_RECORD_ALREADY_SOFTLOCKED);
            this.setAppStatusString("User " + userId + " has already been locked by session " + lockingSessionId);

        } else {
            // 'Lock' record
            final long lockingSessionId = getUniqueId();
            this.setAppStatusCode(ReferenceData.STATUS_RECORD_HAS_BEEN_SOFTLOCKED);

            // Note how we pass the lock ID back...
            this.setAppStatusString("" + lockingSessionId);
            voltQueueSQL(upsertUserLock, getUniqueId(), ReferenceData.LOCK_TIMEOUT_MS, currentTimestamp, userId);
        }

        voltQueueSQL(getUser, userId);
        voltQueueSQL(getAllTxn, userId);
        voltQueueSQL(getUserUsage, userId);

        return voltExecuteSQL(true);

    }
}
