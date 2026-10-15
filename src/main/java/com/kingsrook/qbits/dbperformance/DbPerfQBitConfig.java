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
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfig;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;


/*******************************************************************************
 ** Host configuration; runtime thresholds live in the Settings table.
 *******************************************************************************/
public class DbPerfQBitConfig implements QBitConfig
{
   public static final String TABLE_NAME_PREFIX = "db_perf_";
   public static final String PERMISSION_BASE_NAME = "db_performance";

   private String backendName;
   private String schedulerName;
   private String appName = "dbPerformance";
   private String parentAppName;
   private String permissionBaseName = "db_performance";
   private Boolean attributionEnabled = true;
   private String sqlWorkbenchRunnerPath = "/app/sqlWorkbenchRunner";
   private String podName = System.getenv("HOSTNAME") == null ? "local" : System.getenv("HOSTNAME");
   private String snapshotCron = "0 */5 * * * ?";
   private String analyzeCron = "0 7 * * * ?";
   private String rollupCron = "0 15 3 * * ?";
   private String timeZone = "UTC";


   /*******************************************************************************
    ** Validate backend and application identity before registration.
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, List<String> errors)
   {
      assertCondition(backendName != null && qInstance.getBackend(backendName) instanceof RDBMSBackendMetaData, "backendName must name an existing RDBMS backend", errors);
      assertCondition(appName != null && !appName.isBlank() && qInstance.getApp(appName) == null, "appName must be nonblank and unused", errors);
      assertCondition(permissionBaseName != null && !permissionBaseName.isBlank(), "permissionBaseName is required", errors);
      assertCondition(parentAppName == null || qInstance.getApp(parentAppName) != null, "parentAppName must name an existing app", errors);
      try
      {
         java.time.ZoneId.of(timeZone);
      }
      catch(Exception e)
      {
         errors.add("timeZone must be a valid zone ID");
      }
   }


   /*******************************************************************************
    ** Default storage backend.
    *******************************************************************************/
   @Override
   public String getDefaultBackendNameForTables()
   {
      return (backendName);
   }


   /*******************************************************************************
    ** Getter for backendName.
    *******************************************************************************/
   public String getBackendName()
   {
      return (this.backendName);
   }


   /*******************************************************************************
    ** Setter for backendName.
    *******************************************************************************/
   public void setBackendName(String backendName)
   {
      this.backendName = backendName;
   }


