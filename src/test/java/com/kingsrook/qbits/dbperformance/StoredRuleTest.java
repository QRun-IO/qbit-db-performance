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
import java.util.Arrays;
import java.util.List;
import com.kingsrook.qbits.dbperformance.analyze.AnalyzeService;
import com.kingsrook.qbits.dbperformance.analyze.DbPerfRule;
import com.kingsrook.qbits.dbperformance.analyze.FindingDraft;
import com.kingsrook.qbits.dbperformance.analyze.FindingService;
import com.kingsrook.qbits.dbperformance.analyze.RuleContext;
import com.kingsrook.qbits.dbperformance.analyze.StoredRule;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Every seeded rule is exercised against migrated H2 rows, including lifecycle
 ** reconciliation and live edits of rule configuration.
 *******************************************************************************/
class StoredRuleTest
{
   /*******************************************************************************
    ** Created, deduped, acknowledged, ignored, resolved, reopened and disabled.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(StoredRule.class)
   void ruleLifecycle(StoredRule rule) throws Exception
   {
      B1Fixture f = new B1Fixture();
      List<FindingDraft> drafts = rule.evaluate(f.context(rule));
      assertThat(drafts).as(rule.code()).isNotEmpty().allSatisfy(d ->
      {
         assertThat(d.severity()).isEqualTo("WARNING");
         assertThat(d.evidence()).contains("| Measurement | Value |");
         assertThat(d.score()).isBetween(BigDecimal.valueOf(0.2), BigDecimal.valueOf(2.2));
      });
      FindingService service = new FindingService(f.runtime, B1Fixture.CLOCK);
      assertThat(service.apply(f.backend, rule.code(), drafts).created()).isEqualTo(drafts.size());
      assertThat(service.apply(f.backend, rule.code(), drafts).updated()).isEqualTo(drafts.size());
      List<QRecord> findings = RuleContext.read("db_perf_finding", new QQueryFilter());
      assertThat(findings).hasSize(drafts.size()).allSatisfy(r -> assertThat(r.getValueInteger("occurrences")).isEqualTo(2));
      QRecord first = findings.getFirst();
      DbPerfStore.update("db_perf_finding", List.of(new QRecord().withValue("id", first.getValueInteger("id")).withValue("status", "ACKNOWLEDGED")));
      service.apply(f.backend, rule.code(), drafts);
      assertThat(finding(first).getValueString("status")).isEqualTo("ACKNOWLEDGED");
      assertThat(service.apply(f.backend, rule.code(), List.of()).resolved()).isEqualTo(drafts.size());
      assertThat(finding(first).getValueString("statusNote")).isEqualTo("No longer detected");
      service.apply(f.backend, rule.code(), drafts);
      assertThat(finding(first).getValueString("status")).isEqualTo("OPEN");
      assertThat(finding(first).getValue("resolvedAt")).isNull();
      DbPerfStore.update("db_perf_finding", List.of(new QRecord().withValue("id", first.getValueInteger("id")).withValue("status", "IGNORED").withValue("statusNote", "Accepted")));
      service.apply(f.backend, rule.code(), drafts);
      service.apply(f.backend, rule.code(), List.of());
      assertThat(finding(first).getValueString("status")).isEqualTo("IGNORED");
      assertThat(finding(first).getValueString("statusNote")).isEqualTo("Accepted");
      QRecord row = f.context(rule).rule;
      DbPerfStore.update("db_perf_rule", List.of(new QRecord().withValue("id", row.getValueInteger("id")).withValue("enabled", false)));
      int occurrences = finding(first).getValueInteger("occurrences");
      assertThat(rule.evaluate(f.context(rule))).isEmpty();
      assertThat(service.apply(f.backend, rule.code(), drafts)).isEqualTo(new FindingService.Counts(0, 0, 0));
      assertThat(finding(first).getValueInteger("occurrences")).isEqualTo(occurrences);
   }


   /*******************************************************************************
    ** Changing thresholds in the persisted rule row changes the result.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(value = StoredRule.class, names = "DUPLICATE_INDEX", mode = EnumSource.Mode.EXCLUDE)
   void thresholdsAreData(StoredRule rule) throws Exception
   {
      B1Fixture f = new B1Fixture();
      QRecord row = f.context(rule).rule;
      assertThat(rule.evaluate(f.context(rule))).isNotEmpty();
      DbPerfStore.update("db_perf_rule", List.of(new QRecord().withValue("id", row.getValueInteger("id"))
         .withValue("threshold1", rule == StoredRule.CACHE_HIT_LOW ? 0 : 999999999).withValue("threshold2", 999999999)));
      assertThat(rule.evaluate(f.context(rule))).isEmpty();
   }


   /*******************************************************************************
    ** One failing rule cannot resolve its findings or abort subsequent rules.
    *******************************************************************************/
   @Test
   void failureIsolationAndSeverity() throws Exception
   {
      B1Fixture f = new B1Fixture();
      DbPerfRule broken = new DbPerfRule()
      {
         @Override
         public String code()
         {
            return ("SLOW_QUERY");
         }

         @Override
         public List<FindingDraft> evaluate(RuleContext context)
         {
            throw (new IllegalStateException("private SQL must not escape"));
         }
      };
      new FindingService(f.runtime).apply(f.backend, "SLOW_QUERY", StoredRule.SLOW_QUERY.evaluate(f.context(StoredRule.SLOW_QUERY)));
      var result = new AnalyzeService(f.runtime, B1Fixture.CLOCK, List.of(broken, StoredRule.BLOAT)).run(f.backend);
      assertThat(result.getFirst().error()).contains("IllegalStateException").doesNotContain("private SQL");
      assertThat(result.getLast().counts().created()).isEqualTo(1);
      assertThat(DbPerfStore.first("db_perf_finding", DbPerfStore.eq("ruleCode", "SLOW_QUERY")).getValueString("status")).isEqualTo("OPEN");
      QRecord row = f.context(StoredRule.SLOW_QUERY).rule;
      DbPerfStore.update("db_perf_rule", List.of(new QRecord().withValue("id", row.getValueInteger("id")).withValue("severity", "CRITICAL")));
      FindingDraft draft = StoredRule.SLOW_QUERY.evaluate(f.context(StoredRule.SLOW_QUERY)).getFirst();
      assertThat(draft.score()).isEqualByComparingTo("1.4");
      assertThat(draft.recommendation()).contains("Query plan", "?sql=EXPLAIN%20");
      assertThat(new AnalyzeService(f.runtime, B1Fixture.CLOCK, Arrays.asList(StoredRule.values())).run(f.backend)).allSatisfy(r -> assertThat(r.error()).isNull());
      assertThatThrownBy(() -> new AnalyzeService(f.runtime).run("other")).isInstanceOf(IllegalArgumentException.class);
   }


