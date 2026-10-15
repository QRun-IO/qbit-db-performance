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


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qbits.dbperformance.normalize.SqlNormalizer;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** The fourteen seeded rules. Every decision is based on stored collector rows;
 ** administrators control severities and thresholds through db_perf_rule.
 *******************************************************************************/
public enum StoredRule implements DbPerfRule
{
   SLOW_QUERY, MISSING_INDEX, UNUSED_INDEX, DUPLICATE_INDEX, N_PLUS_ONE, HOT_JOB,
   OVER_AUDITED, BLOAT, VACUUM_STALE, POOL_SIZING, IDLE_IN_TRANSACTION,
   CACHE_HIT_LOW, TEMP_SPILL, DEADLOCKS;


   /*******************************************************************************
    ** Seed identity.
    *******************************************************************************/
   @Override
   public String code()
   {
      return (name());
   }


   /*******************************************************************************
    ** Disabled evaluation is inert even when called outside AnalyzeService.
    *******************************************************************************/
   @Override
   public List<FindingDraft> evaluate(RuleContext ctx) throws Exception
   {
      if(!Boolean.TRUE.equals(ctx.rule.getValueBoolean("enabled")))
      {
         return (List.of());
      }
      return (switch(this)
      {
         case SLOW_QUERY -> slow(ctx);
         case MISSING_INDEX, OVER_AUDITED, BLOAT, VACUUM_STALE -> tables(ctx);
         case UNUSED_INDEX, DUPLICATE_INDEX -> indexes(ctx);
         case N_PLUS_ONE -> repeats(ctx);
         case HOT_JOB -> jobs(ctx);
         case POOL_SIZING -> pools(ctx);
         case IDLE_IN_TRANSACTION -> idle(ctx);
         case CACHE_HIT_LOW, DEADLOCKS, TEMP_SPILL -> database(ctx);
      });
   }


   /*******************************************************************************
    ** Mean and share use the collector's 24-hour aggregates, excluding stale rows.
    *******************************************************************************/
   private List<FindingDraft> slow(RuleContext ctx) throws Exception
   {
      List<FindingDraft> findings = new ArrayList<>();
      for(QRecord row : ctx.identities("statement"))
      {
         if(row.getValueInstant("lastSeenAt") == null || row.getValueInstant("lastSeenAt").isBefore(ctx.now.minusSeconds(86400)))
         {
            continue;
         }
         double share = n(row, "shareOfDbTime24h");
         if(n(row, "meanMs24h") >= ctx.threshold(1) || share >= ctx.threshold(2))
         {
            String sql = URLEncoder.encode("EXPLAIN " + row.getValueString("queryText"), StandardCharsets.UTF_8).replace("+", "%20");
            String recommendation = "Open the [Query plan action](/record/db_perf_statement/" + row.getValueInteger("id")
               + ") to inspect this query, or [Explain in SQL Workbench](" + ctx.runtime.getConfig().getSqlWorkbenchRunnerPath() + "?sql=" + sql + "). Review scan and join costs before changing indexes.";
            findings.add(draft(ctx, row.getValueString("id"), share / 100, "High query execution cost over 24 hours",
               evidence("Calls", n(row, "calls24h"), "Mean ms", n(row, "meanMs24h"), "Total ms", n(row, "totalMs24h"), "DB time %", share), recommendation, null, row, "statement", null));
         }
      }
      return (findings);
   }


