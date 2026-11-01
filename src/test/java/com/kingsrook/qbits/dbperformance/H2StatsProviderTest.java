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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import com.kingsrook.qbits.dbperformance.provider.ConnectionCount;
import com.kingsrook.qbits.dbperformance.provider.DbStatsProviders;
import com.kingsrook.qbits.dbperformance.provider.H2StatsProvider;
import com.kingsrook.qbits.dbperformance.provider.IndexStats;
import com.kingsrook.qbits.dbperformance.provider.PostgresStatsProvider;
import com.kingsrook.qbits.dbperformance.provider.ProviderCapabilities;
import com.kingsrook.qbits.dbperformance.provider.SessionInfo;
import com.kingsrook.qbits.dbperformance.provider.StatementStats;
import com.kingsrook.qbits.dbperformance.provider.StatsFeature;
import com.kingsrook.qbits.dbperformance.provider.TableStats;
import com.kingsrook.qbits.dbperformance.provider.UnsupportedStatsProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** H2StatsProvider against real in-memory H2 databases: every read returns
 ** rows produced by activity in the test, never fixtures.
 *******************************************************************************/
class H2StatsProviderTest
{
   private final H2StatsProvider provider = new H2StatsProvider();



   /*******************************************************************************
    ** The registry maps JDBC product names and saved names to providers.
    *******************************************************************************/
   @Test
   void registryChoosesByProductName() throws Exception
   {
      assertThat(DbStatsProviders.forProductName("H2")).isInstanceOf(H2StatsProvider.class);
      assertThat(DbStatsProviders.forProductName("PostgreSQL")).isInstanceOf(PostgresStatsProvider.class);
      assertThat(DbStatsProviders.forProductName("MySQL")).isInstanceOf(UnsupportedStatsProvider.class);
      assertThat(DbStatsProviders.forProductName(null)).isInstanceOf(UnsupportedStatsProvider.class);
      assertThat(DbStatsProviders.forProviderName("H2")).isInstanceOf(H2StatsProvider.class);
      assertThat(DbStatsProviders.forProviderName("POSTGRES")).isInstanceOf(PostgresStatsProvider.class);
      assertThat(DbStatsProviders.forProviderName(null)).isInstanceOf(UnsupportedStatsProvider.class);
      try(Connection connection = open(false, ""))
      {
         assertThat(DbStatsProviders.forConnection(connection)).isInstanceOf(H2StatsProvider.class);
      }
      assertThat(DbStatsProviders.forProductName("ExampleDB")).isInstanceOf(UnsupportedStatsProvider.class);
      DbStatsProviders.register("ExampleDB", H2StatsProvider::new);
      assertThat(DbStatsProviders.forProductName("exampledb")).isInstanceOf(H2StatsProvider.class);
      assertThat(new UnsupportedStatsProvider().capabilities(null).features()).isEmpty();
   }



