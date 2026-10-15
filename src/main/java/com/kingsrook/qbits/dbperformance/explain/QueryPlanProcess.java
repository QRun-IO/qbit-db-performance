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

package com.kingsrook.qbits.dbperformance.explain;


import java.util.Arrays;
import java.util.List;
import com.kingsrook.qbits.dbperformance.DbPerfPermissions;
import com.kingsrook.qbits.dbperformance.DbPerfQBitConfig;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QRecordListMetaData;


/*******************************************************************************
 ** The one-record Query plan action; user-supplied values remain transient.
 *******************************************************************************/
public class QueryPlanProcess implements BackendStep
{
   public static final String NAME = "dbPerfQueryPlan";



   /*******************************************************************************
    ** Register form, execution and copyable result fields, without a navigation item.
    *******************************************************************************/
   public static void register(QInstance instance, DbPerfQBitConfig config)
   {
      instance.addPossibleValueSource(new QPossibleValueSource().withName("db_perf_ExplainRequestMode").withType(QPossibleValueSourceType.ENUM)
         .withEnumValues(List.of(new QPossibleValue<>("PLAN", "PLAN"), new QPossibleValue<>("ANALYZE", "ANALYZE"))));
      instance.addProcess(new QProcessMetaData().withName(NAME).withLabel("Query plan").withTableName("db_perf_statement")
         .withMinInputRecords(1).withMaxInputRecords(1)
         .withPermissionRules(DbPerfPermissions.rules(config.getPermissionBaseName() + ".explain"))
         .withStep(new QFrontendStepMetaData().withName("review").withLabel("Review")
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.EDIT_FORM))
            .withFormFields(List.of(new QFieldMetaData("mode", QFieldType.STRING).withLabel("Mode").withPossibleValueSourceName("db_perf_ExplainRequestMode").withDefaultValue("PLAN").withIsRequired(true),
               new QFieldMetaData("parameterValues", QFieldType.TEXT).withLabel("Parameter values (ANALYZE only, one per line)").withIsSensitive(true))))
         .withStep(new QBackendStepMetaData().withName("explain").withCode(new QCodeReference(QueryPlanProcess.class))
            .withInputData(new QFunctionInputMetaData().withRecordListMetaData(new QRecordListMetaData().withTableName("db_perf_statement"))))
         .withStep(new QFrontendStepMetaData().withName("result").withLabel("Result")
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.VIEW_FORM))
            .withViewField(code("planText", "Query plan"))
            .withViewField(new QFieldMetaData("totalCost", QFieldType.DECIMAL).withLabel("Total cost"))
            .withViewField(new QFieldMetaData("sqlWorkbenchUrl", QFieldType.STRING).withLabel("Open in SQL Workbench")
               .withFieldAdornment(new FieldAdornment(AdornmentType.LINK)))
            .withViewField(code("assistantPrompt", "Copy to assistant"))));
   }



   /*******************************************************************************
    ** Monospace text that remains selectable and copyable.
    *******************************************************************************/
   private static QFieldMetaData code(String name, String label)
   {
      return (new QFieldMetaData(name, QFieldType.TEXT).withLabel(label)
         .withFieldAdornment(new FieldAdornment(AdornmentType.CODE_EDITOR).withValue(AdornmentType.CodeEditorValues.languageMode("text"))));
   }



   /*******************************************************************************
    ** Re-check permissions inside the service even when invoked without UI middleware.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      try
      {
         if(input.getRecords() == null || input.getRecords().size() != 1)
         {
            throw (new QException("Select exactly one statement."));
         }
         DbPerfRuntime runtime = DbPerfRuntime.of(QContext.getQInstance());
         String values = input.getValueString("parameterValues");
         List<String> parameters = values == null || values.isEmpty() ? List.of() : Arrays.asList(values.split("\\R", -1));
         ExplainRequest.Mode mode;
         try
         {
            mode = ExplainRequest.Mode.valueOf(input.getValueString("mode") == null ? "PLAN" : input.getValueString("mode"));
         }
         catch(IllegalArgumentException e)
         {
            mode = null;
         }
         ExplainResult result = new ExplainService(runtime).explain(new ExplainRequest(runtime.getConfig().getBackendName(),
            input.getRecords().getFirst().getValueInteger("id"), null, mode, parameters));
         output.addValue("planText", result.planText());
         output.addValue("totalCost", result.totalCost());
         output.addValue("sqlWorkbenchUrl", result.sqlWorkbenchUrl());
         output.addValue("assistantPrompt", result.assistantPrompt());
      }
      catch(QException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         throw (new QException("Unable to complete the query plan request."));
      }
      finally
      {
         input.getValues().remove("parameterValues");
         output.getValues().remove("parameterValues");
      }
   }
}