   /*******************************************************************************
    ** Inventory-based rules share the same current table list and daily deltas.
    *******************************************************************************/
   private List<FindingDraft> tables(RuleContext ctx) throws Exception
   {
      List<FindingDraft> findings = new ArrayList<>();
      for(QRecord table : ctx.inventory("table_stat"))
      {
         String name = table.getValueString("tableName");
         String subject = table.getValueString("schemaName") + "." + name;
         if(this == MISSING_INDEX)
         {
            double reads = ctx.perDay(table, "seqTupRead");
            if(n(table, "seqScanShare") < ctx.threshold(1) || reads < ctx.threshold(2))
            {
               continue;
            }
            Set<String> columns = new LinkedHashSet<>();
            for(QRecord statement : ctx.identities("statement").stream()
               .filter(s -> name.equals(s.getValueString("mainTable")) || subject.equals(s.getValueString("mainTable")))
               .filter(s -> n(s, "totalMs24h") > 0 && s.getValueInstant("lastSeenAt") != null && !s.getValueInstant("lastSeenAt").isBefore(ctx.now.minusSeconds(86400)))
               .sorted(Comparator.comparingDouble((QRecord s) -> n(s, "totalMs24h")).reversed()).limit(20).toList())
            {
               columns.addAll(PredicateParser.candidates(statement.getValueString("queryText"), table.getValueString("schemaName"), name));
            }
            for(QRecord index : ctx.inventory("index_stat"))
            {
               List<String> indexed = PredicateParser.indexColumns(index.getValueString("columns"));
               if(sameTable(table, index) && Boolean.TRUE.equals(index.getValueBoolean("isValid")) && !indexed.isEmpty() && plainIndex(index))
               {
                  columns.remove(indexed.getFirst());
               }
            }
            if(!columns.isEmpty())
            {
               String reason = reads + " sequential tuples/day; " + n(table, "seqScanShare") + "% sequential scans";
               findings.add(draft(ctx, subject, n(table, "seqScanShare") / 100, reason,
                  evidence("Sequential scan %", n(table, "seqScanShare"), "Tuples/day", reads, "Candidate columns", String.join(", ", columns)),
                  "Check Query plan and selectivity for " + String.join(", ", columns) + ". Review the suggested column order and migration before committing it.",
                  MigrationSnippets.create(ctx.now, table, List.copyOf(columns), reason), table, "table", null));
            }
         }
         else if(this == BLOAT && n(table, "deadRatio") >= ctx.threshold(1) && n(table, "nDeadTup") >= ctx.threshold(2))
         {
            String reason = n(table, "nDeadTup") + " dead tuples (" + n(table, "deadRatio") + "%)";
            findings.add(draft(ctx, subject, n(table, "deadRatio") / 100, reason,
               evidence("Dead tuples", n(table, "nDeadTup"), "Live tuples", n(table, "nLiveTup"), "Dead %", n(table, "deadRatio")),
               "Review long transactions and autovacuum capacity. Consider a lower per-table vacuum scale factor; the optional rollback resets it to the server default.",
               MigrationSnippets.vacuum(ctx.now, table, reason), table, "table", null));
         }
         else if(this == VACUUM_STALE)
         {
            Instant vacuum = table.getValueInstant("lastVacuum");
            Instant auto = table.getValueInstant("lastAutovacuum");
            if(vacuum == null || auto != null && auto.isAfter(vacuum))
            {
               vacuum = auto;
            }
            double age = vacuum == null ? ctx.statsAgeDays() : Duration.between(vacuum, ctx.now).toSeconds() / 86400.0;
            double writes = n(table, "nTupIns") + n(table, "nTupUpd") + n(table, "nTupDel");
            if(writes > 0 && age > ctx.threshold(1) || n(table, "freezeAge") >= ctx.threshold(2))
            {
               findings.add(draft(ctx, subject, Math.min(1, n(table, "freezeAge") / Math.max(1, ctx.threshold(2))), "Vacuum maintenance needs attention",
                  evidence("Days since vacuum (or observation age)", age, "Writes", writes, "Freeze age", n(table, "freezeAge")),
                  "Review autovacuum settings and long transactions preventing vacuum. Schedule maintenance and investigate transaction freeze age.", null, table, "table", null));
            }
         }
         else if(this == OVER_AUDITED && name.toLowerCase(Locale.ROOT).matches("(?:audit.*|.*_audit.*)"))
         {
            double inserts = ctx.perDay(table, "nTupIns");
            if(inserts >= ctx.threshold(1))
            {
               Map<String, Double> units = new LinkedHashMap<>();
               for(QRecord row : ctx.window("attribution", 1))
               {
                  if((name.equals(row.getValueString("tableName")) || subject.equals(row.getValueString("tableName"))) && "INSERT".equalsIgnoreCase(row.getValueString("operation")))
                  {
                     units.merge(String.valueOf(row.getValueString("unitName")), n(row, "statements"), Double::sum);
                  }
               }
               String contributors = units.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(5)
                  .map(e -> e.getKey() + " (" + e.getValue() + " inserts)").collect(Collectors.joining(", "));
               Map<String, Double> sources = new LinkedHashMap<>();
               for(QRecord row : ctx.window("attribution", 1))
               {
                  String source = row.getValueString("tableName");
                  if(source != null && !source.toLowerCase(Locale.ROOT).matches("(?:audit.*|.*_audit.*)")
                     && units.containsKey(row.getValueString("unitName")) && Set.of("INSERT", "UPDATE", "DELETE").contains(String.valueOf(row.getValueString("operation")).toUpperCase(Locale.ROOT)))
                  {
                     sources.merge(source, n(row, "statements"), Double::sum);
                  }
               }
               String sourceTables = sources.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(5)
                  .map(e -> e.getKey() + " (" + e.getValue() + " writes)").collect(Collectors.joining(", "));
               findings.add(draft(ctx, subject, Math.min(1, inserts / Math.max(1, ctx.threshold(1) * 10)), "High audit insert volume",
                  evidence("Audit inserts/day", inserts, "Attributed units", contributors.isEmpty() ? "Unavailable" : contributors),
                  "Lower AuditLevel on noisy source tables after reviewing audit requirements. " + (contributors.isEmpty() ? "Source-table attribution is unavailable." : "Largest audit-producing units: " + contributors + ".")
                     + (sourceTables.isEmpty() ? "" : " Source tables with the most writes in those units: " + sourceTables + ". These are correlated writes, not measured per-source audit row counts."),
                  null, table, "table", null));
            }
         }
      }
      return (findings);
   }


