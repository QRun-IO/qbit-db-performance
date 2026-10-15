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

package com.kingsrook.qbits.dbperformance.api;


import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.normalize.SqlNormalizer;
import com.kingsrook.qbits.dbperformance.provider.DbStatsProvider;
import com.kingsrook.qbits.dbperformance.provider.DbStatsProviders;
import com.kingsrook.qbits.dbperformance.provider.SessionInfo;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;


/*******************************************************************************
 ** Read models shared by the API and widgets, under the caller's QQQ permissions.
 *******************************************************************************/
public class DbPerformanceData
{
   private final DbPerfRuntime runtime;



   /*******************************************************************************
    ** Resolve one configured monitored backend, never arbitrary host credentials.
    *******************************************************************************/
   public DbPerformanceData(DbPerfRuntime runtime)
   {
      this.runtime = runtime;
   }



   /*******************************************************************************
    ** Explicit permission check even for hosts that disable core action enforcement.
    *******************************************************************************/
   public void requireView(String backend) throws QException
   {
      if(QContext.getQSession() == null || !QContext.getQSession().hasPermission(runtime.getConfig().getPermissionBaseName() + ".view"))
      {
         throw (new QPermissionDeniedException("Database Performance view permission is required."));
      }
      if(backend != null && !runtime.getConfig().getBackendName().equals(backend))
      {
         throw (new QBadRequestException("Choose the configured monitored backend."));
      }
   }



   /*******************************************************************************
    ** Backend equality filter, reused by every backend-owned table.
    *******************************************************************************/
   public QQueryFilter backendFilter()
   {
      return (DbPerfStore.eq("backendName", runtime.getConfig().getBackendName()));
   }



   /*******************************************************************************
    ** Read settings without creating or changing them during a dashboard request.
    *******************************************************************************/
   public QRecord settings() throws QException
   {
      requireView(null);
      return (DbPerfStore.first("db_perf_settings", backendFilter()));
   }



   /*******************************************************************************
    ** Latest collected numbers, including degraded and failed collection status.
    *******************************************************************************/
   public QRecord latest() throws QException
   {
      requireView(null);
      return (DbPerfStore.first("db_perf_snapshot", backendFilter().withOrderBy(new QFilterOrderBy("capturedAt", false))));
   }



   /*******************************************************************************
    ** Snapshot deltas over the trailing day, ordered for charting.
    *******************************************************************************/
   public List<QRecord> snapshots() throws QException
   {
      requireView(null);
      return (DbPerfStore.query("db_perf_snapshot", backendFilter().withCriteria(new QFilterCriteria("capturedAt", QCriteriaOperator.GREATER_THAN_OR_EQUALS, Instant.now().minusSeconds(86400)))
         .withOrderBy(new QFilterOrderBy("capturedAt", true))));
   }



   /*******************************************************************************
    ** Ranked statements with literal-free text and canonical history links.
    *******************************************************************************/
   public List<Map<String, Object>> statements(int limit, String fingerprint) throws QException
   {
      requireView(null);
      QQueryFilter filter = backendFilter().withOrderBy(new QFilterOrderBy("totalMs24h", false)).withLimit(limit);
      if(fingerprint != null)
      {
         filter.withCriteria(new QFilterCriteria("fingerprint", QCriteriaOperator.EQUALS, fingerprint));
      }
      List<Map<String, Object>> results = new ArrayList<>();
      for(QRecord record : DbPerfStore.query("db_perf_statement", filter))
      {
         Map<String, Object> item = fields(record, "id", "calls24h", "meanMs24h", "totalMs24h", "shareOfDbTime24h");
         String text = SqlNormalizer.normalize(record.getValueString("queryText"));
         item.put("queryText", fingerprint == null ? text.substring(0, Math.min(300, text.length())) : text);
         item.put("historyPath", recordPath("db_perf_statement", record.getValueInteger("id")));
         item.put("hitRatio", hitRatio(record));
         results.add(item);
      }
      return (results);
   }



   /*******************************************************************************
    ** 24-hour cache hit ratio uses deltas rather than cumulative statement counters.
    *******************************************************************************/
   private BigDecimal hitRatio(QRecord record) throws QException
   {
      List<QRecord> rows = DbPerfStore.query("db_perf_query_snapshot", DbPerfStore.eq("statementId", record.getValueInteger("id"))
         .withCriteria(new QFilterCriteria("capturedAt", QCriteriaOperator.GREATER_THAN_OR_EQUALS, Instant.now().minusSeconds(86400))));
      double hits = rows.stream().mapToDouble(row -> number(row, "sharedBlksHit")).sum();
      double reads = rows.stream().mapToDouble(row -> number(row, "sharedBlksRead")).sum();
      return (hits + reads == 0 ? null : BigDecimal.valueOf(hits / (hits + reads)));
   }



