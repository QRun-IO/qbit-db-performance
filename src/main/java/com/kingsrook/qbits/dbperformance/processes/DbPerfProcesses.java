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


/*******************************************************************************
 ** Scheduled maintenance and the Snapshot table action; no process navigation.
 *******************************************************************************/
public final class DbPerfProcesses
{
   public static final String COLLECT = "dbPerfCollectSnapshot";
   public static final String ROLLUP = "dbPerfRollupAndPurge";
   public static final String COLLECT_NOW = "dbPerfCollectNow";


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
      instance.addProcess(scheduled(config, COLLECT, "Collect database snapshot", DbPerfCollectStep.class, config.getSnapshotCron()));
      instance.addProcess(scheduled(config, ROLLUP, "Roll up and purge database performance", DbPerfRollupStep.class, config.getRollupCron()));
      instance.addProcess(base(config, COLLECT_NOW, "Collect now", DbPerfCollectStep.class, true)
         .withTableName("db_perf_snapshot")
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
