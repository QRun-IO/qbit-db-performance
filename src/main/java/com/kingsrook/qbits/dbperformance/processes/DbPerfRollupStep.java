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


import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.collect.RetentionService;
import com.kingsrook.qbits.dbperformance.collect.RollupService;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;


/*******************************************************************************
 ** Roll up yesterday and backfill missing recent days, then enforce retention.
 *******************************************************************************/
public class DbPerfRollupStep implements BackendStep
{
   /*******************************************************************************
    ** Yesterday includes late detail; missing older days are repaired only while
    ** their detail is retained. Daily history outlives the detail-retention window.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      DbPerfRuntime runtime = DbPerfRuntime.of(QContext.getQInstance());
      if(QContext.getQSession() == null || !QContext.getQSession().hasPermission(runtime.getConfig().getPermissionBaseName() + ".manage_settings"))
      {
         throw (new QException("Managing settings permission is required"));
      }
      try
      {
         LocalDate today = LocalDate.now(ZoneId.of(runtime.getConfig().getTimeZone()));
         RollupService rollup = new RollupService(runtime);
         int rolledUp = 0;
         for(int days = 1; days <= 7; days++)
         {
            LocalDate day = today.minusDays(days);
            if(days == 1 || rollup.needsBackfill(day))
            {
               rollup.rollupDay(day);
               rolledUp++;
            }
         }
         int removed = new RetentionService(runtime).purge();
         ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
         lines.add(new ProcessSummaryLine(Status.INFO, rolledUp, "days rolled up"));
         lines.add(new ProcessSummaryLine(Status.INFO, removed, "expired records deleted"));
         output.addValue("processResults", lines);
      }
      catch(Exception e)
      {
         throw (new QException("Database performance rollup and purge failed", e));
      }
   }
}
