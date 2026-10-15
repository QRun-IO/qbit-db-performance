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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import com.kingsrook.qbits.dbperformance.metadata.DbPerfMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Every metadata reference is backed by an actual PostgreSQL foreign key.
 *******************************************************************************/
@Testcontainers(disabledWithoutDocker = true)
class DbPerfForeignKeyTest
{
   @Container
   private static final PostgreSQLContainer<?> POSTGRES = DbPerfPostgresSupport.container();


   /*******************************************************************************
    ** The host changelog creates all references and reruns without changes.
    *******************************************************************************/
   @Test
   void everyReferenceHasForeignKey() throws Exception
   {
      RDBMSBackendMetaData backend = DbPerfPostgresSupport.backend(POSTGRES);
      QInstance instance = DbPerfTestSupport.instance(backend);
      DbPerfTestSupport.migrate(backend);
      try(Connection connection = DbPerfPostgresSupport.connect(POSTGRES))
      {
         for(QTableMetaData table : instance.getTables().values())
         {
            Set<String> references = new HashSet<>();
            try(ResultSet keys = connection.getMetaData().getImportedKeys(null, "public", table.getName()))
            {
               while(keys.next())
               {
                  references.add(keys.getString("FKCOLUMN_NAME") + ":" + keys.getString("PKTABLE_NAME"));
               }
            }
            for(QFieldMetaData field : table.getFields().values())
            {
               if(field.getPossibleValueSourceName() != null && DbPerfMetaData.TABLES.contains(field.getPossibleValueSourceName()))
               {
                  assertThat(references).as(table.getName() + "." + field.getBackendName()).contains(field.getBackendName() + ":" + field.getPossibleValueSourceName());
               }
            }
         }
         try(Statement statement = connection.createStatement())
         {
            statement.execute("INSERT INTO db_perf_statement(id) VALUES (999)");
            statement.execute("INSERT INTO db_perf_explain(statement_id) VALUES (999)");
            statement.execute("DELETE FROM db_perf_statement WHERE id=999");
            try(ResultSet row = statement.executeQuery("SELECT statement_id FROM db_perf_explain"))
            {
               row.next();
               assertThat(row.getObject(1)).isNull();
            }
         }
      }
   }
}
