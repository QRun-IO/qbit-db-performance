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


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.explain.ExplainRequest;
import com.kingsrook.qbits.dbperformance.explain.ExplainResult;
import com.kingsrook.qbits.dbperformance.explain.ExplainService;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.core.QueryExecutor;
import org.postgresql.jdbc.PgStatement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;


/*******************************************************************************
 ** Driver contract tests: cleanup, state restoration, unknown binding and audit isolation.
 ** These supplement, but do not replace, the real PostgreSQL acceptance tests.
 *******************************************************************************/
class ExplainConnectionTest
{
   private DbPerfRuntime runtime;
   private final List<String> events = new ArrayList<>();
   private int version = 17;
   private String failureState;
   private boolean autoCommit = true;
   private boolean readOnly = false;
   private String json = "[{\"Plan\":{\"Node Type\":\"Result\",\"Total Cost\":1.25,\"Actual Total Time\":0.1}}]";



   /*******************************************************************************
    ** Real H2 storage for the audit and settings; only plan execution JDBC is simulated.
    *******************************************************************************/
   @BeforeEach
   void setup() throws Exception
   {
      runtime = DbPerfRuntime.of(DbPerfTestSupport.instance());
   }



   /*******************************************************************************
    ** Parameters reach setObject as unknown types; success never commits the plan transaction.
    *******************************************************************************/
   @Test
   void analyzeCleanupAndBinding() throws Exception
   {
      ExplainResult result = service().explain(request("select $1::int", ExplainRequest.Mode.ANALYZE, List.of("876543")));
      assertThat(result.mode()).isEqualTo("ANALYZE");
      assertThat(result.explainId()).isNotNull();
      assertThat(result.actualTotalMs()).isEqualByComparingTo("0.1");
      assertThat(events).anyMatch(event -> event.startsWith("PREPARE dbperf_") && event.endsWith("AS select $1::int"));
      assertThat(events).anyMatch(event -> event.startsWith("EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) EXECUTE dbperf_") && event.endsWith("(?)"));
      assertThat(events).contains("bind:1:" + Types.OTHER).doesNotContain("commit");
      assertThat(events).noneMatch(event -> event.contains("876543"));
      assertThat(events.indexOf("rollback")).isLessThan(index("DEALLOCATE"));
      assertThat(events.getLast()).isEqualTo("close");
      assertThat(autoCommit).isTrue();
      assertThat(readOnly).isFalse();
      assertThat(audit().getValueString("planJson")).isNull();
      assertThat(audit().getValueString("outcome")).isEqualTo("OK");
   }



   /*******************************************************************************
    ** Driver cancellation is classified by SQLSTATE, not a potentially sensitive message.
    *******************************************************************************/
   @Test
   void timeoutAndErrorCleanup() throws Exception
   {
      for(String state : List.of("57014", "25006"))
      {
         failureState = state;
         events.clear();
         assertThatThrownBy(() -> service().explain(request("select $1::int", ExplainRequest.Mode.ANALYZE, List.of("876543"))))
            .hasMessageContaining(state.equals("57014") ? "timed out" : "Writes are not allowed").hasNoCause();
         assertThat(audit().getValueString("outcome")).isEqualTo(state.equals("57014") ? "TIMEOUT" : "ERROR");
         assertThat(audit().getValueString("errorMessage")).doesNotContain("private");
         assertThat(events.indexOf("rollback")).isLessThan(index("DEALLOCATE"));
         assertThat(events).doesNotContain("commit");
         assertThat(autoCommit).isTrue();
      }
   }



   /*******************************************************************************
    ** Generic version guard, literal-free storage, and normalized statement-source linkage.
    *******************************************************************************/
   @Test
   void planStorageAndVersion() throws Exception
   {
      assertThat(service().explain(request("select current_date", ExplainRequest.Mode.PLAN, List.of())).mode()).isEqualTo("PLAN");
      assertThat(audit().getValueString("planJson")).isEqualTo(json);
      assertThat(events).contains("SET TRANSACTION READ ONLY", "SET LOCAL statement_timeout = 10000", "rollback");
      version = 15;
      assertThatThrownBy(() -> service().explain(request("select $1::int", ExplainRequest.Mode.PLAN, List.of()))).hasMessageContaining("PostgreSQL 16");
      version = 17;
      QRecord statement = DbPerfStore.insert("db_perf_statement", List.of(new QRecord().withValue("backendName", runtime.getConfig().getBackendName())
         .withValue("queryId", 42L).withValue("queryText", "select $1::int"))).getFirst();
      ExplainResult generic = service().explain(new ExplainRequest(runtime.getConfig().getBackendName(), statement.getValueInteger("id"), null, ExplainRequest.Mode.PLAN, List.of()));
      assertThat(generic.mode()).isEqualTo("GENERIC");
      assertThat(audit().getValueInteger("statementId")).isEqualTo(statement.getValueInteger("id"));
      assertThat(audit().getValueString("planJson")).isEqualTo(json);
      assertThat(events).contains("EXPLAIN (GENERIC_PLAN, FORMAT JSON) select $1::int");
      service().explain(request("SELECT 'private'", ExplainRequest.Mode.PLAN, List.of()));
      assertThat(audit().getValueString("planJson")).isNull();
      assertThat(audit().getValueString("sqlText")).isEqualTo("select ?");
   }



