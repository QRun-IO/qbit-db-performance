/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qbits.dbperformance;


import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dbperformance.provider.ConnectionCount;
import com.kingsrook.qbits.dbperformance.provider.DatabaseStats;
import com.kingsrook.qbits.dbperformance.provider.IndexStats;
import com.kingsrook.qbits.dbperformance.provider.ProviderCapabilities;
import com.kingsrook.qbits.dbperformance.provider.SessionInfo;
import com.kingsrook.qbits.dbperformance.provider.StatementResetInfo;
import com.kingsrook.qbits.dbperformance.provider.StatementStats;
import com.kingsrook.qbits.dbperformance.provider.TableStats;
import com.kingsrook.qbits.dbperformance.provider.UnsupportedStatsProvider;


/*******************************************************************************
 ** Deterministic statistics stream for real QQQ/H2 service integration tests.
 *******************************************************************************/
public class DbPerfFixtureProvider extends UnsupportedStatsProvider
{
   public static final Instant START = Instant.parse("2026-10-01T12:00:00Z");
   public long calls = 10;
   public Instant reset = START.minusSeconds(3600);
   public boolean limited;
   public boolean fail;
   public Error   error;


   /*******************************************************************************
    ** Supported full or reduced access.
    *******************************************************************************/
   @Override
   public ProviderCapabilities capabilities(Connection connection)
   {
      return (new ProviderCapabilities(true, 170000, true, !limited, limited ? "NO_READ_ALL_STATS" : "FULL", limited ? "Grant pg_read_all_stats" : "Full statistics access"));
   }


   /*******************************************************************************
    ** PostgreSQL identity.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return ("POSTGRES");
   }


   /*******************************************************************************
    ** Cumulative database counters.
    *******************************************************************************/
   @Override
   public DatabaseStats readDatabase(Connection connection)
   {
      return (new DatabaseStats(Map.of("xactCommit", calls, "xactRollback", 0L, "blksHit", calls * 9, "blksRead", calls, "tempFiles", 0L, "tempBytes", 0L, "deadlocks", 0L, "conflicts", 0L), reset, 100, 4096));
   }


   /*******************************************************************************
    ** Two shapes with different total time exercise top-N denominators.
    *******************************************************************************/
   @Override
   public List<StatementStats> readStatements(Connection connection)
   {
      if(error != null)
      {
         throw (error);
      }
      return (List.of(new StatementStats(1, "app", true, "select * from public.accounts where id=$1", calls, BigDecimal.valueOf(calls * 10), BigDecimal.TEN, BigDecimal.valueOf(15), calls, calls * 9, calls, 0, reset, reset),
         new StatementStats(2, "app", true, "select * from public.accounts where name='private'", calls, BigDecimal.valueOf(calls * 5), BigDecimal.valueOf(5), BigDecimal.TEN, calls, calls, calls, 2, reset, reset)));
   }


   /*******************************************************************************
    ** Statement reset epoch.
    *******************************************************************************/
   @Override
   public StatementResetInfo readStatementResetInfo(Connection connection)
   {
      return (new StatementResetInfo(reset, 0));
   }


   /*******************************************************************************
    ** A failure here occurs after statement writes, proving atomic rollback.
    *******************************************************************************/
   @Override
   public List<TableStats> readTables(Connection connection)
   {
      if(fail)
      {
         throw (new IllegalStateException("raw SQL contains private patient text"));
      }
      return (List.of(new TableStats("public", "accounts", 44, Map.of("seqScan", calls, "idxScan", calls * 3, "nTupIns", calls, "nLiveTup", calls, "nDeadTup", 1L, "totalBytes", calls * 100, "tableBytes", calls * 90, "indexBytes", calls * 10, "freezeAge", 100L), Map.of("lastAutovacuum", START))));
   }


   /*******************************************************************************
    ** Index gauge linked to the collected table.
    *******************************************************************************/
   @Override
   public List<IndexStats> readIndexes(Connection connection)
   {
      return (List.of(new IndexStats("public", "accounts", "accounts_pkey", 45, "CREATE UNIQUE INDEX accounts_pkey ON public.accounts(id)", "id", true, true, true, calls * 3, calls, calls, 1024, calls)));
   }


   /*******************************************************************************
    ** A blocked query, its idle blocker, and a long idle transaction.
    *******************************************************************************/
   @Override
   public List<SessionInfo> readSessions(Connection connection)
   {
      return (List.of(new SessionInfo(10, "app", "site", "127.0.0.1", "active", "Lock", "relation", "client backend", START, START, START, BigDecimal.valueOf(50), BigDecimal.valueOf(60), List.of(20), 1L, "select * from accounts where id=999"),
         new SessionInfo(20, "app", "site", null, "idle", null, null, "client backend", START, START, START, BigDecimal.TEN, BigDecimal.TEN, List.of(), null, "select 'private secret'"),
         new SessionInfo(30, "app", "site", null, "idle in transaction", null, null, "client backend", START, START, START.minusSeconds(120), BigDecimal.valueOf(100), BigDecimal.valueOf(100), List.of(), null, "select $$private secret$$")));
   }


   /*******************************************************************************
    ** Capacity gauges.
    *******************************************************************************/
   @Override
   public List<ConnectionCount> readConnections(Connection connection)
   {
      return (List.of(new ConnectionCount("app", "site", "active", 2), new ConnectionCount("app", "site", "idle", 3), new ConnectionCount("app", "site", "idle in transaction", 1)));
   }
}
