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

package com.kingsrook.qbits.dbperformance.alerts;


import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qbits.dbperformance.DbPerfQBitConfig;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.analyze.RuleContext;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.normalize.SqlNormalizer;
import com.kingsrook.qqq.backend.core.alerts.AlertRecordRef;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData;
import com.kingsrook.qqq.backend.core.alerts.QAlertTypeMetaData;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;


/*******************************************************************************
 ** Optional QAlerts signals from committed observations. Alert delivery cannot
 ** turn a successfully collected snapshot into a failed collection.
 *******************************************************************************/
public class SnapshotEvaluator
{
   public static final String COLLECTION_FAILED = "dbperf.collection-failed";
   public static final String QUERY_SPIKE = "dbperf.query-spike";
   public static final String CONNECTION_SATURATION = "dbperf.connection-saturation";
   public static final String BLOCKED_CHAIN = "dbperf.blocked-chain";
   private static final QLogger LOG = QLogger.getLogger(SnapshotEvaluator.class);
   private final DbPerfRuntime runtime;


   /*******************************************************************************
    ** Instance-bound evaluator; the core resolves the optional alert sink.
    *******************************************************************************/
   public SnapshotEvaluator(DbPerfRuntime runtime)
   {
      this.runtime = runtime;
   }


   /*******************************************************************************
    ** Alert types are discoverable even without an installed Alerts QBit.
    *******************************************************************************/
   public static void register(QInstance instance, DbPerfQBitConfig config)
   {
      Map<String, String> labels = Map.of(COLLECTION_FAILED, "Database performance collection failed", QUERY_SPIKE, "Database query time spike",
         CONNECTION_SATURATION, "Database connections saturated", BLOCKED_CHAIN, "Database blocking chain");
      Map<String, String> descriptions = Map.of(COLLECTION_FAILED, "Collection failed; resolves after the next OK snapshot.",
         QUERY_SPIKE, "Interval query time exceeds its seven-day median and configured floor.",
         CONNECTION_SATURATION, "Observed connections reached the configured percentage of capacity.",
         BLOCKED_CHAIN, "A blocked session exceeded the configured duration.");
      for(String type : List.of(COLLECTION_FAILED, QUERY_SPIKE, CONNECTION_SATURATION, BLOCKED_CHAIN))
      {
         AlertsInstanceMetaData.ofOrWithNew(instance).withAlertType(new QAlertTypeMetaData().withName(type).withLabel(labels.get(type))
            .withDescription(descriptions.get(type)).withSourceQBitName("db-performance")
            .withDefaultSeverity(type.equals(COLLECTION_FAILED) ? AlertSeverity.ERROR : AlertSeverity.WARNING)
            .withSubjectTableName(type.equals(QUERY_SPIKE) ? "db_perf_statement" : "db_perf_snapshot")
            .withVisibilityPermission(config.getPermissionBaseName() + ".view"));
      }
   }


   /*******************************************************************************
    ** A failed evaluation is logged without SQL, credentials or exception text.
    *******************************************************************************/
   public void evaluate(QRecord snapshot)
   {
      try
      {
         runtime.asSystem(() ->
         {
            evaluateStored(snapshot);
            return (null);
         });
      }
      catch(Exception e)
      {
         LOG.warn("Database performance alert evaluation could not read stored observations");
      }
   }


   /*******************************************************************************
    ** Read the current settings every pass. Failed/partial collection cannot
    ** clear connection, blocking or query signals without fresh measurements.
    *******************************************************************************/
   private void evaluateStored(QRecord snapshot) throws Exception
   {
      String backend = snapshot.getValueString("backendName");
      QRecord settings = DbPerfStore.first("db_perf_settings", DbPerfStore.eq("backendName", backend));
      if(settings == null || !Boolean.TRUE.equals(settings.getValueBoolean("alertsEnabled")))
      {
         return;
      }
      String status = snapshot.getValueString("status");
      if("FAILED".equals(status))
      {
         signal(COLLECTION_FAILED, backend, null, true, "Database performance collection failed", snapshot.getValueString("errorMessage"), snapshot, "db_perf_snapshot");
         return;
      }
      if("OK".equals(status))
      {
         signal(COLLECTION_FAILED, backend, null, false, "Collection recovered", "An OK snapshot was collected", snapshot, "db_perf_snapshot");
      }
      else if(!"PARTIAL".equals(status) || snapshot.getValue("connectionsTotal") == null)
      {
         return;
      }
      double maximum = RuleContext.number(snapshot, "maxConnections");
      if(maximum > 0)
      {
         double used = 100 * RuleContext.number(snapshot, "connectionsTotal") / maximum;
         signal(CONNECTION_SATURATION, backend, null, used >= RuleContext.number(settings, "connectionAlertPercent"),
            "Database connection capacity", used + "% of " + maximum + " connections", snapshot, "db_perf_snapshot");
      }
      Instant now = snapshot.getValueInstant("capturedAt");
      RuleContext ctx = new RuleContext(runtime, backend, new QRecord(), settings, now);
      List<QRecord> activity = DbPerfStore.query("db_perf_activity_sample", DbPerfStore.eq("snapshotId", snapshot.getValueInteger("id")));
      long blocked = activity.stream().filter(r -> "BLOCKED".equals(r.getValueString("reason")))
         .filter(r -> Math.max(RuleContext.number(r, "querySeconds"), RuleContext.number(r, "xactSeconds")) >= RuleContext.number(settings, "blockedAlertSeconds")).count();
      signal(BLOCKED_CHAIN, backend, null, blocked > 0, "Database blocking chain", blocked + " blocked sessions exceed the duration threshold", snapshot, "db_perf_snapshot");
      if("OK".equals(status) || "NO_READ_ALL_STATS".equals(settings.getValueString("statsAccess")))
      {
         spikes(ctx, snapshot);
      }
   }


