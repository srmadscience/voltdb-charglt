--
-- Copyright (C) 2025 Volt Active Data Inc.
--
-- Use of this source code is governed by an MIT
-- license that can be found in the LICENSE file or at
-- https://opensource.org/licenses/MIT.
--

file -inlinebatch END_OF_BATCH

DROP PROCEDURE GetUsersWithMultipleSessions IF EXISTS;

DROP PROCEDURE showTransactions IF EXISTS;

DROP procedure FindByLoyaltyCard IF EXISTS;

DROP PROCEDURE ShowCurrentAllocations__promBL IF EXISTS;

DROP PROCEDURE GetUser IF EXISTS;
   
DROP PROCEDURE GetAndLockUser IF EXISTS;
   
DROP PROCEDURE UpdateLockedUser IF EXISTS;
   
DROP PROCEDURE UpsertUser IF EXISTS;
   
DROP PROCEDURE DelUser IF EXISTS;
   
DROP PROCEDURE ReportQuotaUsage IF EXISTS;  
   
DROP PROCEDURE AddCredit IF EXISTS;  

DROP view current_locks IF EXISTS; 

DROP view user_balance IF EXISTS; 

DROP view allocated_credit IF EXISTS;

DROP view recent_activity_in IF EXISTS;

DROP view recent_activity_out IF EXISTS;

DROP view cluster_activity_by_users IF EXISTS;

DROP view cluster_activity IF EXISTS;

DROP view last_cluster_activity IF EXISTS;

DROP view cluster_users IF EXISTS;

DROP view users_sessions IF EXISTS;
   
DROP table user_table IF EXISTS;
DROP table user_usage_table IF EXISTS;
DROP table user_recent_transactions IF EXISTS;
DROP STREAM user_financial_events IF EXISTS;





END_OF_BATCH