   /*******************************************************************************
    ** Open and acknowledged findings stay actionable and are ordered by score.
    *******************************************************************************/
   public List<QRecord> findings(int limit) throws QException
   {
      requireView(null);
      return (DbPerfStore.query("db_perf_finding", backendFilter()
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.IN, List.of("OPEN", "ACKNOWLEDGED")))
         .withOrderBy(new QFilterOrderBy("score", false)).withLimit(limit)));
   }



   /*******************************************************************************
    ** Small overview response; last snapshot metrics are interval deltas.
    *******************************************************************************/
   public Map<String, Object> overview(String backend) throws QException
   {
      requireView(backend);
      Map<String, Integer> counts = new LinkedHashMap<>();
      List<Map<String, Object>> top = new ArrayList<>();
      for(QRecord record : findings(Integer.MAX_VALUE))
      {
         counts.merge(record.getValueString("severity"), 1, Integer::sum);
         if(top.size() < 5)
         {
            top.add(fields(record, "id", "title", "severity", "score"));
         }
      }
      return (map("settings", fields(settings(), "provider", "statsAccess", "statsAccessMessage", "lastSnapshotAt"),
         "snapshot", fields(latest(), "id", "capturedAt", "status", "connectionsTotal", "maxConnections", "cacheHitRatio", "tempBytes", "deadlocks", "statementTotalMs", "statementCalls"),
         "topStatements", statements(10, null), "findingsBySeverity", counts, "topFindings", top));
   }



   /*******************************************************************************
    ** Link arbitrary SQL shapes back to their normalized statement history.
    *******************************************************************************/
   public Map<String, Object> lookup(String backend, String sql) throws QException
   {
      requireView(backend);
      if(sql == null || sql.isBlank())
      {
         throw (new QBadRequestException("Supply SQL to look up."));
      }
      String normalized = SqlNormalizer.normalize(sql);
      String fingerprint = SqlNormalizer.fingerprint(normalized);
      return (map("normalizedSql", normalized, "fingerprint", fingerprint, "matches", statements(100, fingerprint)));
   }



   /*******************************************************************************
    ** Live activity is read now, not from saved samples, and is never persisted.
    *******************************************************************************/
   public Map<String, Object> activity(String backend, boolean allowRawText) throws Exception
   {
      requireView(backend);
      try(Connection connection = ConnectionManager.getConnection((RDBMSBackendMetaData) runtime.getInstance().getBackend(runtime.getConfig().getBackendName())))
      {
         DbStatsProvider provider = DbStatsProviders.forConnection(connection);
         return (activity(provider.readSessions(connection), allowRawText));
      }
      catch(java.sql.SQLException e)
      {
         throw (new QException("Live activity is unavailable. Check the backend and statistics access."));
      }
   }



   /*******************************************************************************
    ** Pure projection also used by provider fixture tests; widgets always pass false.
    *******************************************************************************/
   public Map<String, Object> activity(List<SessionInfo> sessions, boolean allowRawText) throws QException
   {
      requireView(null);
      boolean redacted = !allowRawText || !QContext.getQSession().hasPermission(runtime.getConfig().getPermissionBaseName() + ".view_live_text");
      Map<Integer, Set<Integer>> blocked = new LinkedHashMap<>();
      for(SessionInfo session : sessions)
      {
         for(Integer blocker : session.blockedBy())
         {
            blocked.computeIfAbsent(blocker, key -> new HashSet<>()).add(session.pid());
         }
      }
      List<Map<String, Object>> rows = new ArrayList<>();
      List<Map<String, Object>> chains = new ArrayList<>();
      Set<Integer> allBlocked = new HashSet<>();
      blocked.values().forEach(allBlocked::addAll);
      for(SessionInfo session : sessions)
      {
         String normalized = SqlNormalizer.normalize(session.queryText());
         rows.add(map("pid", session.pid(), "user", session.dbUser(), "application", session.applicationName(), "state", session.state(),
            "waitEvent", session.waitEvent(), "querySeconds", session.querySeconds(), "xactSeconds", session.xactSeconds(),
            "blockedBy", session.blockedBy(), "blockingCount", blocked.getOrDefault(session.pid(), Set.of()).size(),
            "fingerprint", SqlNormalizer.fingerprint(normalized), "normalizedText", normalized, "query", redacted ? null : session.queryText(), "liveTextRedacted", redacted));
      }
      for(Integer root : blocked.keySet())
      {
         if(!allBlocked.contains(root))
         {
            Set<Integer> descendants = new java.util.TreeSet<>();
            descendants(root, blocked, descendants);
            descendants.remove(root);
            chains.add(map("rootPid", root, "blockedPids", new ArrayList<>(descendants)));
         }
      }
      return (map("sessions", rows, "blockingChains", chains, "liveTextRedacted", redacted));
   }



   /*******************************************************************************
    ** Traverse transitive blocking with cycle protection.
    *******************************************************************************/
   private static void descendants(Integer pid, Map<Integer, Set<Integer>> blocked, Set<Integer> result)
   {
      for(Integer child : blocked.getOrDefault(pid, Set.of()))
      {
         if(result.add(child))
         {
            descendants(child, blocked, result);
         }
      }
   }



   /*******************************************************************************
    ** One record path builder for API and widgets, including custom app names.
    *******************************************************************************/
   public String recordPath(String table, Integer id)
   {
      return ("/app/" + runtime.getConfig().getAppName() + "/" + table + "/" + id);
   }



   /*******************************************************************************
    ** Explicit allowlist prevents returning raw counters, configuration or future fields.
    *******************************************************************************/
   public static Map<String, Object> fields(QRecord record, String... names)
   {
      Map<String, Object> result = new LinkedHashMap<>();
      for(String name : names)
      {
         result.put(name, record == null ? null : record.getValue(name));
      }
      return (result);
   }



   /*******************************************************************************
    ** Nullable JSON object with stable property order.
    *******************************************************************************/
   public static Map<String, Object> map(Object... pairs)
   {
      Map<String, Object> result = new LinkedHashMap<>();
      for(int i = 0; i < pairs.length; i += 2)
      {
         result.put((String) pairs[i], pairs[i + 1]);
      }
      return (result);
   }



   /*******************************************************************************
    ** Nullable statistics arithmetic.
    *******************************************************************************/
   public static double number(QRecord row, String field)
   {
      return (row == null || row.getValue(field) == null ? 0 : ((Number) row.getValue(field)).doubleValue());
   }
}
