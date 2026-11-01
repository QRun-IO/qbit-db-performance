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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.kingsrook.qbits.dbperformance.analyze.AnalyzeService;
import com.kingsrook.qbits.dbperformance.api.DbPerformanceData;
import com.kingsrook.qbits.dbperformance.collect.DbPerfSettingsService;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.collect.SnapshotCollector;
import com.kingsrook.qbits.dbperformance.explain.ExplainRequest;
import com.kingsrook.qbits.dbperformance.explain.ExplainResult;
import com.kingsrook.qbits.dbperformance.explain.ExplainService;
import com.kingsrook.qbits.dbperformance.processes.DbPerfEnableStatisticsStep;
import com.kingsrook.qbits.dbperformance.widgets.DbPerfWidgetRenderer;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** The qbit end to end on an H2 backend: collection, the enable action,
 ** feature-gated analysis, setup rows and text plans.
 *******************************************************************************/
class H2CollectionTest
{
   /*******************************************************************************
    ** Two collections store H2 statements, tables, indexes and connections.
    *******************************************************************************/
   @Test
   void collectAnalyzeAndExplain() throws Exception
   {
      DbPerfRuntime runtime = runtime(";QUERY_STATISTICS=TRUE;QUERY_STATISTICS_MAX_ENTRIES=1000");
      String backend = runtime.getConfig().getBackendName();
      workload(runtime);
      QRecord first = new SnapshotCollector(runtime).collect(backend, true);
      assertThat(first.getValueString("status")).as(first.getValueString("errorMessage")).isEqualTo("OK");
      workload(runtime);
      QRecord second = new SnapshotCollector(runtime).collect(backend, true);
      assertThat(second.getValueString("status")).as(second.getValueString("errorMessage")).isEqualTo("OK");
      assertThat(second.getValueInteger("intervalSeconds")).isNotNull();
      assertThat(second.getValue("cacheHitRatio")).isNull();
      assertThat(second.getValue("maxConnections")).isNull();
      assertThat(second.getValueInteger("connectionsTotal")).isPositive();

      QRecord settings = new DbPerfSettingsService(runtime).ensure(backend);
      assertThat(settings.getValueString("provider")).isEqualTo("H2");
      assertThat(settings.getValueString("statsAccess")).isEqualTo("FULL");

      List<QRecord> statements = DbPerfStore.query("db_perf_statement", DbPerfStore.eq("backendName", backend));
      assertThat(statements).anySatisfy(row ->
      {
         assertThat(row.getValueString("queryText")).containsIgnoringCase("h2_probe");
         assertThat(row.getValueLong("lastCalls")).isGreaterThanOrEqualTo(6);
      });
      assertThat(DbPerfStore.query("db_perf_query_snapshot", new QQueryFilter())).anySatisfy(row -> assertThat(row.getValueLong("calls")).isEqualTo(3));
      assertThat(DbPerfStore.query("db_perf_table_stat", DbPerfStore.eq("backendName", backend)))
         .anySatisfy(row -> assertThat(row.getValueString("tableName")).isEqualToIgnoringCase("h2_probe"));
      assertThat(DbPerfStore.query("db_perf_index_stat", DbPerfStore.eq("backendName", backend)))
         .anySatisfy(row -> assertThat(row.getValueString("indexName")).isEqualToIgnoringCase("h2_probe_name"));

      Map<String, String> results = new AnalyzeService(runtime).run(backend).stream().collect(Collectors.toMap(AnalyzeService.Result::ruleCode, r -> String.valueOf(r.error())));
      assertThat(results).containsKeys("SLOW_QUERY", "DUPLICATE_INDEX", "IDLE_IN_TRANSACTION", "N_PLUS_ONE")
         .doesNotContainKeys("UNUSED_INDEX", "MISSING_INDEX", "CACHE_HIT_LOW", "BLOAT", "VACUUM_STALE", "DEADLOCKS", "TEMP_SPILL", "POOL_SIZING");
      assertThat(results.values()).containsOnly("null");

      assertThat(new DbPerfWidgetRenderer().connectionsByApplication(runtime).getRows()).isNotEmpty();
      assertThat(new DbPerfWidgetRenderer().setup(runtime, new DbPerformanceData(runtime)).getRows()).extracting(row -> row.get("Check"))
         .contains("H2 QUERY_STATISTICS", "H2 admin user").doesNotContain("pg_read_all_stats");

      ExplainService explain = new ExplainService(runtime);
      ExplainResult plan = explain.explain(new ExplainRequest(backend, null, "select * from h2_probe where name = ?", ExplainRequest.Mode.PLAN, List.of()));
      assertThat(plan.mode()).isEqualTo("GENERIC");
      assertThat(plan.planJson()).isNull();
      assertThat(plan.planText()).containsIgnoringCase("h2_probe_name");
      assertThat(plan.assistantPrompt()).contains("database query plan");
      assertThatThrownBy(() -> explain.explain(new ExplainRequest(backend, null, "select * from h2_probe where name = $1", ExplainRequest.Mode.ANALYZE, List.of("secret-value"))))
         .hasMessageContaining("ANALYZE is not available");
      assertThatThrownBy(() -> explain.explain(new ExplainRequest(backend, null, "select file_write('x', '/tmp/p') from h2_probe", ExplainRequest.Mode.PLAN, List.of())))
         .hasMessageContaining("FILE_WRITE");
   }



