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
import java.sql.ResultSet;
import java.sql.Statement;
import com.kingsrook.qbits.dbperformance.metadata.DbPerfMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Real Liquibase migration and insert-only defaults on H2.
 *******************************************************************************/
class DbPerfChangelogTest
{
   /*******************************************************************************
    ** Tables exist, all defaults are seeded and reapplying preserves edits.
    *******************************************************************************/
   @Test
   void migrateTwice() throws Exception
   {
      QInstance instance = DbPerfTestSupport.instance();
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) instance.getBackends().values().iterator().next();
      try(Connection connection = DriverManager.getConnection(backend.getJdbcUrl(), "sa", ""); Statement statement = connection.createStatement())
      {
         for(String table : DbPerfMetaData.TABLES)
         {
            try(ResultSet rows = statement.executeQuery("SELECT count(*) FROM " + table))
            {
               rows.next();
               assertThat(rows.getInt(1)).isEqualTo(table.equals("db_perf_rule") ? 14 : 0);
            }
         }
         statement.executeUpdate("UPDATE db_perf_rule SET threshold_1=123 WHERE code='SLOW_QUERY'");
         DbPerfTestSupport.migrate(backend);
         try(var resource = getClass().getClassLoader().getResourceAsStream(DbPerfMetaData.CHANGELOG))
         {
            String script = new String(resource.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            String seeds = script.substring(script.indexOf("INSERT INTO db_perf_rule"));
            for(String sql : seeds.split(";"))
            {
               if(!sql.isBlank())
               {
                  statement.executeUpdate(sql);
               }
            }
         }
         try(ResultSet rows = statement.executeQuery("SELECT threshold_1 FROM db_perf_rule WHERE code='SLOW_QUERY'"))
         {
            rows.next();
            assertThat(rows.getInt(1)).isEqualTo(123);
         }
      }
   }
}
