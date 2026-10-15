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
import java.sql.Statement;
import com.kingsrook.qbits.dbperformance.provider.DbStatsProviders;
import com.kingsrook.qbits.dbperformance.provider.PostgresStatsProvider;
import com.kingsrook.qbits.dbperformance.provider.ProviderCapabilities;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Live PostgreSQL view contracts; skipped only when Docker is unavailable.
 *******************************************************************************/
@Testcontainers(disabledWithoutDocker = true)
class PostgresStatsProviderIT
{
   @Container
   private static final PostgreSQLContainer<?> POSTGRES = DbPerfPostgresSupport.container();


   /*******************************************************************************
    ** Read every provider surface against pg_stat_statements on PostgreSQL 17.
    *******************************************************************************/
   @Test
   void readAllStatistics() throws Exception
   {
      DbPerfPostgresSupport.backend(POSTGRES);
      try(Connection connection = DbPerfPostgresSupport.connect(POSTGRES); Statement statement = connection.createStatement())
      {
         statement.execute("CREATE TABLE provider_sample (id int PRIMARY KEY, value text)");
         statement.execute("INSERT INTO provider_sample VALUES (1, 'private value')");
         statement.execute("SELECT * FROM provider_sample WHERE id=1");
         PostgresStatsProvider provider = (PostgresStatsProvider) DbStatsProviders.forConnection(connection);
         ProviderCapabilities capabilities = provider.capabilities(connection);
         assertThat(provider.getName()).isEqualTo("POSTGRES");
         assertThat(capabilities.supported()).isTrue();
         assertThat(capabilities.version()).isGreaterThanOrEqualTo(170000);
         assertThat(capabilities.statsAccess()).isEqualTo("FULL");
         assertThat(provider.readDatabase(connection).sizeBytes()).isPositive();
         assertThat(provider.readStatements(connection)).anySatisfy(row ->
         {
            assertThat(row.queryText()).contains("SELECT * FROM provider_sample");
            assertThat(row.toplevel()).isTrue();
            assertThat(row.statsSince()).isNotNull();
         });
         assertThat(provider.readStatementResetInfo(connection).statsReset()).isNotNull();
         assertThat(provider.readTables(connection)).anySatisfy(row -> assertThat(row.tableName()).isEqualTo("provider_sample"));
         assertThat(provider.readIndexes(connection)).anySatisfy(row -> assertThat(row.indexName()).isEqualTo("provider_sample_pkey"));
         try(Connection other = DbPerfPostgresSupport.connect(POSTGRES))
         {
            assertThat(provider.readSessions(connection)).isNotEmpty();
         }
         assertThat(provider.readConnections(connection)).isNotEmpty();
         assertThat(provider.readSettings(connection)).containsKeys("server_version_num", "max_connections", "block_size");
      }
   }
}