   /*******************************************************************************
    ** Every read returns real rows after activity, in default and MySQL mode.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"", ";MODE=MySQL", ";MODE=PostgreSQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE"})
   void readsReturnRealRowsAfterActivity(String mode) throws Exception
   {
      String url = url(true, mode);
      try(Connection connection = DriverManager.getConnection(url, "sa", ""))
      {
         activity(connection);
         ProviderCapabilities capabilities = provider.capabilities(connection);
         assertThat(capabilities.supported()).isTrue();
         assertThat(capabilities.statsAccess()).isEqualTo("FULL");
         assertThat(capabilities.statements()).isTrue();
         assertThat(capabilities.readAllStats()).isTrue();
         assertThat(capabilities.version()).isGreaterThanOrEqualTo(2000000);
         assertThat(capabilities.features()).contains(StatsFeature.STATEMENTS, StatsFeature.SESSIONS, StatsFeature.BLOCKING, StatsFeature.TABLE_ROWS, StatsFeature.EXPLAIN_TEXT)
            .doesNotContain(StatsFeature.CACHE_COUNTERS, StatsFeature.DATABASE_COUNTERS, StatsFeature.INDEX_USAGE, StatsFeature.TABLE_MAINTENANCE, StatsFeature.MAX_CONNECTIONS, StatsFeature.EXPLAIN_JSON);
         assertThat(provider.features()).containsAll(capabilities.features());

         assertThat(provider.readDatabase(connection).counters()).isEmpty();
         assertThat(provider.readDatabase(connection).maxConnections()).isZero();
         assertThat(provider.readStatementResetInfo(connection).statsReset()).isNull();

         provider.readTables(connection);
         List<StatementStats> statements = provider.readStatements(connection);
         StatementStats lookup = statements.stream().filter(s -> s.queryText().contains("WHERE customer_id = 3")).findFirst().orElseThrow();
         assertThat(lookup.calls()).isEqualTo(5);
         assertThat(lookup.rows()).isEqualTo(5 * 10);
         assertThat(lookup.totalExecMs()).isGreaterThanOrEqualTo(lookup.maxExecMs());
         assertThat(lookup.dbUser()).isNull();
         assertThat(statements).noneMatch(s -> s.queryText().toUpperCase().contains("INFORMATION_SCHEMA") || s.queryText().contains("DB_PERF_PROBE")
            || s.queryText().toUpperCase().startsWith("SET ") || s.queryText().contains("DISK_SPACE_USED") || s.queryText().equalsIgnoreCase("CALL DATABASE()"));
         assertThat(provider.readStatements(connection)).anyMatch(s -> s.queryId() == lookup.queryId() && s.queryText().equals(lookup.queryText()));

         TableStats orders = provider.readTables(connection).stream().filter(t -> t.tableName().equalsIgnoreCase("orders")).findFirst().orElseThrow();
         assertThat(orders.counters()).containsEntry("nLiveTup", 50L).containsKeys("tableBytes", "totalBytes").doesNotContainKeys("seqScan", "nDeadTup");
         assertThat(orders.relid()).isPositive();

         List<IndexStats> indexes = provider.readIndexes(connection).stream().filter(i -> i.tableName().equalsIgnoreCase("orders")).toList();
         assertThat(indexes).anySatisfy(i -> assertThat(i.primary() && i.unique()).isTrue());
         assertThat(indexes).anySatisfy(i ->
         {
            assertThat(i.indexName()).isEqualToIgnoringCase("orders_status");
            assertThat(i.unique()).isFalse();
            assertThat(i.columns()).isEqualToIgnoringCase("status, created DESC");
            assertThat(i.definition()).contains("CREATE INDEX").doesNotContain("USING CONSTRAINT");
         });
         assertThat(indexes).anySatisfy(i -> assertThat(i.definition()).contains("USING CONSTRAINT"));

         try(Connection holder = DriverManager.getConnection(url, "sa", ""); Connection waiter = DriverManager.getConnection(url + ";LOCK_TIMEOUT=10000", "sa", ""))
         {
            holder.setAutoCommit(false);
            try(Statement statement = holder.createStatement())
            {
               statement.executeUpdate("UPDATE orders SET status = 'HELD' WHERE id = 1");
            }
            CompletableFuture<Void> blocked = CompletableFuture.runAsync(() -> update(waiter));
            List<SessionInfo> sessions = waitForBlocked(connection);
            SessionInfo idle = sessions.stream().filter(s -> s.state().equals("idle in transaction")).findFirst().orElseThrow();
            assertThat(idle.stateChange()).isNotNull();
            assertThat(idle.xactSeconds()).isNotNull();
            SessionInfo waiting = sessions.stream().filter(s -> !s.blockedBy().isEmpty()).findFirst().orElseThrow();
            assertThat(waiting.state()).isEqualTo("active");
            assertThat(waiting.waitEventType()).isEqualTo("Lock");
            assertThat(waiting.blockedBy()).containsExactly(idle.pid());
            assertThat(waiting.queryText()).contains("UPDATE orders");
            assertThat(waiting.queryId()).isNotNull();
            assertThat(waiting.querySeconds()).isNotNull();
            assertThat(provider.readConnections(connection).stream().mapToInt(ConnectionCount::connectionCount).sum()).isGreaterThanOrEqualTo(3);
            assertThat(provider.readConnections(connection)).anyMatch(c -> "idle in transaction".equals(c.state()));
            holder.rollback();
            blocked.get();
         }

         Map<String, String> settings = provider.readSettings(connection);
         assertThat(settings).containsEntry("QUERY_STATISTICS", "TRUE").containsKey("info.VERSION");
      }
   }



   /*******************************************************************************
    ** Without QUERY_STATISTICS statements are unavailable until enabled.
    *******************************************************************************/
   @Test
   void capabilitiesWithoutQueryStatisticsAndEnable() throws Exception
   {
      try(Connection connection = open(false, ""))
      {
         activity(connection);
         ProviderCapabilities off = provider.capabilities(connection);
         assertThat(off.supported()).isTrue();
         assertThat(off.statements()).isFalse();
         assertThat(off.statsAccess()).isEqualTo("NO_QUERY_STATISTICS");
         assertThat(off.message()).contains("QUERY_STATISTICS=TRUE");
         assertThat(off.has(StatsFeature.STATEMENTS)).isFalse();
         assertThat(off.has(StatsFeature.SESSIONS)).isTrue();
         assertThat(provider.readStatements(connection)).isEmpty();
         assertThat(provider.readTables(connection)).isNotEmpty();
         assertThat(provider.readSettings(connection)).containsEntry("QUERY_STATISTICS", "FALSE");

         assertThat(provider.enableStatementStatistics(connection)).contains("JDBC URL");
         activity(connection);
         ProviderCapabilities on = provider.capabilities(connection);
         assertThat(on.statsAccess()).isEqualTo("FULL");
         assertThat(on.has(StatsFeature.STATEMENTS)).isTrue();
         assertThat(provider.readStatements(connection)).anyMatch(s -> s.queryText().contains("WHERE customer_id = 3") && s.calls() == 5);
         assertThat(provider.enableStatementStatistics(connection)).contains("already on");
      }
   }



