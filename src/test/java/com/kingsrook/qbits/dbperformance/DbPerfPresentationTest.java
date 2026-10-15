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


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dbperformance.api.ActivitySpec;
import com.kingsrook.qbits.dbperformance.api.DbPerformanceData;
import com.kingsrook.qbits.dbperformance.api.ExplainSpec;
import com.kingsrook.qbits.dbperformance.api.LookupSpec;
import com.kingsrook.qbits.dbperformance.api.OverviewSpec;
import com.kingsrook.qbits.dbperformance.collect.DbPerfSettingsService;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.explain.ExplainRequest;
import com.kingsrook.qbits.dbperformance.explain.ExplainService;
import com.kingsrook.qbits.dbperformance.explain.QueryPlanProcess;
import com.kingsrook.qbits.dbperformance.normalize.SqlNormalizer;
import com.kingsrook.qbits.dbperformance.provider.SessionInfo;
import com.kingsrook.qbits.dbperformance.widgets.DbPerfWidgetRenderer;
import com.kingsrook.qbits.dbperformance.widgets.DbPerfWidgets;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChartData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.MultiTableData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.AbstractJsonEndpointSpec.JsonInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** H2 fixtures exercise real QQQ reads, typed widgets and the actual spec handlers.
 *******************************************************************************/
class DbPerfPresentationTest
{
   private DbPerfRuntime runtime;
   private DbPerformanceData data;



   /*******************************************************************************
    ** Isolated migrated tables and production producer registration.
    *******************************************************************************/
   @BeforeEach
   void setup() throws Exception
   {
      runtime = DbPerfRuntime.of(DbPerfTestSupport.instance());
      data = new DbPerformanceData(runtime);
   }



   /*******************************************************************************
    ** Every home widget renders an honest guided empty state and requires view.
    *******************************************************************************/
   @Test
   void emptyWidgetsAndPermissions() throws Exception
   {
      assertThat(runtime.getInstance().getApp("dbPerformance").getWidgets()).containsExactlyElementsOf(DbPerfWidgets.HOME);
      for(String name : DbPerfWidgets.HOME)
      {
         var meta = runtime.getInstance().getWidget(name);
         assertThat(meta.getGridColumns()).isEqualTo(12);
         assertThat(meta.getPermissionRules().getPermissionBaseName()).isEqualTo(DbPerfPermissions.VIEW);
         var output = new DbPerfWidgetRenderer().render(new RenderWidgetInput().withWidgetMetaData(meta));
         assertThat(output.getWidgetData()).isNotNull();
         if(output.getWidgetData() instanceof TableData table)
         {
            assertThat(table.getFixedHeight()).isNull();
            assertThat(table.getRowsPerPage()).isEqualTo(10);
         }
      }
      assertThat(new DbPerfWidgetRenderer().topQueries(data).getNoRowsFoundHTML()).contains("two snapshots");
      assertThat(new DbPerfWidgetRenderer().liveActivity(data).getNoRowsFoundHTML()).isEqualTo("No long-running or blocked sessions right now");
      assertThat(new DbPerfWidgetRenderer().attribution().getTableDataList().getFirst().getNoRowsFoundHTML()).isEqualTo("Attribution starts after the next flush");
      assertThat(new DbPerfWidgetRenderer().connectionStatistics(data).getType()).isEqualTo("multiStatistics");
      assertThat(new DbPerfWidgetRenderer().connectionsByApplication(runtime).getRows()).isEmpty();
      QContext.setQSession(new QSession());
      for(String name : DbPerfWidgets.HOME)
      {
         assertThatThrownBy(() -> new DbPerfWidgetRenderer().render(new RenderWidgetInput().withWidgetMetaData(runtime.getInstance().getWidget(name)))).hasMessageContaining("permission");
      }
      assertThatThrownBy(() -> new OverviewSpec().handle(new JsonInput())).hasMessageContaining("permission");
      assertThatThrownBy(() -> new ActivitySpec().handle(new JsonInput())).hasMessageContaining("permission");
      assertThatThrownBy(() -> new LookupSpec().handle(new JsonInput())).hasMessageContaining("permission");
   }



