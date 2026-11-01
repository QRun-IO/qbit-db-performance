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

package com.kingsrook.qbits.dbperformance.provider;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static com.kingsrook.qbits.dbperformance.provider.JdbcReads.instant;
import static com.kingsrook.qbits.dbperformance.provider.JdbcReads.read;
import static com.kingsrook.qbits.dbperformance.provider.JdbcReads.scalar;


/*******************************************************************************
 ** PostgreSQL 13+ statistics reader; every statement is a read-only SELECT.
 *******************************************************************************/
public class PostgresStatsProvider implements DbStatsProvider
{
   /*******************************************************************************
    ** Provider name.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return ("POSTGRES");
   }


   /*******************************************************************************
    ** Server version and privileges, including explicit installation guidance.
    *******************************************************************************/
   @Override
   public ProviderCapabilities capabilities(Connection connection) throws SQLException
   {
      int version = version(connection);
      boolean installed = scalar(connection, "SELECT count(*) FROM pg_extension WHERE extname = 'pg_stat_statements'") > 0;
      boolean privileged = scalar(connection, "SELECT CASE WHEN pg_has_role(current_user, 'pg_read_all_stats', 'MEMBER') OR (SELECT rolsuper FROM pg_roles WHERE rolname=current_user) THEN 1 ELSE 0 END") > 0;
      String access = version < 130000 ? "UNSUPPORTED" : !installed ? "NO_PG_STAT_STATEMENTS" : !privileged ? "NO_READ_ALL_STATS" : "FULL";
      String message = switch(access)
      {
         case "UNSUPPORTED" -> "PostgreSQL 13 or newer is required.";
         case "NO_PG_STAT_STATEMENTS" -> "Install pg_stat_statements in this database and preload it in shared_preload_libraries.";
         case "NO_READ_ALL_STATS" -> "Grant pg_read_all_stats to the application database role to see all statement statistics.";
         default -> "Full statistics access.";
      };
      if(installed && version >= 130000)
      {
         try
         {
            scalar(connection, "SELECT count(*) FROM " + statementsView(connection));
         }
         catch(SQLException e)
         {
            if("55000".equals(e.getSQLState()) || "42501".equals(e.getSQLState()))
            {
               installed = false;
               access = "NO_PG_STAT_STATEMENTS";
               message = "pg_stat_statements must be preloaded and readable by the application database role.";
            }
            else
            {
               throw (e);
            }
         }
      }
      EnumSet<StatsFeature> available = EnumSet.copyOf(features());
      if(!installed)
      {
         available.removeAll(EnumSet.of(StatsFeature.STATEMENTS, StatsFeature.STATEMENT_IO, StatsFeature.STATEMENT_RESET));
      }
      if(version < 140000)
      {
         available.remove(StatsFeature.STATEMENT_RESET);
      }
      if(version < 160000)
      {
         available.remove(StatsFeature.EXPLAIN_GENERIC);
      }
      return (new ProviderCapabilities(version >= 130000, version, installed, privileged, access, message, version >= 130000 ? available : EnumSet.noneOf(StatsFeature.class)));
   }



   /*******************************************************************************
    ** Every area: the cumulative statistics views cover them all.
    *******************************************************************************/
   @Override
   public Set<StatsFeature> features()
   {
      return (StatsFeature.of(StatsFeature.values()));
   }



   /*******************************************************************************
    ** Role and extension prerequisites for each access code.
    *******************************************************************************/
   @Override
   public String remedy(String statsAccess, String databaseUser)
   {
      return (switch(statsAccess == null ? "" : statsAccess)
      {
         case "FULL" -> "Full statistics access";
         case "NO_READ_ALL_STATS" -> "grant pg_read_all_stats to " + databaseUser;
         case "NO_PG_STAT_STATEMENTS" -> "Enable pg_stat_statements in shared_preload_libraries and create the extension";
         default -> "Use a supported PostgreSQL version (13 or newer) and statistics provider";
      });
   }



   /*******************************************************************************
    ** Guided setup rows: the extension and the statistics role.
    *******************************************************************************/
   @Override
   public List<SetupCheck> setupChecks(String statsAccess, String message)
   {
      return (List.of(new SetupCheck("pg_stat_statements", statsAccess, "Preload and create pg_stat_statements; then run Collect now."),
         new SetupCheck("pg_read_all_stats", statsAccess, message == null ? "Grant pg_read_all_stats to the application's database role." : message)));
   }


   /*******************************************************************************
    ** Current database cumulative counters and capacity.
    *******************************************************************************/
   @Override
   public DatabaseStats readDatabase(Connection connection) throws SQLException
   {
      return (read(connection, "SELECT d.*, pg_database_size(current_database()) AS size_bytes, current_setting('max_connections')::int AS max_connections FROM pg_stat_database d WHERE datname=current_database()", row ->
         new DatabaseStats(counters(row, "xact_commit", "xact_rollback", "blks_hit", "blks_read", "temp_files", "temp_bytes", "deadlocks", "conflicts"), instant(row, "stats_reset"), row.getInt("max_connections"), row.getLong("size_bytes"))).getFirst());
   }


