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

package com.kingsrook.qbits.dbperformance.explain;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.collect.DbPerfSettingsService;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.collect.SnapshotCollector;
import com.kingsrook.qbits.dbperformance.normalize.SqlNormalizer;
import com.kingsrook.qbits.dbperformance.provider.DbStatsProvider;
import com.kingsrook.qbits.dbperformance.provider.DbStatsProviders;
import com.kingsrook.qbits.dbperformance.provider.StatsFeature;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.postgresql.core.QueryExecutor;
import org.postgresql.jdbc.PgStatement;


/*******************************************************************************
 ** Audited, read-only plan execution on a dedicated monitored-backend connection.
 ** Neither database error messages nor supplied values enter the persistent audit.
 *******************************************************************************/
public class ExplainService
{
   private final DbPerfRuntime runtime;



   /*******************************************************************************
    ** Use the registered host runtime.
    *******************************************************************************/
   public ExplainService(DbPerfRuntime runtime)
   {
      this.runtime = runtime;
   }



   /*******************************************************************************
    ** Audit every request, including validation and authorization failures.
    *******************************************************************************/
   public ExplainResult explain(ExplainRequest request) throws Exception
   {
      long started = System.nanoTime();
      QSession session = QContext.getQSession();
      QRecord audit = new QRecord().withValue("requestedAt", Instant.now()).withValue("outcome", "ERROR")
         .withValue("requestedBy", session == null || session.getUser() == null ? null : session.getUser().getIdReference())
         .withValue("mode", request != null && request.mode() == ExplainRequest.Mode.ANALYZE ? "ANALYZE" : "PLAN")
         .withValue("parameterCount", request == null || request.parameters() == null ? 0 : request.parameters().size());
      String sql = request == null ? "" : request.sql();
      String json = null;
      PlanTextRenderer.Plan plan = null;
      QException failure = null;
      List<String> warnings = List.of();
      try
      {
         String permission = runtime.getConfig().getPermissionBaseName() + ("ANALYZE".equals(audit.getValueString("mode")) ? ".explain_analyze" : ".explain");
         if(session == null || !session.hasPermission(permission))
         {
            throw (new QPermissionDeniedException("Query plan permission required: " + permission));
         }
         if(request == null || request.mode() == null)
         {
            throw (new QBadRequestException("Choose PLAN or ANALYZE."));
         }
         if(!runtime.getConfig().getBackendName().equals(request.backendName()))
         {
            throw (new QBadRequestException("Choose the configured monitored backend."));
         }
         if((request.statementId() == null) == (sql == null || sql.isBlank()))
         {
            throw (new QBadRequestException("Supply either a statementId or SQL, not both."));
         }
         boolean storedStatement = request.statementId() != null;
         if(storedStatement)
         {
            QRecord row = runtime.asSystem(() -> DbPerfStore.first("db_perf_statement", DbPerfStore.eq("id", request.statementId())));
            if(row == null || !request.backendName().equals(row.getValueString("backendName")))
            {
               throw (new QBadRequestException("The statement was not found on the monitored backend."));
            }
            audit.setValue("statementId", request.statementId());
            sql = row.getValueString("queryText");
         }
         ExplainSql.Parsed parsed = ExplainSql.parse(sql);
         List<String> parameters = request.parameters() == null ? List.of() : request.parameters();
         audit.setValue("parameterCount", parsed.parameterCount());
         boolean analyze = request.mode() == ExplainRequest.Mode.ANALYZE;
         if(analyze)
         {
            parsed.requireSelect();
            if(parameters.size() != parsed.parameterCount())
            {
               throw (new QBadRequestException("Supply exactly " + parsed.parameterCount() + " parameter values for ANALYZE."));
            }
         }
         else if(!parameters.isEmpty())
         {
            throw (new QBadRequestException("Parameter values are accepted only for ANALYZE."));
         }
         boolean generic = !analyze && parsed.parameterCount() > 0;
         audit.setValue("mode", analyze ? "ANALYZE" : generic ? "GENERIC" : "PLAN");
         QRecord settings = new DbPerfSettingsService(runtime).ensure(request.backendName());
         Explained explained = execute(parsed, parameters, analyze, generic, SnapshotCollector.positive(settings, "explainTimeoutMs", 10000));
         json = explained.json();
         plan = explained.plan();
         if(!analyze && (storedStatement || sql.equals(SqlNormalizer.normalize(sql))))
         {
            audit.setValue("planJson", json);
            audit.setValue("planText", plan.text());
            audit.setValue("totalCost", plan.totalCost());
         }
         else
         {
            warnings = List.of("This plan is transient; only a literal-free request audit is stored.");
         }
         audit.setValue("outcome", "OK");
      }
      catch(QPermissionDeniedException e)
      {
         audit.setValue("outcome", "DENIED");
         failure = e;
      }
      catch(QBadRequestException e)
      {
         failure = e;
      }
      catch(Exception e)
      {
         boolean timeout = isTimeout(e);
         audit.setValue("outcome", timeout ? "TIMEOUT" : "ERROR");
         failure = new QException(timeout ? "Query plan timed out. Reduce the query scope or ask an administrator to review the explain timeout."
            : "Query plan failed. Check SQL, parameter types and database access. Writes are not allowed.");
      }
      finally
      {
         String normalized = SqlNormalizer.normalize(sql);
         audit.setValue("sqlText", normalized);
         audit.setValue("sqlFingerprint", SqlNormalizer.fingerprint(normalized));
         audit.setValue("durationMs", (System.nanoTime() - started) / 1000000);
         audit.setValue("errorMessage", failure == null ? null : failure.getMessage());
         QRecord inserted = runtime.asSystem(() -> DbPerfStore.insert("db_perf_explain", List.of(audit)).getFirst());
         audit.setValue("id", inserted.getValueInteger("id"));
      }
      if(failure != null)
      {
         throw (failure);
      }
      return (new ExplainResult(audit.getValueString("mode"), json, plan.text(), plan.totalCost(), plan.actualTotalMs(), warnings,
         audit.getValueInteger("id"), runtime.getConfig().getSqlWorkbenchRunnerPath() + "?sql=" + encode("EXPLAIN " + sql) + "&backend=" + encode(request.backendName()),
         "Explain this " + (json == null ? "database" : "PostgreSQL") + " query plan and suggest improvements.\n\nQuery:\n" + SqlNormalizer.normalize(sql) + "\n\nPlan:\n" + plan.safeText()));
   }



