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


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static com.kingsrook.qbits.dbperformance.provider.JdbcReads.instant;
import static com.kingsrook.qbits.dbperformance.provider.JdbcReads.read;
import static com.kingsrook.qbits.dbperformance.provider.JdbcReads.scalar;


/*******************************************************************************
 ** H2 2.x statistics from INFORMATION_SCHEMA. Statement statistics need the
 ** database setting QUERY_STATISTICS (URL {@code ;QUERY_STATISTICS=TRUE} or
 ** {@code SET QUERY_STATISTICS TRUE} as an admin); every other area works
 ** without it. H2 keeps no transaction, cache, scan or vacuum counters, so
 ** those areas are reported as unavailable rather than as zeros. See
 ** docs/PROVIDERS.md for what is exact, approximated and unsupported.
 *******************************************************************************/
public class H2StatsProvider implements DbStatsProvider
{
   public static final String NAME = "H2";

   private static final String NO_QUERY_STATISTICS = "NO_QUERY_STATISTICS";
   private static final String NOT_ADMIN = "NOT_ADMIN";
   private static final int MAX_ENTRIES = 1000;
   private static final Pattern WORD = Pattern.compile("[A-Za-z_][A-Za-z0-9_$]*");

   //////////////////////////////////////////////////////////////////////////
   // H2 has no READ ONLY transaction and a rollback does not undo file,  //
   // session, sequence or DDL side effects, so plans refuse SQL naming   //
   // these (and any user-defined alias) outright                          //
   //////////////////////////////////////////////////////////////////////////
   private static final Set<String> DENIED = Set.of("FILE_WRITE", "FILE_READ", "CSVWRITE", "CSVREAD", "LINK_SCHEMA", "SCRIPT", "RUNSCRIPT",
      "DISK_SPACE_USED", "CANCEL_SESSION", "ABORT_SESSION", "BACKUP", "SHUTDOWN", "CALL", "SET", "DROP", "CREATE", "ALTER", "TRUNCATE", "GRANT",
      "REVOKE", "COMMIT", "ROLLBACK", "SAVEPOINT", "CHECKPOINT", "NEXTVAL", "SETVAL", "CURRVAL", "LOCK", "EXECUTE", "INSERT", "UPDATE",
      "DELETE", "MERGE", "UPSERT", "REPLACE", "INTO", "FOR", "JAVA_OBJECT", "SLEEP");

   ///////////////////////////////////////////////////////////////////////////
   // statement that guarantees QUERY_STATISTICS has a row when it is on;   //
   // it and INFORMATION_SCHEMA reads (the collector's own) are not stored  //
   ///////////////////////////////////////////////////////////////////////////
   private static final String PROBE = "SELECT 1 AS DB_PERF_PROBE";
   private static final String DISK_SPACE = "SELECT DISK_SPACE_USED(?)";

   private static final Set<StatsFeature> FEATURES = StatsFeature.of(StatsFeature.DATABASE_SIZE, StatsFeature.STATEMENTS,
      StatsFeature.TABLE_ROWS, StatsFeature.TABLE_SIZES, StatsFeature.INDEX_DEFINITIONS, StatsFeature.SESSIONS, StatsFeature.BLOCKING,
      StatsFeature.CONNECTIONS, StatsFeature.EXPLAIN_TEXT, StatsFeature.EXPLAIN_GENERIC, StatsFeature.EXPLAIN_ANALYZE);

   private static final List<String> SETTINGS = List.of("info.VERSION", "MODE", "MV_STORE", "info.FILE_SIZE", "info.PAGE_SIZE",
      "info.CACHE_MAX_SIZE", "info.CACHE_SIZE", "info.CACHE_HIT_RATIO", "info.FILE_READ", "info.FILE_WRITE", "MAX_QUERY_TIMEOUT", "QUERY_TIMEOUT");



