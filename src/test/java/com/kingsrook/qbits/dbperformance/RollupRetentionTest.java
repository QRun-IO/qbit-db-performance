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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.dbperformance.collect.DbPerfSettingsService;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.collect.RetentionService;
import com.kingsrook.qbits.dbperformance.collect.RollupService;
import com.kingsrook.qbits.dbperformance.collect.SnapshotCollector;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Daily replacement and keyset retention integration using real QQQ/H2 writes.
 *******************************************************************************/
class RollupRetentionTest
{
   /*******************************************************************************
    ** Weighted query aggregates, last table gauges and null-safe attribution keys.
    *******************************************************************************/
   @Test
   void rollupIsIdempotent() throws Exception
   {
      DbPerfRuntime runtime = DbPerfRuntime.of(DbPerfTestSupport.instance());
      DbPerfFixtureProvider provider = new DbPerfFixtureProvider();
      snapshot(runtime, provider, 0);
      provider.calls = 15;
      snapshot(runtime, provider, 300);
      provider.calls = 25;
      snapshot(runtime, provider, 600);
      QRecord attribution = new QRecord().withValue("windowStart", DbPerfFixtureProvider.START).withValue("windowEnd", DbPerfFixtureProvider.START.plusSeconds(60))
         .withValue("podName", "one").withValue("unitKind", "PROCESS").withValue("unitName", "example").withValue("operation", "SELECT").withValue("fingerprint", "a".repeat(64))
         .withValue("sqlText", "select ?").withValue("units", 2L).withValue("statements", 5L).withValue("totalMs", BigDecimal.TEN).withValue("maxMs", BigDecimal.valueOf(4)).withValue("rowsCount", 3L).withValue("errors", 1L);
      QRecord second = new QRecord(attribution).withValue("podName", "two");
      DbPerfStore.insert("db_perf_attribution", List.of(attribution, second));
      RollupService rollup = new RollupService(runtime);
      LocalDate day = LocalDate.of(2026, 10, 1);
      assertThat(rollup.needsBackfill(day)).isTrue();
      rollup.rollupDay(day);
      assertThat(rollup.needsBackfill(day)).isFalse();
      rollup.rollupDay(day);
      List<QRecord> daily = DbPerfStore.query("db_perf_query_daily", new QQueryFilter());
      assertThat(daily).hasSize(2);
      QRecord first = daily.stream().filter(row -> row.getValueBigDecimal("totalMs").compareTo(BigDecimal.valueOf(150)) == 0).findFirst().orElseThrow();
      assertThat(first.getValueLong("calls")).isEqualTo(15);
      assertThat(first.getValueBigDecimal("meanMs")).isEqualByComparingTo("10");
      assertThat(first.getValueBigDecimal("hitRatio")).isEqualByComparingTo("90");
      assertThat(first.getValueBigDecimal("shareOfDbTime")).isEqualByComparingTo("66.6667");
      QRecord table = DbPerfStore.query("db_perf_table_daily", new QQueryFilter()).getFirst();
      assertThat(table.getValueLong("seqScan")).isEqualTo(15);
      assertThat(table.getValueLong("totalBytes")).isEqualTo(2500);
      List<QRecord> attributed = DbPerfStore.query("db_perf_attribution_daily", new QQueryFilter());
      assertThat(attributed).hasSize(1);
      assertThat(attributed.getFirst().getValueLong("units")).isEqualTo(4);
      assertThat(attributed.getFirst().getValueLong("statements")).isEqualTo(10);
      assertThat(attributed.getFirst().getValueBigDecimal("totalMs")).isEqualByComparingTo("20");
      assertThat(attributed.getFirst().getValueString("rollupKey")).hasSize(64);
      rollup.rollupDay(day.plusDays(1));
      assertThat(DbPerfStore.query("db_perf_query_daily", new QQueryFilter())).hasSize(2);
      QRecord nextWeek = new SnapshotCollector(runtime, Clock.fixed(DbPerfFixtureProvider.START.plusSeconds(7 * 86400L), ZoneOffset.UTC), provider)
         .collect(runtime.getConfig().getBackendName(), true);
      assertThat(nextWeek.getValueString("status")).isEqualTo("OK");
      assertThat(DbPerfStore.query("db_perf_table_stat", new QQueryFilter()).getFirst().getValueLong("growthBytes7d")).isEqualTo(1500);
   }


