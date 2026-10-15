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


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.dbperformance.alerts.SnapshotEvaluator;
import com.kingsrook.qbits.dbperformance.api.DbPerformanceUiApi;
import com.kingsrook.qbits.dbperformance.explain.QueryPlanProcess;
import com.kingsrook.qbits.dbperformance.health.DbPerfHealthChecks;
import com.kingsrook.qbits.dbperformance.metadata.DbPerfHelp;
import com.kingsrook.qbits.dbperformance.metadata.DbPerfMetaData;
import com.kingsrook.qbits.dbperformance.processes.DbPerfProcesses;
import com.kingsrook.qbits.dbperformance.widgets.DbPerfWidgets;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfigValidationException;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaDataProducer;


/*******************************************************************************
 ** Produces the DB Performance QBit. Component producers in this package are found by
 ** package scanning.
 *******************************************************************************/
public class DbPerfQBitProducer implements QBitMetaDataProducer<DbPerfQBitConfig>
{
   public static final String GROUP_ID    = "com.kingsrook.qbits";
   public static final String ARTIFACT_ID = "qbit-db-performance";
   public static final String VERSION     = "0.1.0";

   private DbPerfQBitConfig qBitConfig;
   private String namespace;



   /*******************************************************************************
    ** Fluent setter for the config.
    *******************************************************************************/
   public DbPerfQBitProducer withQBitConfig(DbPerfQBitConfig config)
   {
      this.qBitConfig = config;
      return (this);
   }



   /*******************************************************************************
    ** Getter for the config.
    *******************************************************************************/
   @Override
   public DbPerfQBitConfig getQBitConfig()
   {
      return (qBitConfig);
   }



   /*******************************************************************************
    ** Fluent setter for the namespace.
    *******************************************************************************/
   public DbPerfQBitProducer withNamespace(String namespace)
   {
      this.namespace = namespace;
      return (this);
   }



   /*******************************************************************************
    ** Getter for the namespace.
    *******************************************************************************/
   @Override
   public String getNamespace()
   {
      return (namespace);
   }



   /*******************************************************************************
    ** The qbit's identity.
    *******************************************************************************/
   @Override
   public QBitMetaData getQBitMetaData()
   {
      return (new QBitMetaData().withGroupId(GROUP_ID).withArtifactId(ARTIFACT_ID).withVersion(VERSION).withNamespace(namespace).withConfig(qBitConfig));
   }



   /*******************************************************************************
    ** Producers found by package scanning have no config: skip them.
    *******************************************************************************/
   @Override
   public boolean isEnabled()
   {
      return (qBitConfig != null);
   }

   /*******************************************************************************
    ** Validate and register the Phase A metadata.
    *******************************************************************************/
   @Override
   public MetaDataProducerMultiOutput produce(QInstance instance) throws QException
   {
      if(qBitConfig == null)
      {
         throw (new QException("DB Performance configuration is required"));
      }
      List<String> errors = new ArrayList<>();
      qBitConfig.validate(instance, errors);
      for(String table : DbPerfMetaData.TABLES)
      {
         if(instance.getTable(table) != null)
         {
            errors.add("Table already registered: " + table);
         }
      }
      if(!errors.isEmpty())
      {
         throw (new QBitConfigValidationException(qBitConfig, errors));
      }
      instance.addQBit(getQBitMetaData());
      DbPerfMetaData.addTo(instance, qBitConfig);
      new DbPerfRuntime(instance, qBitConfig).addSelfToInstance(instance);
      DbPerfProcesses.addTo(instance, qBitConfig);
      SnapshotEvaluator.register(instance, qBitConfig);
      DbPerfHealthChecks.register(instance);
      QueryPlanProcess.register(instance, qBitConfig);
      registerUiApi(instance);
      DbPerfWidgets.register(instance, qBitConfig);
      DbPerfHelp.register(instance, qBitConfig);
      return (new MetaDataProducerMultiOutput());
   }

   /*******************************************************************************
    ** Middleware is optional for hosts that use only the core tables and collector.
    *******************************************************************************/
   private static void registerUiApi(QInstance instance)
   {
      try
      {
         Class.forName("com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtensionsMetaData");
         DbPerformanceUiApi.register(instance);
      }
      catch(ClassNotFoundException e)
      {
         // Core-only hosts have no HTTP API to register.
      }
   }
}