   /*******************************************************************************
    ** Provider name.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** Version, admin rights and whether QUERY_STATISTICS is collecting.
    *******************************************************************************/
   @Override
   public ProviderCapabilities capabilities(Connection connection) throws SQLException
   {
      int major = connection.getMetaData().getDatabaseMajorVersion();
      int version = version(connection);
      boolean supported = major >= 2;
      if(!supported)
      {
         return (new ProviderCapabilities(false, version, false, false, "UNSUPPORTED", "H2 2.0 or newer is required.", Set.of()));
      }
      boolean statistics = queryStatistics(connection);
      boolean admin = admin(connection);
      String access = !statistics ? NO_QUERY_STATISTICS : !admin ? NOT_ADMIN : "FULL";
      String message = switch(access)
      {
         case NO_QUERY_STATISTICS -> "H2 query statistics are off. Run Enable statement statistics on Settings as an H2 admin user, or add ;QUERY_STATISTICS=TRUE;QUERY_STATISTICS_MAX_ENTRIES=1000 to the JDBC URL.";
         case NOT_ADMIN -> "The H2 user is not an admin, so only its own session is visible. Use an admin user (ALTER USER ... ADMIN TRUE) to see every session and blocking.";
         default -> "Full statistics access (H2 does not keep transaction, cache, scan or vacuum counters).";
      };
      EnumSet<StatsFeature> available = EnumSet.copyOf(FEATURES);
      if(!statistics)
      {
         available.remove(StatsFeature.STATEMENTS);
      }
      if(!admin)
      {
         available.remove(StatsFeature.BLOCKING);
      }
      else
      {
         ////////////////////////////////////////////////////////////////////
         // an admin may call file and session functions; ANALYZE executes //
         // the query, so it is offered only to non-admin users            //
         ////////////////////////////////////////////////////////////////////
         available.remove(StatsFeature.EXPLAIN_ANALYZE);
      }
      return (new ProviderCapabilities(true, version, statistics, admin, access, message, available));
   }



   /*******************************************************************************
    ** Areas H2 can supply at best.
    *******************************************************************************/
   @Override
   public Set<StatsFeature> features()
   {
      return (FEATURES);
   }



   /*******************************************************************************
    ** Size from the store; H2 has no cumulative counters, reset time or limit.
    *******************************************************************************/
   @Override
   public DatabaseStats readDatabase(Connection connection) throws SQLException
   {
      List<Long> size = read(connection, "SELECT SETTING_VALUE FROM INFORMATION_SCHEMA.SETTINGS WHERE SETTING_NAME = 'info.FILE_SIZE'", row -> parseLong(row.getString(1)));
      return (new DatabaseStats(Map.of(), null, 0, size.isEmpty() || size.getFirst() == null ? 0 : size.getFirst()));
   }



   /*******************************************************************************
    ** QUERY_STATISTICS rows (times in milliseconds), without connection setup
    ** (see {@link #excluded}). H2 has no query id or user, so the id is a stable
    ** hash of the statement text and the user is null.
    *******************************************************************************/
   @Override
   public List<StatementStats> readStatements(Connection connection) throws SQLException
   {
      List<StatementStats> result = new ArrayList<>();
      read(connection, "SELECT SQL_STATEMENT, EXECUTION_COUNT, CUMULATIVE_EXECUTION_TIME, AVERAGE_EXECUTION_TIME, MAX_EXECUTION_TIME, CUMULATIVE_ROW_COUNT FROM INFORMATION_SCHEMA.QUERY_STATISTICS", row ->
      {
         String sql = row.getString("SQL_STATEMENT");
         if(sql != null && !excluded(sql))
         {
            result.add(new StatementStats(hash(sql), null, true, sql, row.getLong("EXECUTION_COUNT"), millis(row.getDouble("CUMULATIVE_EXECUTION_TIME")),
               millis(row.getDouble("AVERAGE_EXECUTION_TIME")), millis(row.getDouble("MAX_EXECUTION_TIME")), row.getLong("CUMULATIVE_ROW_COUNT"), 0, 0, 0, null, null));
         }
         return (null);
      });
      return (result);
   }