   /*******************************************************************************
    ** Roll back even on parse, execution or timeout failure, deallocate after rollback,
    ** and restore pool state before releasing this dedicated connection.
    *******************************************************************************/
   private Explained execute(ExplainSql.Parsed parsed, List<String> parameters, boolean analyze, boolean generic, int timeout) throws Exception
   {
      try(Connection connection = openConnection())
      {
         if(!"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName()))
         {
            //////////////////////////////////////////////////////////////////
            // other vendors plan through their provider as text (no JSON) //
            //////////////////////////////////////////////////////////////////
            DbStatsProvider provider = DbStatsProviders.forConnection(connection);
            if(!provider.features().contains(StatsFeature.EXPLAIN_TEXT) || analyze && !provider.features().contains(StatsFeature.EXPLAIN_ANALYZE)
               || generic && !provider.features().contains(StatsFeature.EXPLAIN_GENERIC))
            {
               throw (new QBadRequestException("Query plans are not available for this database (" + provider.getName() + ")."));
            }
            String text = provider.explainText(connection, parsed.indexedSql(), parameters, analyze, timeout);
            return (new Explained(null, new PlanTextRenderer.Plan(text, PlanTextRenderer.redactText(text), null, null)));
         }
         return (new Explained(postgres(connection, parsed.sql(), parameters, analyze, generic, timeout), null).rendered());
      }
   }



   /*******************************************************************************
    ** PostgreSQL JSON plan; see {@link #execute}.
    *******************************************************************************/
   private String postgres(Connection connection, String sql, List<String> parameters, boolean analyze, boolean generic, int timeout) throws Exception
   {
      if(generic && connection.getMetaData().getDatabaseMajorVersion() < 16)
      {
         throw (new QBadRequestException("Generic plans require PostgreSQL 16 or later. Use ANALYZE with parameter values if authorized."));
      }
      boolean autoCommit = connection.getAutoCommit();
      boolean readOnly = connection.isReadOnly();
      String preparedName = "dbperf_" + UUID.randomUUID().toString().replace("-", "");
      boolean prepared = false;
      try
      {
         connection.setAutoCommit(false);
         try(Statement statement = connection.createStatement())
         {
            statement.execute("SET TRANSACTION READ ONLY");
            statement.execute("SET LOCAL statement_timeout = " + timeout);
            statement.execute("SET LOCAL standard_conforming_strings = on");
            if(!analyze)
            {
               // GENERIC_PLAN must not be parsed as an external JDBC bind request.
               statement.unwrap(PgStatement.class).executeWithFlags("EXPLAIN (" + (generic ? "GENERIC_PLAN, " : "") + "FORMAT JSON) " + sql, QueryExecutor.QUERY_EXECUTE_AS_SIMPLE);
               try(ResultSet rows = statement.getResultSet())
               {
                  rows.next();
                  return (rows.getString(1));
               }
            }
            statement.execute("PREPARE " + preparedName + " AS " + sql);
            prepared = true;
         }
         String arguments = parameters.isEmpty() ? "" : "(" + String.join(", ", Collections.nCopies(parameters.size(), "?")) + ")";
         try(PreparedStatement statement = connection.prepareStatement("EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) EXECUTE " + preparedName + arguments))
         {
            for(int i = 0; i < parameters.size(); i++)
            {
               statement.setObject(i + 1, parameters.get(i), Types.OTHER);
            }
            // EXECUTE's arguments are utility expressions: unknown bind types cannot
            // be inferred in extended Parse. Keep JDBC binding/escaping, but send this
            // one command in simple mode so PostgreSQL coerces the quoted strings.
            statement.unwrap(PgStatement.class).executeWithFlags(QueryExecutor.QUERY_EXECUTE_AS_SIMPLE);
            try(ResultSet rows = statement.getResultSet())
            {
               rows.next();
               return (rows.getString(1));
            }
         }
      }
      finally
      {
         try
         {
            connection.rollback();
            if(prepared)
            {
               try(Statement cleanup = connection.createStatement())
               {
                  cleanup.execute("DEALLOCATE " + preparedName);
               }
               finally
               {
                  connection.rollback();
               }
            }
         }
         finally
         {
            connection.setReadOnly(readOnly);
            connection.setAutoCommit(autoCommit);
         }
      }
   }



   /*******************************************************************************
    ** Raw plan (JSON for PostgreSQL, null for text-plan vendors) and its rendering.
    *******************************************************************************/
   private record Explained(String json, PlanTextRenderer.Plan plan)
   {
      /*******************************************************************************
       ** Render a PostgreSQL JSON plan.
       *******************************************************************************/
      Explained rendered() throws Exception
      {
         return (new Explained(json, PlanTextRenderer.render(json)));
      }
   }



   /*******************************************************************************
    ** Always obtain a dedicated connection, independent of any caller QQQ transaction.
    ** Protected for contract tests that verify pool state restoration on driver errors.
    *******************************************************************************/
   protected Connection openConnection() throws SQLException
   {
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) runtime.getInstance().getBackend(runtime.getConfig().getBackendName());
      return (ConnectionManager.getConnection(backend));
   }



   /*******************************************************************************
    ** SQLSTATE survives JDBC wrapper exceptions without exposing their messages.
    *******************************************************************************/
   private static boolean isTimeout(Throwable exception)
   {
      for(Throwable current = exception; current != null; current = current.getCause())
      {
         if(current instanceof SQLException sql && "57014".equals(sql.getSQLState()))
         {
            return (true);
         }
      }
      return (false);
   }



   /*******************************************************************************
    ** UTF-8 query parameter encoding.
    *******************************************************************************/
   private static String encode(String value)
   {
      return (URLEncoder.encode(value, StandardCharsets.UTF_8));
   }
}