   /*******************************************************************************
    ** Never propose dropping unique, primary, invalid or structurally incomparable
    ** indexes. Equivalent peers choose one survivor deterministically.
    *******************************************************************************/
   private List<FindingDraft> indexes(RuleContext ctx) throws Exception
   {
      List<QRecord> indexes = ctx.inventory("index_stat");
      double bytes = indexes.stream().mapToDouble(r -> n(r, "sizeBytes")).sum();
      List<FindingDraft> findings = new ArrayList<>();
      for(QRecord index : indexes)
      {
         if(Boolean.TRUE.equals(index.getValueBoolean("isUnique")) || Boolean.TRUE.equals(index.getValueBoolean("isPrimary")) || !Boolean.TRUE.equals(index.getValueBoolean("isValid")))
         {
            continue;
         }
         String reason = null;
         if(this == UNUSED_INDEX && n(index, "idxScan") == 0 && ctx.statsAgeDays() >= ctx.threshold(1) && n(index, "sizeBytes") / 1048576 >= ctx.threshold(2))
         {
            reason = "Zero scans over " + ctx.statsAgeDays() + " days; " + n(index, "sizeBytes") / 1048576 + " MB";
         }
         if(this == DUPLICATE_INDEX && plainIndex(index))
         {
            List<String> columns = PredicateParser.indexColumns(index.getValueString("columns"));
            for(QRecord other : indexes)
            {
               List<String> covering = PredicateParser.indexColumns(other.getValueString("columns"));
               if(!index.getValueInteger("id").equals(other.getValueInteger("id")) && Objects.equals(index.getValueInteger("tableStatId"), other.getValueInteger("tableStatId"))
                  && Boolean.TRUE.equals(other.getValueBoolean("isValid")) && plainIndex(other) && !columns.isEmpty() && covering.size() >= columns.size()
                  && covering.subList(0, columns.size()).equals(columns))
               {
                  boolean exact = columns.equals(covering);
                  boolean unique = Boolean.TRUE.equals(other.getValueBoolean("isUnique")) || Boolean.TRUE.equals(other.getValueBoolean("isPrimary"));
                  if(exact && (unique || other.getValueInteger("id") < index.getValueInteger("id")) || !exact && !unique)
                  {
                     reason = "Redundant with " + other.getValueString("indexName") + ": " + other.getValueString("definition");
                     break;
                  }
               }
            }
         }
         if(reason != null)
         {
            findings.add(draft(ctx, index.getValueString("schemaName") + "." + index.getValueString("indexName"), n(index, "sizeBytes") / Math.max(1, bytes), reason,
               evidence("Index bytes", n(index, "sizeBytes"), "Scans", n(index, "idxScan"), "Write cost", n(index, "writeCost"), "Definition", index.getValueString("definition")),
               "Review rare workloads and dependencies before dropping this index. Commit the migration only after verifying the replacement and rollback.",
               MigrationSnippets.drop(ctx.now, index, code(), reason), index, "index", null));
         }
      }
      return (findings);
   }