   /*******************************************************************************
    ** A non-admin user sees only its own session and cannot enable statistics.
    *******************************************************************************/
   @Test
   void nonAdminUser() throws Exception
   {
      String url = url(true, "");
      try(Connection admin = DriverManager.getConnection(url, "sa", ""))
      {
         try(Statement statement = admin.createStatement())
         {
            statement.execute("CREATE USER app PASSWORD 'secret'");
            statement.execute("CREATE TABLE probe (id INT PRIMARY KEY)");
            statement.execute("GRANT SELECT ON probe TO app");
         }
         try(Connection app = DriverManager.getConnection(url.replace(";DB_CLOSE_DELAY=-1", "").replace(";QUERY_STATISTICS=TRUE", ""), "app", "secret"))
         {
            ProviderCapabilities capabilities = provider.capabilities(app);
            assertThat(capabilities.statsAccess()).isEqualTo("NOT_ADMIN");
            assertThat(capabilities.readAllStats()).isFalse();
            assertThat(capabilities.statements()).isTrue();
            assertThat(capabilities.has(StatsFeature.BLOCKING)).isFalse();
            assertThat(provider.readSessions(app)).isEmpty();
            assertThat(provider.remedy("NOT_ADMIN", "APP")).contains("ALTER USER APP ADMIN TRUE");
            try(Statement statement = admin.createStatement())
            {
               statement.execute("SET QUERY_STATISTICS FALSE");
            }
            assertThatThrownBy(() -> provider.enableStatementStatistics(app)).isInstanceOf(SQLException.class);
         }
      }
   }



   /*******************************************************************************
    ** EXPLAIN plans without executing; ANALYZE only for a non-admin SELECT;
    ** side-effecting functions, aliases, DDL and multiple statements refused.
    *******************************************************************************/
   @Test
   void explainText() throws Exception
   {
      String url = url(true, ";MODE=MySQL");
      try(Connection connection = DriverManager.getConnection(url, "sa", ""))
      {
         activity(connection);
         assertThat(provider.capabilities(connection).has(StatsFeature.EXPLAIN_ANALYZE)).isFalse();
         assertThat(provider.capabilities(connection).has(StatsFeature.EXPLAIN_TEXT)).isTrue();
         assertThat(provider.explainText(connection, "SELECT * FROM orders WHERE status = 'NEW'", List.of(), false, 5000)).containsIgnoringCase("orders_status").doesNotContain("scanCount");
         assertThat(provider.explainText(connection, "SELECT * FROM orders WHERE customer_id = ?1", List.of(), false, 5000)).contains("?1");
         assertThat(connection.getAutoCommit()).isTrue();
         assertThat(connection.isReadOnly()).isFalse();
         assertThatThrownBy(() -> provider.explainText(connection, "SELECT * FROM orders WHERE customer_id = ?1", List.of("3"), true, 5000)).hasMessageContaining("admin");
         assertThatThrownBy(() -> provider.explainText(connection, "SELECT FILE_WRITE('x', '/tmp/dbperf-h2-explain-probe') FROM orders", List.of(), false, 5000)).hasMessageContaining("FILE_WRITE");
         assertThatThrownBy(() -> provider.explainText(connection, "SELECT \"file_write\"('x', '/tmp/p')", List.of(), false, 5000)).hasMessageContaining("FILE_WRITE");
         assertThatThrownBy(() -> provider.explainText(connection, "DROP TABLE orders", List.of(), false, 5000)).hasMessageContaining("DROP");
         assertThatThrownBy(() -> provider.explainText(connection, "SELECT 1; SELECT 2", List.of(), false, 5000)).hasMessageContaining("one statement");
         assertThat(provider.explainText(connection, "SELECT 'drop; file_write' AS label FROM orders", List.of(), false, 5000)).containsIgnoringCase("orders");
         try(Statement statement = connection.createStatement())
         {
            statement.execute("CREATE ALIAS my_side_effect FOR \"java.lang.System.nanoTime\"");
            statement.execute("CREATE USER reader PASSWORD 'secret'");
            statement.execute("GRANT SELECT ON orders TO reader");
         }
         assertThatThrownBy(() -> provider.explainText(connection, "SELECT my_side_effect() FROM orders", List.of(), false, 5000)).hasMessageContaining("MY_SIDE_EFFECT");
         try(Connection reader = DriverManager.getConnection(url.replace(";DB_CLOSE_DELAY=-1", "").replace(";QUERY_STATISTICS=TRUE", ""), "reader", "secret"))
         {
            assertThat(provider.capabilities(reader).has(StatsFeature.EXPLAIN_ANALYZE)).isTrue();
            assertThat(provider.explainText(reader, "SELECT * FROM orders WHERE customer_id = ?1", List.of("3"), true, 5000)).contains("scanCount");
            assertThatThrownBy(() -> provider.explainText(reader, "VALUES (1)", List.of(), true, 5000)).hasMessageContaining("single SELECT");
         }
      }
      assertThat(java.nio.file.Files.exists(java.nio.file.Path.of("/tmp/dbperf-h2-explain-probe"))).isFalse();
   }