   /*******************************************************************************
    ** Without QUERY_STATISTICS the snapshot is partial until the action runs.
    *******************************************************************************/
   @Test
   void enableActionTurnsOnStatistics() throws Exception
   {
      DbPerfRuntime runtime = runtime("");
      String backend = runtime.getConfig().getBackendName();
      QRecord partial = new SnapshotCollector(runtime).collect(backend, true);
      assertThat(partial.getValueString("status")).isEqualTo("PARTIAL");
      assertThat(partial.getValueString("errorMessage")).contains("QUERY_STATISTICS");
      assertThat(DbPerfStore.query("db_perf_table_stat", DbPerfStore.eq("backendName", backend))).isNotEmpty();

      RunBackendStepOutput output = new RunBackendStepOutput();
      new DbPerfEnableStatisticsStep().run(new RunBackendStepInput(), output);
      assertThat(String.valueOf(output.getValue("processResults"))).contains("FULL");
      assertThat(new DbPerfSettingsService(runtime).ensure(backend).getValueString("statsAccess")).isEqualTo("FULL");
      workload(runtime);
      assertThat(new SnapshotCollector(runtime).collect(backend, false).getValueString("status")).isEqualTo("OK");
   }



   /*******************************************************************************
    ** QQQ runtime on a fresh H2 backend with the given URL options.
    *******************************************************************************/
   private static DbPerfRuntime runtime(String options) throws Exception
   {
      String name = "dbperfh2" + UUID.randomUUID().toString().replace("-", "");
      RDBMSBackendMetaData backend = new RDBMSBackendMetaData().withName(name).withVendor("h2").withHostName("mem").withDatabaseName(name).withUsername("sa");
      backend.setJdbcUrl("jdbc:h2:mem:" + name + ";CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1;MODE=PostgreSQL" + options);
      return (DbPerfRuntime.of(DbPerfTestSupport.instance(backend)));
   }



   /*******************************************************************************
    ** A table with a duplicate-prone index and three identical lookups.
    *******************************************************************************/
   private static void workload(DbPerfRuntime runtime) throws Exception
   {
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) runtime.getInstance().getBackend(runtime.getConfig().getBackendName());
      try(Connection connection = DriverManager.getConnection(backend.getJdbcUrl(), "sa", ""); Statement statement = connection.createStatement())
      {
         statement.execute("CREATE TABLE IF NOT EXISTS h2_probe (id INT PRIMARY KEY, name VARCHAR(40))");
         statement.execute("CREATE INDEX IF NOT EXISTS h2_probe_name ON h2_probe (name)");
         statement.execute("MERGE INTO h2_probe KEY (id) SELECT x, 'n' || x FROM SYSTEM_RANGE(1, 20)");
         for(int i = 0; i < 3; i++)
         {
            statement.executeQuery("SELECT * FROM h2_probe WHERE name = 'n7'").close();
         }
      }
   }
}
