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


import java.time.Instant;
import java.util.ArrayList;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.collect.DbPerfSettingsService;
import com.kingsrook.qbits.dbperformance.collect.SnapshotCollector;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Collect on schedule or explicitly from the Snapshots table.
 *******************************************************************************/
public class DbPerfCollectStep implements BackendStep
{
   /*******************************************************************************
    ** Respect runtime interval settings on scheduled retries; Collect now forces
    ** table statistics and always returns the recorded outcome on its result screen.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      DbPerfRuntime runtime = DbPerfRuntime.of(QContext.getQInstance());
      if(QContext.getQSession() == null || !QContext.getQSession().hasPermission(runtime.getConfig().getPermissionBaseName() + ".manage_settings"))
      {
         throw (new QException("Managing settings permission is required"));
      }
      ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
      boolean force = Boolean.TRUE.equals(input.getValueBoolean("forceTables"));
      try
      {
         QRecord settings = new DbPerfSettingsService(runtime).ensure(runtime.getConfig().getBackendName());
         Instant last = settings.getValueInstant("lastSnapshotAt");
         if(!force && last != null && last.plusSeconds(60L * SnapshotCollector.positive(settings, "snapshotIntervalMinutes", 5)).isAfter(Instant.now()))
         {
            lines.add(new ProcessSummaryLine(Status.INFO, 0, "snapshots collected; the next interval is not due"));
         }
         else
         {
            QRecord snapshot = new SnapshotCollector(runtime).collect(runtime.getConfig().getBackendName(), force);
            output.addRecord(snapshot);
            lines.add(new ProcessSummaryLine("FAILED".equals(snapshot.getValueString("status")) ? Status.ERROR : Status.INFO, 1, "snapshot: " + snapshot.getValueString("status")));
            lines.add(new ProcessSummaryLine(Status.INFO, snapshot.getValueInteger("storedStatementCount") == null ? 0 : snapshot.getValueInteger("storedStatementCount"), "query details stored"));
            if(snapshot.getValueString("errorMessage") != null)
            {
               lines.add(new ProcessSummaryLine(Status.INFO, 0, snapshot.getValueString("errorMessage")));
            }
         }
      }
      catch(Exception e)
      {
         throw (new QException("Unable to collect database performance snapshot", e));
      }
      output.addValue("processResults", lines);
   }
}
