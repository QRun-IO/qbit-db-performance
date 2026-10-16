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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.normalize.SqlNormalizer;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Daily aggregates replaced atomically; reruns include late arriving detail rows.
 *******************************************************************************/
public class RollupService
{
   private final DbPerfRuntime runtime;


   /*******************************************************************************
    ** Runtime dependency.
    *******************************************************************************/
   public RollupService(DbPerfRuntime runtime)
   {
      this.runtime = runtime;
   }


   /*******************************************************************************
    ** Replace all three rollups for a local calendar day in one transaction.
    *******************************************************************************/
   public void rollupDay(LocalDate day) throws Exception
   {
      synchronized(runtime)
      {
         runtime.transaction(() ->
         {
            ZoneId zone = ZoneId.of(runtime.getConfig().getTimeZone());
            Instant start = day.atStartOfDay(zone).toInstant();
            Instant end = day.plusDays(1).atStartOfDay(zone).toInstant();
            rollupQueries(day, start, end);
            rollupTables(day, start, end);
            rollupAttribution(day, start, end);
            return (null);
         });
      }
   }


   /*******************************************************************************
    ** A day needs backfill only if retained detail exists without its daily rows.
    ** Existing history must survive after a short detail-retention window expires.
    *******************************************************************************/
   public boolean needsBackfill(LocalDate day) throws Exception
   {
      return (runtime.asSystem(() ->
      {
         ZoneId zone = ZoneId.of(runtime.getConfig().getTimeZone());
         Instant start = day.atStartOfDay(zone).toInstant();
         Instant end = day.plusDays(1).atStartOfDay(zone).toInstant();
         for(String kind : List.of("query", "table", "attribution"))
         {
            String detail = "db_perf_" + kind + (kind.equals("attribution") ? "" : "_snapshot");
            String field = kind.equals("attribution") ? "windowStart" : "capturedAt";
            if(DbPerfStore.first("db_perf_" + kind + "_daily", DbPerfStore.eq("day", day)) == null
               && DbPerfStore.first(detail, range(field, start, end)) != null)
            {
               return (true);
            }
         }
         return (false);
      }));
   }