   /*******************************************************************************
    ** Fluent setter for backendName.
    *******************************************************************************/
   public DbPerfQBitConfig withBackendName(String backendName)
   {
      this.backendName = backendName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for schedulerName.
    *******************************************************************************/
   public String getSchedulerName()
   {
      return (this.schedulerName);
   }


   /*******************************************************************************
    ** Setter for schedulerName.
    *******************************************************************************/
   public void setSchedulerName(String schedulerName)
   {
      this.schedulerName = schedulerName;
   }


   /*******************************************************************************
    ** Fluent setter for schedulerName.
    *******************************************************************************/
   public DbPerfQBitConfig withSchedulerName(String schedulerName)
   {
      this.schedulerName = schedulerName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for appName.
    *******************************************************************************/
   public String getAppName()
   {
      return (this.appName);
   }


   /*******************************************************************************
    ** Setter for appName.
    *******************************************************************************/
   public void setAppName(String appName)
   {
      this.appName = appName;
   }


   /*******************************************************************************
    ** Fluent setter for appName.
    *******************************************************************************/
   public DbPerfQBitConfig withAppName(String appName)
   {
      this.appName = appName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for parentAppName.
    *******************************************************************************/
   public String getParentAppName()
   {
      return (this.parentAppName);
   }


   /*******************************************************************************
    ** Setter for parentAppName.
    *******************************************************************************/
   public void setParentAppName(String parentAppName)
   {
      this.parentAppName = parentAppName;
   }


   /*******************************************************************************
    ** Fluent setter for parentAppName.
    *******************************************************************************/
   public DbPerfQBitConfig withParentAppName(String parentAppName)
   {
      this.parentAppName = parentAppName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for permissionBaseName.
    *******************************************************************************/
   public String getPermissionBaseName()
   {
      return (this.permissionBaseName);
   }


   /*******************************************************************************
    ** Setter for permissionBaseName.
    *******************************************************************************/
   public void setPermissionBaseName(String permissionBaseName)
   {
      this.permissionBaseName = permissionBaseName;
   }


   /*******************************************************************************
    ** Fluent setter for permissionBaseName.
    *******************************************************************************/
   public DbPerfQBitConfig withPermissionBaseName(String permissionBaseName)
   {
      this.permissionBaseName = permissionBaseName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for attributionEnabled.
    *******************************************************************************/
   public Boolean getAttributionEnabled()
   {
      return (this.attributionEnabled);
   }


   /*******************************************************************************
    ** Setter for attributionEnabled.
    *******************************************************************************/
   public void setAttributionEnabled(Boolean attributionEnabled)
   {
      this.attributionEnabled = attributionEnabled;
   }


   /*******************************************************************************
    ** Fluent setter for attributionEnabled.
    *******************************************************************************/
   public DbPerfQBitConfig withAttributionEnabled(Boolean attributionEnabled)
   {
      this.attributionEnabled = attributionEnabled;
      return (this);
   }


   /*******************************************************************************
    ** Getter for sqlWorkbenchRunnerPath.
    *******************************************************************************/
   public String getSqlWorkbenchRunnerPath()
   {
      return (this.sqlWorkbenchRunnerPath);
   }


   /*******************************************************************************
    ** Setter for sqlWorkbenchRunnerPath.
    *******************************************************************************/
   public void setSqlWorkbenchRunnerPath(String sqlWorkbenchRunnerPath)
   {
      this.sqlWorkbenchRunnerPath = sqlWorkbenchRunnerPath;
   }


   /*******************************************************************************
    ** Fluent setter for sqlWorkbenchRunnerPath.
    *******************************************************************************/
   public DbPerfQBitConfig withSqlWorkbenchRunnerPath(String sqlWorkbenchRunnerPath)
   {
      this.sqlWorkbenchRunnerPath = sqlWorkbenchRunnerPath;
      return (this);
   }


   /*******************************************************************************
    ** Getter for podName.
    *******************************************************************************/
   public String getPodName()
   {
      return (this.podName);
   }


   /*******************************************************************************
    ** Setter for podName.
    *******************************************************************************/
   public void setPodName(String podName)
   {
      this.podName = podName;
   }


   /*******************************************************************************
    ** Fluent setter for podName.
    *******************************************************************************/
   public DbPerfQBitConfig withPodName(String podName)
   {
      this.podName = podName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for snapshotCron.
    *******************************************************************************/
   public String getSnapshotCron()
   {
      return (this.snapshotCron);
   }


   /*******************************************************************************
    ** Setter for snapshotCron.
    *******************************************************************************/
   public void setSnapshotCron(String snapshotCron)
   {
      this.snapshotCron = snapshotCron;
   }


   /*******************************************************************************
    ** Fluent setter for snapshotCron.
    *******************************************************************************/
   public DbPerfQBitConfig withSnapshotCron(String snapshotCron)
   {
      this.snapshotCron = snapshotCron;
      return (this);
   }


   /*******************************************************************************
    ** Getter for analyzeCron.
    *******************************************************************************/
   public String getAnalyzeCron()
   {
      return (this.analyzeCron);
   }


   /*******************************************************************************
    ** Setter for analyzeCron.
    *******************************************************************************/
   public void setAnalyzeCron(String analyzeCron)
   {
      this.analyzeCron = analyzeCron;
   }


   /*******************************************************************************
    ** Fluent setter for analyzeCron.
    *******************************************************************************/
   public DbPerfQBitConfig withAnalyzeCron(String analyzeCron)
   {
      this.analyzeCron = analyzeCron;
      return (this);
   }


   /*******************************************************************************
    ** Getter for rollupCron.
    *******************************************************************************/
   public String getRollupCron()
   {
      return (this.rollupCron);
   }


   /*******************************************************************************
    ** Setter for rollupCron.
    *******************************************************************************/
   public void setRollupCron(String rollupCron)
   {
      this.rollupCron = rollupCron;
   }


   /*******************************************************************************
    ** Fluent setter for rollupCron.
    *******************************************************************************/
   public DbPerfQBitConfig withRollupCron(String rollupCron)
   {
      this.rollupCron = rollupCron;
      return (this);
   }


   /*******************************************************************************
    ** Getter for timeZone.
    *******************************************************************************/
   public String getTimeZone()
   {
      return (this.timeZone);
   }


   /*******************************************************************************
    ** Setter for timeZone.
    *******************************************************************************/
   public void setTimeZone(String timeZone)
   {
      this.timeZone = timeZone;
   }


   /*******************************************************************************
    ** Fluent setter for timeZone.
    *******************************************************************************/
   public DbPerfQBitConfig withTimeZone(String timeZone)
   {
      this.timeZone = timeZone;
      return (this);
   }
}