   /*******************************************************************************
    ** Current database statements only. Version-dependent columns are projected
    ** as null or true when older PostgreSQL versions do not provide them.
    *******************************************************************************/
   @Override
   public List<StatementStats> readStatements(Connection connection) throws SQLException
   {
      int version = version(connection);
      String sql = "SELECT s.queryid, r.rolname, s.query, s.calls, s.total_exec_time, s.mean_exec_time, s.max_exec_time, s.rows, s.shared_blks_hit, s.shared_blks_read, s.temp_blks_written, "
         + (version >= 140000 ? "s.toplevel" : "true") + " AS top_level, "
         + (version >= 170000 ? "s.stats_since, s.minmax_stats_since" : "NULL::timestamptz AS stats_since, NULL::timestamptz AS minmax_stats_since")
         + " FROM " + statementsView(connection) + " s LEFT JOIN pg_roles r ON r.oid=s.userid WHERE s.dbid=(SELECT oid FROM pg_database WHERE datname=current_database()) AND s.queryid IS NOT NULL AND s.query IS NOT NULL";
      return (read(connection, sql, row -> new StatementStats(row.getLong("queryid"), row.getString("rolname"), row.getBoolean("top_level"), row.getString("query"), row.getLong("calls"), row.getBigDecimal("total_exec_time"), row.getBigDecimal("mean_exec_time"), row.getBigDecimal("max_exec_time"), row.getLong("rows"), row.getLong("shared_blks_hit"), row.getLong("shared_blks_read"), row.getLong("temp_blks_written"), instant(row, "stats_since"), instant(row, "minmax_stats_since"))));
   }


   /*******************************************************************************
    ** Global statement reset is available starting with PostgreSQL 14.
    *******************************************************************************/
   @Override
   public StatementResetInfo readStatementResetInfo(Connection connection) throws SQLException
   {
      if(version(connection) < 140000)
      {
         return (new StatementResetInfo(null, 0));
      }
      return (read(connection, "SELECT stats_reset, dealloc FROM " + statementsView(connection) + "_info", row -> new StatementResetInfo(instant(row, "stats_reset"), row.getLong("dealloc"))).getFirst());
   }


   /*******************************************************************************
    ** Latest table counters, tuple gauges, maintenance times and sizes.
    *******************************************************************************/
   @Override
   public List<TableStats> readTables(Connection connection) throws SQLException
   {
      String sql = "SELECT s.*, pg_total_relation_size(s.relid) AS total_bytes, pg_relation_size(s.relid) AS table_bytes, pg_indexes_size(s.relid) AS index_bytes, CASE WHEN c.reltoastrelid=0 THEN 0 ELSE pg_total_relation_size(c.reltoastrelid) END AS toast_bytes, age(c.relfrozenxid)::bigint AS freeze_age FROM pg_stat_user_tables s JOIN pg_class c ON c.oid=s.relid";
      return (read(connection, sql, row ->
      {
         Map<String, Instant> maintenance = new LinkedHashMap<>();
         for(String field : List.of("last_vacuum", "last_autovacuum", "last_analyze", "last_autoanalyze"))
         {
            maintenance.put(camel(field), instant(row, field));
         }
         return (new TableStats(row.getString("schemaname"), row.getString("relname"), row.getLong("relid"), counters(row, "seq_scan", "seq_tup_read", "idx_scan", "idx_tup_fetch", "n_tup_ins", "n_tup_upd", "n_tup_del", "n_tup_hot_upd", "n_live_tup", "n_dead_tup", "total_bytes", "table_bytes", "index_bytes", "toast_bytes", "vacuum_count", "autovacuum_count", "analyze_count", "autoanalyze_count", "freeze_age"), maintenance));
      }));
   }


   /*******************************************************************************
    ** Index definitions, ordered keys (including expressions) and write cost.
    *******************************************************************************/
   @Override
   public List<IndexStats> readIndexes(Connection connection) throws SQLException
   {
      String sql = "SELECT s.*, i.indisunique, i.indisprimary, i.indisvalid, pg_get_indexdef(s.indexrelid) AS definition, (SELECT string_agg(pg_get_indexdef(s.indexrelid, k, true), ', ' ORDER BY k) FROM generate_series(1, i.indnatts) AS k) AS column_names, pg_relation_size(s.indexrelid) AS size_bytes, t.n_tup_ins+t.n_tup_upd+t.n_tup_del AS write_cost FROM pg_stat_user_indexes s JOIN pg_index i ON i.indexrelid=s.indexrelid JOIN pg_stat_user_tables t ON t.relid=s.relid";
      return (read(connection, sql, row -> new IndexStats(row.getString("schemaname"), row.getString("relname"), row.getString("indexrelname"), row.getLong("indexrelid"), row.getString("definition"), row.getString("column_names"), row.getBoolean("indisunique"), row.getBoolean("indisprimary"), row.getBoolean("indisvalid"), row.getLong("idx_scan"), row.getLong("idx_tup_read"), row.getLong("idx_tup_fetch"), row.getLong("size_bytes"), row.getLong("write_cost"))));
   }