   /*******************************************************************************
    ** Column prefixes alone cannot prove equivalence for partial/expression or
    ** non-btree indexes. Such structures need human review, not a drop snippet.
    *******************************************************************************/
   private static boolean plainIndex(QRecord index)
   {
      String definition = index.getValueString("definition");
      if(definition == null)
      {
         return (false);
      }
      String lower = definition.toLowerCase(Locale.ROOT);
      return (!PredicateParser.indexColumns(index.getValueString("columns")).isEmpty() && !lower.contains(" where ") && !lower.contains(" include ")
         && (!lower.contains(" using ") || lower.contains(" using btree ")));
   }


   /*******************************************************************************
    ** Aggregate pods/windows before applying the minimum affected-unit threshold.
    *******************************************************************************/
   private List<FindingDraft> repeats(RuleContext ctx) throws Exception
   {
      Map<String, List<QRecord>> groups = group(ctx.window("repeat_pattern", 1), "unitKind", "unitName", "fingerprint");
      List<FindingDraft> findings = new ArrayList<>();
      for(var entry : groups.entrySet())
      {
         List<QRecord> rows = entry.getValue();
         double max = rows.stream().mapToDouble(r -> n(r, "maxRepeats")).max().orElse(0);
         double units = rows.stream().mapToDouble(r -> n(r, "unitsAffected")).sum();
         if(max >= ctx.threshold(1) && units >= ctx.threshold(2))
         {
            QRecord row = rows.getFirst();
            findings.add(draft(ctx, entry.getKey(), Math.min(1, max / Math.max(1, ctx.threshold(1) * 10)), "Repeated queries in " + row.getValueString("unitKind") + " " + row.getValueString("unitName"),
               evidence("Max repeats", max, "Units affected", units, "Total repeats", rows.stream().mapToDouble(r -> n(r, "totalRepeats")).sum()),
               "Batch lookups on " + row.getValueString("tableName") + " in " + row.getValueString("unitName") + " into one query with IN (...), a QQQ QueryJoin, or a record pre-fetch.", null, row, "unit", row.getValueString("unitName")));
         }
      }
      return (findings);
   }


   /*******************************************************************************
    ** Share is relative to all attributed work, including HTTP routes.
    *******************************************************************************/
   private List<FindingDraft> jobs(RuleContext ctx) throws Exception
   {
      List<QRecord> rows = ctx.window("attribution", 1);
      double total = rows.stream().mapToDouble(r -> n(r, "totalMs")).sum();
      List<FindingDraft> findings = new ArrayList<>();
      for(var entry : group(rows, "unitKind", "unitName").entrySet())
      {
         QRecord row = entry.getValue().getFirst();
         double time = entry.getValue().stream().mapToDouble(r -> n(r, "totalMs")).sum();
         double share = total == 0 ? 0 : time / total;
         if(List.of("SCHEDULED_JOB", "PROCESS").contains(row.getValueString("unitKind")) && total > 0 && share * 100 >= ctx.threshold(1))
         {
            findings.add(draft(ctx, entry.getKey(), share, "Expensive " + row.getValueString("unitKind") + " " + row.getValueString("unitName"),
               evidence("Total ms", time, "Attributed time %", share * 100), "Review the schedule and batch database operations in " + row.getValueString("unitName") + ".", null, row, "unit", row.getValueString("unitName")));
         }
      }
      return (findings);
   }


