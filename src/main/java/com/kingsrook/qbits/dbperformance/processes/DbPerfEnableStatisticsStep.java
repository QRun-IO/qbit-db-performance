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


import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.collect.DbPerfSettingsService;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.provider.DbStatsProvider;
import com.kingsrook.qbits.dbperformance.provider.DbStatsProviders;
import com.kingsrook.qbits.dbperformance.provider.ProviderCapabilities;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;


/*******************************************************************************
 ** Settings action: ask the backend's provider to turn on statement statistics
 ** (H2: SET QUERY_STATISTICS TRUE as an admin), then record the new access.
 ** This is the qbit's only change to the monitored database and it is explicit.
 *******************************************************************************/
public class DbPerfEnableStatisticsStep implements BackendStep
{
   /*******************************************************************************
    ** Requires manage_settings; vendor errors become result lines.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      DbPerfRuntime runtime = DbPerfRuntime.of(QContext.getQInstance());
      if(QContext.getQSession() == null || !QContext.getQSession().hasPermission(runtime.getConfig().getPermissionBaseName() + ".manage_settings"))
      {
         throw (new QException("Managing settings permission is required"));
      }
      List<ProcessSummaryLineInterface> lines = new ArrayList<>();
      String backend = runtime.getConfig().getBackendName();
      try(Connection connection = ConnectionManager.getConnection((RDBMSBackendMetaData) runtime.getInstance().getBackend(backend)))
      {
         DbStatsProvider provider = DbStatsProviders.forConnection(connection);
         try
         {
            lines.add(new ProcessSummaryLine(Status.OK, 1, provider.enableStatementStatistics(connection)));
         }
         catch(SQLFeatureNotSupportedException e)
         {
            lines.add(new ProcessSummaryLine(Status.ERROR, 0, e.getMessage()));
         }
         catch(SQLException e)
         {
            lines.add(new ProcessSummaryLine(Status.ERROR, 0, "The database refused the change (SQLSTATE " + e.getSQLState() + "); an admin database user is required."));
         }
         ProviderCapabilities capability = provider.capabilities(connection);
         QRecord settings = new DbPerfSettingsService(runtime).ensure(backend);
         DbPerfStore.update("db_perf_settings", List.of(new QRecord().withValue("id", settings.getValueInteger("id")).withValue("provider", provider.getName())
            .withValue("statsAccess", capability.statsAccess()).withValue("statsAccessMessage", capability.message())));
         lines.add(new ProcessSummaryLine(Status.INFO, 0, "statistics access: " + capability.statsAccess()));
      }
      catch(QException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         throw (new QException("Unable to enable statement statistics", e));
      }
      output.addValue("processResults", new ArrayList<>(lines));
   }
}