   /*******************************************************************************
    ** Persisted rows change the API and widgets, with correct units and record links.
    *******************************************************************************/
   @Test
   @SuppressWarnings("unchecked")
   void fixtureRows() throws Exception
   {
      fixture();
      Map<String, Object> overview = (Map<String, Object>) new OverviewSpec().handle(new JsonInput());
      assertThat((List<?>) overview.get("topStatements")).hasSize(1);
      assertThat((Map<String, Integer>) overview.get("findingsBySeverity")).containsEntry("HIGH", 1);
      TableData queries = new DbPerfWidgetRenderer().topQueries(data);
      assertThat(queries.getRows()).hasSize(1);
      assertThat((BigDecimal) queries.getRows().getFirst().get("Share of DB time %")).isEqualByComparingTo("25");
      assertThat(JsonUtils.toJson(queries)).contains("/app/dbPerformance/db_perf_statement/", "composite").doesNotContain("private-value");
      ChartData chart = new DbPerfWidgetRenderer().databaseTime(data);
      assertThat(chart.getChartData().getDatasets()).hasSize(2);
      assertThat(chart.getChartData().getDatasets().getFirst().getData()).hasSize(1);
      assertThat(new DbPerfWidgetRenderer().findings(data).getRows()).hasSize(1);
      assertThat(new DbPerfWidgetRenderer().tables(data).getTableDataList().getFirst().getRows()).hasSize(1);
      MultiTableData attribution = new DbPerfWidgetRenderer().attribution();
      assertThat(JsonUtils.toJson(attribution.getTableDataList().getFirst().getRows().getFirst())).contains("top table patient");
      assertThat(attribution.getTableDataList().get(1).getRows()).hasSize(1);
      assertThat(JsonUtils.toJson(new DbPerfWidgetRenderer().connectionStatistics(data))).contains("Deadlocks", "Cache hit");
      JsonInput lookup = new JsonInput();
      lookup.getQueryParams().put("sql", "SELECT * FROM patient WHERE name = 'different'");
      assertThat((List<?>) ((Map<?, ?>) new LookupSpec().handle(lookup)).get("matches")).hasSize(1);
      runtime.getConfig().withAppName("custom");
      assertThat(data.recordPath("db_perf_statement", 1)).isEqualTo("/app/custom/db_perf_statement/1");
      JsonInput invalid = new JsonInput();
      invalid.getQueryParams().put("backend", "other");
      assertThatThrownBy(() -> new OverviewSpec().handle(invalid)).hasMessageContaining("monitored backend");
      assertThatThrownBy(() -> new LookupSpec().handle(new JsonInput())).hasMessageContaining("Supply SQL");
   }



   /*******************************************************************************
    ** Raw text is permission gated; transitive blocking includes indirect descendants.
    *******************************************************************************/
   @Test
   @SuppressWarnings("unchecked")
   void liveTextAndBlocking() throws Exception
   {
      List<SessionInfo> sessions = List.of(session(10, List.of()), session(20, List.of(10)), session(30, List.of(20)));
      QContext.setQSession(new QSession().withPermissions(DbPerfPermissions.VIEW));
      Map<String, Object> redacted = data.activity(sessions, true);
      assertThat(JsonUtils.toJson(redacted)).doesNotContain("private-value").contains("liveTextRedacted");
      List<Map<String, Object>> chains = (List<Map<String, Object>>) redacted.get("blockingChains");
      assertThat(chains).hasSize(1);
      assertThat(chains.getFirst()).containsEntry("rootPid", 10).containsEntry("blockedPids", List.of(20, 30));
      QContext.setQSession(new QSession().withPermissions(DbPerfPermissions.VIEW, DbPerfPermissions.VIEW_LIVE_TEXT));
      assertThat(JsonUtils.toJson(data.activity(sessions, true))).contains("private-value");
      assertThat(JsonUtils.toJson(data.activity(sessions, false))).doesNotContain("private-value");
      assertThat(((Map<?, ?>) new ActivitySpec().handle(new JsonInput())).get("sessions")).isEqualTo(List.of());
   }