   /*******************************************************************************
    ** More than one deletion batch, strict cutoff boundaries and open findings.
    *******************************************************************************/
   @Test
   void retentionBatchesAndClosedFindings() throws Exception
   {
      DbPerfRuntime runtime = DbPerfRuntime.of(DbPerfTestSupport.instance());
      String backend = runtime.getConfig().getBackendName();
      QRecord settings = new DbPerfSettingsService(runtime).ensure(backend);
      settings.withValue("detailRetentionDays", 2).withValue("rollupRetentionDays", 5);
      DbPerfStore.update("db_perf_settings", List.of(settings));
      Instant now = DbPerfFixtureProvider.START;
      List<QRecord> rows = new ArrayList<>();
      for(int i = 0; i < 1005; i++)
      {
         rows.add(new QRecord().withValue("windowStart", now.minusSeconds(3 * 86400)).withValue("unitName", "old" + i));
      }
      rows.add(new QRecord().withValue("windowStart", now.minusSeconds(2 * 86400)).withValue("unitName", "boundary"));
      DbPerfStore.insert("db_perf_attribution", rows);
      DbPerfStore.insert("db_perf_repeat_pattern", List.of(
         new QRecord().withValue("windowStart", now.minusSeconds(3 * 86400)),
         new QRecord().withValue("windowStart", now.minusSeconds(2 * 86400))));
      DbPerfStore.insert("db_perf_attribution_daily", List.of(new QRecord().withValue("day", LocalDate.of(2026, 9, 20)).withValue("rollupKey", "old")));
      int rule = DbPerfStore.query("db_perf_rule", new QQueryFilter()).getFirst().getValueInteger("id");
      List<QRecord> findings = new ArrayList<>();
      for(String status : List.of("OPEN", "ACKNOWLEDGED", "RESOLVED", "IGNORED"))
      {
         findings.add(new QRecord().withValue("backendName", backend).withValue("ruleId", rule).withValue("dedupeKey", status).withValue("status", status).withValue("lastSeenAt", now.minusSeconds(6 * 86400)));
      }
      DbPerfStore.insert("db_perf_finding", findings);
      RetentionService retention = new RetentionService(runtime, Clock.fixed(now, ZoneOffset.UTC));
      assertThat(retention.purge()).isEqualTo(1009);
      assertThat(retention.purge()).isZero();
      assertThat(DbPerfStore.query("db_perf_repeat_pattern", new QQueryFilter())).hasSize(1);
      assertThat(DbPerfStore.query("db_perf_attribution", new QQueryFilter())).singleElement().satisfies(row -> assertThat(row.getValueString("unitName")).isEqualTo("boundary"));
      assertThat(DbPerfStore.query("db_perf_finding", new QQueryFilter())).extracting(row -> row.getValueString("status")).containsExactlyInAnyOrder("OPEN", "ACKNOWLEDGED");
   }


   /*******************************************************************************
    ** Timestamped forced table capture.
    *******************************************************************************/
   private static void snapshot(DbPerfRuntime runtime, DbPerfFixtureProvider provider, int seconds)
   {
      QRecord result = new SnapshotCollector(runtime, Clock.fixed(DbPerfFixtureProvider.START.plusSeconds(seconds), ZoneOffset.UTC), provider).collect(runtime.getConfig().getBackendName(), true);
      assertThat(result.getValueString("status")).as(result.getValueString("errorMessage")).isEqualTo("OK");
   }
}
