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

package com.kingsrook.qbits.dbperformance.collect;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.normalize.SqlNormalizer;
import com.kingsrook.qbits.dbperformance.provider.ConnectionCount;
import com.kingsrook.qbits.dbperformance.provider.DatabaseStats;
import com.kingsrook.qbits.dbperformance.provider.DbStatsProvider;
import com.kingsrook.qbits.dbperformance.provider.DbStatsProviders;
import com.kingsrook.qbits.dbperformance.provider.IndexStats;
import com.kingsrook.qbits.dbperformance.provider.ProviderCapabilities;
import com.kingsrook.qbits.dbperformance.provider.SessionInfo;
import com.kingsrook.qbits.dbperformance.provider.StatementStats;
import com.kingsrook.qbits.dbperformance.provider.TableStats;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Transactional delta collection; raw JDBC is used only for statistics reads.
 *******************************************************************************/
public class SnapshotCollector
{
   private static final QLogger LOG = QLogger.getLogger(SnapshotCollector.class);

   private final DbPerfRuntime runtime;
   private final Clock clock;
   private final DbStatsProvider providerOverride;


   /*******************************************************************************
    ** Production provider is chosen from the backend connection.
    *******************************************************************************/
   public SnapshotCollector(DbPerfRuntime runtime)
   {
      this(runtime, Clock.systemUTC(), null);
   }


   /*******************************************************************************
    ** Explicit clock and provider allow deterministic reset/failure tests.
    *******************************************************************************/
   public SnapshotCollector(DbPerfRuntime runtime, Clock clock, DbStatsProvider providerOverride)
   {
      this.runtime = runtime;
      this.clock = clock;
      this.providerOverride = providerOverride;
   }


   /*******************************************************************************
    ** Serialize within an instance and commit counters with their detail rows.
    ** A failure leaves a FAILED run record and never escapes to the scheduler.
    *******************************************************************************/
   public QRecord collect(String backendName, boolean forceTables)
   {
      synchronized(runtime)
      {
         long started = System.nanoTime();
         QRecord snapshot = new QRecord().withValue("backendName", backendName).withValue("capturedAt", clock.instant()).withValue("status", "RUNNING");
         try
         {
            if(!Objects.equals(backendName, runtime.getConfig().getBackendName()))
            {
               throw (new IllegalArgumentException("Backend is not configured for this runtime"));
            }
            QRecord settings = new DbPerfSettingsService(runtime).ensure(backendName);
            failAbandonedRuns(backendName, settings);
            QRecord inserted = runtime.asSystem(() -> DbPerfStore.insert("db_perf_snapshot", List.of(snapshot)).getFirst());
            snapshot.setValue("id", inserted.getValueInteger("id"));
            runtime.transaction(() ->
            {
               if(!Boolean.TRUE.equals(settings.getValueBoolean("enabled")))
               {
                  snapshot.withValue("status", "PARTIAL").withValue("errorMessage", "Collection is disabled in Settings.");
               }
               else
               {
                  collectInTransaction(backendName, forceTables, settings, snapshot);
               }
               snapshot.setValue("durationMs", (int) Math.min(Integer.MAX_VALUE, (System.nanoTime() - started) / 1000000));
               DbPerfStore.update("db_perf_snapshot", List.of(snapshot));
               return (null);
            });
         }
         catch(Throwable e)
         {
            //////////////////////////////////////////////////////////////////////////
            // Errors too (a StackOverflowError once left runs RUNNING with no log). //
            // Never persist or log a JDBC exception message: it may contain SQL    //
            // literals. The class and the throwing frame identify the cause.       //
            //////////////////////////////////////////////////////////////////////////
            if(e instanceof OutOfMemoryError outOfMemory)
            {
               throw (outOfMemory);
            }
            LOG.error("Database performance snapshot failed", logPair("backend", backendName), logPair("snapshotId", snapshot.getValueInteger("id")),
               logPair("exception", e.getClass().getName()), logPair("at", e.getStackTrace().length == 0 ? null : e.getStackTrace()[0].toString()));
            snapshot.withValue("status", "FAILED").withValue("errorMessage", "Collection failed (" + e.getClass().getSimpleName() + "). Check database connectivity, schema and statistics access.")
               .withValue("durationMs", (int) Math.min(Integer.MAX_VALUE, (System.nanoTime() - started) / 1000000));
            if(snapshot.getValueInteger("id") != null)
            {
               try
               {
                  runtime.asSystem(() ->
                  {
                     DbPerfStore.update("db_perf_snapshot", List.of(new QRecord().withValue("id", snapshot.getValueInteger("id"))
                        .withValue("status", "FAILED").withValue("errorMessage", snapshot.getValueString("errorMessage")).withValue("durationMs", snapshot.getValueInteger("durationMs"))));
                     return (null);
                  });
               }
               catch(Exception persistenceFailure)
               {
                  snapshot.setValue("errorMessage", snapshot.getValueString("errorMessage") + " Failure status could not be saved.");
               }
            }
         }
         new com.kingsrook.qbits.dbperformance.alerts.SnapshotEvaluator(runtime).evaluate(snapshot);
         return (snapshot);
      }
   }


