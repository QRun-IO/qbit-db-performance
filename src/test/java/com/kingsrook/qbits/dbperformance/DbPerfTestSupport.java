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


import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import com.kingsrook.qbits.dbperformance.metadata.DbPerfMetaData;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;


/*******************************************************************************
 ** Isolated H2 fixtures using the real host migration and QQQ actions.
 *******************************************************************************/
public final class DbPerfTestSupport
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DbPerfTestSupport()
   {
   }


   /*******************************************************************************
    ** New isolated instance with schema and system context.
    *******************************************************************************/
   public static QInstance instance() throws Exception
   {
      String name = "dbperf" + UUID.randomUUID().toString().replace("-", "");
      RDBMSBackendMetaData backend = new RDBMSBackendMetaData().withName(name).withVendor("h2").withHostName("mem").withDatabaseName(name).withUsername("sa");
      backend.setJdbcUrl("jdbc:h2:mem:" + name + ";CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
      return (instance(backend));
   }


   /*******************************************************************************
    ** Wire the producer onto an existing backend.
    *******************************************************************************/
   public static QInstance instance(RDBMSBackendMetaData backend) throws Exception
   {
      QInstance instance = new QInstance();
      instance.addBackend(backend);
      instance.withInstanceDefaultAuthentication(new QAuthenticationMetaData().withName("mock").withType(QAuthenticationType.MOCK));
      new DbPerfQBitProducer().withQBitConfig(new DbPerfQBitConfig().withBackendName(backend.getName())).produce(instance);
      migrate(backend);
      QContext.init(instance, new QSystemUserSession(instance));
      return (instance);
   }


   /*******************************************************************************
    ** Apply or reapply the shipped changelog through Liquibase.
    *******************************************************************************/
   public static void migrate(RDBMSBackendMetaData backend) throws Exception
   {
      try(Connection connection = DriverManager.getConnection(backend.getJdbcUrl(), backend.getUsername(), backend.getPassword());
         Liquibase liquibase = new Liquibase(DbPerfMetaData.CHANGELOG, new ClassLoaderResourceAccessor(), DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection))))
      {
         liquibase.getDatabase().setDatabaseChangeLogTableName("databasechangelog");
         liquibase.getDatabase().setDatabaseChangeLogLockTableName("databasechangeloglock");
         liquibase.update("");
      }
   }
}