   /*******************************************************************************
    ** Pool state can start non-default; no result can escape without valid JSON.
    *******************************************************************************/
   @Test
   void restoreNonDefaultAndBadPlan() throws Exception
   {
      autoCommit = false;
      readOnly = true;
      json = "{}";
      assertThatThrownBy(() -> service().explain(request("select current_date", ExplainRequest.Mode.PLAN, List.of()))).hasMessageContaining("Query plan failed");
      assertThat(autoCommit).isFalse();
      assertThat(readOnly).isTrue();
      assertThat(events).contains("rollback", "close");
      assertThat(audit().getValueString("outcome")).isEqualTo("ERROR");
   }



   /*******************************************************************************
    ** A separate connection boundary keeps real QQQ settings and audits untouched.
    *******************************************************************************/
   private ExplainService service()
   {
      return (new ExplainService(runtime)
      {
         @Override
         protected Connection openConnection()
         {
            return (connection());
         }
      });
   }



   /*******************************************************************************
    ** JDBC protocol recorder. Unexpected calls fail instead of silently succeeding.
    *******************************************************************************/
   private Connection connection()
   {
      return (proxy(Connection.class, (proxy, method, args) ->
      {
         switch(method.getName())
         {
            case "getMetaData":
               return (proxy(DatabaseMetaData.class, (p, m, a) -> m.getName().equals("getDatabaseProductName") ? "PostgreSQL" : version));
            case "getAutoCommit":
               return (autoCommit);
            case "isReadOnly":
               return (readOnly);
            case "setAutoCommit":
               autoCommit = (Boolean) args[0];
               events.add("autoCommit:" + autoCommit);
               return (null);
            case "setReadOnly":
               readOnly = (Boolean) args[0];
               events.add("readOnly:" + readOnly);
               return (null);
            case "createStatement":
               return (statement(false));
            case "prepareStatement":
               events.add((String) args[0]);
               return (statement(true));
            case "rollback", "commit", "close":
               events.add(method.getName());
               return (null);
            default:
               throw (new AssertionError("Unexpected connection call: " + method.getName()));
         }
      }));
   }



   /*******************************************************************************
    ** Record only bind position and type, never the synthetic sensitive fixture value.
    *******************************************************************************/
   private Statement statement(boolean prepared)
   {
      InvocationHandler handler = (proxy, method, args) ->
      {
         switch(method.getName())
         {
            case "execute":
               events.add((String) args[0]);
               return (false);
            case "unwrap":
               return (proxy);
            case "executeWithFlags":
               assertThat(args[args.length - 1]).isEqualTo(QueryExecutor.QUERY_EXECUTE_AS_SIMPLE);
               if(args.length == 2)
               {
                  events.add((String) args[0]);
               }
               if(failureState != null)
               {
                  throw (new SQLException("private-value in driver error", failureState));
               }
               return (true);
            case "getResultSet":
               return (proxy(ResultSet.class, (p, m, a) -> switch(m.getName())
               {
                  case "next" -> true;
                  case "getString" -> json;
                  case "close" -> null;
                  default -> throw new AssertionError("Unexpected result call: " + m.getName());
               }));
            case "setObject":
               assertThat(args[1]).isEqualTo("876543");
               events.add("bind:" + args[0] + ":" + args[2]);
               return (null);
            case "close":
               return (null);
            default:
               throw (new AssertionError("Unexpected statement call: " + method.getName()));
         }
      };
      var settings = withSettings().defaultAnswer(invocation -> handler.invoke(invocation.getMock(), invocation.getMethod(), invocation.getArguments()));
      if(prepared)
      {
         settings.extraInterfaces(PreparedStatement.class);
      }
      return (mock(PgStatement.class, settings));
   }



   /*******************************************************************************
    ** Type-safe interface proxy for the small JDBC contract exercised above.
    *******************************************************************************/
   private static <T> T proxy(Class<T> type, InvocationHandler handler)
   {
      return (type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler)));
   }



   /*******************************************************************************
    ** Position of a protocol event for ordering assertions.
    *******************************************************************************/
   private int index(String prefix)
   {
      for(int i = 0; i < events.size(); i++)
      {
         if(events.get(i).startsWith(prefix))
         {
            return (i);
         }
      }
      throw (new AssertionError("Missing event " + prefix));
   }



   /*******************************************************************************
    ** Current backend request.
    *******************************************************************************/
   private ExplainRequest request(String sql, ExplainRequest.Mode mode, List<String> parameters)
   {
      return (new ExplainRequest(runtime.getConfig().getBackendName(), null, sql, mode, parameters));
   }



   /*******************************************************************************
    ** Most recent persisted audit.
    *******************************************************************************/
   private QRecord audit() throws Exception
   {
      return (DbPerfStore.first("db_perf_explain", new QQueryFilter().withOrderBy(new QFilterOrderBy("id", false))));
   }
}