   /*******************************************************************************
    ** Per-application peaks sum users/states within each snapshot first.
    *******************************************************************************/
   private List<FindingDraft> pools(RuleContext ctx) throws Exception
   {
      double peak = 0;
      double idle = 0;
      double maxConnections = 0;
      for(QRecord row : ctx.window("snapshot", 1))
      {
         if(n(row, "maxConnections") > 0)
         {
            peak = Math.max(peak, n(row, "connectionsTotal") / n(row, "maxConnections") * 100);
            maxConnections = Math.max(maxConnections, n(row, "maxConnections"));
         }
         if(n(row, "connectionsTotal") > 0)
         {
            idle = Math.max(idle, n(row, "connectionsIdle") / n(row, "connectionsTotal") * 100);
         }
      }
      if(maxConnections == 0 || peak < ctx.threshold(1) && idle < ctx.threshold(2))
      {
         return (List.of());
      }
      Map<String, Double> peaks = new LinkedHashMap<>();
      Map<String, Double> activePeaks = new LinkedHashMap<>();
      for(List<QRecord> rows : group(ctx.window("connection_sample", 1), "snapshotId", "applicationName").values())
      {
         String app = String.valueOf(rows.getFirst().getValueString("applicationName"));
         peaks.merge(app, rows.stream().mapToDouble(r -> n(r, "connectionCount")).sum(), Math::max);
         activePeaks.merge(app, rows.stream().filter(r -> "active".equals(r.getValueString("state"))).mapToDouble(r -> n(r, "connectionCount")).sum(), Math::max);
      }
      double desired = activePeaks.values().stream().mapToDouble(v -> Math.max(1, Math.ceil(v * 1.2))).sum();
      double scale = Math.min(1, maxConnections * 0.8 / Math.max(1, desired));
      String sizes = activePeaks.entrySet().stream().map(e -> e.getKey() + ": combined pool cap " + Math.max(1, (int) Math.floor(Math.max(1, Math.ceil(e.getValue() * 1.2)) * scale))
         + " across replicas (peak active " + e.getValue().intValue() + ")").collect(Collectors.joining("; "));
      String applications = peaks.entrySet().stream().map(e -> e.getKey() + ": peak " + e.getValue().intValue()).collect(Collectors.joining("; "));
      return (List.of(draft(ctx, "database", Math.max(peak, idle) / 100, "Review connection pool sizes",
         evidence("Peak used %", peak, "Peak idle %", idle, "Max connections", maxConnections, "Application peaks", applications),
         "Starting pool sizes from observed active demand: " + sizes + ". Validate under representative load and divide combined caps across replicas; reserve capacity for administration and other applications. Observed application peaks: " + applications,
         null, null, "database", null)));
   }


   /*******************************************************************************
    ** One finding per application, retaining the worst observed transaction age.
    *******************************************************************************/
   private List<FindingDraft> idle(RuleContext ctx) throws Exception
   {
      List<QRecord> rows = ctx.window("activity_sample", 1).stream().filter(r -> "IDLE_IN_TRANSACTION".equals(r.getValueString("reason"))).toList();
      List<FindingDraft> findings = new ArrayList<>();
      for(var entry : group(rows, "applicationName").entrySet())
      {
         double seconds = entry.getValue().stream().mapToDouble(r -> n(r, "xactSeconds")).max().orElse(0);
         if(seconds >= ctx.threshold(1))
         {
            String application = String.valueOf(entry.getValue().getFirst().getValueString("applicationName"));
            findings.add(draft(ctx, entry.getKey(), Math.min(1, seconds / Math.max(1, ctx.threshold(1) * 10)), "Idle transaction in " + application,
               evidence("Maximum transaction seconds", seconds, "Samples", entry.getValue().size()),
               "Fix transaction scope in " + application + "; consider idle_in_transaction_session_timeout.", null, null, "database", application));
         }
      }
      return (findings);
   }