   /*******************************************************************************
    ** Invalid scopes and duplicate identities roll back the entire reconciliation.
    *******************************************************************************/
   @Test
   void atomicValidation() throws Exception
   {
      B1Fixture f = new B1Fixture();
      FindingService service = new FindingService(f.runtime);
      FindingDraft draft = StoredRule.SLOW_QUERY.evaluate(f.context(StoredRule.SLOW_QUERY)).getFirst();
      assertThatThrownBy(() -> service.apply(f.backend, "SLOW_QUERY", List.of(draft, draft))).isInstanceOf(IllegalArgumentException.class);
      assertThat(RuleContext.read("db_perf_finding", new QQueryFilter())).isEmpty();
      assertThatThrownBy(() -> service.apply(f.backend, "BLOAT", List.of(draft))).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> service.apply("other", "SLOW_QUERY", List.of(draft))).isInstanceOf(IllegalArgumentException.class);
      assertThat(service.apply(f.backend, "NO_SUCH_RULE", List.of())).isEqualTo(new FindingService.Counts(0, 0, 0));
   }


   /*******************************************************************************
    ** Unrelated backends, old samples and future samples cannot influence rules.
    *******************************************************************************/
   @Test
   void evidenceWindowsAndIndexSafety() throws Exception
   {
      B1Fixture f = new B1Fixture();
      FindingDraft missing = StoredRule.MISSING_INDEX.evaluate(f.context(StoredRule.MISSING_INDEX)).getFirst();
      assertThat(missing.migrationSnippet()).contains("(status)").doesNotContain("(customer_id");
      assertThat(StoredRule.POOL_SIZING.evaluate(f.context(StoredRule.POOL_SIZING)).getFirst().recommendation()).contains("orders-app: peak 90");
      assertThat(StoredRule.OVER_AUDITED.evaluate(f.context(StoredRule.OVER_AUDITED)).getFirst().recommendation()).contains("shipOrders");
      QRecord unique = f.index("idx_unique", "status", true, 0);
      QRecord partial = f.index("idx_partial", "customer_id", false, 1);
      DbPerfStore.update("db_perf_index_stat", List.of(new QRecord().withValue("id", partial.getValueInteger("id"))
         .withValue("definition", "CREATE INDEX idx_partial ON public.audit_orders USING btree (customer_id) WHERE status = 1")));
      assertThat(StoredRule.DUPLICATE_INDEX.evaluate(f.context(StoredRule.DUPLICATE_INDEX)))
         .noneSatisfy(d -> assertThat(d.indexStatId()).isEqualTo(unique.getValueInteger("id")))
         .noneSatisfy(d -> assertThat(d.indexStatId()).isEqualTo(partial.getValueInteger("id")));
      assertThat(StoredRule.MISSING_INDEX.evaluate(f.context(StoredRule.MISSING_INDEX))).isEmpty();
      QRecord other = B1Fixture.insert("snapshot", new QRecord().withValue("backendName", "another").withValue("capturedAt", B1Fixture.NOW).withValue("status", "OK"));
      B1Fixture.insert("activity_sample", new QRecord().withValue("snapshotId", other.getValueInteger("id")).withValue("capturedAt", B1Fixture.NOW).withValue("reason", "IDLE_IN_TRANSACTION").withValue("xactSeconds", 99999).withValue("applicationName", "foreign"));
      assertThat(StoredRule.IDLE_IN_TRANSACTION.evaluate(f.context(StoredRule.IDLE_IN_TRANSACTION))).hasSize(1);
      f.settings.withValue("lastTableSnapshotAt", B1Fixture.NOW.plusSeconds(60));
      assertThat(StoredRule.BLOAT.evaluate(f.context(StoredRule.BLOAT))).isEmpty();
      assertThat(StoredRule.UNUSED_INDEX.evaluate(f.context(StoredRule.UNUSED_INDEX))).isEmpty();
   }


   /*******************************************************************************
    ** A later failed collection must not hide a recent statistics reset.
    *******************************************************************************/
   @Test
   void resetAgeAndLongUnitIdentity() throws Exception
   {
      B1Fixture f = new B1Fixture();
      B1Fixture.insert("snapshot", new QRecord().withValue("backendName", f.backend).withValue("capturedAt", B1Fixture.NOW.minusSeconds(40 * 86400L)).withValue("status", "OK"));
      DbPerfStore.update("db_perf_snapshot", List.of(new QRecord().withValue("id", f.snapshot.getValueInteger("id")).withValue("rawStatsReset", B1Fixture.NOW.minusSeconds(86400))));
      B1Fixture.insert("snapshot", new QRecord().withValue("backendName", f.backend).withValue("capturedAt", B1Fixture.NOW.plusSeconds(1)).withValue("status", "FAILED"));
      assertThat(StoredRule.UNUSED_INDEX.evaluate(f.context(StoredRule.UNUSED_INDEX))).isEmpty();
      String unit = "job".repeat(100);
      B1Fixture.insert("attribution", new QRecord().withValue("windowStart", B1Fixture.NOW.minusSeconds(60)).withValue("windowEnd", B1Fixture.NOW)
         .withValue("unitKind", "SCHEDULED_JOB").withValue("unitName", unit).withValue("totalMs", 1000000));
      List<FindingDraft> drafts = StoredRule.HOT_JOB.evaluate(f.context(StoredRule.HOT_JOB));
      assertThat(drafts).allSatisfy(d ->
      {
         assertThat(d.dedupeKey().length()).isLessThanOrEqualTo(300);
         assertThat(d.title().length()).isLessThanOrEqualTo(300);
      });
      assertThat(new FindingService(f.runtime).apply(f.backend, "HOT_JOB", drafts).created()).isPositive();
      assertThat(StoredRule.POOL_SIZING.evaluate(f.context(StoredRule.POOL_SIZING)).getFirst().recommendation()).contains("orders-app: combined pool cap 36");
   }


   /*******************************************************************************
    ** Reload lifecycle state after each transaction.
    *******************************************************************************/
   private static QRecord finding(QRecord row) throws Exception
   {
      return (DbPerfStore.first("db_perf_finding", DbPerfStore.eq("id", row.getValueInteger("id"))));
   }
}
