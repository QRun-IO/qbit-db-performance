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

package com.kingsrook.qbits.dbperformance.widgets;


import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.api.DbPerformanceData;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.collect.SnapshotCollector;
import com.kingsrook.qbits.dbperformance.provider.ConnectionCount;
import com.kingsrook.qbits.dbperformance.provider.DbStatsProviders;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChartData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.CompositeWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.MultiStatisticsData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.MultiTableData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ParentWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.QWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.BlockLink;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.text.TextBlockData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.text.TextStyles;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.text.TextValues;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import static com.kingsrook.qbits.dbperformance.api.DbPerformanceData.map;
import static com.kingsrook.qbits.dbperformance.api.DbPerformanceData.number;


/*******************************************************************************
 ** Typed data only: paginated tables, full-width panels and explicit empty guidance.
 *******************************************************************************/
public class DbPerfWidgetRenderer extends AbstractWidgetRenderer
{



   /*******************************************************************************
    ** No privileged read context: both widget and service enforce the caller's view grant.
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      DbPerfRuntime runtime = DbPerfRuntime.of(QContext.getQInstance());
      DbPerformanceData data = new DbPerformanceData(runtime);
      data.requireView(null);
      try
      {
         QWidgetData result = switch(input.getWidgetMetaData().getName())
         {
            case "dbPerfSetup" -> setup(runtime, data);
            case "dbPerfDatabaseTime" -> databaseTime(data);
            case "dbPerfTopQueries" -> topQueries(data);
            case "dbPerfFindings" -> findings(data);
            case "dbPerfLiveActivity" -> liveActivity(data);
            case "dbPerfConnections" -> connections();
            case "dbPerfConnectionStatistics" -> connectionStatistics(data);
            case "dbPerfConnectionsByApplication" -> connectionsByApplication(runtime);
            case "dbPerfAttribution" -> attribution();
            case "dbPerfTables" -> tables(data);
            default -> throw new QException("Unknown Database Performance widget.");
         };
         return (new RenderWidgetOutput(result));
      }
      catch(QException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         throw (new QException("Database Performance panel unavailable. Check the backend and statistics access."));
      }
   }



   /*******************************************************************************
    ** Guided checks do not write settings or require a first successful snapshot.
    *******************************************************************************/
   public TableData setup(DbPerfRuntime runtime, DbPerformanceData data) throws Exception
   {
      QRecord settings = data.settings();
      String access = settings == null ? "NOT_COLLECTED" : settings.getValueString("statsAccess");
      Instant last = settings == null ? null : settings.getValueInstant("lastSnapshotAt");
      boolean attribution = settings == null ? Boolean.TRUE.equals(runtime.getConfig().getAttributionEnabled()) : Boolean.TRUE.equals(settings.getValueBoolean("attributionEnabled"));
      List<Map<String, Object>> rows = new ArrayList<>();
      rows.add(check("Monitored backend", runtime.getConfig().getBackendName(), "Review Settings for this backend."));
      rows.add(check("Provider", settings == null ? "Not detected yet" : settings.getValueString("provider"), "PostgreSQL 13+ is supported."));
      rows.add(check("pg_stat_statements", access, "Preload and create pg_stat_statements; then run Collect now."));
      rows.add(check("pg_read_all_stats", access, settings == null ? "Grant pg_read_all_stats to the application's database role." : settings.getValueString("statsAccessMessage")));
      rows.add(check("Last snapshot", last == null ? "Nothing collected yet" : Duration.between(last, Instant.now()).toSeconds() + " seconds ago",
         last == null ? "Run Collect now from Snapshots, or wait for the next scheduled collection." : "Collection normally runs every " + SnapshotCollector.positive(settings, "snapshotIntervalMinutes", 5) + " minutes."));
      rows.add(check("Attribution", attribution ? "On" : "Off", attribution ? "Attribution starts after the next flush." : "Enable attribution in Settings and the host configuration."));
      rows.add(check("Schedules", runtime.getConfig().getSchedulerName() == null ? "Not configured" : "Configured", "Set schedulerName on the qbit and register that scheduler in the host."));
      return (table("Run Collect now to start collecting database performance.", rows, "Check", "State", "Next step"));
   }



