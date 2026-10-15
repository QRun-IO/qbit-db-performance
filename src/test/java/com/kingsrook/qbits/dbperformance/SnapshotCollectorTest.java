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
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import com.kingsrook.qbits.dbperformance.collect.DbPerfSettingsService;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.collect.SnapshotCollector;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Real H2 QQQ actions with deterministic vendor samples: no mocked persistence.
 *******************************************************************************/
class SnapshotCollectorTest
{
   /*******************************************************************************
    ** Baseline, delta top-N, reset, tables, normalized activity and partial mode.
    *******************************************************************************/
   @Test
   void collectionLifecycle() throws Exception
   {
      QInstance instance = DbPerfTestSupport.instance();
      DbPerfRuntime runtime = DbPerfRuntime.of(instance);
      String backend = runtime.getConfig().getBackendName();
      QRecord settings = new DbPerfSettingsService(runtime).ensure(backend);
      settings.setValue("topStatementsPerSnapshot", 1);
      DbPerfStore.update("db_perf_settings", List.of(settings));
      assertThat(new DbPerfSettingsService(runtime).ensure(backend).getValueInteger("topStatementsPerSnapshot")).isEqualTo(1);
      DbPerfFixtureProvider provider = new DbPerfFixtureProvider();
      QRecord baseline = collect(runtime, provider, 0, true);
      assertThat(baseline.getValueString("status")).as(baseline.getValueString("errorMessage")).isEqualTo("OK");
      assertThat(baseline.getValueLong("statementCalls")).isZero();
      provider.calls = 15;
      QRecord delta = collect(runtime, provider, 300, false);
      assertThat(delta.getValueString("status")).as(delta.getValueString("errorMessage")).isEqualTo("OK");
      assertThat(delta.getValueLong("statementCalls")).isEqualTo(10);
      assertThat(delta.getValueBigDecimal("statementTotalMs")).isEqualByComparingTo("75");
      assertThat(delta.getValueInteger("storedStatementCount")).isEqualTo(1);
      assertThat(delta.getValueBoolean("includesTables")).isFalse();
      assertThat(delta.getValueLong("xactCommit")).isEqualTo(5);
      assertThat(delta.getValueBigDecimal("cacheHitRatio")).isEqualByComparingTo("90");
      assertThat(delta.getValueInteger("connectionsTotal")).isEqualTo(6);
      QRecord detail = DbPerfStore.query("db_perf_query_snapshot", new QQueryFilter()).getFirst();
      assertThat(detail.getValueBigDecimal("shareOfDbTime")).isEqualByComparingTo("66.6667");
      QRecord statement = DbPerfStore.first("db_perf_statement", DbPerfStore.eq("queryId", 1L));
      assertThat(statement.getValueLong("calls24h")).isEqualTo(5);
      assertThat(statement.getValueBigDecimal("totalMs24h")).isEqualByComparingTo("50");
      assertThat(statement.getValueBigDecimal("meanMs24h")).isEqualByComparingTo("10");
      assertThat(statement.getValueBigDecimal("shareOfDbTime24h")).isEqualByComparingTo("66.6667");
      assertThat(DbPerfStore.query("db_perf_activity_sample", new QQueryFilter())).hasSize(6).allSatisfy(row -> assertThat(row.getValueString("queryTextNormalized")).doesNotContain("private", "999"));
      assertThat(DbPerfStore.query("db_perf_table_stat", new QQueryFilter())).hasSize(1);
      assertThat(DbPerfStore.query("db_perf_index_stat", new QQueryFilter())).hasSize(1);
      provider.calls = 2;
      provider.reset = DbPerfFixtureProvider.START.plusSeconds(400);
      provider.limited = true;
      QRecord reset = collect(runtime, provider, 600, true);
      assertThat(reset.getValueString("status")).isEqualTo("PARTIAL");
      assertThat(reset.getValueBoolean("statsResetDetected")).isTrue();
      assertThat(reset.getValueLong("statementCalls")).isEqualTo(4);
      assertThat(new DbPerfSettingsService(runtime).ensure(backend).getValueString("statsAccess")).isEqualTo("NO_READ_ALL_STATS");
      assertThat(DbPerfStore.query("db_perf_table_snapshot", new QQueryFilter())).hasSize(2);
   }


