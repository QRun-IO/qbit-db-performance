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


import java.util.ArrayList;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.analyze.AnalyzeService;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;


/*******************************************************************************
 ** Scheduled analysis and Analyze now use the same stored-data evaluation path.
 *******************************************************************************/
public class DbPerfAnalyzeStep implements BackendStep
{
   /*******************************************************************************
    ** Include per-rule counts and failures in the standard Result screen.
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
      try
      {
         for(AnalyzeService.Result result : new AnalyzeService(runtime).run(runtime.getConfig().getBackendName()))
         {
            if(result.error() != null)
            {
               lines.add(new ProcessSummaryLine(Status.ERROR, null, result.ruleCode() + ": " + result.error()));
            }
            else
            {
               lines.add(new ProcessSummaryLine(Status.INFO, result.counts().created(), result.ruleCode() + " findings created"));
               lines.add(new ProcessSummaryLine(Status.INFO, result.counts().updated(), result.ruleCode() + " findings updated"));
               lines.add(new ProcessSummaryLine(Status.INFO, result.counts().resolved(), result.ruleCode() + " findings resolved"));
            }
         }
      }
      catch(Exception e)
      {
         throw (new QException("Unable to analyze database performance", e));
      }
      output.addValue("processResults", lines);
   }
}
