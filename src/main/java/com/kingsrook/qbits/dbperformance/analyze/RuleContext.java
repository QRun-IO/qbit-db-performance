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

package com.kingsrook.qbits.dbperformance.analyze;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Backend-scoped, time-bounded reads of the qbit's own tables. Ratios in storage
 ** are percentages. Attribution belongs to the single configured backend.
 *******************************************************************************/
public class RuleContext
{
   public final DbPerfRuntime runtime;
   public final String backend;
   public final QRecord rule;
   public final QRecord settings;
   public final Instant now;
   private final Map<String, List<QRecord>> cache = new HashMap<>();


   /*******************************************************************************
    ** An explicit observation time makes windows repeatable in tests and runs.
    *******************************************************************************/
   public RuleContext(DbPerfRuntime runtime, String backend, QRecord rule, QRecord settings, Instant now)
   {
      if(!runtime.getConfig().getBackendName().equals(backend))
      {
         throw (new IllegalArgumentException("Backend is not configured for this runtime"));
      }
      this.runtime = runtime;
      this.backend = backend;
      this.rule = rule;
      this.settings = settings;
      this.now = now;
   }


   /*******************************************************************************
    ** Seeded thresholds remain editable by the administrator.
    *******************************************************************************/
   public double threshold(int ordinal)
   {
      return (number(rule, "threshold" + ordinal));
   }


   /*******************************************************************************
    ** Page by primary key rather than silently accepting backend row limits.
    *******************************************************************************/
   public static List<QRecord> read(String table, QQueryFilter filter) throws Exception
   {
      List<QRecord> rows = new ArrayList<>();
      int last = 0;
      while(true)
      {
         List<QRecord> page = DbPerfStore.query(table, new QQueryFilter().withSubFilter(filter)
            .withCriteria(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, last))
            .withOrderBy(new QFilterOrderBy("id", true)).withLimit(DbPerfStore.BATCH_SIZE));
         rows.addAll(page);
         if(page.size() < DbPerfStore.BATCH_SIZE)
         {
            return (rows);
         }
         last = page.getLast().getValueInteger("id");
      }
   }


   /*******************************************************************************
    ** Current identities, filtered by backend. Cached only for this evaluation.
    *******************************************************************************/
   public List<QRecord> identities(String suffix) throws Exception
   {
      String key = "identity:" + suffix;
      if(!cache.containsKey(key))
      {
         cache.put(key, read("db_perf_" + suffix, DbPerfStore.eq("backendName", backend)));
      }
      return (cache.get(key));
   }


   /*******************************************************************************
    ** Read observations inside (now-days, now], excluding other backends through
    ** their snapshot foreign key. Partial snapshots still contain useful data.
    *******************************************************************************/
   public List<QRecord> window(String suffix, int days) throws Exception
   {
      String key = suffix + ":" + days;
      if(!cache.containsKey(key))
      {
         String time = Set.of("attribution", "repeat_pattern").contains(suffix) ? "windowEnd" : "capturedAt";
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria(time, QCriteriaOperator.GREATER_THAN, now.minusSeconds(days * 86400L)),
            new QFilterCriteria(time, QCriteriaOperator.LESS_THAN_OR_EQUALS, now));
         if(suffix.equals("snapshot"))
         {
            filter.withCriteria(new QFilterCriteria("backendName", QCriteriaOperator.EQUALS, backend));
         }
         List<QRecord> rows = read("db_perf_" + suffix, filter);
         if(!Set.of("snapshot", "attribution", "repeat_pattern").contains(suffix))
         {
            Set<Integer> ids = window("snapshot", days).stream()
               .filter(r -> List.of("OK", "PARTIAL").contains(r.getValueString("status")))
               .map(r -> r.getValueInteger("id")).collect(Collectors.toSet());
            rows = rows.stream().filter(r -> ids.contains(r.getValueInteger("snapshotId"))).toList();
         }
         cache.put(key, rows);
      }
      return (cache.get(key));
   }


   /*******************************************************************************
    ** Latest inventory only: retired tables and dropped indexes cannot recur.
    *******************************************************************************/
   public List<QRecord> inventory(String suffix) throws Exception
   {
      Instant latest = settings.getValueInstant("lastTableSnapshotAt");
      return (identities(suffix).stream().filter(r -> latest == null || latest.equals(r.getValueInstant("lastSeenAt"))).toList());
   }


   /*******************************************************************************
    ** Scale observed table deltas to a day when the observation window is short.
    ** Table sampling is independent of the faster database snapshot interval.
    *******************************************************************************/
   public double perDay(QRecord table, String field) throws Exception
   {
      List<QRecord> rows = window("table_snapshot", 1).stream()
         .filter(r -> table.getValueInteger("id").equals(r.getValueInteger("tableStatId"))).toList();
      if(rows.isEmpty())
      {
         return (0);
      }
      Instant first = rows.stream().map(r -> r.getValueInstant("capturedAt")).min(Instant::compareTo).orElseThrow();
      Instant last = rows.stream().map(r -> r.getValueInstant("capturedAt")).max(Instant::compareTo).orElseThrow();
      double seconds = Math.min(86400, Duration.between(first, last).getSeconds() + Math.max(60, number(settings, "tableSnapshotMinutes") * 60));
      return (rows.stream().mapToDouble(r -> number(r, field)).sum() * 86400 / seconds);
   }


   /*******************************************************************************
    ** Minimum age of the current database statistics, including retained history.
    *******************************************************************************/
   public double statsAgeDays() throws Exception
   {
      QRecord latest = DbPerfStore.first("db_perf_snapshot", DbPerfStore.eq("backendName", backend)
         .withCriteria(new QFilterCriteria("rawStatsReset", QCriteriaOperator.IS_NOT_BLANK))
         .withOrderBy(new QFilterOrderBy("capturedAt", false)));
      Instant start = latest == null ? null : latest.getValueInstant("rawStatsReset");
      if(start == null)
      {
         QRecord first = DbPerfStore.first("db_perf_snapshot", DbPerfStore.eq("backendName", backend)
            .withOrderBy(new QFilterOrderBy("capturedAt", true)));
         start = first == null ? now : first.getValueInstant("capturedAt");
      }
      return (Math.max(0, Duration.between(start, now).toSeconds() / 86400.0));
   }


   /*******************************************************************************
    ** Null counters mean no observed activity, never an invented measurement.
    *******************************************************************************/
   public static double number(QRecord row, String field)
   {
      return (row == null || row.getValue(field) == null ? 0 : row.getValueBigDecimal(field).doubleValue());
   }
}