   /*******************************************************************************
    ** Plain text check row, never markup.
    *******************************************************************************/
   private static Map<String, Object> check(String name, String state, String next)
   {
      return (map("Check", name, "State", state, "Next step", next));
   }



   /*******************************************************************************
    ** Separate labeled time and call series from each snapshot's interval deltas.
    *******************************************************************************/
   public ChartData databaseTime(DbPerformanceData data) throws QException
   {
      List<QRecord> snapshots = data.snapshots();
      List<String> labels = snapshots.stream().map(row -> row.getValueString("capturedAt")).toList();
      List<Number> milliseconds = snapshots.stream().map(row -> (Number) row.getValue("statementTotalMs")).toList();
      List<Number> calls = snapshots.stream().map(row -> (Number) row.getValue("statementCalls")).toList();
      return (new DbPerfTimeChart().withDescription(snapshots.isEmpty() ? "Database time appears after the next snapshot." : "Last 24 hours; each point is one collection interval.")
         .withChartData(new ChartData.Data().withLabels(labels).withDatasets(List.of(
            new ChartData.Data.Dataset().withLabel("Database time (ms)").withData(milliseconds).withBackgroundColor("#2563eb"),
            new ChartData.Data.Dataset().withLabel("Calls").withData(calls).withBackgroundColor("#16a34a")))));
   }



   /*******************************************************************************
    ** Truncated query shapes link to their record and paginate rather than scroll.
    *******************************************************************************/
   public TableData topQueries(DbPerformanceData data) throws QException
   {
      List<Map<String, Object>> rows = new ArrayList<>();
      for(Map<String, Object> statement : data.statements(100, null))
      {
         rows.add(map("Query", stack((String) statement.get("queryText"), (String) statement.get("historyPath"),
               format(statement.get("calls24h")) + " calls, mean " + format(statement.get("meanMs24h")) + " ms"),
            "Total ms (24h)", statement.get("totalMs24h"), "Share of DB time %", statement.get("shareOfDbTime24h"), "Cache hit ratio", statement.get("hitRatio")));
      }
      TableData table = table("Top queries appear after two snapshots establish interval deltas.", rows, "Query", "Total ms (24h)", "Share of DB time %", "Cache hit ratio");
      table.getColumns().getFirst().setType("composite");
      return (table);
   }



   /*******************************************************************************
    ** Show actionable findings from the parallel findings engine, without changing them.
    *******************************************************************************/
   public TableData findings(DbPerformanceData data) throws QException
   {
      List<Map<String, Object>> rows = new ArrayList<>();
      for(QRecord row : data.findings(100))
      {
         rows.add(map("Severity", row.getValueString("severity"), "Title", link(row.getValueString("title"), data.recordPath("db_perf_finding", row.getValueInteger("id"))),
            "Rule", row.getValueString("ruleCode"), "Last seen", row.getValue("lastSeenAt")));
      }
      TableData table = table("Findings appear after the next analysis of collected snapshots.", rows, "Severity", "Title", "Rule", "Last seen");
      table.getColumns().get(1).setType("composite");
      return (table);
   }