   /*******************************************************************************
    ** Weighted statement means/hit ratios, since-reset maximum, exact day share.
    *******************************************************************************/
   private void rollupQueries(LocalDate day, Instant start, Instant end) throws Exception
   {
      Map<Integer, QRecord> groups = new LinkedHashMap<>();
      Map<Integer, long[]> hits = new LinkedHashMap<>();
      BigDecimal denominator = BigDecimal.ZERO;
      for(QRecord snapshot : DbPerfStore.query("db_perf_snapshot", range("capturedAt", start, end)
         .withCriteria(new QFilterCriteria("backendName", QCriteriaOperator.EQUALS, runtime.getConfig().getBackendName()))
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.IN, List.of("OK", "PARTIAL")))))
      {
         denominator = denominator.add(SnapshotCollector.decimal(snapshot, "statementTotalMs"));
      }
      for(QRecord row : DbPerfStore.query("db_perf_query_snapshot", range("capturedAt", start, end)))
      {
         int id = row.getValueInteger("statementId");
         QRecord aggregate = groups.computeIfAbsent(id, key -> new QRecord().withValue("statementId", key).withValue("day", day));
         sum(aggregate, row, "calls", "rowsCount");
         sumDecimal(aggregate, row, "totalMs");
         aggregate.setValue("maxMs", SnapshotCollector.decimal(aggregate, "maxMs").max(SnapshotCollector.decimal(row, "maxMs")));
         aggregate.setValue("snapshots", (int) SnapshotCollector.number(aggregate, "snapshots") + 1);
         long[] counts = hits.computeIfAbsent(id, key -> new long[2]);
         counts[0] += SnapshotCollector.number(row, "sharedBlksHit");
         counts[1] += SnapshotCollector.number(row, "sharedBlksRead");
      }
      for(Map.Entry<Integer, QRecord> entry : groups.entrySet())
      {
         QRecord row = entry.getValue();
         long[] counts = hits.get(entry.getKey());
         row.withValue("meanMs", SnapshotCollector.divide(SnapshotCollector.decimal(row, "totalMs"), SnapshotCollector.number(row, "calls")))
            .withValue("hitRatio", SnapshotCollector.ratio(counts[0], counts[0] + counts[1]))
            .withValue("shareOfDbTime", denominator.signum() == 0 ? BigDecimal.ZERO : SnapshotCollector.decimal(row, "totalMs").multiply(BigDecimal.valueOf(100)).divide(denominator, 4, RoundingMode.HALF_UP));
      }
      replace("db_perf_query_daily", day, new ArrayList<>(groups.values()));
   }


   /*******************************************************************************
    ** Sum scan/write deltas, keeping the last size and tuple gauges of the day.
    *******************************************************************************/
   private void rollupTables(LocalDate day, Instant start, Instant end) throws Exception
   {
      Map<Integer, QRecord> groups = new LinkedHashMap<>();
      for(QRecord row : DbPerfStore.query("db_perf_table_snapshot", range("capturedAt", start, end).withOrderBy(new QFilterOrderBy("capturedAt", true)).withOrderBy(new QFilterOrderBy("id", true))))
      {
         QRecord aggregate = groups.computeIfAbsent(row.getValueInteger("tableStatId"), key -> new QRecord().withValue("tableStatId", key).withValue("day", day));
         sum(aggregate, row, "seqScan", "idxScan", "seqTupRead", "nTupIns", "nTupUpd", "nTupDel");
         for(String gauge : List.of("nLiveTup", "nDeadTup", "totalBytes"))
         {
            aggregate.setValue(gauge, row.getValue(gauge));
         }
      }
      replace("db_perf_table_daily", day, new ArrayList<>(groups.values()));
   }


   /*******************************************************************************
    ** Merge pods by all attribution dimensions, using an unambiguous length-
    ** prefixed digest for null-safe uniqueness and bounded index size.
    *******************************************************************************/
   private void rollupAttribution(LocalDate day, Instant start, Instant end) throws Exception
   {
      Map<String, QRecord> groups = new LinkedHashMap<>();
      List<String> dimensions = List.of("unitKind", "unitName", "processName", "tableName", "operation", "fingerprint");
      DbPerfStore.forEachPage("db_perf_attribution", range("windowStart", start, end), DbPerfStore.BATCH_SIZE, page -> mergeAttribution(day, page, dimensions, groups));
      replace("db_perf_attribution_daily", day, new ArrayList<>(groups.values()));
   }



   /*******************************************************************************
    ** Fold one page of attribution detail into the day's groups. Memory is
    ** bounded by distinct dimension keys, never by the day's detail row count.
    *******************************************************************************/
   private static void mergeAttribution(LocalDate day, List<QRecord> page, List<String> dimensions, Map<String, QRecord> groups)
   {
      for(QRecord row : page)
      {
         StringBuilder key = new StringBuilder();
         for(String field : dimensions)
         {
            String value = row.getValueString(field);
            key.append(value == null ? "-1:" : value.length() + ":" + value);
         }
         String hash = SqlNormalizer.fingerprint(key.toString());
         QRecord aggregate = groups.computeIfAbsent(hash, ignored -> new QRecord().withValue("day", day).withValue("rollupKey", hash));
         for(String field : dimensions)
         {
            aggregate.setValue(field, row.getValue(field));
         }
         aggregate.setValue("sqlText", row.getValueString("sqlText"));
         sum(aggregate, row, "units", "statements", "rowsCount", "errors");
         sumDecimal(aggregate, row, "totalMs");
         aggregate.setValue("maxMs", SnapshotCollector.decimal(aggregate, "maxMs").max(SnapshotCollector.decimal(row, "maxMs")));
      }
   }


   /*******************************************************************************
    ** Half-open day window works on 23/25-hour local days too.
    *******************************************************************************/
   private static QQueryFilter range(String field, Instant start, Instant end)
   {
      return (new QQueryFilter(new QFilterCriteria(field, QCriteriaOperator.GREATER_THAN_OR_EQUALS, start), new QFilterCriteria(field, QCriteriaOperator.LESS_THAN, end)));
   }


   /*******************************************************************************
    ** Replace inside the caller's transaction.
    *******************************************************************************/
   private static void replace(String table, LocalDate day, List<QRecord> rows) throws Exception
   {
      DbPerfStore.delete(table, DbPerfStore.eq("day", day));
      DbPerfStore.insert(table, rows);
   }


   /*******************************************************************************
    ** Sum integral counters.
    *******************************************************************************/
   private static void sum(QRecord target, QRecord source, String... fields)
   {
      for(String field : fields)
      {
         target.setValue(field, SnapshotCollector.number(target, field) + SnapshotCollector.number(source, field));
      }
   }


   /*******************************************************************************
    ** Sum elapsed time without floating-point precision loss.
    *******************************************************************************/
   private static void sumDecimal(QRecord target, QRecord source, String field)
   {
      target.setValue(field, SnapshotCollector.decimal(target, field).add(SnapshotCollector.decimal(source, field)));
   }
}
