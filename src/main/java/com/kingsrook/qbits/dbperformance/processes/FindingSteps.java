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
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.analyze.FindingService;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ProcessSummaryProviderInterface;


/*******************************************************************************
 ** Selected-record streamed actions. Review does not write; Load rechecks access
 ** under the caller session before narrowly scoped system writes.
 *******************************************************************************/
public final class FindingSteps
{
   public static final String NOTE = "statusNote";
   public static final String TARGET = "findingStatus";
   private static final Map<String, String> VERBS = Map.of("ACKNOWLEDGED", "acknowledged", "RESOLVED", "marked resolved", "IGNORED", "ignored", "OPEN", "reopened");


   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private FindingSteps()
   {
   }


   /*******************************************************************************
    ** Both phases validate permission, requested state and Ignore's required note.
    *******************************************************************************/
   private static String validate(RunBackendStepInput input) throws QException
   {
      DbPerfRuntime runtime = DbPerfRuntime.of(QContext.getQInstance());
      if(QContext.getQSession() == null || !QContext.getQSession().hasPermission(runtime.getConfig().getPermissionBaseName() + ".manage_findings"))
      {
         throw (new QException("Managing findings permission is required"));
      }
      String status = input.getValueString(TARGET);
      if(!VERBS.containsKey(status == null ? "" : status))
      {
         throw (new QException("Invalid finding status"));
      }
      String note = input.getValueString(NOTE);
      if("IGNORED".equals(status) && (note == null || note.isBlank()) || note != null && note.length() > 2000)
      {
         throw (new QException("Ignore requires a note; notes must be at most 2000 characters"));
      }
      return (status);
   }


   /*******************************************************************************
    ** Standard singular/plural and future/past wording.
    *******************************************************************************/
   private static ProcessSummaryLine line(Status status, String future, String past)
   {
      ProcessSummaryLine line = new ProcessSummaryLine(status, 0, null);
      line.setSingularFutureMessage("finding will be " + future);
      line.setPluralFutureMessage("findings will be " + future);
      line.setSingularPastMessage("finding was " + past);
      line.setPluralPastMessage("findings were " + past);
      return (line);
   }


   /*******************************************************************************
    ** The Review screen describes precisely the selected records.
    *******************************************************************************/
   public static class Transform extends AbstractTransformStep implements ProcessSummaryProviderInterface
   {
      private ProcessSummaryLine changes;


      /*******************************************************************************
       ** Validate and forward a bounded page with no mutations.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         String verb = VERBS.get(validate(input));
         if(changes == null)
         {
            changes = line(Status.OK, verb, verb);
         }
         for(QRecord record : input.getRecords())
         {
            changes.incrementCountAndAddPrimaryKey(record.getValue("id"));
            output.addRecord(record);
         }
      }


      /*******************************************************************************
       ** The frontend chooses the correct tense and plurality.
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean result)
      {
         return (changes == null ? new ArrayList<>() : new ArrayList<>(List.of(changes)));
      }
   }


   /*******************************************************************************
    ** Persist the status fields only; supplied record data is never trusted.
    *******************************************************************************/
   public static class Load extends AbstractLoadStep implements ProcessSummaryProviderInterface
   {
      private ProcessSummaryLine changes;
      private final ProcessSummaryLine errors = line(Status.ERROR, "checked for access", "not updated; unavailable or write failed");


      /*******************************************************************************
       ** Preserve the human user id before entering the system write context.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         String status = validate(input);
         String actor = QContext.getQSession().getUser() == null ? null : QContext.getQSession().getUser().getIdReference();
         DbPerfRuntime runtime = DbPerfRuntime.of(QContext.getQInstance());
         if(changes == null)
         {
            changes = line(Status.OK, VERBS.get(status), VERBS.get(status));
         }
         for(QRecord selected : input.getRecords())
         {
            try
            {
               // Read under the caller context so record security still applies.
               QRecord visible = DbPerfStore.first("db_perf_finding", DbPerfStore.eq("id", selected.getValueInteger("id")));
               if(visible == null || !runtime.getConfig().getBackendName().equals(visible.getValueString("backendName")))
               {
                  errors.incrementCountAndAddPrimaryKey(selected.getValue("id"));
                  continue;
               }
               QRecord patch = new QRecord().withValue("id", visible.getValueInteger("id")).withValue("status", status)
                  .withValue("statusChangedBy", actor).withValue("resolvedAt", "RESOLVED".equals(status) ? Instant.now() : null);
               if(input.getValueString(NOTE) != null && !input.getValueString(NOTE).isBlank())
               {
                  patch.withValue(NOTE, input.getValueString(NOTE));
               }
               synchronized(runtime)
               {
                  runtime.transaction(() ->
                  {
                     FindingService.lockRule(runtime, visible.getValueString("ruleCode"));
                     DbPerfStore.update("db_perf_finding", List.of(patch));
                     return (null);
                  });
               }
               changes.incrementCountAndAddPrimaryKey(selected.getValue("id"));
            }
            catch(Exception e)
            {
               errors.incrementCountAndAddPrimaryKey(selected.getValue("id"));
            }
         }
      }


      /*******************************************************************************
       ** Include failures while allowing other selected records to complete.
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean result)
      {
         ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
         if(changes != null)
         {
            lines.add(changes);
         }
         errors.addSelfToListIfAnyCount(lines);
         return (lines);
      }
   }
}