   /*******************************************************************************
    ** Even callers with live-text permission see only fingerprinted text on home.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   public TableData liveActivity(DbPerformanceData data) throws Exception
   {
      QRecord settings = data.settings();
      int longRunning = settings == null ? 30 : SnapshotCollector.positive(settings, "activityLongRunningSeconds", 30);
      int idle = settings == null ? 60 : SnapshotCollector.positive(settings, "idleInTransactionSeconds", 60);
      List<Map<String, Object>> sessions = (List<Map<String, Object>>) data.activity((String) null, false).get("sessions");
      List<Map<String, Object>> rows = sessions.stream().filter(row -> !((List<?>) row.get("blockedBy")).isEmpty() || ((Number) row.get("blockingCount")).intValue() > 0
         || "active".equals(row.get("state")) && numeric(row.get("querySeconds")) >= longRunning
         || Objects.toString(row.get("state"), "").startsWith("idle in transaction") && numeric(row.get("xactSeconds")) >= idle).toList();
      List<Map<String, Object>> cells = new ArrayList<>();
      for(Map<String, Object> row : rows)
      {
         boolean blocked = !((List<?>) row.get("blockedBy")).isEmpty();
         int     blocking = ((Number) row.get("blockingCount")).intValue();
         cells.add(map("Session", stack("pid " + row.get("pid") + ", " + Objects.toString(row.get("user"), "?") + ", " + Objects.toString(row.get("application"), ""), null,
               Objects.toString(row.get("normalizedText"), "")),
            "State", Objects.toString(row.get("state"), "") + (row.get("waitEvent") == null ? "" : " (" + row.get("waitEvent") + ")"),
            "Query / transaction s", format(row.get("querySeconds")) + " / " + format(row.get("xactSeconds")),
            "Blocking", blocked ? "blocked by " + row.get("blockedBy") : (blocking > 0 ? "blocks " + blocking : "")));
      }
      TableData table = table("No long-running or blocked sessions right now", cells, "Session", "State", "Query / transaction s", "Blocking");
      table.getColumns().getFirst().setType("composite");
      return (table);
   }



   /*******************************************************************************
    ** Capacity and breakdown are stacked, each occupying the whole row.
    *******************************************************************************/
   public ParentWidgetData connections()
   {
      return (new ParentWidgetData().withChildWidgetNameList(List.of("dbPerfConnectionStatistics", "dbPerfConnectionsByApplication")));
   }



   /*******************************************************************************
    ** Last observed capacity plus summed 24-hour delta metrics, never cumulative totals.
    *******************************************************************************/
   public MultiStatisticsData connectionStatistics(DbPerformanceData data) throws QException
   {
      QRecord latest = data.latest();
      List<QRecord> snapshots = data.snapshots();
      List<MultiStatisticsData.StatisticsGroupData.Statistic> values = new ArrayList<>();
      values.add(stat("Total connections", latest == null ? null : number(latest, "connectionsTotal")));
      values.add(stat("Maximum connections", latest == null ? null : number(latest, "maxConnections")));
      values.add(stat("Active", latest == null ? null : number(latest, "connectionsActive")));
      values.add(stat("Idle", latest == null ? null : number(latest, "connectionsIdle")));
      values.add(stat("Idle in transaction", latest == null ? null : number(latest, "connectionsIdleInTx")));
      values.add(stat("Cache hit %", latest == null ? null : 100 * number(latest, "cacheHitRatio")));
      values.add(stat("Temp MB (24h)", snapshots.isEmpty() ? null : snapshots.stream().mapToDouble(row -> number(row, "tempBytes")).sum() / 1048576));
      values.add(stat("Deadlocks (24h)", snapshots.isEmpty() ? null : snapshots.stream().mapToDouble(row -> number(row, "deadlocks")).sum()));
      return (new MultiStatisticsData(null, List.of(new MultiStatisticsData.StatisticsGroupData().withHeader("Connection capacity")
         .withSubheader(latest == null ? "Capacity appears after the next snapshot." : "Latest snapshot; percentages and MB rounded to whole numbers.").withStatisticList(values))));
   }