   /*******************************************************************************
    ** Database counters are already deltas; temporary blocks are 8 KiB each.
    *******************************************************************************/
   private List<FindingDraft> database(RuleContext ctx) throws Exception
   {
      List<QRecord> snapshots = ctx.window("snapshot", 1).stream().filter(r -> "OK".equals(r.getValueString("status")) || this != CACHE_HIT_LOW && "PARTIAL".equals(r.getValueString("status"))).toList();
      List<FindingDraft> findings = new ArrayList<>();
      if(this == CACHE_HIT_LOW)
      {
         var average = snapshots.stream().filter(r -> r.getValue("cacheHitRatio") != null).mapToDouble(r -> n(r, "cacheHitRatio")).average();
         if(average.isPresent() && average.getAsDouble() < ctx.threshold(1))
         {
            findings.add(draft(ctx, "database", 1 - average.getAsDouble() / 100, "Low average database cache hit ratio", evidence("Average hit %", average.getAsDouble(), "OK snapshots", snapshots.size()),
               "Review query scans and the working set before increasing database memory.", null, null, "database", null));
         }
      }
      else if(this == DEADLOCKS)
      {
         double count = snapshots.stream().mapToDouble(r -> n(r, "deadlocks")).sum();
         if(!snapshots.isEmpty() && count >= ctx.threshold(1))
         {
            findings.add(draft(ctx, "database", Math.min(1, count / Math.max(1, ctx.threshold(1) * 10)), "Database deadlocks detected", evidence("Deadlocks in 24h", count),
               "Use consistent lock ordering and shorter transactions. Review affected application operations.", null, null, "database", null));
         }
      }
      else
      {
         double databaseMb = snapshots.stream().mapToDouble(r -> n(r, "tempBytes")).sum() / 1048576;
         Map<Integer, QRecord> statements = ctx.identities("statement").stream().collect(Collectors.toMap(r -> r.getValueInteger("id"), r -> r));
         for(var entry : group(ctx.window("query_snapshot", 1), "statementId").entrySet())
         {
            double mb = entry.getValue().stream().mapToDouble(r -> n(r, "tempBlksWritten")).sum() / 128;
            if(mb >= ctx.threshold(1))
            {
               QRecord statement = statements.get(entry.getValue().getFirst().getValueInteger("statementId"));
               findings.add(draft(ctx, entry.getKey(), mb / Math.max(1, databaseMb), "Query spilled temporary data to disk",
                  evidence("Query temp MB", mb, "Database temp MB", databaseMb), "Inspect sorts and hash joins in Query plan. Tune the query and review work_mem per operation, accounting for concurrent queries.", null, statement, "statement", null));
            }
         }
         if(databaseMb >= ctx.threshold(1))
         {
            findings.add(draft(ctx, "database", Math.min(1, databaseMb / Math.max(1, ctx.threshold(1) * 10)), "Database temporary disk spill",
               evidence("Database temp MB in 24h", databaseMb), "Review spilling queries, including those outside the stored top-N. Size work_mem with concurrency in mind.", null, null, "database", null));
         }
      }
      return (findings);
   }


   /*******************************************************************************
    ** Delimit parts with lengths to avoid collisions between unit names.
    *******************************************************************************/
   private static Map<String, List<QRecord>> group(List<QRecord> rows, String... fields)
   {
      Map<String, List<QRecord>> groups = new LinkedHashMap<>();
      for(QRecord row : rows)
      {
         String key = java.util.Arrays.stream(fields).map(f -> String.valueOf(row.getValue(f))).map(s -> s.length() + ":" + s).collect(Collectors.joining("|"));
         groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
      }
      return (groups);
   }


   /*******************************************************************************
    ** Shared foreign-key comparison.
    *******************************************************************************/
   private static boolean sameTable(QRecord table, QRecord index)
   {
      return (table.getValueInteger("id").equals(index.getValueInteger("tableStatId")));
   }


   /*******************************************************************************
    ** Numeric convenience keeps units visible at the comparison sites.
    *******************************************************************************/
   private static double n(QRecord row, String field)
   {
      return (RuleContext.number(row, field));
   }


   /*******************************************************************************
    ** Small Markdown table with escaped cell delimiters.
    *******************************************************************************/
   private static String evidence(Object... cells)
   {
      StringBuilder text = new StringBuilder("| Measurement | Value |\n| --- | --- |\n");
      for(int i = 0; i < cells.length; i += 2)
      {
         text.append("| ").append(cells[i]).append(" | ").append(String.valueOf(cells[i + 1]).replace("|", "\\|").replace("\n", " ")).append(" |\n");
      }
      return (text.toString());
   }


   /*******************************************************************************
    ** Centralize persisted links and severity; no rule invents a fixed severity.
    *******************************************************************************/
   private FindingDraft draft(RuleContext ctx, String subject, double impact, String summary, String evidence, String recommendation,
                              String snippet, QRecord row, String kind, String unit)
   {
      String table = row == null ? null : row.getValueString(kind.equals("statement") ? "mainTable" : "tableName");
      Integer id = row == null ? null : row.getValueInteger("id");
      String key = code() + ":" + ctx.backend + ":" + subject;
      if(key.length() > 300)
      {
         key = code() + ":" + ctx.backend + ":" + SqlNormalizer.fingerprint(subject);
      }
      String title = ctx.rule.getValueString("label") + ": " + (unit == null ? subject : unit);
      return (new FindingDraft(code(), key, ctx.rule.getValueString("severity"), impact,
         title.substring(0, Math.min(300, title.length())), summary.substring(0, Math.min(2000, summary.length())), evidence, recommendation, snippet, table,
         kind.equals("statement") ? id : null, kind.equals("table") ? id : kind.equals("index") ? row.getValueInteger("tableStatId") : null,
         kind.equals("index") ? id : null, unit));
   }
}
