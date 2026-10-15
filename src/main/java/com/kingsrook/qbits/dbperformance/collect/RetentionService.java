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

package com.kingsrook.qbits.dbperformance.collect;


import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Configurable retention through bounded QQQ keyset deletes.
 *******************************************************************************/
public class RetentionService
{
   private final DbPerfRuntime runtime;
   private final Clock clock;


   /*******************************************************************************
    ** Production clock.
    *******************************************************************************/
   public RetentionService(DbPerfRuntime runtime)
   {
      this(runtime, Clock.systemUTC());
   }


   /*******************************************************************************
    ** Clock injection for exact boundary tests.
    *******************************************************************************/
   public RetentionService(DbPerfRuntime runtime, Clock clock)
   {
      this.runtime = runtime;
      this.clock = clock;
   }


   /*******************************************************************************
    ** Only closed old findings are removed; FK actions preserve open findings.
    *******************************************************************************/
   public int purge() throws Exception
   {
      QRecord settings = new DbPerfSettingsService(runtime).ensure(runtime.getConfig().getBackendName());
      Instant detailCutoff = clock.instant().minusSeconds(86400L * SnapshotCollector.positive(settings, "detailRetentionDays", 30));
      int rollupDays = SnapshotCollector.positive(settings, "rollupRetentionDays", 365);
      Instant staleCutoff = clock.instant().minusSeconds(86400L * rollupDays);
      LocalDate dayCutoff = LocalDate.now(clock.withZone(ZoneId.of(runtime.getConfig().getTimeZone()))).minusDays(rollupDays);
      return (runtime.asSystem(() ->
      {
         int count = 0;
         for(String table : List.of("db_perf_query_snapshot", "db_perf_table_snapshot", "db_perf_activity_sample", "db_perf_connection_sample"))
         {
            count += DbPerfStore.delete(table, before("capturedAt", detailCutoff));
         }
         for(String table : List.of("db_perf_attribution", "db_perf_repeat_pattern"))
         {
            count += DbPerfStore.delete(table, before("windowStart", detailCutoff));
         }
         count += DbPerfStore.delete("db_perf_explain", before("requestedAt", detailCutoff));
         count += DbPerfStore.delete("db_perf_snapshot", backend(before("capturedAt", detailCutoff)));
         for(String table : List.of("db_perf_query_daily", "db_perf_table_daily", "db_perf_attribution_daily"))
         {
            count += DbPerfStore.delete(table, new QQueryFilter(new QFilterCriteria("day", QCriteriaOperator.LESS_THAN, dayCutoff)));
         }
         for(String table : List.of("db_perf_index_stat", "db_perf_statement", "db_perf_table_stat"))
         {
            count += DbPerfStore.delete(table, backend(before("lastSeenAt", staleCutoff)));
         }
         count += DbPerfStore.delete("db_perf_finding", backend(before("lastSeenAt", staleCutoff))
            .withCriteria(new QFilterCriteria("status", QCriteriaOperator.IN, List.of("RESOLVED", "IGNORED"))));
         return (count);
      }));
   }


   /*******************************************************************************
    ** Scope backend-bearing rows to the configured monitored backend.
    *******************************************************************************/
   private QQueryFilter backend(QQueryFilter filter)
   {
      return (filter.withCriteria(new QFilterCriteria("backendName", QCriteriaOperator.EQUALS, runtime.getConfig().getBackendName())));
   }


   /*******************************************************************************
    ** Strict age boundary preserves records at the cutoff.
    *******************************************************************************/
   private static QQueryFilter before(String field, Instant cutoff)
   {
      return (new QQueryFilter(new QFilterCriteria(field, QCriteriaOperator.LESS_THAN, cutoff)));
   }
}