   /*******************************************************************************
    ** The actual home renderer requests redacted live data, even for privileged viewers.
    *******************************************************************************/
   @Test
   void liveWidgetRedaction() throws Exception
   {
      List<SessionInfo> sessions = List.of(session(10, List.of()), session(20, List.of(10)));
      DbPerformanceData fixtureData = new DbPerformanceData(runtime)
      {
         @Override
         public Map<String, Object> activity(String backend, boolean allowRawText) throws Exception
         {
            assertThat(allowRawText).isFalse();
            return (activity(sessions, allowRawText));
         }
      };
      QContext.setQSession(new QSession().withPermissions(DbPerfPermissions.VIEW));
      TableData table = new DbPerfWidgetRenderer().liveActivity(fixtureData);
      assertThat(table.getRows()).hasSize(2);
      assertThat(JsonUtils.toJson(table)).doesNotContain("private-value").contains("select * from patient where name = ?", "blocks 1", "blocked by");
      QContext.setQSession(new QSession().withPermissions(DbPerfPermissions.VIEW, DbPerfPermissions.VIEW_LIVE_TEXT));
      assertThat(JsonUtils.toJson(new DbPerfWidgetRenderer().liveActivity(fixtureData))).doesNotContain("private-value");
   }



   /*******************************************************************************
    ** The record action requires one statement and removes parameter text on every exit.
    *******************************************************************************/
   @Test
   void recordAction() throws Exception
   {
      var process = runtime.getInstance().getProcess(QueryPlanProcess.NAME);
      assertThat(process.getTableName()).isEqualTo("db_perf_statement");
      assertThat(process.getMinInputRecords()).isEqualTo(1);
      assertThat(process.getMaxInputRecords()).isEqualTo(1);
      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("parameterValues", "private-value");
      assertThatThrownBy(() -> new QueryPlanProcess().run(input, new RunBackendStepOutput())).hasMessageContaining("exactly one");
      assertThat(input.getValues()).doesNotContainKey("parameterValues");
      input.withRecords(List.of(new QRecord().withValue("id", 1)));
      input.addValue("mode", "ANALYZE");
      input.addValue("parameterValues", "private-value\nsecond-value");
      QContext.setQSession(new QSession().withPermissions(DbPerfPermissions.VIEW, DbPerfPermissions.EXPLAIN));
      assertThatThrownBy(() -> new QueryPlanProcess().run(input, new RunBackendStepOutput())).hasMessageContaining(DbPerfPermissions.EXPLAIN_ANALYZE);
      assertThat(input.getValues()).doesNotContainKey("parameterValues");
      input.addValue("mode", "INVALID");
      assertThatThrownBy(() -> new QueryPlanProcess().run(input, new RunBackendStepOutput())).hasMessageContaining("Choose PLAN");
      assertThat(runtime.getInstance().getProcesses().values()).noneMatch(p -> "db_perf_explain".equals(p.getTableName()));
   }



   /*******************************************************************************
    ** Audit denied, invalid and unsupported-vendor paths using H2; no JDBC execution.
    *******************************************************************************/
   @Test
   void explainAuditAndHandler() throws Exception
   {
      ExplainService service = new ExplainService(runtime);
      assertThatThrownBy(() -> service.explain(new ExplainRequest(runtime.getConfig().getBackendName(), null, "select current_date", ExplainRequest.Mode.PLAN, List.of()))).hasMessageContaining("PostgreSQL");
      assertThatThrownBy(() -> service.explain(null)).hasMessageContaining("Choose PLAN");
      assertThatThrownBy(() -> service.explain(new ExplainRequest(runtime.getConfig().getBackendName(), null, null, ExplainRequest.Mode.PLAN, List.of()))).hasMessageContaining("either");
      QContext.setQSession(new QSession());
      JsonInput input = new JsonInput();
      input.getBody().put("mode", "ANALYZE");
      input.getBody().put("sql", "select 'private-value'");
      input.getBody().put("parameters", List.of("private-value"));
      assertThatThrownBy(() -> new ExplainSpec().handle(input)).hasMessageContaining(DbPerfPermissions.EXPLAIN_ANALYZE);
      List<QRecord> rows = runtime.asSystem(() -> DbPerfStore.query("db_perf_explain", new QQueryFilter()));
      assertThat(rows).hasSize(4);
      assertThat(rows).anyMatch(row -> "DENIED".equals(row.getValueString("outcome")));
      assertThat(JsonUtils.toJson(rows)).doesNotContain("private-value");
      input.getBody().put("parameters", List.of(5));
      assertThatThrownBy(() -> new ExplainSpec().handle(input)).hasMessageContaining("array of strings");
      input.getBody().put("statementId", "bad");
      assertThatThrownBy(() -> new ExplainSpec().handle(input)).hasMessageContaining("integer");
   }



