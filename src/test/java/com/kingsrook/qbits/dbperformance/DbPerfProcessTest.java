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
import com.kingsrook.qbits.dbperformance.collect.DbPerfSettingsService;
import com.kingsrook.qbits.dbperformance.processes.DbPerfCollectStep;
import com.kingsrook.qbits.dbperformance.processes.DbPerfProcesses;
import com.kingsrook.qbits.dbperformance.processes.DbPerfRollupStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.jobs.DurableProcessMetaData;
import com.kingsrook.qqq.backend.core.jobs.model.JobIdempotency;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Process metadata, system execution, summaries and runtime permission guards.
 *******************************************************************************/
class DbPerfProcessTest
{
   /*******************************************************************************
    ** Scheduled processes stay out of navigation and register optional cron jobs.
    *******************************************************************************/
   @Test
   void metadataAndExecution() throws Exception
   {
      QInstance instance = DbPerfTestSupport.instance();
      assertThat(instance.getProcesses()).containsKeys(DbPerfProcesses.COLLECT, DbPerfProcesses.ROLLUP, DbPerfProcesses.COLLECT_NOW);
      for(String name : List.of(DbPerfProcesses.COLLECT, DbPerfProcesses.ROLLUP))
      {
         QProcessMetaData process = instance.getProcess(name);
         assertThat(process.getIsHidden()).isTrue();
         assertThat(process.getSchedule()).isNull();
         assertThat(DurableProcessMetaData.of(process).getEligible()).isTrue();
         assertThat(DurableProcessMetaData.of(process).getIdempotency()).isEqualTo(JobIdempotency.NATURAL);
      }
      assertThat(instance.getProcess(DbPerfProcesses.COLLECT_NOW).getTableName()).isEqualTo("db_perf_snapshot");
      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("forceTables", true);
      RunBackendStepOutput output = new RunBackendStepOutput();
      new DbPerfCollectStep().run(input, output);
      assertThat(output.getValue("processResults")).isNotNull();
      input.addValue("forceTables", false);
      new DbPerfCollectStep().run(input, new RunBackendStepOutput());
      new DbPerfRollupStep().run(input, new RunBackendStepOutput());
      DbPerfRuntime.of(instance).getConfig().setSchedulerName("scheduler");
      QInstance scheduledInstance = new QInstance();
      DbPerfProcesses.addTo(scheduledInstance, DbPerfRuntime.of(instance).getConfig());
      assertThat(scheduledInstance.getProcess(DbPerfProcesses.COLLECT).getSchedule().getCronExpression()).isEqualTo("0 */5 * * * ?");
      QContext.setQSession(new QSession().withPermissions(DbPerfPermissions.VIEW));
      assertThatThrownBy(() -> new DbPerfCollectStep().run(input, output)).hasMessageContaining("permission");
      assertThatThrownBy(() -> new DbPerfRollupStep().run(input, output)).hasMessageContaining("permission");
   }
}
