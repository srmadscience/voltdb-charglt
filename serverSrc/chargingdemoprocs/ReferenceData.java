/*
 * Copyright (C) 2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package chargingdemoprocs;


/**
 * Possible response codes.
 *
 */
public class ReferenceData {

    public static final byte STATUS_OK = 42;

    public static final byte STATUS_NO_MONEY = 43;
    public static final byte STATUS_SOME_UNITS_ALLOCATED = 44;
    public static final byte STATUS_ALL_UNITS_ALLOCATED = 45;
    public static final byte STATUS_TXN_ALREADY_HAPPENED = 46;
    public static final byte STATUS_USER_DOESNT_EXIST = 50;
    public static final byte STATUS_RECORD_ALREADY_SOFTLOCKED = 53;
    public static final byte STATUS_RECORD_HAS_BEEN_SOFTLOCKED = 54;
    public static final byte STATUS_CREDIT_ADDED = 56;

    public static final int LOCK_TIMEOUT_MS = 50;

}