   /*******************************************************************************
    ** Runs left RUNNING by a crashed pod or an earlier failure are closed as
    ** FAILED once they are older than three collection intervals.
    *******************************************************************************/
   private void failAbandonedRuns(String backendName, QRecord settings) throws Exception
   {
      int     interval = positive(settings, "snapshotIntervalMinutes", 5);
      Instant cutoff   = clock.instant().minus(Duration.ofMinutes(3L * interval));
      List<QRecord> abandoned = runtime.asSystem(() -> DbPerfStore.query("db_perf_snapshot", DbPerfStore.eq("backendName", backendName)
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, "RUNNING"))
         .withCriteria(new QFilterCriteria("capturedAt", QCriteriaOperator.LESS_THAN, cutoff))));
      if(abandoned.isEmpty())
      {
         return;
      }
      List<QRecord> updates = abandoned.stream().map(row -> new QRecord().withValue("id", row.getValueInteger("id")).withValue("status", "FAILED")
         .withValue("errorMessage", "Collection did not finish (the run was interrupted or its pod stopped).")).toList();
      runtime.asSystem(() ->
      {
         DbPerfStore.update("db_perf_snapshot", updates);
         return (null);
      });
      LOG.warn("Closed abandoned database performance snapshots", logPair("backend", backendName), logPair("count", updates.size()));
   }



   /*******************************************************************************
    ** Statistics read and writes under the ambient system transaction.
    *******************************************************************************/
   private void collectInTransaction(String backendName, boolean forceTables, QRecord settings, QRecord snapshot) throws Exception
   {
      RDBMSBackendMetaData storageBackend = (RDBMSBackendMetaData) runtime.getInstance().getBackend(backendName);
      if(storageBackend.getActionStrategy().supportsForUpdateSkipLocked())
      {
         var locked = new com.kingsrook.qqq.backend.core.actions.tables.QueryAction().execute(new com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput("db_perf_settings")
            .withFilter(DbPerfStore.eq("backendName", backendName)).withTransaction(com.kingsrook.qqq.backend.core.context.QContext.getQBackendTransaction())
            .withLockMode(com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryLockMode.FOR_UPDATE_SKIP_LOCKED)).getRecords();
         if(locked.isEmpty())
         {
            snapshot.withValue("status", "PARTIAL").withValue("errorMessage", "Another collector is already running for this backend.");
            return;
         }
         settings = locked.getFirst();
         if(!Boolean.TRUE.equals(settings.getValueBoolean("enabled")))
         {
            snapshot.withValue("status", "PARTIAL").withValue("errorMessage", "Collection is disabled in Settings.");
            return;
         }
      }
      Instant now = snapshot.getValueInstant("capturedAt");
      QQueryFilter filter = DbPerfStore.eq("backendName", backendName)
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.IN, List.of("OK", "PARTIAL")))
         .withCriteria(new QFilterCriteria("rawXactCommit", QCriteriaOperator.IS_NOT_BLANK))
         .withOrderBy(new QFilterOrderBy("capturedAt", false));
      QRecord previous = DbPerfStore.first("db_perf_snapshot", filter);
      Instant previousCapture = previous == null ? null : previous.getValueInstant("capturedAt");
      snapshot.withValue("intervalSeconds", previousCapture == null ? 0 : Math.max(0, (int) Duration.between(previousCapture, now).toSeconds()))
         .withValue("statsResetDetected", false).withValue("includesTables", false);
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) runtime.getInstance().getBackend(backendName);
      try(Connection connection = ConnectionManager.getConnection(backend))
      {
         connection.setReadOnly(true);
         DbStatsProvider provider = providerOverride == null ? DbStatsProviders.forConnection(connection) : providerOverride;
         ProviderCapabilities capability = provider.capabilities(connection);
         settings.withValue("provider", provider.getName()).withValue("statsAccess", capability.statsAccess()).withValue("statsAccessMessage", capability.message());
         if(capability.supported())
         {
            DatabaseStats database = provider.readDatabase(connection);
            boolean databaseReset = previous != null && database.statsReset() != null && !Objects.equals(database.statsReset(), previous.getValueInstant("rawStatsReset"));
            if(previous != null)
            {
               for(Map.Entry<String, Long> entry : database.counters().entrySet())
               {
                  String raw = "raw" + Character.toUpperCase(entry.getKey().charAt(0)) + entry.getKey().substring(1);
                  if(previous.getValueLong(raw) != null && entry.getValue() < previous.getValueLong(raw))
                  {
                     databaseReset = true;
                  }
               }
            }
            for(Map.Entry<String, Long> entry : database.counters().entrySet())
            {
               String raw = "raw" + Character.toUpperCase(entry.getKey().charAt(0)) + entry.getKey().substring(1);
               Long before = previous == null ? null : previous.getValueLong(raw);
               snapshot.withValue(entry.getKey(), DeltaCalculator.counter(before, entry.getValue(), databaseReset)).withValue(raw, entry.getValue());
               if(before != null && entry.getValue() < before)
               {
                  databaseReset = true;
               }
            }
            snapshot.withValue("rawStatsReset", database.statsReset()).withValue("statsResetDetected", databaseReset)
               .withValue("databaseSizeBytes", database.sizeBytes()).withValue("maxConnections", database.maxConnections())
               .withValue("cacheHitRatio", ratio(number(snapshot, "blksHit"), number(snapshot, "blksHit") + number(snapshot, "blksRead")));
            if(capability.statements())
            {
               Instant reset = provider.readStatementResetInfo(connection).statsReset();
               snapshot.setValue("rawStatementStatsReset", reset);
               if(reset != null && previousCapture != null && reset.isAfter(previousCapture))
               {
                  snapshot.setValue("statsResetDetected", true);
               }
               collectStatements(backendName, provider.readStatements(connection), previousCapture, reset, snapshot, settings);
            }
            List<ConnectionCount> connections = provider.readConnections(connection);
            collectConnections(connections, snapshot);
            collectSessions(provider.readSessions(connection), snapshot, settings, backendName);
            Instant lastTables = settings.getValueInstant("lastTableSnapshotAt");
            if(forceTables || lastTables == null || !lastTables.plusSeconds(60L * positive(settings, "tableSnapshotMinutes", 60)).isAfter(now))
            {
               collectTables(backendName, provider.readTables(connection), provider.readIndexes(connection), snapshot, databaseReset);
               settings.setValue("lastTableSnapshotAt", now);
               snapshot.setValue("includesTables", true);
            }
         }
         snapshot.withValue("status", "FULL".equals(capability.statsAccess()) ? "OK" : "PARTIAL")
            .withValue("errorMessage", "FULL".equals(capability.statsAccess()) ? null : capability.message());
         QRecord runtimeSettings = new QRecord().withValue("id", settings.getValueInteger("id")).withValue("lastSnapshotAt", now)
            .withValue("provider", settings.getValueString("provider")).withValue("statsAccess", settings.getValueString("statsAccess"))
            .withValue("statsAccessMessage", settings.getValueString("statsAccessMessage"));
         if(Boolean.TRUE.equals(snapshot.getValueBoolean("includesTables")))
         {
            runtimeSettings.setValue("lastTableSnapshotAt", now);
         }
         DbPerfStore.update("db_perf_settings", List.of(runtimeSettings));
      }
   }


   /*******************************************************************************
    ** Upsert all statement baselines, but retain only the top N interval rows.
    ** The denominator and total calls include every observed statement delta.
    *******************************************************************************/
   private void collectStatements(String backend, List<StatementStats> current, Instant previousCapture, Instant reset, QRecord snapshot, QRecord settings) throws Exception
   {
      Map<String, QRecord> previous = new HashMap<>();
      for(QRecord row : DbPerfStore.query("db_perf_statement", DbPerfStore.eq("backendName", backend)))
      {
         previous.put(statementKey(row.getValueLong("queryId"), row.getValueString("dbUser"), Boolean.TRUE.equals(row.getValueBoolean("toplevel"))), row);
      }
      List<QRecord> inserts = new ArrayList<>();
      List<QRecord> updates = new ArrayList<>();
      List<QRecord> deltas = new ArrayList<>();
      Map<QRecord, String> keys = new LinkedHashMap<>();
      BigDecimal total = BigDecimal.ZERO;
      long calls = 0;
      Instant now = snapshot.getValueInstant("capturedAt");
      for(StatementStats entry : current)
      {
         String key = statementKey(entry.queryId(), entry.dbUser(), entry.toplevel());
         QRecord old = previous.get(key);
         DeltaCalculator.Delta delta = DeltaCalculator.calculate(old == null ? null : storedStatement(old), entry, previousCapture, reset);
         String text = SqlNormalizer.normalize(entry.queryText());
         QRecord row = new QRecord().withValue("backendName", backend).withValue("queryId", entry.queryId()).withValue("dbUser", entry.dbUser()).withValue("toplevel", entry.toplevel())
            .withValue("queryText", text).withValue("fingerprint", SqlNormalizer.fingerprint(text)).withValue("operation", SqlNormalizer.operation(text)).withValue("mainTable", SqlNormalizer.mainTable(text))
            .withValue("lastSeenAt", now).withValue("lastCalls", entry.calls()).withValue("lastTotalExecMs", entry.totalExecMs()).withValue("lastRows", entry.rows())
            .withValue("lastSharedBlksHit", entry.sharedBlksHit()).withValue("lastSharedBlksRead", entry.sharedBlksRead()).withValue("lastTempBlksWritten", entry.tempBlksWritten())
            .withValue("lastMaxExecMs", entry.maxExecMs()).withValue("lastStatsSince", entry.statsSince());
         if(old == null)
         {
            row.setValue("firstSeenAt", now);
            inserts.add(row);
         }
         else
         {
            row.setValue("id", old.getValueInteger("id"));
            updates.add(row);
         }
         if(delta.reset())
         {
            snapshot.setValue("statsResetDetected", true);
         }
         total = total.add(delta.totalMs());
         calls += delta.calls();
         if(!delta.baseline() && (delta.calls() > 0 || delta.totalMs().signum() > 0))
         {
            QRecord detail = new QRecord().withValue("snapshotId", snapshot.getValueInteger("id")).withValue("capturedAt", now)
               .withValue("calls", delta.calls()).withValue("totalMs", delta.totalMs()).withValue("meanMs", divide(delta.totalMs(), delta.calls()))
               .withValue("maxMs", entry.maxExecMs()).withValue("rowsCount", delta.rows()).withValue("sharedBlksHit", delta.sharedBlksHit())
               .withValue("sharedBlksRead", delta.sharedBlksRead()).withValue("hitRatio", ratio(delta.sharedBlksHit(), delta.sharedBlksHit() + delta.sharedBlksRead()))
               .withValue("tempBlksWritten", delta.tempBlksWritten());
            deltas.add(detail);
            keys.put(detail, key);
         }
      }
      for(QRecord inserted : DbPerfStore.insert("db_perf_statement", inserts))
      {
         previous.put(statementKey(inserted.getValueLong("queryId"), inserted.getValueString("dbUser"), Boolean.TRUE.equals(inserted.getValueBoolean("toplevel"))), inserted);
      }
      DbPerfStore.update("db_perf_statement", updates);
      deltas.sort(Comparator.comparing((QRecord row) -> row.getValueBigDecimal("totalMs")).reversed());
      List<QRecord> top = deltas.subList(0, Math.min(deltas.size(), positive(settings, "topStatementsPerSnapshot", 200)));
      for(QRecord row : top)
      {
         row.withValue("statementId", previous.get(keys.get(row)).getValueInteger("id"))
            .withValue("shareOfDbTime", total.signum() == 0 ? BigDecimal.ZERO : row.getValueBigDecimal("totalMs").multiply(BigDecimal.valueOf(100)).divide(total, 4, RoundingMode.HALF_UP));
      }
      DbPerfStore.insert("db_perf_query_snapshot", top);
      snapshot.withValue("statementCount", current.size()).withValue("statementCalls", calls).withValue("statementTotalMs", total).withValue("storedStatementCount", top.size());
      refresh24hours(backend, previous, current, snapshot);
   }


   /*******************************************************************************
    ** Stored counters have the same shape as provider counters.
    *******************************************************************************/
   private StatementStats storedStatement(QRecord row)
   {
      return (new StatementStats(number(row, "queryId"), row.getValueString("dbUser"), Boolean.TRUE.equals(row.getValueBoolean("toplevel")), row.getValueString("queryText"), number(row, "lastCalls"), decimal(row, "lastTotalExecMs"), BigDecimal.ZERO, decimal(row, "lastMaxExecMs"), number(row, "lastRows"), number(row, "lastSharedBlksHit"), number(row, "lastSharedBlksRead"), number(row, "lastTempBlksWritten"), row.getValueInstant("lastStatsSince"), null));
   }


   /*******************************************************************************
    ** Recompute touched statements against the exact all-statement denominator.
    *******************************************************************************/
   private void refresh24hours(String backend, Map<String, QRecord> known, List<StatementStats> touched, QRecord snapshot) throws Exception
   {
      Instant since = snapshot.getValueInstant("capturedAt").minusSeconds(86400);
      BigDecimal denominator = decimal(snapshot, "statementTotalMs");
      for(QRecord row : DbPerfStore.query("db_perf_snapshot", DbPerfStore.eq("backendName", backend).withCriteria(new QFilterCriteria("capturedAt", QCriteriaOperator.GREATER_THAN_OR_EQUALS, since))
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.IN, List.of("OK", "PARTIAL")))))
      {
         denominator = denominator.add(decimal(row, "statementTotalMs"));
      }
      Map<Integer, List<QRecord>> details = DbPerfStore.query("db_perf_query_snapshot", new QQueryFilter(new QFilterCriteria("capturedAt", QCriteriaOperator.GREATER_THAN_OR_EQUALS, since)))
         .stream().collect(Collectors.groupingBy(row -> row.getValueInteger("statementId")));
      //////////////////////////////////////////////////////////////////////////
      // refresh every statement with activity in the window, plus any whose  //
      // stored 24h numbers are non-zero (so idle statements decay to zero)   //
      //////////////////////////////////////////////////////////////////////////
      Set<Integer> ids = new LinkedHashSet<>(details.keySet());
      for(StatementStats entry : touched)
      {
         ids.add(known.get(statementKey(entry.queryId(), entry.dbUser(), entry.toplevel())).getValueInteger("id"));
      }
      for(QRecord row : known.values())
      {
         if(row.getValueInteger("id") != null && number(row, "calls24h") != 0)
         {
            ids.add(row.getValueInteger("id"));
         }
      }
      List<QRecord> updates = new ArrayList<>();
      for(Integer id : ids)
      {
         List<QRecord> rows = details.getOrDefault(id, List.of());
         long calls = rows.stream().mapToLong(row -> number(row, "calls")).sum();
         BigDecimal total = rows.stream().map(row -> decimal(row, "totalMs")).reduce(BigDecimal.ZERO, BigDecimal::add);
         updates.add(new QRecord().withValue("id", id).withValue("calls24h", calls).withValue("totalMs24h", total).withValue("meanMs24h", divide(total, calls))
            .withValue("shareOfDbTime24h", denominator.signum() == 0 ? BigDecimal.ZERO : total.multiply(BigDecimal.valueOf(100)).divide(denominator, 4, RoundingMode.HALF_UP)));
      }
      DbPerfStore.update("db_perf_statement", updates);
   }


   /*******************************************************************************
    ** Upsert table/index gauges; snapshot scan/write/maintenance counters as deltas.
    *******************************************************************************/
   private void collectTables(String backend, List<TableStats> tables, List<IndexStats> indexes, QRecord snapshot, boolean reset) throws Exception
   {
      Map<String, QRecord> known = new HashMap<>();
      for(QRecord row : DbPerfStore.query("db_perf_table_stat", DbPerfStore.eq("backendName", backend)))
      {
         known.put(row.getValueString("schemaName") + "." + row.getValueString("tableName"), row);
      }
      List<QRecord> inserts = new ArrayList<>();
      List<QRecord> updates = new ArrayList<>();
      List<QRecord> details = new ArrayList<>();
      Map<QRecord, String> detailKeys = new LinkedHashMap<>();
      Instant now = snapshot.getValueInstant("capturedAt");
      for(TableStats entry : tables)
      {
         String key = entry.schemaName() + "." + entry.tableName();
         QRecord old = known.get(key);
         QRecord row = new QRecord().withValue("backendName", backend).withValue("schemaName", entry.schemaName()).withValue("tableName", entry.tableName()).withValue("relid", entry.relid()).withValue("lastSeenAt", now);
         entry.counters().forEach(row::setValue);
         entry.maintenance().forEach(row::setValue);
         row.withValue("deadRatio", ratio(number(row, "nDeadTup"), number(row, "nLiveTup") + number(row, "nDeadTup")))
            .withValue("seqScanShare", ratio(number(row, "seqScan"), number(row, "seqScan") + number(row, "idxScan")));
         if(old == null)
         {
            inserts.add(row);
         }
         else
         {
            row.setValue("id", old.getValueInteger("id"));
            updates.add(row);
         }
         if(old != null)
         {
            QRecord weekAgo = DbPerfStore.first("db_perf_table_snapshot", DbPerfStore.eq("tableStatId", old.getValueInteger("id"))
               .withCriteria(new QFilterCriteria("capturedAt", QCriteriaOperator.LESS_THAN_OR_EQUALS, now.minusSeconds(7 * 86400L)))
               .withOrderBy(new QFilterOrderBy("capturedAt", false)));
            row.setValue("growthBytes7d", weekAgo == null ? null : number(row, "totalBytes") - number(weekAgo, "totalBytes"));
         }
         QRecord detail = new QRecord().withValue("snapshotId", snapshot.getValueInteger("id")).withValue("capturedAt", now);
         boolean tableReset = reset || old != null && number(old, "relid") != entry.relid();
         for(String counter : List.of("seqScan", "seqTupRead", "idxScan", "idxTupFetch", "nTupIns", "nTupUpd", "nTupDel", "nTupHotUpd", "autovacuumCount", "autoanalyzeCount"))
         {
            detail.setValue(counter, DeltaCalculator.counter(old == null ? null : old.getValueLong(counter), number(row, counter), tableReset));
         }
         for(String gauge : List.of("nLiveTup", "nDeadTup", "totalBytes", "tableBytes", "indexBytes", "freezeAge"))
         {
            detail.setValue(gauge, number(row, gauge));
         }
         details.add(detail);
         detailKeys.put(detail, key);
      }
      for(QRecord inserted : DbPerfStore.insert("db_perf_table_stat", inserts))
      {
         known.put(inserted.getValueString("schemaName") + "." + inserted.getValueString("tableName"), inserted);
      }
      DbPerfStore.update("db_perf_table_stat", updates);
      for(QRecord detail : details)
      {
         detail.setValue("tableStatId", known.get(detailKeys.get(detail)).getValueInteger("id"));
      }
      DbPerfStore.insert("db_perf_table_snapshot", details);
      collectIndexes(backend, indexes, known, now);
   }


   /*******************************************************************************
    ** Indexes reference collected tables and are written in batches.
    *******************************************************************************/
   private void collectIndexes(String backend, List<IndexStats> entries, Map<String, QRecord> tables, Instant now) throws Exception
   {
      Map<String, QRecord> old = new HashMap<>();
      for(QRecord row : DbPerfStore.query("db_perf_index_stat", DbPerfStore.eq("backendName", backend)))
      {
         old.put(row.getValueString("schemaName") + "." + row.getValueString("indexName"), row);
      }
      List<QRecord> inserts = new ArrayList<>();
      List<QRecord> updates = new ArrayList<>();
      for(IndexStats entry : entries)
      {
         QRecord table = tables.get(entry.schemaName() + "." + entry.tableName());
         if(table == null)
         {
            continue;
         }
         QRecord row = new QRecord().withValue("backendName", backend).withValue("tableStatId", table.getValueInteger("id"))
            .withValue("schemaName", entry.schemaName()).withValue("tableName", entry.tableName()).withValue("indexName", entry.indexName()).withValue("relid", entry.relid())
            .withValue("definition", truncate(entry.definition(), 2000)).withValue("columns", truncate(entry.columns(), 1000)).withValue("isUnique", entry.unique()).withValue("isPrimary", entry.primary()).withValue("isValid", entry.valid())
            .withValue("idxScan", entry.idxScan()).withValue("idxTupRead", entry.idxTupRead()).withValue("idxTupFetch", entry.idxTupFetch()).withValue("sizeBytes", entry.sizeBytes()).withValue("writeCost", entry.writeCost())
            .withValue("scansSinceReset", entry.idxScan()).withValue("lastSeenAt", now);
         QRecord previous = old.get(entry.schemaName() + "." + entry.indexName());
         if(previous == null)
         {
            inserts.add(row);
         }
         else
         {
            row.setValue("id", previous.getValueInteger("id"));
            updates.add(row);
         }
      }
      DbPerfStore.insert("db_perf_index_stat", inserts);
      DbPerfStore.update("db_perf_index_stat", updates);
   }


   /*******************************************************************************
    ** All-database connection counts for capacity comparison.
    *******************************************************************************/
   private void collectConnections(List<ConnectionCount> connections, QRecord snapshot) throws Exception
   {
      List<QRecord> rows = new ArrayList<>();
      int total = 0;
      int active = 0;
      int idle = 0;
      int idleTx = 0;
      for(ConnectionCount entry : connections)
      {
         total += entry.connectionCount();
         active += "active".equals(entry.state()) ? entry.connectionCount() : 0;
         idle += "idle".equals(entry.state()) ? entry.connectionCount() : 0;
         idleTx += entry.state() != null && entry.state().startsWith("idle in transaction") ? entry.connectionCount() : 0;
         rows.add(new QRecord().withValue("snapshotId", snapshot.getValueInteger("id")).withValue("capturedAt", snapshot.getValueInstant("capturedAt"))
            .withValue("dbUser", entry.dbUser()).withValue("applicationName", truncate(entry.applicationName(), 200)).withValue("state", entry.state()).withValue("connectionCount", entry.connectionCount()));
      }
      DbPerfStore.insert("db_perf_connection_sample", rows);
      snapshot.withValue("connectionsTotal", total).withValue("connectionsActive", active).withValue("connectionsIdle", idle).withValue("connectionsIdleInTx", idleTx);
   }


   /*******************************************************************************
    ** Persist only notable sessions; normalize before truncating or hashing.
    *******************************************************************************/
   private void collectSessions(List<SessionInfo> sessions, QRecord snapshot, QRecord settings, String backend) throws Exception
   {
      Map<Integer, Integer> blocking = new HashMap<>();
      for(SessionInfo session : sessions)
      {
         session.blockedBy().forEach(pid -> blocking.merge(pid, 1, Integer::sum));
      }
      Map<String, QRecord> statements = new HashMap<>();
      for(QRecord row : DbPerfStore.query("db_perf_statement", DbPerfStore.eq("backendName", backend)))
      {
         if(Boolean.TRUE.equals(row.getValueBoolean("toplevel")))
         {
            statements.put(row.getValueString("queryId") + "/" + row.getValueString("dbUser"), row);
         }
      }
      List<QRecord> rows = new ArrayList<>();
      for(SessionInfo entry : sessions)
      {
         String reason = !entry.blockedBy().isEmpty() ? "BLOCKED" : blocking.containsKey(entry.pid()) ? "BLOCKING"
            : entry.state() != null && entry.state().startsWith("idle in transaction") && entry.stateChange() != null && Duration.between(entry.stateChange(), snapshot.getValueInstant("capturedAt")).getSeconds() >= positive(settings, "idleInTransactionSeconds", 60) ? "IDLE_IN_TRANSACTION"
            : "active".equals(entry.state()) && entry.querySeconds() != null && entry.querySeconds().doubleValue() >= positive(settings, "activityLongRunningSeconds", 30) ? "LONG_RUNNING" : null;
         if(reason == null)
         {
            continue;
         }
         QRecord statement = statements.get(entry.queryId() + "/" + entry.dbUser());
         String normalized = SqlNormalizer.normalize(statement == null ? entry.queryText() : statement.getValueString("queryText"));
         rows.add(new QRecord().withValue("snapshotId", snapshot.getValueInteger("id")).withValue("capturedAt", snapshot.getValueInstant("capturedAt"))
            .withValue("pid", entry.pid()).withValue("dbUser", entry.dbUser()).withValue("applicationName", truncate(entry.applicationName(), 200)).withValue("clientAddr", entry.clientAddr())
            .withValue("state", entry.state()).withValue("waitEventType", entry.waitEventType()).withValue("waitEvent", entry.waitEvent()).withValue("backendType", entry.backendType())
            .withValue("xactStart", entry.xactStart()).withValue("queryStart", entry.queryStart()).withValue("stateChange", entry.stateChange()).withValue("querySeconds", entry.querySeconds()).withValue("xactSeconds", entry.xactSeconds())
            .withValue("blockedBy", truncate(entry.blockedBy().stream().map(String::valueOf).collect(Collectors.joining(",")), 500)).withValue("blockingCount", blocking.getOrDefault(entry.pid(), 0))
            .withValue("reason", reason).withValue("queryId", entry.queryId()).withValue("statementId", statement == null ? null : statement.getValueInteger("id"))
            .withValue("queryFingerprint", SqlNormalizer.fingerprint(normalized)).withValue("queryTextNormalized", truncate(normalized, 4000)));
      }
      DbPerfStore.insert("db_perf_activity_sample", rows);
   }


   /*******************************************************************************
    ** Stable statement identity including role and nesting level.
    *******************************************************************************/
   private static String statementKey(long queryId, String user, boolean top)
   {
      return (queryId + "/" + top + "/" + user);
   }


   /*******************************************************************************
    ** Null counters are zero.
    *******************************************************************************/
   public static long number(QRecord row, String field)
   {
      Long value = row.getValueLong(field);
      return (value == null ? 0 : value);
   }


   /*******************************************************************************
    ** Null decimal counters are zero.
    *******************************************************************************/
   public static BigDecimal decimal(QRecord row, String field)
   {
      BigDecimal value = row.getValueBigDecimal(field);
      return (value == null ? BigDecimal.ZERO : value);
   }


   /*******************************************************************************
    ** Percentage, not a fractional ratio; matches seeded rule thresholds.
    *******************************************************************************/
   public static BigDecimal ratio(long numerator, long denominator)
   {
      return (denominator == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP));
   }


   /*******************************************************************************
    ** Weighted mean with a zero-call guard.
    *******************************************************************************/
   public static BigDecimal divide(BigDecimal total, long count)
   {
      return (count == 0 ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(count), 3, RoundingMode.HALF_UP));
   }


   /*******************************************************************************
    ** Invalid persisted intervals fall back to safe positive defaults.
    *******************************************************************************/
   public static int positive(QRecord settings, String field, int fallback)
   {
      Integer value = settings.getValueInteger(field);
      return (value == null || value < 1 ? fallback : value);
   }


   /*******************************************************************************
    ** Bound display strings after normalization where applicable.
    *******************************************************************************/
   private static String truncate(String value, int length)
   {
      return (value == null ? null : value.substring(0, Math.min(length, value.length())));
   }
}
