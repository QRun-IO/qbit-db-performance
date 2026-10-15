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
import java.util.List;
import com.kingsrook.qbits.dbperformance.collect.DbPerfSettingsService;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.explain.ExplainRequest;
import com.kingsrook.qbits.dbperformance.explain.ExplainResult;
import com.kingsrook.qbits.dbperformance.explain.ExplainService;
import com.kingsrook.qbits.dbperformance.explain.QueryPlanProcess;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** PostgreSQL 17 acceptance tests for permission, PHI and READ ONLY boundaries.
 *******************************************************************************/
@Testcontainers
class ExplainServiceTest
{
   @Container
   static final PostgreSQLContainer<?> POSTGRES = DbPerfPostgresSupport.container();
   private DbPerfRuntime runtime;
   private ExplainService service;



   /*******************************************************************************
    ** A volatile function attempts a write even though its caller is a SELECT.
    *******************************************************************************/
   @BeforeAll
   static void fixture() throws Exception
   {
      try(Connection connection = DbPerfPostgresSupport.connect(POSTGRES); Statement statement = connection.createStatement())
      {
         statement.execute("CREATE TABLE explain_probe (id integer)");
         statement.execute("CREATE FUNCTION explain_write() RETURNS integer LANGUAGE plpgsql VOLATILE AS $$ BEGIN INSERT INTO explain_probe VALUES (1); RETURN 1; END $$");
      }
   }



   /*******************************************************************************
    ** Fresh QQQ runtime, sharing the container's migrated schema.
    *******************************************************************************/
   @BeforeEach
   void setup() throws Exception
   {
      runtime = DbPerfRuntime.of(DbPerfTestSupport.instance(DbPerfPostgresSupport.backend(POSTGRES)));
      service = new ExplainService(runtime);
   }



   /*******************************************************************************
    ** Literal-free PLAN is persisted, while literals and ANALYZE are transient.
    *******************************************************************************/
   @Test
   void planAndGeneric() throws Exception
   {
      ExplainResult plain = explain("select * from explain_probe", ExplainRequest.Mode.PLAN, List.of());
      assertThat(plain.mode()).isEqualTo("PLAN");
      assertThat(plain.explainId()).isNotNull();
      assertThat(audit().getValueString("planJson")).isEqualTo(plain.planJson());
      assertThat(plain.planText()).contains("Seq Scan", "explain_probe", "Total Cost", "Plan Rows");
      assertThat(plain.sqlWorkbenchUrl()).contains("?sql=EXPLAIN+", "&backend=");
      ExplainResult generic = explain("select * from explain_probe where id = $1", ExplainRequest.Mode.PLAN, List.of());
      assertThat(generic.mode()).isEqualTo("GENERIC");
      assertThat(explain("select * from explain_probe where id = ?", ExplainRequest.Mode.PLAN, List.of()).mode()).isEqualTo("GENERIC");
      ExplainResult literal = explain("select * from explain_probe where id = 987654", ExplainRequest.Mode.PLAN, List.of());
      assertThat(audit().getValueString("planJson")).isNull();
      assertThat(audit().getValueString("sqlText")).doesNotContain("987654");
      assertThat(literal.assistantPrompt()).doesNotContain("987654");
   }



   /*******************************************************************************
    ** Bind values as unknown JDBC values, never in SQL interpolation or persistent rows.
    *******************************************************************************/
   @Test
   void analyze() throws Exception
   {
      ExplainResult result = explain("select * from explain_probe where id = $1", ExplainRequest.Mode.ANALYZE, List.of("987654"));
      assertThat(result.actualTotalMs()).isNotNull();
      assertThat(result.mode()).isEqualTo("ANALYZE");
      assertThat(result.assistantPrompt()).doesNotContain("987654");
      assertThat(audit().getValueString("outcome")).isEqualTo("OK");
      assertThat(audit().getValueString("planJson")).isNull();
      assertThat(audit().getValueString("planText")).isNull();
      assertThat(audit().getValueInteger("parameterCount")).isEqualTo(1);
      assertThat(explain("select $1::text", ExplainRequest.Mode.ANALYZE, List.of("'; DELETE FROM explain_probe; --")).actualTotalMs()).isNotNull();
      assertThat(explain("select current_date", ExplainRequest.Mode.ANALYZE, List.of()).actualTotalMs()).isNotNull();
   }



   /*******************************************************************************
    ** Rejected requests still receive sanitized audit rows.
    *******************************************************************************/
   @Test
   void rejectUnsafeSql() throws Exception
   {
      for(String sql : List.of("DELETE FROM explain_probe", "WITH d AS (DELETE FROM explain_probe RETURNING *) SELECT * FROM d",
         "SELECT * FROM explain_probe; DELETE FROM explain_probe", "SELECT * INTO probe_copy FROM explain_probe", "SELECT * FROM explain_probe FOR UPDATE",
         "SELECT * FROM explain_probe FOR SHARE", "WITH d AS (SELECT 1) DELETE FROM explain_probe"))
      {
         assertThatThrownBy(() -> explain(sql, ExplainRequest.Mode.ANALYZE, List.of())).isInstanceOf(com.kingsrook.qqq.backend.core.exceptions.QBadRequestException.class).satisfies(e -> assertThat(e.getCause()).isNull());
         assertThat(audit().getValueString("outcome")).isEqualTo("ERROR");
      }
      assertThatThrownBy(() -> explain("select $1::int", ExplainRequest.Mode.ANALYZE, List.of())).hasMessageContaining("exactly 1");
      assertThatThrownBy(() -> explain("select current_date", ExplainRequest.Mode.PLAN, List.of("secret"))).hasMessageContaining("only for ANALYZE");
   }