   /*******************************************************************************
    ** Not application statements: the collector's own probe, table sizing and
    ** INFORMATION_SCHEMA reads, and connection setup. H2 replays every JDBC URL setting as a SET on
    ** each new connection and drivers call DATABASE() for the catalog, so with
    ** unpooled connections these dominate the call counts without being work.
    *******************************************************************************/
   static boolean excluded(String sql)
   {
      String upper = sql.strip().toUpperCase(Locale.ROOT);
      return (PROBE.equals(sql) || DISK_SPACE.equals(sql) || upper.contains("INFORMATION_SCHEMA.") || upper.startsWith("SET ") || upper.equals("CALL DATABASE()"));
   }



   /*******************************************************************************
    ** H2 keeps no reset time; a reset shows as falling call counts instead.
    *******************************************************************************/
   @Override
   public StatementResetInfo readStatementResetInfo(Connection connection)
   {
      return (new StatementResetInfo(null, 0));
   }



   /*******************************************************************************
    ** Base tables with H2's row-count estimate and DISK_SPACE_USED (table data
    ** only: H2 cannot size indexes, so totalBytes equals tableBytes).
    *******************************************************************************/
   @Override
   public List<TableStats> readTables(Connection connection) throws SQLException
   {
      List<TableStats> result = new ArrayList<>();
      List<String[]> tables = read(connection, "SELECT TABLE_SCHEMA, TABLE_NAME, ROW_COUNT_ESTIMATE FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE' AND TABLE_SCHEMA <> 'INFORMATION_SCHEMA' ORDER BY TABLE_SCHEMA, TABLE_NAME",
         row -> new String[] {row.getString(1), row.getString(2), row.getString(3)});
      try(PreparedStatement size = connection.prepareStatement(DISK_SPACE))
      {
         size.setQueryTimeout(30);
         for(String[] table : tables)
         {
            Map<String, Long> counters = new LinkedHashMap<>();
            counters.put("nLiveTup", table[2] == null ? 0L : Long.parseLong(table[2]));
            Long bytes = diskSpace(size, quote(table[0]) + "." + quote(table[1]));
            if(bytes != null)
            {
               counters.put("tableBytes", bytes);
               counters.put("totalBytes", bytes);
            }
            result.add(new TableStats(table[0], table[1], hash("TABLE:" + table[0] + "." + table[1]) & Long.MAX_VALUE, counters, Map.of()));
         }
      }
      return (result);
   }



   /*******************************************************************************
    ** Index definitions and ordered key columns. H2 has no index usage or size
    ** statistics; constraint-generated indexes are marked USING CONSTRAINT so
    ** they are never proposed for removal.
    *******************************************************************************/
   @Override
   public List<IndexStats> readIndexes(Connection connection) throws SQLException
   {
      Map<String, IndexBuilder> indexes = new LinkedHashMap<>();
      String sql = "SELECT i.INDEX_SCHEMA, i.TABLE_NAME, i.INDEX_NAME, i.INDEX_TYPE_NAME, i.IS_GENERATED, c.COLUMN_NAME, c.ORDERING_SPECIFICATION FROM INFORMATION_SCHEMA.INDEXES i "
         + "JOIN INFORMATION_SCHEMA.INDEX_COLUMNS c ON c.INDEX_SCHEMA = i.INDEX_SCHEMA AND c.TABLE_NAME = i.TABLE_NAME AND c.INDEX_NAME = i.INDEX_NAME "
         + "WHERE i.INDEX_SCHEMA <> 'INFORMATION_SCHEMA' ORDER BY i.INDEX_SCHEMA, i.TABLE_NAME, i.INDEX_NAME, c.ORDINAL_POSITION";
      read(connection, sql, row ->
      {
         IndexBuilder index = indexes.computeIfAbsent(row.getString(1) + "." + row.getString(3), key -> new IndexBuilder());
         index.schema = row.getString(1);
         index.table = row.getString(2);
         index.name = row.getString(3);
         index.type = row.getString(4) == null ? "" : row.getString(4).toUpperCase(Locale.ROOT);
         index.generated = row.getBoolean(5);
         String order = "DESC".equalsIgnoreCase(row.getString(7)) ? " DESC" : "";
         index.columns.add(row.getString(6) + order);
         index.quoted.add(quote(row.getString(6)) + order);
         return (null);
      });
      List<IndexStats> result = new ArrayList<>();
      for(IndexBuilder index : indexes.values())
      {
         boolean primary = index.type.contains("PRIMARY");
         boolean unique = primary || index.type.contains("UNIQUE");
         String columns = String.join(", ", index.columns);
         String keys = String.join(", ", index.quoted);
         String definition = primary ? "ALTER TABLE " + quote(index.schema) + "." + quote(index.table) + " ADD PRIMARY KEY (" + keys + ")"
            : "CREATE " + (unique ? "UNIQUE " : "") + "INDEX " + quote(index.name) + " ON " + quote(index.schema) + "." + quote(index.table) + (index.generated ? " USING CONSTRAINT" : "") + " (" + keys + ")";
         result.add(new IndexStats(index.schema, index.table, index.name, hash("INDEX:" + index.schema + "." + index.name) & Long.MAX_VALUE, definition, columns, unique, primary, true, 0, 0, 0, 0, 0));
      }
      return (result);
   }



