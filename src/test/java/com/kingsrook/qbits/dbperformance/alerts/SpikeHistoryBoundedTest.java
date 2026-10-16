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

package com.kingsrook.qbits.dbperformance.alerts;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.DbPerfTestSupport;
import com.kingsrook.qbits.dbperformance.analyze.RuleContext;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Spike detection reads seven days of query snapshots (~230,000 rows in kof22)
 ** on every evaluation. It must page them and keep only per-statement numbers.
 *******************************************************************************/
class SpikeHistoryBoundedTest
{
   private static final AtomicInteger MAX_PAGE = new AtomicInteger();
   private static final AtomicInteger LOADED   = new AtomicInteger();



   /*******************************************************************************
    ** Records the largest page QQQ hands back.
    *******************************************************************************/
   public static class CountingCustomizer implements TableCustomizerInterface
   {
      /***************************************************************************
       ** Count, change nothing.
       ***************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface queryInput, List<QRecord> records)
      {
         LOADED.addAndGet(records.size());
         MAX_PAGE.accumulateAndGet(records.size(), Math::max);
         return (records);
      }
   }



   /*******************************************************************************
    ** 2,500 samples across page boundaries: pages stay at BATCH_SIZE, invalid
    ** snapshots and the window are honored, the current interval is found.
    *******************************************************************************/
   @Test
   void historyIsPagedAndExact() throws Exception
   {
      DbPerfRuntime runtime = DbPerfRuntime.of(DbPerfTestSupport.instance());
      String backend = runtime.getConfig().getBackendName();
      runtime.getInstance().getTable("db_perf_query_snapshot").withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(CountingCustomizer.class));
      Instant now = Instant.parse("2026-10-16T20:00:00Z");

      QRecord ok = DbPerfStore.insert("db_perf_snapshot", List.of(new QRecord().withValue("backendName", backend).withValue("capturedAt", now.minusSeconds(3600)).withValue("status", "OK"))).getFirst();
      QRecord failed = DbPerfStore.insert("db_perf_snapshot", List.of(new QRecord().withValue("backendName", backend).withValue("capturedAt", now.minusSeconds(1800)).withValue("status", "FAILED"))).getFirst();
      QRecord current = DbPerfStore.insert("db_perf_snapshot", List.of(new QRecord().withValue("backendName", backend).withValue("capturedAt", now).withValue("status", "OK"))).getFirst();

      int one = statement(backend, 1L);
      int two = statement(backend, 2L);
      List<QRecord> rows = new ArrayList<>();
      for(int i = 0; i < 2500; i++)
      {
         rows.add(sample(ok, i % 2 == 0 ? one : two, now.minusSeconds(3600 + i), i));
      }
      rows.add(sample(failed, one, now.minusSeconds(1800), 999999));
      rows.add(sample(ok, one, now.minusSeconds(8 * 86400L), 999999));
      rows.add(sample(current, one, now, 42));
      DbPerfStore.insert("db_perf_query_snapshot", rows);
      MAX_PAGE.set(0);
      LOADED.set(0);

      RuleContext ctx = new RuleContext(runtime, backend, new QRecord(), DbPerfStore.first("db_perf_settings", new com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter()), now);
      Map<Integer, SnapshotEvaluator.StatementHistory> history = SnapshotEvaluator.history(ctx, current.getValueInteger("id"));

      assertThat(MAX_PAGE.get()).as("largest page of query snapshots held at once").isLessThanOrEqualTo(DbPerfStore.BATCH_SIZE);
      assertThat(history.get(one).sortedPrevious()).hasSize(1250).doesNotContain(999999.0);
      assertThat(history.get(two).sortedPrevious()).hasSize(1250);
      assertThat(history.get(one).sortedPrevious()[0]).isEqualTo(0.0);
      assertThat(history.get(one)).extracting("current").isEqualTo(42.0);
      assertThat(history.get(two)).extracting("current").isNull();
   }



   /*******************************************************************************
    ** One statement identity; returns its id.
    *******************************************************************************/
   private static int statement(String backend, long queryId) throws Exception
   {
      return (DbPerfStore.insert("db_perf_statement", List.of(new QRecord().withValue("backendName", backend).withValue("queryId", queryId)
         .withValue("queryText", "select " + queryId).withValue("fingerprint", String.valueOf(queryId).repeat(64).substring(0, 64)))).getFirst().getValueInteger("id"));
   }



   /*******************************************************************************
    ** One query snapshot row.
    *******************************************************************************/
   private static QRecord sample(QRecord snapshot, int statementId, Instant at, int totalMs)
   {
      return (new QRecord().withValue("snapshotId", snapshot.getValueInteger("id")).withValue("statementId", statementId)
         .withValue("capturedAt", at).withValue("totalMs", BigDecimal.valueOf(totalMs)));
   }
}
