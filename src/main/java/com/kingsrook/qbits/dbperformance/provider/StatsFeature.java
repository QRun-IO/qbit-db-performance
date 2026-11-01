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

package com.kingsrook.qbits.dbperformance.provider;


import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;


/*******************************************************************************
 ** One area of database statistics. A provider declares which areas its vendor
 ** can supply ({@link DbStatsProvider#features()}) and which are available on a
 ** given connection ({@link ProviderCapabilities#features()}); presentation and
 ** analysis degrade per area instead of per vendor.
 *******************************************************************************/
public enum StatsFeature
{
   /** Transaction, block, temp-file and deadlock counters for the database. */
   DATABASE_COUNTERS("Database counters"),
   /** Buffer-cache hit and read counters (cache hit ratio). */
   CACHE_COUNTERS("Cache hit ratio"),
   /** Database size on disk. */
   DATABASE_SIZE("Database size"),
   /** Configured connection limit. */
   MAX_CONNECTIONS("Connection limit"),
   /** Cumulative per-statement calls, time and rows. */
   STATEMENTS("Statement statistics"),
   /** Per-statement shared-block and temp-block I/O. */
   STATEMENT_IO("Statement I/O"),
   /** A server-side reset time for statement statistics. */
   STATEMENT_RESET("Statement reset time"),
   /** Live row estimates per table. */
   TABLE_ROWS("Table row estimates"),
   /** Table, index and toast sizes. */
   TABLE_SIZES("Table sizes"),
   /** Sequential/index scan and tuple write counters per table. */
   TABLE_ACCESS("Table access counters"),
   /** Dead tuples, vacuum/analyze times and freeze age. */
   TABLE_MAINTENANCE("Table maintenance"),
   /** Index definitions and key columns. */
   INDEX_DEFINITIONS("Index definitions"),
   /** Per-index scan counters. */
   INDEX_USAGE("Index usage"),
   /** Per-index size. */
   INDEX_SIZES("Index sizes"),
   /** Live sessions with state and current statement. */
   SESSIONS("Live sessions"),
   /** Which session blocks which. */
   BLOCKING("Blocking"),
   /** Wait event type and name per session. */
   WAIT_EVENTS("Wait events"),
   /** Transaction start time per session. */
   TRANSACTION_AGE("Transaction age"),
   /** Connection counts by user, application and state. */
   CONNECTIONS("Connection counts"),
   /** Plans as structured JSON with costs (renderable tree). */
   EXPLAIN_JSON("Structured query plans"),
   /** Plans as vendor text (no cost tree). */
   EXPLAIN_TEXT("Text query plans"),
   /** Plans for parameterized SQL without values. */
   EXPLAIN_GENERIC("Generic plans"),
   /** Executing the query to measure the plan (ANALYZE). */
   EXPLAIN_ANALYZE("Explain analyze");

   private final String label;



   /*******************************************************************************
    ** Constructor.
    *******************************************************************************/
   StatsFeature(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Human-readable area name.
    *******************************************************************************/
   public String getLabel()
   {
      return (label);
   }



   /*******************************************************************************
    ** Immutable copy of a feature list.
    *******************************************************************************/
   public static Set<StatsFeature> of(StatsFeature... features)
   {
      EnumSet<StatsFeature> set = EnumSet.noneOf(StatsFeature.class);
      Collections.addAll(set, features);
      return (Collections.unmodifiableSet(set));
   }
}