   /*******************************************************************************
    ** Failed writes do not advance counters, and the caller session is restored.
    *******************************************************************************/
   @Test
   void failureAndDisabled() throws Exception
   {
      QInstance instance = DbPerfTestSupport.instance();
      DbPerfRuntime runtime = DbPerfRuntime.of(instance);
      DbPerfFixtureProvider provider = new DbPerfFixtureProvider();
      assertThat(collect(runtime, provider, 0, true).getValueString("status")).isEqualTo("OK");
      QSession caller = new QSession().withPermissions(DbPerfPermissions.VIEW);
      QContext.setQSession(caller);
      provider.calls = 20;
      provider.fail = true;
      QRecord failed = collect(runtime, provider, 300, true);
      assertThat(failed.getValueString("status")).isEqualTo("FAILED");
      assertThat(failed.getValueString("errorMessage")).doesNotContain("patient", "private");
      assertThat(QContext.getQSession()).isSameAs(caller);
      runtime.asSystem(() ->
      {
         assertThat(DbPerfStore.query("db_perf_statement", new QQueryFilter())).allSatisfy(row -> assertThat(row.getValueLong("lastCalls")).isEqualTo(10));
         assertThat(DbPerfStore.query("db_perf_query_snapshot", new QQueryFilter())).isEmpty();
         QRecord settings = new DbPerfSettingsService(runtime).ensure(runtime.getConfig().getBackendName());
         settings.setValue("enabled", false);
         DbPerfStore.update("db_perf_settings", List.of(settings));
         return (null);
      });
      assertThat(collect(runtime, provider, 600, true).getValueString("status")).isEqualTo("PARTIAL");
      assertThat(new SnapshotCollector(runtime).collect("wrong-backend", false).getValueString("status")).isEqualTo("FAILED");
   }


   /*******************************************************************************
    ** An Error (not just an Exception) still closes the run as FAILED, and runs
    ** left RUNNING by an earlier crash are closed once they are stale.
    *******************************************************************************/
   @Test
   void errorsAndAbandonedRuns() throws Exception
   {
      QInstance             instance = DbPerfTestSupport.instance();
      DbPerfRuntime         runtime  = DbPerfRuntime.of(instance);
      DbPerfFixtureProvider provider = new DbPerfFixtureProvider();
      runtime.asSystem(() -> DbPerfStore.insert("db_perf_snapshot", List.of(new QRecord().withValue("backendName", runtime.getConfig().getBackendName())
         .withValue("capturedAt", DbPerfFixtureProvider.START.minusSeconds(3600)).withValue("status", "RUNNING"))));

      provider.error = new StackOverflowError();
      QRecord failed = collect(runtime, provider, 0, true);
      assertThat(failed.getValueString("status")).isEqualTo("FAILED");
      assertThat(failed.getValueString("errorMessage")).contains("StackOverflowError");
      runtime.asSystem(() ->
      {
         List<QRecord> rows = DbPerfStore.query("db_perf_snapshot", new QQueryFilter());
         assertThat(rows).hasSize(2).allSatisfy(row -> assertThat(row.getValueString("status")).isEqualTo("FAILED"));
         assertThat(rows).anySatisfy(row -> assertThat(row.getValueString("errorMessage")).contains("did not finish"));
         return (null);
      });

      provider.error = null;
      assertThat(collect(runtime, provider, 300, true).getValueString("status")).isEqualTo("OK");
   }



   /*******************************************************************************
    ** H2 remains usable and explicitly unsupported by the production selector.
    *******************************************************************************/
   @Test
   void unsupportedVendor() throws Exception
   {
      DbPerfRuntime runtime = DbPerfRuntime.of(DbPerfTestSupport.instance());
      QRecord snapshot = new SnapshotCollector(runtime).collect(runtime.getConfig().getBackendName(), false);
      assertThat(snapshot.getValueString("status")).isEqualTo("PARTIAL");
      assertThat(snapshot.getValueString("errorMessage")).contains("not supported");
   }


   /*******************************************************************************
    ** A concurrent administrator edit is not overwritten by runtime status writes.
    *******************************************************************************/
   @Test
   void collectionPreservesConcurrentSettingsEdit() throws Exception
   {
      DbPerfRuntime runtime = DbPerfRuntime.of(DbPerfTestSupport.instance());
      QRecord settings = new DbPerfSettingsService(runtime).ensure(runtime.getConfig().getBackendName());
      DbPerfFixtureProvider provider = new DbPerfFixtureProvider()
      {
         /*******************************************************************************
          ** Simulate an independently committed edit after settings were read.
          *******************************************************************************/
         @Override
         public java.util.List<com.kingsrook.qbits.dbperformance.provider.ConnectionCount> readConnections(java.sql.Connection connection)
         {
            try
            {
               runtime.asSystem(() ->
               {
                  DbPerfStore.update("db_perf_settings", List.of(new QRecord().withValue("id", settings.getValueInteger("id")).withValue("topStatementsPerSnapshot", 17)));
                  return (null);
               });
            }
            catch(Exception e)
            {
               throw (new IllegalStateException(e));
            }
            return (super.readConnections(connection));
         }
      };
      assertThat(collect(runtime, provider, 0, true).getValueString("status")).isEqualTo("OK");
      assertThat(new DbPerfSettingsService(runtime).ensure(runtime.getConfig().getBackendName()).getValueInteger("topStatementsPerSnapshot")).isEqualTo(17);
   }


   /*******************************************************************************
    ** One timestamped pass.
    *******************************************************************************/
   private static QRecord collect(DbPerfRuntime runtime, DbPerfFixtureProvider provider, int seconds, boolean tables)
   {
      return (new SnapshotCollector(runtime, Clock.fixed(DbPerfFixtureProvider.START.plusSeconds(seconds), ZoneOffset.UTC), provider).collect(runtime.getConfig().getBackendName(), tables));
   }
}