   /*******************************************************************************
    ** A realistic session whose raw text must not reach a home widget or storage.
    *******************************************************************************/
   private static SessionInfo session(int pid, List<Integer> blockedBy)
   {
      return (new SessionInfo(pid, "app", "QQQ", null, "active", "Lock", "transactionid", "client backend", Instant.now(), Instant.now(), Instant.now(),
         BigDecimal.valueOf(60), BigDecimal.valueOf(60), blockedBy, 1L, "select * from patient where name = 'private-value'"));
   }



   /*******************************************************************************
    ** One row in every presentation source proves the renderer consumes persisted data.
    *******************************************************************************/
   private void fixture() throws Exception
   {
      new DbPerfSettingsService(runtime).ensure(runtime.getConfig().getBackendName());
      QRecord statement = DbPerfStore.insert("db_perf_statement", List.of(new QRecord().withValue("backendName", runtime.getConfig().getBackendName()).withValue("queryId", 1L)
         .withValue("queryText", "select * from patient where name = ?").withValue("fingerprint", SqlNormalizer.fingerprint(SqlNormalizer.normalize("select * from patient where name = ?")))
         .withValue("calls24h", 5L).withValue("totalMs24h", BigDecimal.valueOf(50)).withValue("meanMs24h", BigDecimal.TEN).withValue("shareOfDbTime24h", BigDecimal.valueOf(25)))).getFirst();
      QRecord snapshot = DbPerfStore.insert("db_perf_snapshot", List.of(new QRecord().withValue("backendName", runtime.getConfig().getBackendName()).withValue("capturedAt", Instant.now())
         .withValue("status", "OK").withValue("statementTotalMs", BigDecimal.valueOf(200)).withValue("statementCalls", 20L).withValue("connectionsTotal", 5).withValue("maxConnections", 100)
         .withValue("cacheHitRatio", new BigDecimal("0.95")).withValue("deadlocks", 2L).withValue("tempBytes", 1048576L))).getFirst();
      DbPerfStore.insert("db_perf_query_snapshot", List.of(new QRecord().withValue("statementId", statement.getValueInteger("id")).withValue("snapshotId", snapshot.getValueInteger("id"))
         .withValue("capturedAt", Instant.now()).withValue("sharedBlksHit", 90L).withValue("sharedBlksRead", 10L)));
      DbPerfStore.insert("db_perf_finding", List.of(new QRecord().withValue("backendName", runtime.getConfig().getBackendName()).withValue("dedupeKey", "test").withValue("status", "OPEN")
         .withValue("severity", "HIGH").withValue("title", "Slow query").withValue("ruleCode", "SLOW_QUERY").withValue("score", BigDecimal.TEN)));
      DbPerfStore.insert("db_perf_table_stat", List.of(new QRecord().withValue("backendName", runtime.getConfig().getBackendName()).withValue("schemaName", "public").withValue("tableName", "patient").withValue("relid", 1L)));
      DbPerfStore.insert("db_perf_attribution", List.of(new QRecord().withValue("windowStart", Instant.now().minusSeconds(60)).withValue("windowEnd", Instant.now()).withValue("unitKind", "PROCESS")
         .withValue("unitName", "sync").withValue("tableName", "patient").withValue("statements", 3L).withValue("totalMs", BigDecimal.TEN)));
      DbPerfStore.insert("db_perf_repeat_pattern", List.of(new QRecord().withValue("windowStart", Instant.now().minusSeconds(60)).withValue("windowEnd", Instant.now()).withValue("unitKind", "PROCESS")
         .withValue("unitName", "sync").withValue("maxRepeats", 12L)));
   }
}