   /*******************************************************************************
    ** Other sessions in PostgreSQL's state vocabulary: RUNNING is active, BLOCKED
    ** is active waiting on a Lock, SLEEP with uncommitted changes is idle in
    ** transaction. H2 has no transaction start, so an idle transaction's age is
    ** the time since it went idle (a lower bound).
    *******************************************************************************/
   @Override
   public List<SessionInfo> readSessions(Connection connection) throws SQLException
   {
      Instant now = Instant.now();
      String sql = "SELECT SESSION_ID, USER_NAME, CLIENT_INFO, CLIENT_ADDR, SERVER, SESSION_STATE, CONTAINS_UNCOMMITTED, BLOCKER_ID, EXECUTING_STATEMENT, EXECUTING_STATEMENT_START, SLEEP_SINCE "
         + "FROM INFORMATION_SCHEMA.SESSIONS WHERE SESSION_ID <> SESSION_ID() ORDER BY SESSION_ID";
      return (read(connection, sql, row ->
      {
         String state = state(row.getString("SESSION_STATE"), row.getBoolean("CONTAINS_UNCOMMITTED"));
         boolean blocked = "BLOCKED".equalsIgnoreCase(row.getString("SESSION_STATE"));
         Instant queryStart = instant(row, "EXECUTING_STATEMENT_START");
         Instant sleepSince = instant(row, "SLEEP_SINCE");
         boolean active = "active".equals(state);
         String query = row.getString("EXECUTING_STATEMENT");
         Integer blocker = row.getObject("BLOCKER_ID") == null ? null : row.getInt("BLOCKER_ID");
         return (new SessionInfo(row.getInt("SESSION_ID"), row.getString("USER_NAME"), row.getString("CLIENT_INFO"), row.getString("CLIENT_ADDR"), state,
            blocked ? "Lock" : null, blocked ? "row lock" : null, row.getString("SERVER") == null ? "embedded" : row.getString("SERVER"),
            null, queryStart, active ? queryStart : sleepSince, active ? seconds(queryStart, now) : null,
            state.startsWith("idle in transaction") ? seconds(sleepSince, now) : null, blocker == null ? List.of() : List.of(blocker), query == null ? null : hash(query), query));
      }));
   }



   /*******************************************************************************
    ** Every visible session, including this one, by user, client and state.
    *******************************************************************************/
   @Override
   public List<ConnectionCount> readConnections(Connection connection) throws SQLException
   {
      Map<List<String>, Integer> counts = new LinkedHashMap<>();
      read(connection, "SELECT USER_NAME, CLIENT_INFO, SESSION_STATE, CONTAINS_UNCOMMITTED FROM INFORMATION_SCHEMA.SESSIONS ORDER BY SESSION_ID", row ->
         counts.merge(Arrays.asList(row.getString(1), row.getString(2), state(row.getString(3), row.getBoolean(4))), 1, Integer::sum));
      List<ConnectionCount> result = new ArrayList<>();
      counts.forEach((key, count) -> result.add(new ConnectionCount(key.get(0), key.get(1), key.get(2), count)));
      return (result);
   }



