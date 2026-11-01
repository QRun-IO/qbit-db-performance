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

package com.kingsrook.qbits.dbperformance.processes;


import java.util.List;
import com.kingsrook.qbits.dbperformance.DbPerfPermissions;
import com.kingsrook.qbits.dbperformance.DbPerfQBitConfig;
import com.kingsrook.qqq.backend.core.jobs.DurableProcessMetaData;
import com.kingsrook.qqq.backend.core.jobs.model.JobIdempotency;
import com.kingsrook.qqq.backend.core.jobs.model.JobSemantics;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionOutputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;


/*******************************************************************************
 ** Scheduled maintenance and the Snapshot table action; no process navigation.
 *******************************************************************************/
public final class DbPerfProcesses
{
   public static final String ANALYZE = "dbPerfAnalyze";
   public static final String ANALYZE_NOW = "dbPerfAnalyzeNow";
   public static final String COLLECT = "dbPerfCollectSnapshot";
   public static final String ROLLUP = "dbPerfRollupAndPurge";
   public static final String COLLECT_NOW = "dbPerfCollectNow";
   public static final String ENABLE_STATISTICS = "dbPerfEnableStatementStatistics";


   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DbPerfProcesses()
   {
   }


   /*******************************************************************************
    ** Add schedules only when the host supplies a scheduler name.
    *******************************************************************************/
   public static void addTo(QInstance instance, DbPerfQBitConfig config)
   {
      instance.addProcess(scheduled(config, ANALYZE, "Analyze database performance", DbPerfAnalyzeStep.class, config.getAnalyzeCron()));
      instance.addProcess(base(config, ANALYZE_NOW, "Analyze now", DbPerfAnalyzeStep.class, false).withTableName("db_perf_finding")
         .withStep(new QFrontendStepMetaData().withName("result").withLabel("Result")
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.PROCESS_SUMMARY_RESULTS))));
      for(String status : List.of("ACKNOWLEDGED", "RESOLVED", "IGNORED", "OPEN"))
      {
         String label = switch(status)
         {
            case "ACKNOWLEDGED" -> "Acknowledge";
            case "RESOLVED" -> "Mark resolved";
            case "IGNORED" -> "Ignore";
            default -> "Reopen";
         };
         QProcessMetaData action = StreamedETLWithFrontendProcess.processMetaDataBuilder()
            .withName("dbPerfFinding" + status).withLabel(label).withIcon(new QIcon("done"))
            .withTableName("db_perf_finding").withSourceTable("db_perf_finding").withDestinationTable("db_perf_finding")
            .withExtractStepClass(ExtractViaQueryStep.class).withTransformStepClass(FindingSteps.Transform.class).withLoadStepClass(FindingSteps.Load.class)
            .withSupportsFullValidation(true).withDoFullValidation(true).withMinInputRecords(1)
            .withPreviewMessage("Review the selected findings before changing their status.")
            .withFields(List.of(new QFieldMetaData(FindingSteps.TARGET, QFieldType.STRING).withDefaultValue(status))).getProcessMetaData();
         action.withPermissionRules(DbPerfPermissions.rules(config.getPermissionBaseName() + ".manage_findings"))
            .withStep(0, new QFrontendStepMetaData().withName("note").withLabel("Note")
               .withFormField(new QFieldMetaData(FindingSteps.NOTE, QFieldType.STRING).withLabel("Note").withMaxLength(2000).withIsRequired("IGNORED".equals(status))));
         instance.addProcess(action);
      }
      instance.addProcess(scheduled(config, COLLECT, "Collect database snapshot", DbPerfCollectStep.class, config.getSnapshotCron()));
      instance.addProcess(scheduled(config, ROLLUP, "Roll up and purge database performance", DbPerfRollupStep.class, config.getRollupCron()));
      instance.addProcess(base(config, COLLECT_NOW, "Collect now", DbPerfCollectStep.class, true)
         .withTableName("db_perf_snapshot")
         .withStep(new QFrontendStepMetaData().withName("result").withLabel("Result")
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.PROCESS_SUMMARY_RESULTS))));
      instance.addProcess(base(config, ENABLE_STATISTICS, "Enable statement statistics", DbPerfEnableStatisticsStep.class, false)
         .withTableName("db_perf_settings")
         .withStep(new QFrontendStepMetaData().withName("result").withLabel("Result")
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.PROCESS_SUMMARY_RESULTS))));
   }


   /*******************************************************************************
    ** Hidden durable-eligible maintenance process.
    *******************************************************************************/
   private static QProcessMetaData scheduled(DbPerfQBitConfig config, String name, String label, Class<?> step, String cron)
   {
      QProcessMetaData process = base(config, name, label, step, false).withIsHidden(true)
         .withSupplementalMetaData(new DurableProcessMetaData().withEligible(true).withSemantics(JobSemantics.ATOMIC).withIdempotency(JobIdempotency.NATURAL).withMaxConcurrentJobs(1));
      if(config.getSchedulerName() != null)
      {
         process.withSchedule(new QScheduleMetaData().withSchedulerName(config.getSchedulerName()).withCronExpression(cron).withCronTimeZoneId(config.getTimeZone()));
      }
      return (process);
   }


   /*******************************************************************************
    ** One backend step with standard process result lines.
    *******************************************************************************/
   private static QProcessMetaData base(DbPerfQBitConfig config, String name, String label, Class<?> step, boolean force)
   {
      return (new QProcessMetaData().withName(name).withLabel(label).withIcon(new QIcon("speed"))
         .withPermissionRules(DbPerfPermissions.rules(config.getPermissionBaseName() + ".manage_settings"))
         .withStep(new QBackendStepMetaData().withName("run").withCode(new QCodeReference(step))
            .withInputData(new QFunctionInputMetaData().withField(new QFieldMetaData("forceTables", QFieldType.BOOLEAN).withDefaultValue(force)))
            .withOutputMetaData(new QFunctionOutputMetaData().withField(new QFieldMetaData("processResults", QFieldType.STRING)))));
   }
}
