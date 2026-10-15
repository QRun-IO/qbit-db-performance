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

package com.kingsrook.qbits.dbperformance.metadata;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.dbperformance.DbPerfPermissions;
import com.kingsrook.qbits.dbperformance.DbPerfQBitConfig;
import com.kingsrook.qbits.dbperformance.model.DbPerfActivitySample;
import com.kingsrook.qbits.dbperformance.model.DbPerfAttribution;
import com.kingsrook.qbits.dbperformance.model.DbPerfAttributionDaily;
import com.kingsrook.qbits.dbperformance.model.DbPerfConnectionSample;
import com.kingsrook.qbits.dbperformance.model.DbPerfExplain;
import com.kingsrook.qbits.dbperformance.model.DbPerfFinding;
import com.kingsrook.qbits.dbperformance.model.DbPerfIndexStat;
import com.kingsrook.qbits.dbperformance.model.DbPerfQueryDaily;
import com.kingsrook.qbits.dbperformance.model.DbPerfQuerySnapshot;
import com.kingsrook.qbits.dbperformance.model.DbPerfRepeatPattern;
import com.kingsrook.qbits.dbperformance.model.DbPerfRule;
import com.kingsrook.qbits.dbperformance.model.DbPerfSettings;
import com.kingsrook.qbits.dbperformance.model.DbPerfSnapshot;
import com.kingsrook.qbits.dbperformance.model.DbPerfStatement;
import com.kingsrook.qbits.dbperformance.model.DbPerfTableDaily;
import com.kingsrook.qbits.dbperformance.model.DbPerfTableSnapshot;
import com.kingsrook.qbits.dbperformance.model.DbPerfTableStat;
import com.kingsrook.qqq.backend.core.actions.ai.AiExposure;
import com.kingsrook.qqq.backend.core.actions.ai.AiTableMetaData;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;


/*******************************************************************************
 ** QQQ tables, exact permissions, sections and parent-child record lists.
 *******************************************************************************/
public final class DbPerfMetaData
{
   public static final String CHANGELOG = "db/liquibase/qbit-db-performance/db_performance.changelog.sql";
   public static final List<String> TABLES = List.of("db_perf_settings", "db_perf_rule", "db_perf_snapshot", "db_perf_statement", "db_perf_query_snapshot", "db_perf_query_daily", "db_perf_table_stat", "db_perf_table_snapshot", "db_perf_table_daily", "db_perf_index_stat", "db_perf_activity_sample", "db_perf_connection_sample", "db_perf_attribution", "db_perf_attribution_daily", "db_perf_repeat_pattern", "db_perf_finding", "db_perf_explain");


   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DbPerfMetaData()
   {
   }