   /*******************************************************************************
    ** Current database activity, excluding the collector's own session.
    *******************************************************************************/
   @Override
   public List<SessionInfo> readSessions(Connection connection) throws SQLException
   {
      String sql = "SELECT pid, usename, application_name, client_addr::text, state, wait_event_type, wait_event, backend_type, xact_start, query_start, state_change, extract(epoch FROM clock_timestamp()-query_start) AS query_seconds, extract(epoch FROM clock_timestamp()-xact_start) AS xact_seconds, pg_blocking_pids(pid) AS blocked_by, "
         + (version(connection) >= 140000 ? "query_id" : "NULL::bigint AS query_id") + ", query FROM pg_stat_activity WHERE datname=current_database() AND pid<>pg_backend_pid()";
      return (read(connection, sql, row ->
      {
         List<Integer> blockedBy = new ArrayList<>();
         java.sql.Array array = row.getArray("blocked_by");
         if(array != null)
         {
            try
            {
               for(Object pid : (Object[]) array.getArray())
               {
                  blockedBy.add(((Number) pid).intValue());
               }
            }
            finally
            {
               array.free();
            }
         }
         Long queryId = row.getObject("query_id") == null ? null : row.getLong("query_id");
         return (new SessionInfo(row.getInt("pid"), row.getString("usename"), row.getString("application_name"), row.getString("client_addr"), row.getString("state"), row.getString("wait_event_type"), row.getString("wait_event"), row.getString("backend_type"), instant(row, "xact_start"), instant(row, "query_start"), instant(row, "state_change"), row.getBigDecimal("query_seconds"), row.getBigDecimal("xact_seconds"), blockedBy, queryId, row.getString("query")));
      }));
   }


   /*******************************************************************************
    ** Cluster-wide capacity counts, including connections in other databases.
    *******************************************************************************/
   @Override
   public List<ConnectionCount> readConnections(Connection connection) throws SQLException
   {
      return (read(connection, "SELECT usename, application_name, state, count(*) AS connections FROM pg_stat_activity GROUP BY usename, application_name, state", row -> new ConnectionCount(row.getString("usename"), row.getString("application_name"), row.getString("state"), row.getInt("connections"))));
   }


   /*******************************************************************************
    ** Only operational, non-secret server settings relevant to this qbit.
    *******************************************************************************/
   @Override
   public Map<String, String> readSettings(Connection connection) throws SQLException
   {
      Map<String, String> result = new LinkedHashMap<>();
      read(connection, "SELECT name, setting FROM pg_settings WHERE name IN ('server_version_num','max_connections','shared_preload_libraries','pg_stat_statements.track','track_io_timing','block_size')", row -> result.put(row.getString("name"), row.getString("setting")));
      return (result);
   }


   /*******************************************************************************
    ** Extension schema is obtained from the catalog, then identifier-quoted.
    *******************************************************************************/
   private String statementsView(Connection connection) throws SQLException
   {
      List<String> schemas = read(connection, "SELECT n.nspname FROM pg_extension e JOIN pg_namespace n ON n.oid=e.extnamespace WHERE e.extname='pg_stat_statements'", row -> row.getString(1));
      if(schemas.isEmpty())
      {
         throw (new SQLException("pg_stat_statements is not installed", "42P01"));
      }
      return ("\"" + schemas.getFirst().replace("\"", "\"\"") + "\".pg_stat_statements");
   }


   /*******************************************************************************
    ** Numeric server version avoids JDBC minor-version ambiguity.
    *******************************************************************************/
   private int version(Connection connection) throws SQLException
   {
      return ((int) scalar(connection, "SELECT current_setting('server_version_num')::int"));
   }


   /*******************************************************************************
    ** Copy cumulative counters to the entity's camelCase names.
    *******************************************************************************/
   private static Map<String, Long> counters(ResultSet row, String... columns) throws SQLException
   {
      Map<String, Long> counters = new LinkedHashMap<>();
      for(String column : columns)
      {
         counters.put(camel(column), row.getLong(column));
      }
      return (counters);
   }


   /*******************************************************************************
    ** PostgreSQL column to entity field.
    *******************************************************************************/
   private static String camel(String column)
   {
      StringBuilder value = new StringBuilder();
      for(String part : column.split("_"))
      {
         value.append(value.isEmpty() ? part : Character.toUpperCase(part.charAt(0)) + part.substring(1));
      }
      return (value.toString());
   }
}
