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
import java.sql.Statement;
import java.util.UUID;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.testcontainers.containers.PostgreSQLContainer;


/*******************************************************************************
 ** Shared PostgreSQL 17 test setup, using the same JDBC role as the host backend.
 *******************************************************************************/
public final class DbPerfPostgresSupport
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DbPerfPostgresSupport()
   {
   }


   /*******************************************************************************
    ** pg_stat_statements must be loaded at server startup.
    *******************************************************************************/
   public static PostgreSQLContainer<?> container()
   {
      return (new PostgreSQLContainer<>("postgres:17-alpine")
         .withCommand("postgres", "-c", "shared_preload_libraries=pg_stat_statements", "-c", "pg_stat_statements.track=top"));
   }


   /*******************************************************************************
    ** Install the extension in the current database.
    *******************************************************************************/
   public static RDBMSBackendMetaData backend(PostgreSQLContainer<?> postgres) throws Exception
   {
      try(Connection connection = connect(postgres); Statement statement = connection.createStatement())
      {
         statement.execute("CREATE EXTENSION IF NOT EXISTS pg_stat_statements");
      }
      RDBMSBackendMetaData backend = new PostgreSQLBackendMetaData().withName("postgres" + UUID.randomUUID().toString().replace("-", ""))
         .withVendor("postgresql").withHostName(postgres.getHost()).withDatabaseName(postgres.getDatabaseName())
         .withUsername(postgres.getUsername()).withPassword(postgres.getPassword());
      backend.setJdbcUrl(postgres.getJdbcUrl());
      return (backend);
   }


   /*******************************************************************************
    ** Direct test fixture connection.
    *******************************************************************************/
   public static Connection connect(PostgreSQLContainer<?> postgres) throws Exception
   {
      return (DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
   }
}