   /*******************************************************************************
    ** Current connection counts by application, user and state, directly from provider.
    *******************************************************************************/
   public TableData connectionsByApplication(DbPerfRuntime runtime) throws Exception
   {
      List<Map<String, Object>> rows = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection((RDBMSBackendMetaData) runtime.getInstance().getBackend(runtime.getConfig().getBackendName())))
      {
         for(ConnectionCount count : DbStatsProviders.forConnection(connection).readConnections(connection))
         {
            rows.add(map("Application", count.applicationName(), "User", count.dbUser(), "State", count.state(), "Connections", count.connectionCount()));
         }
      }
      return (table("No connection counts available for this provider.", rows, "Application", "User", "State", "Connections"));
   }



   /*******************************************************************************
    ** Aggregate units over 24 hours and show repeated-statement patterns below them.
    *******************************************************************************/
   public MultiTableData attribution() throws QException
   {
      List<QRecord> records = DbPerfStore.query("db_perf_attribution", since("windowEnd"));
      Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
      Map<String, Map<String, Double>> tableTimes = new LinkedHashMap<>();
      double total = records.stream().mapToDouble(row -> number(row, "totalMs")).sum();
      for(QRecord row : records)
      {
         String key = row.getValueString("unitKind") + "\n" + row.getValueString("unitName");
         Map<String, Object> group = grouped.computeIfAbsent(key, ignored -> map("Kind", row.getValueString("unitKind"), "Name", row.getValueString("unitName"), "Statements", 0.0, "Total ms", 0.0));
         group.put("Statements", numeric(group.get("Statements")) + number(row, "statements"));
         group.put("Total ms", numeric(group.get("Total ms")) + number(row, "totalMs"));
         tableTimes.computeIfAbsent(key, ignored -> new LinkedHashMap<>()).merge(Objects.toString(row.getValueString("tableName"), "Unknown"), number(row, "totalMs"), Double::sum);
      }
      grouped.forEach((key, group) ->
      {
         group.put("Share %", total == 0 ? 0 : 100 * numeric(group.get("Total ms")) / total);
         group.put("Top table", tableTimes.get(key).entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey());
      });
      List<Map<String, Object>> units = grouped.values().stream().sorted(Comparator.comparingDouble((Map<String, Object> row) -> numeric(row.get("Total ms"))).reversed())
         .map(row -> map("Unit of work", stack(Objects.toString(row.get("Name"), ""), null, kindLabel(row.get("Kind")) + ", top table " + row.get("Top table")),
            "Statements", row.get("Statements"), "Total ms (24h)", row.get("Total ms"), "Share %", row.get("Share %"))).toList();
      List<Map<String, Object>> repeats = DbPerfStore.query("db_perf_repeat_pattern", since("windowEnd").withOrderBy(new QFilterOrderBy("maxRepeats", false)).withLimit(100)).stream()
         .map(row -> map("Unit of work", stack(Objects.toString(row.getValueString("unitName"), ""), null,
               kindLabel(row.getValueString("unitKind")) + ": " + truncate(row.getValueString("sqlText"), 160)),
            "Most repeats in one unit", row.getValue("maxRepeats"), "Units affected", row.getValue("unitsAffected"), "Total repeats", row.getValue("totalRepeats"))).toList();
      TableData unitTable = table("Attribution starts after the next flush", units, "Unit of work", "Statements", "Total ms (24h)", "Share %");
      unitTable.getColumns().getFirst().setType("composite");
      TableData repeatTable = table("Repeated statements appear after the next attribution flush when the repetition threshold is reached.", repeats,
         "Unit of work", "Most repeats in one unit", "Units affected", "Total repeats");
      repeatTable.getColumns().getFirst().setType("composite");
      return (new MultiTableData(List.of(unitTable, repeatTable)));
   }



   /*******************************************************************************
    ** Sequential-scan pressure and seven-day size growth are separate ranked tables.
    *******************************************************************************/
   public MultiTableData tables(DbPerformanceData data) throws QException
   {
      List<QRecord> scans = DbPerfStore.query("db_perf_table_stat", data.backendFilter().withOrderBy(new QFilterOrderBy("seqScanShare", false)).withLimit(100));
      List<QRecord> growing = DbPerfStore.query("db_perf_table_stat", data.backendFilter().withOrderBy(new QFilterOrderBy("growthBytes7d", false)).withLimit(100));
      return (new MultiTableData(List.of(table("Table scans appear after the next table snapshot (normally hourly).", scans.stream()
            .map(row -> map("Table", row.getValueString("schemaName") + "." + row.getValueString("tableName"), "Sequential scans", row.getValue("seqScan"),
               "Index scans", row.getValue("idxScan"), "Sequential share %", row.getValue("seqScanShare"))).toList(),
            "Table", "Sequential scans", "Index scans", "Sequential share %"),
         table("Growth appears after table history has accumulated.", growing.stream()
            .map(row -> map("Table", row.getValueString("schemaName") + "." + row.getValueString("tableName"), "Size MB", mb(row.getValue("totalBytes")),
               "Growth MB (7 days)", mb(row.getValue("growthBytes7d")))).toList(),
            "Table", "Size MB", "Growth MB (7 days)"))));
   }



   /*******************************************************************************
    ** The core TABLE renderer paginates when fixedHeight is absent.
    *******************************************************************************/
   private static TableData table(String empty, List<Map<String, Object>> rows, String... columns)
   {
      return (new TableData(null, Arrays.stream(columns).map(name -> new TableData.Column("default", name, name, null, "left")).toList(), rows)
         .withRowsPerPage(10).withHidePaginationDropdown(false).withNoRowsFoundHTML(empty));
   }



   /*******************************************************************************
    ** Typed record link, never an HTML fragment.
    *******************************************************************************/
   private static CompositeWidgetData link(String text, String path)
   {
      return (new CompositeWidgetData().withLayout(CompositeWidgetData.Layout.FLEX_COLUMN)
         .withBlock(new TextBlockData().withValues(new TextValues(text)).withLink(new BlockLink(path))));
   }



   /*******************************************************************************
    ** A stacked cell: the main text (a record link when a path is given) over a
    ** smaller secondary line, so tables keep at most four columns.
    *******************************************************************************/
   private static CompositeWidgetData stack(String main, String path, String secondary)
   {
      TextBlockData first = new TextBlockData().withValues(new TextValues(truncate(main, 300)));
      if(path != null)
      {
         first.withLink(new BlockLink(path));
      }
      CompositeWidgetData cell = new CompositeWidgetData().withLayout(CompositeWidgetData.Layout.FLEX_COLUMN).withBlock(first);
      if(secondary != null && !secondary.isBlank())
      {
         cell.withBlock(new TextBlockData().withValues(new TextValues(truncate(secondary, 300))).withStyles(new TextStyles().withColor("MUTED")));
      }
      return (cell);
   }



   /*******************************************************************************
    ** Plain label for a unit-of-work kind.
    *******************************************************************************/
   private static String kindLabel(Object kind)
   {
      return (switch(Objects.toString(kind, ""))
      {
         case "HTTP_REQUEST" -> "Request";
         case "SCHEDULED_JOB" -> "Scheduled job";
         case "PROCESS" -> "Process";
         default -> "Other";
      });
   }



   /*******************************************************************************
    ** Text capped with an ellipsis.
    *******************************************************************************/
   private static String truncate(String value, int max)
   {
      String text = value == null ? "" : value;
      return (text.length() <= max ? text : text.substring(0, max - 1) + "…");
   }



   /*******************************************************************************
    ** A number for a secondary line: whole numbers without decimals.
    *******************************************************************************/
   private static String format(Object value)
   {
      if(value == null)
      {
         return ("0");
      }
      double number = ((Number) value).doubleValue();
      return (number == Math.rint(number) ? String.valueOf((long) number) : String.format(Locale.ROOT, "%.1f", number));
   }



   /*******************************************************************************
    ** Bytes as whole megabytes.
    *******************************************************************************/
   private static Long mb(Object bytes)
   {
      return (bytes == null ? null : Math.round(((Number) bytes).doubleValue() / 1048576));
   }



   /*******************************************************************************
    ** Trailing-day filter for time-window records.
    *******************************************************************************/
   private static QQueryFilter since(String field)
   {
      return (new QQueryFilter(new QFilterCriteria(field, QCriteriaOperator.GREATER_THAN_OR_EQUALS, Instant.now().minusSeconds(86400))));
   }



   /*******************************************************************************
    ** Core multi-statistics values are nullable integers.
    *******************************************************************************/
   private static MultiStatisticsData.StatisticsGroupData.Statistic stat(String label, Double value)
   {
      return (new MultiStatisticsData.StatisticsGroupData.Statistic(label, value == null ? null : (int) Math.min(Integer.MAX_VALUE, Math.round(value)), null));
   }



   /*******************************************************************************
    ** Numbers in the shared read model retain their numeric type.
    *******************************************************************************/
   private static double numeric(Object value)
   {
      return (value == null ? 0 : ((Number) value).doubleValue());
   }
}