   /*******************************************************************************
    ** Operational, non-secret settings plus the QUERY_STATISTICS state.
    *******************************************************************************/
   @Override
   public Map<String, String> readSettings(Connection connection) throws SQLException
   {
      Map<String, String> result = new LinkedHashMap<>();
      read(connection, "SELECT SETTING_NAME, SETTING_VALUE FROM INFORMATION_SCHEMA.SETTINGS", row -> SETTINGS.contains(row.getString(1)) ? result.put(row.getString(1), row.getString(2)) : null);
      result.put("QUERY_STATISTICS", queryStatistics(connection) ? "TRUE" : "FALSE");
      return (result);
   }



   /*******************************************************************************
    ** Health and setup next steps for H2's access codes.
    *******************************************************************************/
   @Override
   public String remedy(String statsAccess, String databaseUser)
   {
      return (switch(statsAccess == null ? "" : statsAccess)
      {
         case "FULL" -> "Full statistics access";
         case NO_QUERY_STATISTICS -> "Turn on H2 query statistics: run Enable statement statistics on Settings, or add ;QUERY_STATISTICS=TRUE to the JDBC URL";
         case NOT_ADMIN -> "Make " + databaseUser + " an H2 admin (ALTER USER " + databaseUser + " ADMIN TRUE) to see every session";
         default -> "Use H2 2.0 or newer";
      });
   }



   /*******************************************************************************
    ** Guided setup rows: query statistics and the admin role.
    *******************************************************************************/
   @Override
   public List<SetupCheck> setupChecks(String statsAccess, String message)
   {
      return (List.of(new SetupCheck("H2 QUERY_STATISTICS", statsAccess, "Run Enable statement statistics on Settings (admin user), or add ;QUERY_STATISTICS=TRUE;QUERY_STATISTICS_MAX_ENTRIES=1000 to the JDBC URL; then run Collect now."),
         new SetupCheck("H2 admin user", statsAccess, message == null ? "Use an H2 admin user to see every session and blocking." : message)));
   }



   /*******************************************************************************
    ** SET QUERY_STATISTICS TRUE (admin only), raising H2's default of 100 kept
    ** statements: H2 evicts a third of them, oldest first, whenever the cap is
    ** passed, and an application easily runs more distinct statements. H2 does
    ** not persist either setting: they last until the database closes, so also
    ** put them in the JDBC URL. An existing setup is left untouched.
    *******************************************************************************/
   @Override
   public String enableStatementStatistics(Connection connection) throws SQLException
   {
      if(queryStatistics(connection))
      {
         return ("H2 query statistics are already on.");
      }
      try(Statement statement = connection.createStatement())
      {
         statement.execute("SET QUERY_STATISTICS_MAX_ENTRIES " + MAX_ENTRIES);
         statement.execute("SET QUERY_STATISTICS TRUE");
      }
      return ("H2 query statistics are on (up to " + MAX_ENTRIES + " statements) until the database closes. Add ;QUERY_STATISTICS=TRUE;QUERY_STATISTICS_MAX_ENTRIES=" + MAX_ENTRIES + " to the JDBC URL to keep them on after a restart.");
   }



