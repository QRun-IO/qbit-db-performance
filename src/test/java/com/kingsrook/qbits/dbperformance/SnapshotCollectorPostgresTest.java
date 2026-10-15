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
import java.util.UUID;
import com.kingsrook.qbits.dbperformance.collect.DbPerfSettingsService;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.collect.SnapshotCollector;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Live PostgreSQL collection, reset detection and reduced-role access.
 *******************************************************************************/
@Testcontainers(disabledWithoutDocker = true)
class SnapshotCollectorPostgresTest
{
   @Container
   private static final PostgreSQLContainer<?> POSTGRES = DbPerfPostgresSupport.container();


   /*******************************************************************************
    ** Two real passes retain deltas; reset is explicit and a limited role is PARTIAL.
    *******************************************************************************/
   @Test
   void snapshotsAndReducedAccess() throws Exception
   {
      RDBMSBackendMetaData backend = DbPerfPostgresSupport.backend(POSTGRES);
      QInstance instance = DbPerfTestSupport.instance(backend);
      DbPerfRuntime runtime = DbPerfRuntime.of(instance);
      SnapshotCollector collector = new SnapshotCollector(runtime);
      assertThat(collector.collect(backend.getName(), true).getValueString("status")).isEqualTo("OK");
      try(Connection connection = DbPerfPostgresSupport.connect(POSTGRES); Statement statement = connection.createStatement())
      {
         for(int i = 0; i < 5; i++)
         {
            statement.execute("SELECT sum(id) FROM db_perf_rule WHERE id > 0");
         }
         statement.execute("SELECT pg_stat_force_next_flush()");
      }
      QRecord second = collector.collect(backend.getName(), true);
      assertThat(second.getValueString("status")).as(second.getValueString("errorMessage")).isEqualTo("OK");
      assertThat(second.getValueLong("statementCalls")).isPositive();
      assertThat(DbPerfStore.query("db_perf_query_snapshot", new QQueryFilter())).isNotEmpty();
      assertThat(DbPerfStore.query("db_perf_table_stat", new QQueryFilter())).isNotEmpty();
      assertThat(DbPerfStore.query("db_perf_index_stat", new QQueryFilter())).isNotEmpty();
      try(Connection connection = DbPerfPostgresSupport.connect(POSTGRES); Statement statement = connection.createStatement())
      {
         statement.execute("SELECT pg_stat_statements_reset()");
      }
      assertThat(collector.collect(backend.getName(), true).getValueBoolean("statsResetDetected")).isTrue();
      try(Connection connection = DbPerfPostgresSupport.connect(POSTGRES); Statement statement = connection.createStatement())
      {
         statement.execute("CREATE ROLE dbperf_limited LOGIN PASSWORD 'test-only'");
         statement.execute("GRANT USAGE ON SCHEMA public TO dbperf_limited");
         statement.execute("GRANT ALL ON ALL TABLES IN SCHEMA public TO dbperf_limited");
         statement.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO dbperf_limited");
      }
      RDBMSBackendMetaData limited = new PostgreSQLBackendMetaData().withName("limited" + UUID.randomUUID().toString().replace("-", ""))
         .withVendor("postgresql").withHostName(POSTGRES.getHost()).withDatabaseName(POSTGRES.getDatabaseName()).withUsername("dbperf_limited").withPassword("test-only");
      limited.setJdbcUrl(POSTGRES.getJdbcUrl());
      QInstance limitedInstance = DbPerfTestSupport.instance(limited);
      DbPerfRuntime limitedRuntime = DbPerfRuntime.of(limitedInstance);
      QRecord partial = new SnapshotCollector(limitedRuntime).collect(limited.getName(), true);
      assertThat(partial.getValueString("status")).as(partial.getValueString("errorMessage")).isEqualTo("PARTIAL");
      assertThat(new DbPerfSettingsService(limitedRuntime).ensure(limited.getName()).getValueString("statsAccessMessage")).contains("pg_read_all_stats");
   }
}
