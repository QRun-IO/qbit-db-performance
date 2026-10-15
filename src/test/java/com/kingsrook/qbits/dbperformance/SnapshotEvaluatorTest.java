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


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.dbperformance.alerts.SnapshotEvaluator;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.collect.SnapshotCollector;
import com.kingsrook.qqq.backend.core.alerts.AlertResolution;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.AlertSinkInterface;
import com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData;
import com.kingsrook.qqq.backend.core.alerts.QAlertTypeMetaData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Captured core signals verify stable keys, baseline math, recovery and gating.
 *******************************************************************************/
class SnapshotEvaluatorTest
{
   /*******************************************************************************
    ** Core constructs this sink, with captured signals reset between fixtures.
    *******************************************************************************/
   public static class CapturingSink implements AlertSinkInterface
   {
      static final List<AlertSignal> RAISED = new ArrayList<>();
      static final List<AlertResolution> RESOLVED = new ArrayList<>();


      /*******************************************************************************
       ** Capture raises.
       *******************************************************************************/
      @Override
      public void raise(AlertSignal signal)
      {
         RAISED.add(signal);
      }


      /*******************************************************************************
       ** Capture resolutions.
       *******************************************************************************/
      @Override
      public void resolve(AlertResolution resolution)
      {
         RESOLVED.add(resolution);
      }


      /*******************************************************************************
       ** These producers do not send heartbeat signals.
       *******************************************************************************/
      @Override
      public void heartbeat(String key, Instant at)
      {
         throw (new AssertionError("Unexpected heartbeat"));
      }
   }


   /*******************************************************************************
    ** Connection, blocking and collection failure recover on observed good data.
    *******************************************************************************/
   @Test
   void raisesResolvesAndGates() throws Exception
   {
      B1Fixture f = fixture();
      B1Fixture.insert("activity_sample", new QRecord().withValue("snapshotId", f.snapshot.getValueInteger("id")).withValue("capturedAt", B1Fixture.NOW)
         .withValue("reason", "BLOCKED").withValue("querySeconds", 600));
      SnapshotEvaluator evaluator = new SnapshotEvaluator(f.runtime);
      evaluator.evaluate(f.snapshot);
      assertThat(CapturingSink.RAISED).extracting(AlertSignal::getAlertTypeName).contains(SnapshotEvaluator.CONNECTION_SATURATION, SnapshotEvaluator.BLOCKED_CHAIN);
      assertThat(CapturingSink.RAISED).allSatisfy(signal -> assertThat(signal.getDedupKey()).startsWith(signal.getAlertTypeName().replace("dbperf.", "db_perf.").replace('-', '_') + ":" + f.backend));
      QRecord failed = B1Fixture.insert("snapshot", new QRecord().withValue("backendName", f.backend).withValue("capturedAt", B1Fixture.NOW.plusSeconds(300))
         .withValue("status", "FAILED").withValue("errorMessage", "Collection failed (SQLException). Check connectivity."));
      evaluator.evaluate(failed);
      assertThat(CapturingSink.RAISED).anySatisfy(signal ->
      {
         assertThat(signal.getAlertTypeName()).isEqualTo(SnapshotEvaluator.COLLECTION_FAILED);
         assertThat(signal.getSummary()).contains("SQLException");
      });
      QRecord healthy = B1Fixture.insert("snapshot", new QRecord().withValue("backendName", f.backend).withValue("capturedAt", B1Fixture.NOW.plusSeconds(600))
         .withValue("status", "OK").withValue("connectionsTotal", 10).withValue("maxConnections", 100));
      evaluator.evaluate(healthy);
      assertThat(CapturingSink.RESOLVED).extracting(AlertResolution::getAlertTypeName).contains(SnapshotEvaluator.COLLECTION_FAILED, SnapshotEvaluator.CONNECTION_SATURATION, SnapshotEvaluator.BLOCKED_CHAIN);
      CapturingSink.RAISED.clear();
      CapturingSink.RESOLVED.clear();
      DbPerfStore.update("db_perf_settings", List.of(new QRecord().withValue("id", f.settings.getValueInteger("id")).withValue("alertsEnabled", false)));
      evaluator.evaluate(failed);
      evaluator.evaluate(f.snapshot);
      assertThat(CapturingSink.RAISED).isEmpty();
      assertThat(CapturingSink.RESOLVED).isEmpty();
      for(String type : List.of(SnapshotEvaluator.COLLECTION_FAILED, SnapshotEvaluator.QUERY_SPIKE, SnapshotEvaluator.CONNECTION_SATURATION, SnapshotEvaluator.BLOCKED_CHAIN))
      {
         assertThat(QAlertTypeMetaData.of(f.runtime.getInstance(), type).getDescription()).isNotBlank();
      }
   }