   /*******************************************************************************
    ** EXPLAIN (not executed) or EXPLAIN ANALYZE (executed with bound values).
    ** H2 has no READ ONLY transaction, so both refuse more than one statement
    ** and any SQL naming a side-effecting function, DDL/DML keyword or
    ** user-defined alias; ANALYZE additionally requires a single SELECT/WITH
    ** and a non-admin user. The connection is marked read-only (a hint in H2)
    ** and the transaction always rolls back.
    *******************************************************************************/
   @Override
   public String explainText(Connection connection, String sql, List<String> parameters, boolean analyze, int timeoutMs) throws SQLException
   {
      List<String> words = words(sql);
      if(statements(sql) > 1)
      {
         throw (new SQLException("Query plans on H2 accept exactly one statement.", "42000"));
      }
      List<String> aliases = read(connection, "SELECT UPPER(ROUTINE_NAME) FROM INFORMATION_SCHEMA.ROUTINES WHERE ROUTINE_SCHEMA <> 'INFORMATION_SCHEMA'", row -> row.getString(1));
      for(String word : words)
      {
         if(DENIED.contains(word) || aliases.contains(word))
         {
            throw (new SQLException("Query plans on H2 refuse " + word + ": H2 cannot run it read-only.", "42000"));
         }
      }
      if(analyze)
      {
         if(words.isEmpty() || !Set.of("SELECT", "WITH").contains(words.getFirst()))
         {
            throw (new SQLException("EXPLAIN ANALYZE on H2 accepts a single SELECT only.", "42000"));
         }
         if(admin(connection))
         {
            throw (new SQLException("EXPLAIN ANALYZE on H2 is disabled for admin users (it executes the query and H2 has no read-only transactions). Use PLAN, or connect as a non-admin user.", "42000"));
         }
      }
      boolean autoCommit = connection.getAutoCommit();
      boolean readOnly = connection.isReadOnly();
      try
      {
         connection.setReadOnly(true);
         connection.setAutoCommit(false);
         if(!analyze)
         {
            ////////////////////////////////////////////////////////////////
            // a plain statement plans ?n slots without values (generic) //
            ////////////////////////////////////////////////////////////////
            try(Statement statement = connection.createStatement())
            {
               statement.setQueryTimeout(Math.max(1, (timeoutMs + 999) / 1000));
               return (plan(statement.executeQuery("EXPLAIN " + sql)));
            }
         }
         try(PreparedStatement statement = connection.prepareStatement("EXPLAIN ANALYZE " + sql))
         {
            statement.setQueryTimeout(Math.max(1, (timeoutMs + 999) / 1000));
            for(int i = 0; i < parameters.size(); i++)
            {
               statement.setString(i + 1, parameters.get(i));
            }
            return (plan(statement.executeQuery()));
         }
      }
      finally
      {
         try
         {
            connection.rollback();
         }
         finally
         {
            connection.setAutoCommit(autoCommit);
            connection.setReadOnly(readOnly);
         }
      }
   }



   /*******************************************************************************
    ** Upper-case words outside string literals and comments; double-quoted
    ** identifiers count as words, since H2 resolves quoted function names.
    *******************************************************************************/
   static List<String> words(String sql)
   {
      String code = code(sql);
      List<String> words = new ArrayList<>();
      Matcher matcher = WORD.matcher(code);
      while(matcher.find())
      {
         words.add(matcher.group().toUpperCase(Locale.ROOT));
      }
      return (words);
   }



   /*******************************************************************************
    ** Statements separated by semicolons outside literals and comments.
    *******************************************************************************/
   static int statements(String sql)
   {
      int count = 0;
      for(String part : code(sql).split(";"))
      {
         count += part.isBlank() ? 0 : 1;
      }
      return (count);
   }



   /*******************************************************************************
    ** SQL with string literals and comments blanked, quoted identifiers unquoted.
    *******************************************************************************/
   private static String code(String sql)
   {
      return (sql.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("--[^\n]*", " ").replaceAll("'(?:[^']|'')*'", " ").replace('"', ' '));
   }



   /*******************************************************************************
    ** Plan rows joined as text; closes the result.
    *******************************************************************************/
   private static String plan(ResultSet rows) throws SQLException
   {
      try(rows)
      {
         StringBuilder plan = new StringBuilder();
         while(rows.next())
         {
            plan.append(plan.isEmpty() ? "" : "\n").append(rows.getString(1));
         }
         return (plan.toString());
      }
   }