   /*******************************************************************************
    ** Register the complete Phase A metadata.
    *******************************************************************************/
   public static void addTo(QInstance instance, DbPerfQBitConfig config) throws QException
   {
      QTableMetaData settings = base(config, DbPerfSettings.TABLE_NAME, "Settings", DbPerfSettings.class);
      settings.withRecordLabelFormat("%s").withRecordLabelFields("backendName");
      settings.withUniqueKey(new UniqueKey("backendName"));
      settings.getField("id").setBackendName("id");
      settings.getField("backendName").setBackendName("backend_name");
      settings.getField("enabled").setBackendName("enabled");
      settings.getField("enabled").setDefaultValue(true);
      settings.getField("provider").setBackendName("provider");
      settings.getField("provider").setPossibleValueSourceName("db_perf_Provider");
      settings.getField("snapshotIntervalMinutes").setBackendName("snapshot_interval_minutes");
      settings.getField("snapshotIntervalMinutes").setDefaultValue(5);
      settings.getField("tableSnapshotMinutes").setBackendName("table_snapshot_minutes");
      settings.getField("tableSnapshotMinutes").setDefaultValue(60);
      settings.getField("detailRetentionDays").setBackendName("detail_retention_days");
      settings.getField("detailRetentionDays").setDefaultValue(30);
      settings.getField("rollupRetentionDays").setBackendName("rollup_retention_days");
      settings.getField("rollupRetentionDays").setDefaultValue(365);
      settings.getField("topStatementsPerSnapshot").setBackendName("top_statements_per_snapshot");
      settings.getField("topStatementsPerSnapshot").setDefaultValue(200);
      settings.getField("activityLongRunningSeconds").setBackendName("activity_long_running_seconds");
      settings.getField("activityLongRunningSeconds").setDefaultValue(30);
      settings.getField("idleInTransactionSeconds").setBackendName("idle_in_transaction_seconds");
      settings.getField("idleInTransactionSeconds").setDefaultValue(60);
      settings.getField("attributionEnabled").setBackendName("attribution_enabled");
      settings.getField("attributionEnabled").setDefaultValue(true);
      settings.getField("attributionSampleRate").setBackendName("attribution_sample_rate");
      settings.getField("attributionSampleRate").setDefaultValue(new java.math.BigDecimal("1.0"));
      settings.getField("attributionFlushSeconds").setBackendName("attribution_flush_seconds");
      settings.getField("attributionFlushSeconds").setDefaultValue(60);
      settings.getField("attributionMaxKeys").setBackendName("attribution_max_keys");
      settings.getField("attributionMaxKeys").setDefaultValue(10000);
      settings.getField("nPlusOneThreshold").setBackendName("n_plus_one_threshold");
      settings.getField("nPlusOneThreshold").setDefaultValue(10);
      settings.getField("explainTimeoutMs").setBackendName("explain_timeout_ms");
      settings.getField("explainTimeoutMs").setDefaultValue(10000);
      settings.getField("alertsEnabled").setBackendName("alerts_enabled");
      settings.getField("alertsEnabled").setDefaultValue(true);
      settings.getField("spikeFactor").setBackendName("spike_factor");
      settings.getField("spikeFactor").setDefaultValue(new java.math.BigDecimal("3"));
      settings.getField("spikeMinTotalMs").setBackendName("spike_min_total_ms");
      settings.getField("spikeMinTotalMs").setDefaultValue(5000);
      settings.getField("connectionAlertPercent").setBackendName("connection_alert_percent");
      settings.getField("connectionAlertPercent").setDefaultValue(80);
      settings.getField("blockedAlertSeconds").setBackendName("blocked_alert_seconds");
      settings.getField("blockedAlertSeconds").setDefaultValue(30);
      settings.getField("lastSnapshotAt").setBackendName("last_snapshot_at");
      settings.getField("lastTableSnapshotAt").setBackendName("last_table_snapshot_at");
      settings.getField("statsAccess").setBackendName("stats_access");
      settings.getField("statsAccess").setPossibleValueSourceName("db_perf_StatsAccess");
      settings.getField("statsAccessMessage").setBackendName("stats_access_message");
      settings.getField("createDate").setBackendName("create_date");
      settings.getField("modifyDate").setBackendName("modify_date");
      settings.withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));
      settings.withSupplementalMetaData(new AiTableMetaData().withExposure(AiExposure.NONE));
      settings.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_DELETE);
      settings.withSection(new QFieldSection("identity", new QIcon("table_rows"), Tier.T1, List.of("id", "backendName", "enabled", "provider", "snapshotIntervalMinutes", "tableSnapshotMinutes", "detailRetentionDays")));
      settings.withSection(new QFieldSection("details", new QIcon("table_rows"), Tier.T2, List.of("rollupRetentionDays", "topStatementsPerSnapshot", "activityLongRunningSeconds", "idleInTransactionSeconds", "attributionEnabled", "attributionSampleRate", "attributionFlushSeconds", "attributionMaxKeys", "nPlusOneThreshold", "explainTimeoutMs", "alertsEnabled", "spikeFactor", "spikeMinTotalMs", "connectionAlertPercent", "blockedAlertSeconds", "lastSnapshotAt", "lastTableSnapshotAt", "statsAccess", "statsAccessMessage")));
      settings.withSection(new QFieldSection("system", new QIcon("table_rows"), Tier.T3, List.of("createDate", "modifyDate")));
      instance.addTable(settings);

      QTableMetaData rule = base(config, DbPerfRule.TABLE_NAME, "Suggestion Rules", DbPerfRule.class);
      rule.withRecordLabelFormat("%s").withRecordLabelFields("label");
      rule.withUniqueKey(new UniqueKey("code"));
      rule.getField("id").setBackendName("id");
      rule.getField("code").setBackendName("code");
      rule.getField("label").setBackendName("label");
      rule.getField("description").setBackendName("description");
      rule.getField("enabled").setBackendName("enabled");
      rule.getField("enabled").setDefaultValue(true);
      rule.getField("severity").setBackendName("severity");
      rule.getField("severity").setPossibleValueSourceName("db_perf_Severity");
      rule.getField("threshold1").setBackendName("threshold_1");
      rule.getField("threshold2").setBackendName("threshold_2");
      rule.getField("threshold1Label").setBackendName("threshold_1_label");
      rule.getField("threshold2Label").setBackendName("threshold_2_label");
      rule.getField("createDate").setBackendName("create_date");
      rule.getField("modifyDate").setBackendName("modify_date");
      rule.withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));
      rule.withSupplementalMetaData(new AiTableMetaData().withExposure(AiExposure.NONE));
      rule.withSection(new QFieldSection("identity", new QIcon("table_rows"), Tier.T1, List.of("id", "code", "label", "description", "enabled", "severity", "threshold1")));
      rule.withSection(new QFieldSection("details", new QIcon("table_rows"), Tier.T2, List.of("threshold2", "threshold1Label", "threshold2Label")));
      rule.withSection(new QFieldSection("system", new QIcon("table_rows"), Tier.T3, List.of("createDate", "modifyDate")));
      instance.addTable(rule);

      QTableMetaData snapshot = base(config, DbPerfSnapshot.TABLE_NAME, "Snapshots", DbPerfSnapshot.class);
      snapshot.withRecordLabelFormat("%s").withRecordLabelFields("capturedAt");
      snapshot.withUniqueKey(new UniqueKey("backendName", "capturedAt"));
      snapshot.getField("id").setBackendName("id");
      snapshot.getField("backendName").setBackendName("backend_name");
      snapshot.getField("capturedAt").setBackendName("captured_at");
      snapshot.getField("intervalSeconds").setBackendName("interval_seconds");
      snapshot.getField("status").setBackendName("status");
      snapshot.getField("status").setPossibleValueSourceName("db_perf_SnapshotStatus");
      snapshot.getField("durationMs").setBackendName("duration_ms");
      snapshot.getField("errorMessage").setBackendName("error_message");
      snapshot.getField("statsResetDetected").setBackendName("stats_reset_detected");
      snapshot.getField("includesTables").setBackendName("includes_tables");
      snapshot.getField("xactCommit").setBackendName("xact_commit");
      snapshot.getField("xactRollback").setBackendName("xact_rollback");
      snapshot.getField("blksHit").setBackendName("blks_hit");
      snapshot.getField("blksRead").setBackendName("blks_read");
      snapshot.getField("cacheHitRatio").setBackendName("cache_hit_ratio");
      snapshot.getField("tempFiles").setBackendName("temp_files");
      snapshot.getField("tempBytes").setBackendName("temp_bytes");
      snapshot.getField("deadlocks").setBackendName("deadlocks");
      snapshot.getField("conflicts").setBackendName("conflicts");
      snapshot.getField("connectionsTotal").setBackendName("connections_total");
      snapshot.getField("connectionsActive").setBackendName("connections_active");
      snapshot.getField("connectionsIdle").setBackendName("connections_idle");
      snapshot.getField("connectionsIdleInTx").setBackendName("connections_idle_in_tx");
      snapshot.getField("maxConnections").setBackendName("max_connections");
      snapshot.getField("databaseSizeBytes").setBackendName("database_size_bytes");
      snapshot.getField("statementCount").setBackendName("statement_count");
      snapshot.getField("statementCalls").setBackendName("statement_calls");
      snapshot.getField("statementTotalMs").setBackendName("statement_total_ms");
      snapshot.getField("storedStatementCount").setBackendName("stored_statement_count");
      snapshot.getField("createDate").setBackendName("create_date");
      snapshot.getField("rawXactCommit").setBackendName("raw_xact_commit");
      snapshot.getField("rawXactRollback").setBackendName("raw_xact_rollback");
      snapshot.getField("rawBlksHit").setBackendName("raw_blks_hit");
      snapshot.getField("rawBlksRead").setBackendName("raw_blks_read");
      snapshot.getField("rawTempFiles").setBackendName("raw_temp_files");
      snapshot.getField("rawTempBytes").setBackendName("raw_temp_bytes");
      snapshot.getField("rawDeadlocks").setBackendName("raw_deadlocks");
      snapshot.getField("rawConflicts").setBackendName("raw_conflicts");
      snapshot.getField("rawStatsReset").setBackendName("raw_stats_reset");
      snapshot.getField("rawStatementStatsReset").setBackendName("raw_statement_stats_reset");
      snapshot.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
      snapshot.withSection(new QFieldSection("identity", new QIcon("table_rows"), Tier.T1, List.of("id", "backendName", "capturedAt", "intervalSeconds", "status", "durationMs", "errorMessage")));
      snapshot.withSection(new QFieldSection("details", new QIcon("table_rows"), Tier.T2, List.of("statsResetDetected", "includesTables", "xactCommit", "xactRollback", "blksHit", "blksRead", "cacheHitRatio", "tempFiles", "tempBytes", "deadlocks", "conflicts", "connectionsTotal", "connectionsActive", "connectionsIdle", "connectionsIdleInTx", "maxConnections", "databaseSizeBytes", "statementCount", "statementCalls", "statementTotalMs", "storedStatementCount")));
      snapshot.withSection(new QFieldSection("system", new QIcon("table_rows"), Tier.T3, List.of("createDate", "rawXactCommit", "rawXactRollback", "rawBlksHit", "rawBlksRead", "rawTempFiles", "rawTempBytes", "rawDeadlocks", "rawConflicts", "rawStatsReset", "rawStatementStatsReset")));
      instance.addTable(snapshot);

      QTableMetaData statement = base(config, DbPerfStatement.TABLE_NAME, "Queries", DbPerfStatement.class);
      statement.withRecordLabelFormat("%s").withRecordLabelFields("queryText");
      statement.withUniqueKey(new UniqueKey("backendName", "queryId", "dbUser", "toplevel"));
      statement.getField("id").setBackendName("id");
      statement.getField("backendName").setBackendName("backend_name");
      statement.getField("queryId").setBackendName("query_id");
      statement.getField("dbUser").setBackendName("db_user");
      statement.getField("toplevel").setBackendName("toplevel");
      statement.getField("fingerprint").setBackendName("fingerprint");
      statement.getField("queryText").setBackendName("query_text");
      statement.getField("queryText").setType(QFieldType.TEXT);
      statement.getField("operation").setBackendName("operation");
      statement.getField("operation").setPossibleValueSourceName("db_perf_Operation");
      statement.getField("mainTable").setBackendName("main_table");
      statement.getField("firstSeenAt").setBackendName("first_seen_at");
      statement.getField("lastSeenAt").setBackendName("last_seen_at");
      statement.getField("lastCalls").setBackendName("last_calls");
      statement.getField("lastTotalExecMs").setBackendName("last_total_exec_ms");
      statement.getField("lastRows").setBackendName("last_rows");
      statement.getField("lastSharedBlksHit").setBackendName("last_shared_blks_hit");
      statement.getField("lastSharedBlksRead").setBackendName("last_shared_blks_read");
      statement.getField("lastTempBlksWritten").setBackendName("last_temp_blks_written");
      statement.getField("lastMaxExecMs").setBackendName("last_max_exec_ms");
      statement.getField("lastStatsSince").setBackendName("last_stats_since");
      statement.getField("calls24h").setBackendName("calls_24h");
      statement.getField("totalMs24h").setBackendName("total_ms_24h");
      statement.getField("meanMs24h").setBackendName("mean_ms_24h");
      statement.getField("shareOfDbTime24h").setBackendName("share_of_db_time_24h");
      statement.getField("createDate").setBackendName("create_date");
      statement.getField("modifyDate").setBackendName("modify_date");
      statement.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
      statement.withSection(new QFieldSection("identity", new QIcon("table_rows"), Tier.T1, List.of("id", "backendName", "queryId", "dbUser", "toplevel", "fingerprint", "queryText")));
      statement.withSection(new QFieldSection("details", new QIcon("table_rows"), Tier.T2, List.of("operation", "mainTable", "firstSeenAt", "lastSeenAt", "lastCalls", "lastTotalExecMs", "lastRows", "lastSharedBlksHit", "lastSharedBlksRead", "lastTempBlksWritten", "lastMaxExecMs", "lastStatsSince", "calls24h", "totalMs24h", "meanMs24h", "shareOfDbTime24h")));
      statement.withSection(new QFieldSection("system", new QIcon("table_rows"), Tier.T3, List.of("createDate", "modifyDate")));
      instance.addTable(statement);

      QTableMetaData querySnapshot = base(config, DbPerfQuerySnapshot.TABLE_NAME, "Query Snapshots", DbPerfQuerySnapshot.class);
      querySnapshot.withRecordLabelFormat("%s").withRecordLabelFields("id");
      querySnapshot.getField("id").setBackendName("id");
      querySnapshot.getField("snapshotId").setBackendName("snapshot_id");
      querySnapshot.getField("snapshotId").setPossibleValueSourceName("db_perf_snapshot");
      querySnapshot.getField("statementId").setBackendName("statement_id");
      querySnapshot.getField("statementId").setPossibleValueSourceName("db_perf_statement");
      querySnapshot.getField("capturedAt").setBackendName("captured_at");
      querySnapshot.getField("calls").setBackendName("calls");
      querySnapshot.getField("totalMs").setBackendName("total_ms");
      querySnapshot.getField("meanMs").setBackendName("mean_ms");
      querySnapshot.getField("maxMs").setBackendName("max_ms");
      querySnapshot.getField("rowsCount").setBackendName("rows_count");
      querySnapshot.getField("sharedBlksHit").setBackendName("shared_blks_hit");
      querySnapshot.getField("sharedBlksRead").setBackendName("shared_blks_read");
      querySnapshot.getField("hitRatio").setBackendName("hit_ratio");
      querySnapshot.getField("tempBlksWritten").setBackendName("temp_blks_written");
      querySnapshot.getField("shareOfDbTime").setBackendName("share_of_db_time");
      querySnapshot.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
      querySnapshot.withSection(new QFieldSection("identity", new QIcon("table_rows"), Tier.T1, List.of("id", "snapshotId", "statementId", "capturedAt", "calls", "totalMs", "meanMs")));
      querySnapshot.withSection(new QFieldSection("details", new QIcon("table_rows"), Tier.T2, List.of("maxMs", "rowsCount", "sharedBlksHit", "sharedBlksRead", "hitRatio", "tempBlksWritten", "shareOfDbTime")));
      instance.addTable(querySnapshot);

      QTableMetaData queryDaily = base(config, DbPerfQueryDaily.TABLE_NAME, "Query Daily", DbPerfQueryDaily.class);
      queryDaily.withRecordLabelFormat("%s").withRecordLabelFields("day");
      queryDaily.withUniqueKey(new UniqueKey("statementId", "day"));
      queryDaily.getField("id").setBackendName("id");
      queryDaily.getField("statementId").setBackendName("statement_id");
      queryDaily.getField("statementId").setPossibleValueSourceName("db_perf_statement");
      queryDaily.getField("day").setBackendName("day");
      queryDaily.getField("calls").setBackendName("calls");
      queryDaily.getField("totalMs").setBackendName("total_ms");
      queryDaily.getField("meanMs").setBackendName("mean_ms");
      queryDaily.getField("maxMs").setBackendName("max_ms");
      queryDaily.getField("rowsCount").setBackendName("rows_count");
      queryDaily.getField("hitRatio").setBackendName("hit_ratio");
      queryDaily.getField("shareOfDbTime").setBackendName("share_of_db_time");
      queryDaily.getField("snapshots").setBackendName("snapshots");
      queryDaily.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
      queryDaily.withSection(new QFieldSection("identity", new QIcon("table_rows"), Tier.T1, List.of("id", "statementId", "day", "calls", "totalMs", "meanMs", "maxMs")));
      queryDaily.withSection(new QFieldSection("details", new QIcon("table_rows"), Tier.T2, List.of("rowsCount", "hitRatio", "shareOfDbTime", "snapshots")));
      instance.addTable(queryDaily);

      QTableMetaData tableStat = base(config, DbPerfTableStat.TABLE_NAME, "Tables", DbPerfTableStat.class);
      tableStat.withRecordLabelFormat("%s").withRecordLabelFields("tableName");
      tableStat.withUniqueKey(new UniqueKey("backendName", "schemaName", "tableName"));
      tableStat.getField("id").setBackendName("id");
      tableStat.getField("backendName").setBackendName("backend_name");
      tableStat.getField("schemaName").setBackendName("schema_name");
      tableStat.getField("tableName").setBackendName("table_name");
      tableStat.getField("relid").setBackendName("relid");
      tableStat.getField("seqScan").setBackendName("seq_scan");
      tableStat.getField("seqTupRead").setBackendName("seq_tup_read");
      tableStat.getField("idxScan").setBackendName("idx_scan");
      tableStat.getField("idxTupFetch").setBackendName("idx_tup_fetch");
      tableStat.getField("nTupIns").setBackendName("n_tup_ins");
      tableStat.getField("nTupUpd").setBackendName("n_tup_upd");
      tableStat.getField("nTupDel").setBackendName("n_tup_del");
      tableStat.getField("nTupHotUpd").setBackendName("n_tup_hot_upd");
      tableStat.getField("nLiveTup").setBackendName("n_live_tup");
      tableStat.getField("nDeadTup").setBackendName("n_dead_tup");
      tableStat.getField("deadRatio").setBackendName("dead_ratio");
      tableStat.getField("totalBytes").setBackendName("total_bytes");
      tableStat.getField("tableBytes").setBackendName("table_bytes");
      tableStat.getField("indexBytes").setBackendName("index_bytes");
      tableStat.getField("toastBytes").setBackendName("toast_bytes");
      tableStat.getField("growthBytes7d").setBackendName("growth_bytes_7d");
      tableStat.getField("lastVacuum").setBackendName("last_vacuum");
      tableStat.getField("lastAutovacuum").setBackendName("last_autovacuum");
      tableStat.getField("lastAnalyze").setBackendName("last_analyze");
      tableStat.getField("lastAutoanalyze").setBackendName("last_autoanalyze");
      tableStat.getField("vacuumCount").setBackendName("vacuum_count");
      tableStat.getField("autovacuumCount").setBackendName("autovacuum_count");
      tableStat.getField("analyzeCount").setBackendName("analyze_count");
      tableStat.getField("autoanalyzeCount").setBackendName("autoanalyze_count");
      tableStat.getField("freezeAge").setBackendName("freeze_age");
      tableStat.getField("seqScanShare").setBackendName("seq_scan_share");
      tableStat.getField("lastSeenAt").setBackendName("last_seen_at");
      tableStat.getField("createDate").setBackendName("create_date");
      tableStat.getField("modifyDate").setBackendName("modify_date");
      tableStat.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
      tableStat.withSection(new QFieldSection("identity", new QIcon("table_rows"), Tier.T1, List.of("id", "backendName", "schemaName", "tableName", "relid", "seqScan", "seqTupRead")));
      tableStat.withSection(new QFieldSection("details", new QIcon("table_rows"), Tier.T2, List.of("idxScan", "idxTupFetch", "nTupIns", "nTupUpd", "nTupDel", "nTupHotUpd", "nLiveTup", "nDeadTup", "deadRatio", "totalBytes", "tableBytes", "indexBytes", "toastBytes", "growthBytes7d", "lastVacuum", "lastAutovacuum", "lastAnalyze", "lastAutoanalyze", "vacuumCount", "autovacuumCount", "analyzeCount", "autoanalyzeCount", "freezeAge", "seqScanShare", "lastSeenAt")));
      tableStat.withSection(new QFieldSection("system", new QIcon("table_rows"), Tier.T3, List.of("createDate", "modifyDate")));
      instance.addTable(tableStat);

      QTableMetaData tableSnapshot = base(config, DbPerfTableSnapshot.TABLE_NAME, "Table Snapshots", DbPerfTableSnapshot.class);
      tableSnapshot.withRecordLabelFormat("%s").withRecordLabelFields("id");
      tableSnapshot.getField("id").setBackendName("id");
      tableSnapshot.getField("snapshotId").setBackendName("snapshot_id");
      tableSnapshot.getField("snapshotId").setPossibleValueSourceName("db_perf_snapshot");
      tableSnapshot.getField("tableStatId").setBackendName("table_stat_id");
      tableSnapshot.getField("tableStatId").setPossibleValueSourceName("db_perf_table_stat");
      tableSnapshot.getField("capturedAt").setBackendName("captured_at");
      tableSnapshot.getField("seqScan").setBackendName("seq_scan");
      tableSnapshot.getField("seqTupRead").setBackendName("seq_tup_read");
      tableSnapshot.getField("idxScan").setBackendName("idx_scan");
      tableSnapshot.getField("idxTupFetch").setBackendName("idx_tup_fetch");
      tableSnapshot.getField("nTupIns").setBackendName("n_tup_ins");
      tableSnapshot.getField("nTupUpd").setBackendName("n_tup_upd");
      tableSnapshot.getField("nTupDel").setBackendName("n_tup_del");
      tableSnapshot.getField("nTupHotUpd").setBackendName("n_tup_hot_upd");
      tableSnapshot.getField("nLiveTup").setBackendName("n_live_tup");
      tableSnapshot.getField("nDeadTup").setBackendName("n_dead_tup");
      tableSnapshot.getField("totalBytes").setBackendName("total_bytes");
      tableSnapshot.getField("tableBytes").setBackendName("table_bytes");
      tableSnapshot.getField("indexBytes").setBackendName("index_bytes");
      tableSnapshot.getField("autovacuumCount").setBackendName("autovacuum_count");
      tableSnapshot.getField("autoanalyzeCount").setBackendName("autoanalyze_count");
      tableSnapshot.getField("freezeAge").setBackendName("freeze_age");
      tableSnapshot.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
      tableSnapshot.withSection(new QFieldSection("identity", new QIcon("table_rows"), Tier.T1, List.of("id", "snapshotId", "tableStatId", "capturedAt", "seqScan", "seqTupRead", "idxScan")));
      tableSnapshot.withSection(new QFieldSection("details", new QIcon("table_rows"), Tier.T2, List.of("idxTupFetch", "nTupIns", "nTupUpd", "nTupDel", "nTupHotUpd", "nLiveTup", "nDeadTup", "totalBytes", "tableBytes", "indexBytes", "autovacuumCount", "autoanalyzeCount", "freezeAge")));
      instance.addTable(tableSnapshot);

      QTableMetaData tableDaily = base(config, DbPerfTableDaily.TABLE_NAME, "Table Daily", DbPerfTableDaily.class);
      tableDaily.withRecordLabelFormat("%s").withRecordLabelFields("day");
      tableDaily.withUniqueKey(new UniqueKey("tableStatId", "day"));
      tableDaily.getField("id").setBackendName("id");
      tableDaily.getField("tableStatId").setBackendName("table_stat_id");
      tableDaily.getField("tableStatId").setPossibleValueSourceName("db_perf_table_stat");
      tableDaily.getField("day").setBackendName("day");
      tableDaily.getField("seqScan").setBackendName("seq_scan");
      tableDaily.getField("idxScan").setBackendName("idx_scan");
      tableDaily.getField("seqTupRead").setBackendName("seq_tup_read");
      tableDaily.getField("nTupIns").setBackendName("n_tup_ins");
      tableDaily.getField("nTupUpd").setBackendName("n_tup_upd");
      tableDaily.getField("nTupDel").setBackendName("n_tup_del");
      tableDaily.getField("nLiveTup").setBackendName("n_live_tup");
      tableDaily.getField("nDeadTup").setBackendName("n_dead_tup");
      tableDaily.getField("totalBytes").setBackendName("total_bytes");
      tableDaily.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
      tableDaily.withSection(new QFieldSection("identity", new QIcon("table_rows"), Tier.T1, List.of("id", "tableStatId", "day", "seqScan", "idxScan", "seqTupRead", "nTupIns")));
      tableDaily.withSection(new QFieldSection("details", new QIcon("table_rows"), Tier.T2, List.of("nTupUpd", "nTupDel", "nLiveTup", "nDeadTup", "totalBytes")));
      instance.addTable(tableDaily);

      QTableMetaData indexStat = base(config, DbPerfIndexStat.TABLE_NAME, "Indexes", DbPerfIndexStat.class);
      indexStat.withRecordLabelFormat("%s").withRecordLabelFields("indexName");
      indexStat.withUniqueKey(new UniqueKey("backendName", "schemaName", "indexName"));
      indexStat.getField("id").setBackendName("id");
      indexStat.getField("backendName").setBackendName("backend_name");
      indexStat.getField("tableStatId").setBackendName("table_stat_id");
      indexStat.getField("tableStatId").setPossibleValueSourceName("db_perf_table_stat");
      indexStat.getField("schemaName").setBackendName("schema_name");
      indexStat.getField("tableName").setBackendName("table_name");
      indexStat.getField("indexName").setBackendName("index_name");
      indexStat.getField("relid").setBackendName("relid");
      indexStat.getField("definition").setBackendName("definition");
      indexStat.getField("columns").setBackendName("columns");
      indexStat.getField("isUnique").setBackendName("is_unique");
      indexStat.getField("isPrimary").setBackendName("is_primary");
      indexStat.getField("isValid").setBackendName("is_valid");
      indexStat.getField("idxScan").setBackendName("idx_scan");
      indexStat.getField("idxTupRead").setBackendName("idx_tup_read");
      indexStat.getField("idxTupFetch").setBackendName("idx_tup_fetch");
      indexStat.getField("sizeBytes").setBackendName("size_bytes");
      indexStat.getField("writeCost").setBackendName("write_cost");
      indexStat.getField("scansSinceReset").setBackendName("scans_since_reset");
      indexStat.getField("lastSeenAt").setBackendName("last_seen_at");
      indexStat.getField("createDate").setBackendName("create_date");
      indexStat.getField("modifyDate").setBackendName("modify_date");
      indexStat.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
      indexStat.withSection(new QFieldSection("identity", new QIcon("table_rows"), Tier.T1, List.of("id", "backendName", "tableStatId", "schemaName", "tableName", "indexName", "relid")));
      indexStat.withSection(new QFieldSection("details", new QIcon("table_rows"), Tier.T2, List.of("definition", "columns", "isUnique", "isPrimary", "isValid", "idxScan", "idxTupRead", "idxTupFetch", "sizeBytes", "writeCost", "scansSinceReset", "lastSeenAt")));
      indexStat.withSection(new QFieldSection("system", new QIcon("table_rows"), Tier.T3, List.of("createDate", "modifyDate")));
      instance.addTable(indexStat);

      QTableMetaData activitySample = base(config, DbPerfActivitySample.TABLE_NAME, "Activity Samples", DbPerfActivitySample.class);
      activitySample.withRecordLabelFormat("%s").withRecordLabelFields("id");
      activitySample.getField("id").setBackendName("id");
      activitySample.getField("snapshotId").setBackendName("snapshot_id");
      activitySample.getField("snapshotId").setPossibleValueSourceName("db_perf_snapshot");
      activitySample.getField("capturedAt").setBackendName("captured_at");
      activitySample.getField("pid").setBackendName("pid");
      activitySample.getField("dbUser").setBackendName("db_user");
      activitySample.getField("applicationName").setBackendName("application_name");
      activitySample.getField("clientAddr").setBackendName("client_addr");
      activitySample.getField("state").setBackendName("state");
      activitySample.getField("state").setPossibleValueSourceName("db_perf_State");
      activitySample.getField("waitEventType").setBackendName("wait_event_type");
      activitySample.getField("waitEvent").setBackendName("wait_event");
      activitySample.getField("backendType").setBackendName("backend_type");
      activitySample.getField("xactStart").setBackendName("xact_start");
      activitySample.getField("queryStart").setBackendName("query_start");
      activitySample.getField("stateChange").setBackendName("state_change");
      activitySample.getField("querySeconds").setBackendName("query_seconds");
      activitySample.getField("xactSeconds").setBackendName("xact_seconds");
      activitySample.getField("blockedBy").setBackendName("blocked_by");
      activitySample.getField("blockingCount").setBackendName("blocking_count");
      activitySample.getField("reason").setBackendName("reason");
      activitySample.getField("reason").setPossibleValueSourceName("db_perf_Reason");
      activitySample.getField("queryId").setBackendName("query_id");
      activitySample.getField("statementId").setBackendName("statement_id");
      activitySample.getField("statementId").setPossibleValueSourceName("db_perf_statement");
      activitySample.getField("queryFingerprint").setBackendName("query_fingerprint");
      activitySample.getField("queryTextNormalized").setBackendName("query_text_normalized");
      activitySample.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
      activitySample.withSection(new QFieldSection("identity", new QIcon("table_rows"), Tier.T1, List.of("id", "snapshotId", "capturedAt", "pid", "dbUser", "applicationName", "clientAddr")));
      activitySample.withSection(new QFieldSection("details", new QIcon("table_rows"), Tier.T2, List.of("state", "waitEventType", "waitEvent", "backendType", "xactStart", "queryStart", "stateChange", "querySeconds", "xactSeconds", "blockedBy", "blockingCount", "reason", "queryId", "statementId", "queryFingerprint", "queryTextNormalized")));
      instance.addTable(activitySample);

      QTableMetaData connectionSample = base(config, DbPerfConnectionSample.TABLE_NAME, "Connection Samples", DbPerfConnectionSample.class);
      connectionSample.withRecordLabelFormat("%s").withRecordLabelFields("id");
      connectionSample.getField("id").setBackendName("id");
      connectionSample.getField("snapshotId").setBackendName("snapshot_id");
      connectionSample.getField("snapshotId").setPossibleValueSourceName("db_perf_snapshot");
      connectionSample.getField("capturedAt").setBackendName("captured_at");
      connectionSample.getField("dbUser").setBackendName("db_user");
      connectionSample.getField("applicationName").setBackendName("application_name");
      connectionSample.getField("state").setBackendName("state");
      connectionSample.getField("state").setPossibleValueSourceName("db_perf_State");
      connectionSample.getField("connectionCount").setBackendName("connection_count");
      connectionSample.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
      connectionSample.withSection(new QFieldSection("identity", new QIcon("table_rows"), Tier.T1, List.of("id", "snapshotId", "capturedAt", "dbUser", "applicationName", "state", "connectionCount")));
      instance.addTable(connectionSample);

      QTableMetaData attribution = base(config, DbPerfAttribution.TABLE_NAME, "QQQ Attribution", DbPerfAttribution.class);
      attribution.withRecordLabelFormat("%s").withRecordLabelFields("id");
      attribution.getField("id").setBackendName("id");
      attribution.getField("windowStart").setBackendName("window_start");
      attribution.getField("windowEnd").setBackendName("window_end");
      attribution.getField("podName").setBackendName("pod_name");
      attribution.getField("unitKind").setBackendName("unit_kind");
      attribution.getField("unitKind").setPossibleValueSourceName("db_perf_UnitKind");
      attribution.getField("unitName").setBackendName("unit_name");
      attribution.getField("processName").setBackendName("process_name");
      attribution.getField("tableName").setBackendName("table_name");
      attribution.getField("operation").setBackendName("operation");
      attribution.getField("operation").setPossibleValueSourceName("db_perf_Operation");
      attribution.getField("fingerprint").setBackendName("fingerprint");
      attribution.getField("sqlText").setBackendName("sql_text");
      attribution.getField("units").setBackendName("units");
      attribution.getField("statements").setBackendName("statements");
      attribution.getField("totalMs").setBackendName("total_ms");
      attribution.getField("maxMs").setBackendName("max_ms");
      attribution.getField("rowsCount").setBackendName("rows_count");
      attribution.getField("errors").setBackendName("errors");
      attribution.getField("sampleRate").setBackendName("sample_rate");
      attribution.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
      attribution.withSection(new QFieldSection("identity", new QIcon("table_rows"), Tier.T1, List.of("id", "windowStart", "windowEnd", "podName", "unitKind", "unitName", "processName")));
      attribution.withSection(new QFieldSection("details", new QIcon("table_rows"), Tier.T2, List.of("tableName", "operation", "fingerprint", "sqlText", "units", "statements", "totalMs", "maxMs", "rowsCount", "errors", "sampleRate")));
      instance.addTable(attribution);

      QTableMetaData attributionDaily = base(config, DbPerfAttributionDaily.TABLE_NAME, "QQQ Attribution Daily", DbPerfAttributionDaily.class);
      attributionDaily.withRecordLabelFormat("%s").withRecordLabelFields("day");
      attributionDaily.withUniqueKey(new UniqueKey("day", "rollupKey"));
      attributionDaily.getField("id").setBackendName("id");
      attributionDaily.getField("day").setBackendName("day");
      attributionDaily.getField("rollupKey").setBackendName("rollup_key");
      attributionDaily.getField("unitKind").setBackendName("unit_kind");
      attributionDaily.getField("unitKind").setPossibleValueSourceName("db_perf_UnitKind");
      attributionDaily.getField("unitName").setBackendName("unit_name");
      attributionDaily.getField("processName").setBackendName("process_name");
      attributionDaily.getField("tableName").setBackendName("table_name");
      attributionDaily.getField("operation").setBackendName("operation");
      attributionDaily.getField("operation").setPossibleValueSourceName("db_perf_Operation");
      attributionDaily.getField("fingerprint").setBackendName("fingerprint");
      attributionDaily.getField("sqlText").setBackendName("sql_text");
      attributionDaily.getField("units").setBackendName("units");
      attributionDaily.getField("statements").setBackendName("statements");
      attributionDaily.getField("totalMs").setBackendName("total_ms");
      attributionDaily.getField("maxMs").setBackendName("max_ms");
      attributionDaily.getField("rowsCount").setBackendName("rows_count");
      attributionDaily.getField("errors").setBackendName("errors");
      attributionDaily.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
      attributionDaily.withSection(new QFieldSection("identity", new QIcon("table_rows"), Tier.T1, List.of("id", "day", "rollupKey", "unitKind", "unitName", "processName", "tableName")));
      attributionDaily.withSection(new QFieldSection("details", new QIcon("table_rows"), Tier.T2, List.of("operation", "fingerprint", "sqlText", "units", "statements", "totalMs", "maxMs", "rowsCount", "errors")));
      instance.addTable(attributionDaily);

      QTableMetaData repeatPattern = base(config, DbPerfRepeatPattern.TABLE_NAME, "Repeated Statements", DbPerfRepeatPattern.class);
      repeatPattern.withRecordLabelFormat("%s").withRecordLabelFields("id");
      repeatPattern.getField("id").setBackendName("id");
      repeatPattern.getField("windowStart").setBackendName("window_start");
      repeatPattern.getField("windowEnd").setBackendName("window_end");
      repeatPattern.getField("podName").setBackendName("pod_name");
      repeatPattern.getField("unitKind").setBackendName("unit_kind");
      repeatPattern.getField("unitKind").setPossibleValueSourceName("db_perf_UnitKind");
      repeatPattern.getField("unitName").setBackendName("unit_name");
      repeatPattern.getField("processName").setBackendName("process_name");
      repeatPattern.getField("tableName").setBackendName("table_name");
      repeatPattern.getField("fingerprint").setBackendName("fingerprint");
      repeatPattern.getField("sqlText").setBackendName("sql_text");
      repeatPattern.getField("unitsAffected").setBackendName("units_affected");
      repeatPattern.getField("maxRepeats").setBackendName("max_repeats");
      repeatPattern.getField("totalRepeats").setBackendName("total_repeats");
      repeatPattern.getField("sampleRate").setBackendName("sample_rate");
      repeatPattern.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
      repeatPattern.withSection(new QFieldSection("identity", new QIcon("table_rows"), Tier.T1, List.of("id", "windowStart", "windowEnd", "podName", "unitKind", "unitName", "processName")));
      repeatPattern.withSection(new QFieldSection("details", new QIcon("table_rows"), Tier.T2, List.of("tableName", "fingerprint", "sqlText", "unitsAffected", "maxRepeats", "totalRepeats", "sampleRate")));
      instance.addTable(repeatPattern);

      QTableMetaData finding = base(config, DbPerfFinding.TABLE_NAME, "Findings", DbPerfFinding.class);
      finding.withRecordLabelFormat("%s").withRecordLabelFields("title");
      finding.withUniqueKey(new UniqueKey("dedupeKey"));
      finding.getField("id").setBackendName("id");
      finding.getField("backendName").setBackendName("backend_name");
      finding.getField("ruleId").setBackendName("rule_id");
      finding.getField("ruleId").setPossibleValueSourceName("db_perf_rule");
      finding.getField("ruleCode").setBackendName("rule_code");
      finding.getField("dedupeKey").setBackendName("dedupe_key");
      finding.getField("status").setBackendName("status");
      finding.getField("status").setDefaultValue("OPEN");
      finding.getField("status").setPossibleValueSourceName("db_perf_FindingStatus");
      finding.getField("severity").setBackendName("severity");
      finding.getField("severity").setPossibleValueSourceName("db_perf_Severity");
      finding.getField("score").setBackendName("score");
      finding.getField("title").setBackendName("title");
      finding.getField("summary").setBackendName("summary");
      finding.getField("evidence").setBackendName("evidence");
      finding.getField("evidence").setType(QFieldType.TEXT);
      finding.getField("recommendation").setBackendName("recommendation");
      finding.getField("recommendation").setType(QFieldType.TEXT);
      finding.getField("migrationSnippet").setBackendName("migration_snippet");
      finding.getField("migrationSnippet").setType(QFieldType.TEXT);
      finding.getField("subjectTable").setBackendName("subject_table");
      finding.getField("statementId").setBackendName("statement_id");
      finding.getField("statementId").setPossibleValueSourceName("db_perf_statement");
      finding.getField("tableStatId").setBackendName("table_stat_id");
      finding.getField("tableStatId").setPossibleValueSourceName("db_perf_table_stat");
      finding.getField("indexStatId").setBackendName("index_stat_id");
      finding.getField("indexStatId").setPossibleValueSourceName("db_perf_index_stat");
      finding.getField("unitName").setBackendName("unit_name");
      finding.getField("firstSeenAt").setBackendName("first_seen_at");
      finding.getField("lastSeenAt").setBackendName("last_seen_at");
      finding.getField("occurrences").setBackendName("occurrences");
      finding.getField("resolvedAt").setBackendName("resolved_at");
      finding.getField("statusNote").setBackendName("status_note");
      finding.getField("statusChangedBy").setBackendName("status_changed_by");
      finding.getField("createDate").setBackendName("create_date");
      finding.getField("modifyDate").setBackendName("modify_date");
      finding.withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));
      finding.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
      finding.withSection(new QFieldSection("identity", new QIcon("table_rows"), Tier.T1, List.of("id", "backendName", "ruleId", "ruleCode", "dedupeKey", "status", "severity")));
      finding.withSection(new QFieldSection("details", new QIcon("table_rows"), Tier.T2, List.of("score", "title", "summary", "evidence", "recommendation", "migrationSnippet", "subjectTable", "statementId", "tableStatId", "indexStatId", "unitName", "firstSeenAt", "lastSeenAt", "occurrences", "resolvedAt", "statusNote", "statusChangedBy")));
      finding.withSection(new QFieldSection("system", new QIcon("table_rows"), Tier.T3, List.of("createDate", "modifyDate")));
      instance.addTable(finding);

      QTableMetaData explain = base(config, DbPerfExplain.TABLE_NAME, "Query Plans", DbPerfExplain.class);
      explain.withRecordLabelFormat("%s").withRecordLabelFields("requestedAt");
      explain.getField("id").setBackendName("id");
      explain.getField("statementId").setBackendName("statement_id");
      explain.getField("statementId").setPossibleValueSourceName("db_perf_statement");
      explain.getField("requestedBy").setBackendName("requested_by");
      explain.getField("requestedAt").setBackendName("requested_at");
      explain.getField("mode").setBackendName("mode");
      explain.getField("mode").setPossibleValueSourceName("db_perf_ExplainMode");
      explain.getField("sqlFingerprint").setBackendName("sql_fingerprint");
      explain.getField("sqlText").setBackendName("sql_text");
      explain.getField("sqlText").setType(QFieldType.TEXT);
      explain.getField("parameterCount").setBackendName("parameter_count");
      explain.getField("durationMs").setBackendName("duration_ms");
      explain.getField("outcome").setBackendName("outcome");
      explain.getField("outcome").setPossibleValueSourceName("db_perf_Outcome");
      explain.getField("errorMessage").setBackendName("error_message");
      explain.getField("planJson").setBackendName("plan_json");
      explain.getField("planJson").setType(QFieldType.TEXT);
      explain.getField("planText").setBackendName("plan_text");
      explain.getField("planText").setType(QFieldType.TEXT);
      explain.getField("totalCost").setBackendName("total_cost");
      explain.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
      explain.withSection(new QFieldSection("identity", new QIcon("table_rows"), Tier.T1, List.of("id", "statementId", "requestedBy", "requestedAt", "mode", "sqlFingerprint", "sqlText")));
      explain.withSection(new QFieldSection("details", new QIcon("table_rows"), Tier.T2, List.of("parameterCount", "durationMs", "outcome", "errorMessage", "planJson", "planText", "totalCost")));
      instance.addTable(explain);

      instance.addPossibleValueSource(source("db_perf_Provider", DbPerfPossibleValues.Provider.values()));
      instance.addPossibleValueSource(source("db_perf_StatsAccess", DbPerfPossibleValues.StatsAccess.values()));
      instance.addPossibleValueSource(source("db_perf_SnapshotStatus", DbPerfPossibleValues.SnapshotStatus.values()));
      instance.addPossibleValueSource(source("db_perf_Severity", DbPerfPossibleValues.Severity.values()));
      instance.addPossibleValueSource(source("db_perf_Operation", DbPerfPossibleValues.Operation.values()));
      instance.addPossibleValueSource(source("db_perf_UnitKind", DbPerfPossibleValues.UnitKind.values()));
      instance.addPossibleValueSource(source("db_perf_FindingStatus", DbPerfPossibleValues.FindingStatus.values()));
      instance.addPossibleValueSource(source("db_perf_ExplainMode", DbPerfPossibleValues.ExplainMode.values()));
      instance.addPossibleValueSource(source("db_perf_Outcome", DbPerfPossibleValues.Outcome.values()));
      instance.addPossibleValueSource(source("db_perf_Reason", DbPerfPossibleValues.Reason.values()));
      instance.addPossibleValueSource(source("db_perf_State", DbPerfPossibleValues.State.values()));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("db_perf_settings"));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("db_perf_rule"));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("db_perf_snapshot"));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("db_perf_statement"));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("db_perf_query_snapshot"));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("db_perf_query_daily"));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("db_perf_table_stat"));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("db_perf_table_snapshot"));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("db_perf_table_daily"));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("db_perf_index_stat"));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("db_perf_activity_sample"));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("db_perf_connection_sample"));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("db_perf_attribution"));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("db_perf_attribution_daily"));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("db_perf_repeat_pattern"));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("db_perf_finding"));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("db_perf_explain"));
      child(instance, "db_perf_snapshot", "db_perf_query_snapshot", "snapshotId");
      child(instance, "db_perf_statement", "db_perf_query_snapshot", "statementId");
      child(instance, "db_perf_statement", "db_perf_query_daily", "statementId");
      child(instance, "db_perf_snapshot", "db_perf_table_snapshot", "snapshotId");
      child(instance, "db_perf_table_stat", "db_perf_table_snapshot", "tableStatId");
      child(instance, "db_perf_table_stat", "db_perf_table_daily", "tableStatId");
      child(instance, "db_perf_table_stat", "db_perf_index_stat", "tableStatId");
      child(instance, "db_perf_snapshot", "db_perf_activity_sample", "snapshotId");
      child(instance, "db_perf_snapshot", "db_perf_connection_sample", "snapshotId");
      child(instance, "db_perf_rule", "db_perf_finding", "ruleId");
      child(instance, "db_perf_statement", "db_perf_finding", "statementId");
      child(instance, "db_perf_table_stat", "db_perf_finding", "tableStatId");
      child(instance, "db_perf_statement", "db_perf_explain", "statementId");
      QAppMetaData app = new QAppMetaData().withName(config.getAppName()).withLabel("Database Performance").withIcon(new QIcon("speed"))
         .withPermissionRules(DbPerfPermissions.rules(config.getPermissionBaseName() + ".view"))
         .withSection(new QAppSection().withName("data").withLabel("Data").withIcon(new QIcon("table_rows"))
            .withTables(List.of("db_perf_finding", "db_perf_statement", "db_perf_table_stat", "db_perf_index_stat", "db_perf_snapshot", "db_perf_settings", "db_perf_rule")));
      instance.addApp(app);
      if(config.getParentAppName() != null)
      {
         instance.getApp(config.getParentAppName()).withChild(app);
      }
   }


   /*******************************************************************************
    ** Common storage, permissions and write guard.
    *******************************************************************************/
   private static QTableMetaData base(DbPerfQBitConfig config, String name, String label, Class<? extends QRecordEntity> entity) throws QException
   {
      QTableMetaData table = new QTableMetaData().withName(name).withLabel(label).withIcon(new QIcon("speed"))
         .withBackendName(config.getBackendName()).withBackendDetails(new RDBMSTableBackendDetails().withTableName(name))
         .withPrimaryKeyField("id").withFieldsFromEntity(entity)
         .withPermissionRules(DbPerfPermissions.rules(config.getPermissionBaseName() + ".view"));
      table.getPermissionRules().setCustomPermissionChecker(new com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceWithProperties(DbPerfTablePermissionChecker.class,
         java.util.Map.of("permissionName", config.getPermissionBaseName() + ".view")));
      QInstanceEnricher.setInferredFieldBackendNames(table);
      for(TableCustomizers role : List.of(TableCustomizers.PRE_INSERT_RECORD, TableCustomizers.PRE_UPDATE_RECORD, TableCustomizers.PRE_DELETE_RECORD))
      {
         table.withCustomizer(role, new QCodeReference(DbPerfWriteGuard.class));
      }
      return (table);
   }


   /*******************************************************************************
    ** Enum-backed stored values.
    *******************************************************************************/
   private static QPossibleValueSource source(String name, PossibleValueEnum<String>[] values)
   {
      List<QPossibleValue<?>> choices = new ArrayList<>();
      for(PossibleValueEnum<String> value : values)
      {
         choices.add(new QPossibleValue<>(value.getPossibleValueId(), value.getPossibleValueLabel()));
      }
      return (new QPossibleValueSource().withName(name).withType(QPossibleValueSourceType.ENUM).withEnumValues(choices));
   }


   /*******************************************************************************
    ** A join and its record-screen child list.
    *******************************************************************************/
   private static void child(QInstance instance, String parent, String child, String field)
   {
      String name = parent + "_" + child;
      QJoinMetaData join = new QJoinMetaData().withName(name).withLeftTable(parent).withRightTable(child)
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", field));
      instance.addJoin(join);
      instance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(join).withLabel(instance.getTable(child).getLabel())
         .withMaxRows(25).withOrderBys(List.of(new QFilterOrderBy("id", false))).getWidgetMetaData()
         .withPermissionRules(instance.getTable(parent).getPermissionRules()));
      instance.getTable(parent).withSection(new QFieldSection(name, instance.getTable(child).getLabel(), new QIcon("list"), Tier.T2, null).withWidgetName(name));
   }
}