   /*******************************************************************************
    ** A baseline excludes the current observation and needs twelve prior samples.
    ** Missing top-N entries clear after one hour, including after a restart.
    *******************************************************************************/
   private void spikes(RuleContext ctx, QRecord snapshot) throws Exception
   {
      Map<Integer, StatementHistory> history = history(ctx, snapshot.getValueInteger("id"));
      for(QRecord statement : ctx.identities("statement"))
      {
         StatementHistory samples = history.getOrDefault(statement.getValueInteger("id"), new StatementHistory());
         if(samples.current == null)
         {
            Instant last = samples.last == null ? Instant.EPOCH : samples.last;
            if(!last.plusSeconds(3600).isAfter(ctx.now))
            {
               signal(QUERY_SPIKE, ctx.backend, statement.getValueString("id"), false, "Query absent", "Statement absent from snapshots for at least one hour", statement, "db_perf_statement");
            }
            continue;
         }
         double[] previous = samples.sortedPrevious();
         if(previous.length < 12)
         {
            if(samples.current < RuleContext.number(ctx.settings, "spikeMinTotalMs"))
            {
               signal(QUERY_SPIKE, ctx.backend, statement.getValueString("id"), false, "Query recovered", "Interval time is below the configured floor", statement, "db_perf_statement");
            }
            continue;
         }
         int middle = previous.length / 2;
         double median = previous.length % 2 == 0 ? (previous[middle - 1] + previous[middle]) / 2 : previous[middle];
         double time = samples.current;
         boolean spike = time >= RuleContext.number(ctx.settings, "spikeMinTotalMs") && time >= median * RuleContext.number(ctx.settings, "spikeFactor");
         String normalized = SqlNormalizer.normalize(statement.getValueString("queryText"));
         String text = normalized.substring(0, Math.min(200, normalized.length()));
         signal(QUERY_SPIKE, ctx.backend, statement.getValueString("id"), spike, "Database query time spike",
            "Statement " + statement.getValueInteger("id") + ": " + time + " ms; prior median " + median + " ms. " + text, statement, "db_perf_statement");
      }
   }


   /*******************************************************************************
    ** Seven days of per-statement interval times, read in keyset pages and kept
    ** as primitive doubles. kof22 writes ~33,000 query snapshots a day, so the
    ** window is ~230,000 rows; holding them as QRecords on every snapshot
    ** evaluation cost hundreds of MB of worker heap.
    *******************************************************************************/
   static Map<Integer, StatementHistory> history(RuleContext ctx, Integer currentSnapshotId) throws Exception
   {
      Set<Integer> valid = ctx.window("snapshot", 7).stream().filter(r -> List.of("OK", "PARTIAL").contains(r.getValueString("status")))
         .map(r -> r.getValueInteger("id")).collect(Collectors.toSet());
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("capturedAt", QCriteriaOperator.GREATER_THAN, ctx.now.minusSeconds(7 * 86400L)),
         new QFilterCriteria("capturedAt", QCriteriaOperator.LESS_THAN_OR_EQUALS, ctx.now));
      Map<Integer, StatementHistory> history = new HashMap<>();
      DbPerfStore.forEachPage("db_perf_query_snapshot", filter, DbPerfStore.BATCH_SIZE, page ->
      {
         for(QRecord row : page)
         {
            Integer snapshotId = row.getValueInteger("snapshotId");
            if(!valid.contains(snapshotId))
            {
               continue;
            }
            StatementHistory statement = history.computeIfAbsent(row.getValueInteger("statementId"), ignored -> new StatementHistory());
            Instant at = row.getValueInstant("capturedAt");
            double totalMs = RuleContext.number(row, "totalMs");
            if(statement.last == null || at.isAfter(statement.last))
            {
               statement.last = at;
            }
            if(snapshotId.equals(currentSnapshotId) && statement.current == null)
            {
               statement.current = totalMs;
            }
            if(at.isBefore(ctx.now))
            {
               statement.add(totalMs);
            }
         }
      });
      return (history);
   }



   /*******************************************************************************
    ** One statement's samples: the current interval, the latest capture time
    ** and earlier interval times.
    *******************************************************************************/
   static final class StatementHistory
   {
      private Double   current;
      private Instant  last;
      private double[] previous = new double[16];
      private int      size;



      /***************************************************************************
       ** Append an earlier interval time.
       ***************************************************************************/
      void add(double value)
      {
         if(size == previous.length)
         {
            previous = Arrays.copyOf(previous, size * 2);
         }
         previous[size++] = value;
      }



      /***************************************************************************
       ** Earlier interval times, ascending.
       ***************************************************************************/
      double[] sortedPrevious()
      {
         double[] sorted = Arrays.copyOf(previous, size);
         Arrays.sort(sorted);
         return (sorted);
      }
   }



   /*******************************************************************************
    ** Stable backend/subject keys make raises and resolutions restart-safe.
    *******************************************************************************/
   private static void signal(String type, String backend, String subject, boolean raised, String title, String summary, QRecord row, String table)
   {
      String key = type.replace("dbperf.", "db_perf.").replace('-', '_') + ":" + backend + (subject == null ? "" : ":" + subject);
      if(raised)
      {
         QAlerts.raise(new AlertSignal(type).withDedupKey(key).withTitle(title).withSummary(summary)
            .withRecordRef(AlertRecordRef.of(table, row.getValueInteger("id"))));
      }
      else
      {
         QAlerts.resolve(type, key, "Condition no longer detected");
      }
   }
}