   /*******************************************************************************
    ** A write hidden in a volatile function is stopped by the database itself.
    *******************************************************************************/
   @Test
   void readOnlyAndTimeout() throws Exception
   {
      assertThatThrownBy(() -> explain("select explain_write()", ExplainRequest.Mode.ANALYZE, List.of())).hasMessageContaining("Writes are not allowed");
      try(Connection connection = DbPerfPostgresSupport.connect(POSTGRES); Statement statement = connection.createStatement(); var rows = statement.executeQuery("SELECT count(*) FROM explain_probe"))
      {
         rows.next();
         assertThat(rows.getInt(1)).isZero();
      }
      QRecord settings = new DbPerfSettingsService(runtime).ensure(runtime.getConfig().getBackendName());
      DbPerfStore.update("db_perf_settings", List.of(new QRecord().withValue("id", settings.getValueInteger("id")).withValue("explainTimeoutMs", 10)));
      assertThatThrownBy(() -> explain("select pg_sleep($1)", ExplainRequest.Mode.ANALYZE, List.of("1"))).hasMessageContaining("timed out");
      assertThat(audit().getValueString("outcome")).isEqualTo("TIMEOUT");
      assertThat(explain("select current_date", ExplainRequest.Mode.PLAN, List.of()).mode()).isEqualTo("PLAN");
   }



   /*******************************************************************************
    ** Denial is audited as system but caller identity and context are retained.
    *******************************************************************************/
   @Test
   void deniedAndValidation() throws Exception
   {
      QSession caller = new QSession().withUser(new QUser().withIdReference("viewer"));
      QContext.setQSession(caller);
      assertThatThrownBy(() -> explain("select 'private'", ExplainRequest.Mode.PLAN, List.of())).hasMessageContaining(DbPerfPermissions.EXPLAIN);
      assertThat(QContext.getQSession()).isSameAs(caller);
      QRecord denied = runtime.asSystem(this::audit);
      assertThat(denied.getValueString("outcome")).isEqualTo("DENIED");
      assertThat(denied.getValueString("requestedBy")).isEqualTo("viewer");
      assertThat(denied.getValueString("sqlText")).doesNotContain("private");
      caller.withPermissions(DbPerfPermissions.EXPLAIN);
      assertThatThrownBy(() -> explain("select current_date", ExplainRequest.Mode.ANALYZE, List.of())).hasMessageContaining(DbPerfPermissions.EXPLAIN_ANALYZE);
      assertThatThrownBy(() -> service.explain(new ExplainRequest("wrong", null, "select current_date", ExplainRequest.Mode.PLAN, List.of()))).hasMessageContaining("monitored backend");
      assertThatThrownBy(() -> service.explain(new ExplainRequest(runtime.getConfig().getBackendName(), 999999, null, ExplainRequest.Mode.PLAN, List.of()))).hasMessageContaining("not found");
   }



   /*******************************************************************************
    ** The record action carries a real PostgreSQL plan into copyable result fields.
    *******************************************************************************/
   @Test
   void recordActionResult() throws Exception
   {
      QRecord statement = DbPerfStore.insert("db_perf_statement", List.of(new QRecord().withValue("backendName", runtime.getConfig().getBackendName())
         .withValue("queryId", 123456L).withValue("queryText", "select * from explain_probe where id = ?"))).getFirst();
      RunBackendStepInput input = new RunBackendStepInput().withRecords(List.of(statement));
      RunBackendStepOutput output = new RunBackendStepOutput();
      new QueryPlanProcess().run(input, output);
      assertThat(output.getValueString("planText")).contains("explain_probe");
      assertThat(output.getValueBigDecimal("totalCost")).isNotNull();
      assertThat(output.getValueString("sqlWorkbenchUrl")).contains("?sql=EXPLAIN");
      assertThat(output.getValueString("assistantPrompt")).contains("Query:", "Plan:");
      assertThat(audit().getValueString("planJson")).isNotNull();
      input.addValue("mode", "ANALYZE");
      input.addValue("parameterValues", "987654");
      new QueryPlanProcess().run(input, output);
      assertThat(input.getValues()).doesNotContainKey("parameterValues");
      assertThat(audit().getValueString("planJson")).isNull();
      assertThat(output.getValueString("assistantPrompt")).doesNotContain("987654");
   }



   /*******************************************************************************
    ** Use the configured backend for each direct SQL request.
    *******************************************************************************/
   private ExplainResult explain(String sql, ExplainRequest.Mode mode, List<String> parameters) throws Exception
   {
      return (service.explain(new ExplainRequest(runtime.getConfig().getBackendName(), null, sql, mode, parameters)));
   }



   /*******************************************************************************
    ** Last audit across the shared container.
    *******************************************************************************/
   private QRecord audit() throws Exception
   {
      return (DbPerfStore.first("db_perf_explain", new QQueryFilter().withOrderBy(new QFilterOrderBy("id", false))));
   }
}