   /*******************************************************************************
    ** QUERY_STATISTICS is not exposed in SETTINGS; when it is on, the probe just
    ** run is always recorded, and when it is off the view is empty.
    *******************************************************************************/
   boolean queryStatistics(Connection connection) throws SQLException
   {
      read(connection, PROBE, row -> null);
      return (scalar(connection, "SELECT COUNT(*) FROM INFORMATION_SCHEMA.QUERY_STATISTICS") > 0);
   }



   /*******************************************************************************
    ** Non-admin users see only their own USERS row and session.
    *******************************************************************************/
   private boolean admin(Connection connection) throws SQLException
   {
      return (!read(connection, "SELECT IS_ADMIN FROM INFORMATION_SCHEMA.USERS WHERE USER_NAME = CURRENT_USER", row -> row.getBoolean(1)).stream().filter(Boolean::booleanValue).toList().isEmpty());
   }



   /*******************************************************************************
    ** major*1000000 + minor*1000 + build, e.g. 2004240 for 2.4.240.
    *******************************************************************************/
   private int version(Connection connection) throws SQLException
   {
      String[] parts = read(connection, "SELECT H2VERSION()", row -> row.getString(1)).getFirst().split("[.\\s]");
      int version = 0;
      for(int i = 0; i < 3; i++)
      {
         version = version * 1000 + (i < parts.length && parts[i].matches("\\d+") ? Integer.parseInt(parts[i]) : 0);
      }
      return (version);
   }



   /*******************************************************************************
    ** PostgreSQL-compatible state names used by collection and presentation.
    *******************************************************************************/
   static String state(String h2State, boolean uncommitted)
   {
      String value = h2State == null ? "" : h2State.toUpperCase(Locale.ROOT);
      return (switch(value)
      {
         case "RUNNING", "BLOCKED" -> "active";
         case "SLEEP" -> uncommitted ? "idle in transaction" : "idle";
         default -> value.toLowerCase(Locale.ROOT);
      });
   }



   /*******************************************************************************
    ** Stable 64-bit identity for text (statement, table or index key).
    *******************************************************************************/
   static long hash(String text)
   {
      try
      {
         return (ByteBuffer.wrap(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))).getLong());
      }
      catch(NoSuchAlgorithmException e)
      {
         throw (new IllegalStateException("SHA-256 is required", e));
      }
   }



   /*******************************************************************************
    ** DISK_SPACE_USED for one quoted table; null when H2 cannot size it.
    *******************************************************************************/
   private static Long diskSpace(PreparedStatement size, String table)
   {
      try
      {
         size.setString(1, table);
         try(ResultSet rows = size.executeQuery())
         {
            return (rows.next() ? rows.getLong(1) : null);
         }
      }
      catch(SQLException e)
      {
         return (null);
      }
   }



   /*******************************************************************************
    ** Quoted identifier.
    *******************************************************************************/
   private static String quote(String identifier)
   {
      return ("\"" + identifier.replace("\"", "\"\"") + "\"");
   }



   /*******************************************************************************
    ** Milliseconds as a 3-decimal BigDecimal.
    *******************************************************************************/
   private static BigDecimal millis(double value)
   {
      return (BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP));
   }



   /*******************************************************************************
    ** Elapsed seconds, or null without a start.
    *******************************************************************************/
   private static BigDecimal seconds(Instant start, Instant now)
   {
      return (start == null ? null : BigDecimal.valueOf(Math.max(0, Duration.between(start, now).toMillis())).divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP));
   }



   /*******************************************************************************
    ** Numeric setting value, or null.
    *******************************************************************************/
   private static Long parseLong(String value)
   {
      return (value != null && value.matches("\\d+") ? Long.parseLong(value) : null);
   }



   /*******************************************************************************
    ** Index rows folded per index.
    *******************************************************************************/
   private static class IndexBuilder
   {
      private String schema;
      private String table;
      private String name;
      private String type;
      private boolean generated;
      private final List<String> columns = new ArrayList<>();
      private final List<String> quoted = new ArrayList<>();
   }
}