   /*******************************************************************************
    ** Twelve prior samples, median rather than mean, recovery and absence expiry.
    *******************************************************************************/
   @Test
   void spikeBaselineAndRecovery() throws Exception
   {
      B1Fixture f = fixture();
      SnapshotEvaluator evaluator = new SnapshotEvaluator(f.runtime);
      for(int i = 1; i <= 12; i++)
      {
         Instant at = B1Fixture.NOW.minusSeconds(i * 300L);
         QRecord snapshot = B1Fixture.insert("snapshot", new QRecord().withValue("backendName", f.backend).withValue("capturedAt", at).withValue("status", "OK"));
         B1Fixture.insert("query_snapshot", new QRecord().withValue("snapshotId", snapshot.getValueInteger("id")).withValue("statementId", f.statement.getValueInteger("id"))
            .withValue("capturedAt", at).withValue("totalMs", i == 12 ? 1000000 : 1000));
         if(i == 11)
         {
            evaluator.evaluate(f.snapshot);
            assertThat(CapturingSink.RAISED).noneSatisfy(signal -> assertThat(signal.getAlertTypeName()).isEqualTo(SnapshotEvaluator.QUERY_SPIKE));
         }
      }
      DbPerfStore.update("db_perf_statement", List.of(new QRecord().withValue("id", f.statement.getValueInteger("id"))
         .withValue("queryText", "select * from audit_orders where name = 'private patient' and id = 123 " + " and status = ?".repeat(50))));
      evaluator.evaluate(f.snapshot);
      String key = "db_perf.query_spike:" + f.backend + ":" + f.statement.getValueInteger("id");
      AlertSignal spike = CapturingSink.RAISED.stream().filter(signal -> signal.getDedupKey().equals(key)).findFirst().orElseThrow();
      assertThat(spike.getSummary()).contains("prior median 1000.0 ms", "Statement " + f.statement.getValueInteger("id")).doesNotContain("private patient", "123");
      assertThat(spike.getSummary().length()).isLessThan(300);
      CapturingSink.RESOLVED.clear();
      QRecord recovered = B1Fixture.insert("snapshot", new QRecord().withValue("backendName", f.backend).withValue("capturedAt", B1Fixture.NOW.plusSeconds(300)).withValue("status", "OK"));
      B1Fixture.insert("query_snapshot", new QRecord().withValue("snapshotId", recovered.getValueInteger("id")).withValue("statementId", f.statement.getValueInteger("id"))
         .withValue("capturedAt", B1Fixture.NOW.plusSeconds(300)).withValue("totalMs", 100));
      evaluator.evaluate(recovered);
      assertThat(CapturingSink.RESOLVED).extracting(AlertResolution::getDedupKey).contains(key);
      CapturingSink.RESOLVED.clear();
      QRecord absent = B1Fixture.insert("snapshot", new QRecord().withValue("backendName", f.backend).withValue("capturedAt", B1Fixture.NOW.plusSeconds(3899)).withValue("status", "OK"));
      evaluator.evaluate(absent);
      assertThat(CapturingSink.RESOLVED).extracting(AlertResolution::getDedupKey).doesNotContain(key);
      absent.withValue("capturedAt", B1Fixture.NOW.plusSeconds(3900));
      new SnapshotEvaluator(f.runtime).evaluate(absent);
      assertThat(CapturingSink.RESOLVED).extracting(AlertResolution::getDedupKey).contains(key);
   }


   /*******************************************************************************
    ** Partial statistics still report connection pressure, but cannot clear a
    ** collection failure. A low query time can recover without a full baseline.
    *******************************************************************************/
   @Test
   void partialSnapshotsAndRecoveryWithoutBaseline() throws Exception
   {
      B1Fixture f = fixture();
      SnapshotEvaluator evaluator = new SnapshotEvaluator(f.runtime);
      evaluator.evaluate(f.snapshot.withValue("status", "PARTIAL"));
      assertThat(CapturingSink.RAISED).extracting(AlertSignal::getAlertTypeName).contains(SnapshotEvaluator.CONNECTION_SATURATION);
      assertThat(CapturingSink.RESOLVED).extracting(AlertResolution::getAlertTypeName).doesNotContain(SnapshotEvaluator.COLLECTION_FAILED);
      QRecord query = DbPerfStore.first("db_perf_query_snapshot", DbPerfStore.eq("snapshotId", f.snapshot.getValueInteger("id")));
      DbPerfStore.update("db_perf_query_snapshot", List.of(new QRecord().withValue("id", query.getValueInteger("id")).withValue("totalMs", 1)));
      evaluator.evaluate(f.snapshot.withValue("status", "OK"));
      assertThat(CapturingSink.RESOLVED).extracting(AlertResolution::getDedupKey).contains("db_perf.query_spike:" + f.backend + ":" + f.statement.getValueInteger("id"));
   }


   /*******************************************************************************
    ** Collector invokes evaluation after committing; no sink installation needed.
    *******************************************************************************/
   @Test
   void collectorIntegration() throws Exception
   {
      B1Fixture f = fixture();
      new SnapshotCollector(f.runtime, B1Fixture.CLOCK, null).collect(f.backend, true);
      assertThat(CapturingSink.RAISED).extracting(AlertSignal::getAlertTypeName).contains(SnapshotEvaluator.COLLECTION_FAILED);
   }


   /*******************************************************************************
    ** Register a clean capturing sink on the migrated instance.
    *******************************************************************************/
   private static B1Fixture fixture() throws Exception
   {
      B1Fixture f = new B1Fixture();
      CapturingSink.RAISED.clear();
      CapturingSink.RESOLVED.clear();
      AlertsInstanceMetaData.ofOrWithNew(f.runtime.getInstance()).withSink(new QCodeReference(CapturingSink.class));
      return (f);
   }
}