   /*******************************************************************************
    ** H2 session states use PostgreSQL's names.
    *******************************************************************************/
   @Test
   void stateMapping() throws Exception
   {
      assertThat(provider.setupChecks("NO_QUERY_STATISTICS", "m")).extracting(c -> c.check()).containsExactly("H2 QUERY_STATISTICS", "H2 admin user");
      assertThat(provider.remedy("FULL", "sa")).isEqualTo("Full statistics access");
      assertThat(provider.remedy("UNSUPPORTED", "sa")).contains("H2 2.0");
      assertThat(new PostgresStatsProvider().remedy("NO_READ_ALL_STATS", "sa")).isEqualTo("grant pg_read_all_stats to sa");
      assertThat(new PostgresStatsProvider().setupChecks("FULL", null)).extracting(c -> c.check()).containsExactly("pg_stat_statements", "pg_read_all_stats");
   }



   /*******************************************************************************
    ** Tables, a secondary index, a foreign key and repeated queries.
    *******************************************************************************/
   private static void activity(Connection connection) throws SQLException
   {
      try(Statement statement = connection.createStatement())
      {
         statement.execute("CREATE TABLE IF NOT EXISTS customer (id INT PRIMARY KEY, name VARCHAR(50))");
         statement.execute("CREATE TABLE IF NOT EXISTS orders (id INT PRIMARY KEY, customer_id INT REFERENCES customer(id), status VARCHAR(20), created TIMESTAMP)");
         statement.execute("CREATE INDEX IF NOT EXISTS orders_status ON orders (status, created DESC)");
         statement.execute("MERGE INTO customer KEY (id) SELECT x, 'c' || x FROM SYSTEM_RANGE(1, 5)");
         statement.execute("MERGE INTO orders KEY (id) SELECT x, MOD(x, 5) + 1, 'NEW', CURRENT_TIMESTAMP FROM SYSTEM_RANGE(1, 50)");
         for(int i = 0; i < 5; i++)
         {
            statement.executeQuery("SELECT * FROM orders WHERE customer_id = 3").close();
         }
      }
   }



   /*******************************************************************************
    ** Poll until the waiter shows as blocked.
    *******************************************************************************/
   private List<SessionInfo> waitForBlocked(Connection connection) throws Exception
   {
      for(int i = 0; i < 100; i++)
      {
         List<SessionInfo> sessions = provider.readSessions(connection);
         if(sessions.stream().anyMatch(s -> !s.blockedBy().isEmpty()))
         {
            return (sessions);
         }
         Thread.sleep(50);
      }
      throw (new AssertionError("no blocked session observed"));
   }



   /*******************************************************************************
    ** Blocking update for the waiter thread.
    *******************************************************************************/
   private static void update(Connection waiter)
   {
      try(Statement statement = waiter.createStatement())
      {
         statement.executeUpdate("UPDATE orders SET status = 'WAIT' WHERE id = 1");
      }
      catch(SQLException e)
      {
         throw (new IllegalStateException(e));
      }
   }



   /*******************************************************************************
    ** A fresh in-memory database.
    *******************************************************************************/
   private static Connection open(boolean statistics, String mode) throws SQLException
   {
      return (DriverManager.getConnection(url(statistics, mode), "sa", ""));
   }



   /*******************************************************************************
    ** URL of a fresh in-memory database.
    *******************************************************************************/
   private static String url(boolean statistics, String mode)
   {
      return ("jdbc:h2:mem:h2stats" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1" + (statistics ? ";QUERY_STATISTICS=TRUE" : "") + mode);
   }
}
