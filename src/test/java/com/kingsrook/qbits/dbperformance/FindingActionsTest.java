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


import java.util.List;
import com.kingsrook.qbits.dbperformance.analyze.FindingService;
import com.kingsrook.qbits.dbperformance.analyze.StoredRule;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.processes.DbPerfAnalyzeStep;
import com.kingsrook.qbits.dbperformance.processes.DbPerfProcesses;
import com.kingsrook.qbits.dbperformance.processes.FindingSteps;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.jobs.DurableProcessMetaData;
import com.kingsrook.qqq.backend.core.jobs.model.JobSemantics;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Record selection, Review/Result summaries, permission and note enforcement.
 *******************************************************************************/
class FindingActionsTest
{
   /*******************************************************************************
    ** Every action updates only selected records and records the human actor.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = { "ACKNOWLEDGED", "RESOLVED", "IGNORED", "OPEN" })
   void selectedRecordAction(String status) throws Exception
   {
      B1Fixture f = new B1Fixture();
      new FindingService(f.runtime).apply(f.backend, "UNUSED_INDEX", StoredRule.UNUSED_INDEX.evaluate(f.context(StoredRule.UNUSED_INDEX)));
      List<QRecord> findings = DbPerfStore.query("db_perf_finding", new QQueryFilter());
      f.runtime.getInstance().setEnforcePermissionsInCoreActions(true);
      QContext.setQSession(new QSession().withPermissions(DbPerfPermissions.VIEW, DbPerfPermissions.MANAGE_FINDINGS).withUser(new QUser().withIdReference("operator-42")));
      RunBackendStepInput input = new RunBackendStepInput();
      input.setRecords(List.of(findings.getFirst()));
      input.addValue(FindingSteps.TARGET, status);
      input.addValue(FindingSteps.NOTE, "Reviewed by operator");
      FindingSteps.Transform transform = new FindingSteps.Transform();
      RunBackendStepOutput preview = new RunBackendStepOutput();
      transform.runOnePage(input, preview);
      ProcessSummaryLine line = (ProcessSummaryLine) transform.getProcessSummary(preview, false).getFirst();
      assertThat(line.getCount()).isEqualTo(1);
      assertThat(line.getPluralFutureMessage()).startsWith("findings will be ");
      assertThat(DbPerfStore.first("db_perf_finding", DbPerfStore.eq("id", findings.getFirst().getValueInteger("id"))).getValueString("status")).isEqualTo("OPEN");
      FindingSteps.Load load = new FindingSteps.Load();
      RunBackendStepOutput result = new RunBackendStepOutput();
      load.runOnePage(input, result);
      ProcessSummaryLine actual = (ProcessSummaryLine) load.getProcessSummary(result, true).getFirst();
      assertThat(actual.getCount()).isEqualTo(1);
      assertThat(actual.getPluralPastMessage()).startsWith("findings were ");
      QRecord changed = DbPerfStore.first("db_perf_finding", DbPerfStore.eq("id", findings.getFirst().getValueInteger("id")));
      assertThat(changed.getValueString("status")).isEqualTo(status);
      assertThat(changed.getValueString("statusChangedBy")).isEqualTo("operator-42");
      assertThat(changed.getValueString("statusNote")).isEqualTo("Reviewed by operator");
      assertThat(changed.getValueInstant("resolvedAt") != null).isEqualTo(status.equals("RESOLVED"));
      assertThat(DbPerfStore.first("db_perf_finding", DbPerfStore.eq("id", findings.getLast().getValueInteger("id"))).getValueString("status")).isEqualTo("OPEN");
      var metadata = f.runtime.getInstance().getProcess("dbPerfFinding" + status);
      assertThat(metadata.getTableName()).isEqualTo("db_perf_finding");
      assertThat(metadata.getPermissionRules().getPermissionBaseName()).isEqualTo(DbPerfPermissions.MANAGE_FINDINGS);
      assertThat(metadata.getStepList()).extracting(step -> step.getLabel()).contains("Review", "Result");
      QFrontendStepMetaData note = (QFrontendStepMetaData) metadata.getStepList().getFirst();
      assertThat(note.getFormField(FindingSteps.NOTE).getIsRequired()).isEqualTo(status.equals("IGNORED"));
   }


   /*******************************************************************************
    ** Direct invocation cannot bypass permission or Ignore validation.
    *******************************************************************************/
   @Test
   void validationAndErrors() throws Exception
   {
      B1Fixture f = new B1Fixture();
      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue(FindingSteps.TARGET, "IGNORED");
      RunBackendStepOutput output = new RunBackendStepOutput();
      assertThatThrownBy(() -> new FindingSteps.Transform().runOnePage(input, output)).hasMessageContaining("note");
      assertThatThrownBy(() -> new FindingSteps.Load().runOnePage(input, output)).hasMessageContaining("note");
      input.addValue(FindingSteps.NOTE, "x".repeat(2001));
      assertThatThrownBy(() -> new FindingSteps.Load().runOnePage(input, output)).hasMessageContaining("2000");
      input.addValue(FindingSteps.NOTE, "Reviewed");
      QContext.setQSession(new QSession().withPermissions(DbPerfPermissions.VIEW));
      assertThatThrownBy(() -> new FindingSteps.Transform().runOnePage(input, output)).hasMessageContaining("permission");
      assertThatThrownBy(() -> new FindingSteps.Load().runOnePage(input, output)).hasMessageContaining("permission");
      assertThatThrownBy(() -> new DbPerfAnalyzeStep().run(input, output)).hasMessageContaining("permission");
      QContext.setQSession(new QSession().withPermissions(DbPerfPermissions.VIEW, DbPerfPermissions.MANAGE_FINDINGS));
      input.addValue(FindingSteps.TARGET, "BAD_STATUS");
      assertThatThrownBy(() -> new FindingSteps.Load().runOnePage(input, output)).hasMessageContaining("Invalid");
      input.addValue(FindingSteps.TARGET, "RESOLVED");
      input.setRecords(List.of(new QRecord().withValue("id", 999999)));
      FindingSteps.Load load = new FindingSteps.Load();
      load.runOnePage(input, output);
      assertThat(load.getProcessSummary(output, true)).anySatisfy(line ->
      {
         assertThat(((ProcessSummaryLine) line).getStatus()).isEqualTo(Status.ERROR);
         assertThat(((ProcessSummaryLine) line).getCount()).isEqualTo(1);
      });
   }


   /*******************************************************************************
    ** Analyzer metadata follows phase A durable scheduling and summary conventions.
    *******************************************************************************/
   @Test
   void analyzeAction() throws Exception
   {
      B1Fixture f = new B1Fixture();
      var process = f.runtime.getInstance().getProcess(DbPerfProcesses.ANALYZE);
      assertThat(process.getIsHidden()).isTrue();
      assertThat(DurableProcessMetaData.of(process).getSemantics()).isEqualTo(JobSemantics.ATOMIC);
      assertThat(f.runtime.getInstance().getProcess(DbPerfProcesses.ANALYZE_NOW).getTableName()).isEqualTo("db_perf_finding");
      RunBackendStepOutput output = new RunBackendStepOutput();
      new DbPerfAnalyzeStep().run(new RunBackendStepInput(), output);
      assertThat((List<?>) output.getValue("processResults")).hasSize(42);
   }
}
