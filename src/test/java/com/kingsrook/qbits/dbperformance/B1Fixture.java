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


import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import com.kingsrook.qbits.dbperformance.analyze.RuleContext;
import com.kingsrook.qbits.dbperformance.analyze.StoredRule;
import com.kingsrook.qbits.dbperformance.collect.DbPerfSettingsService;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Real H2/QRecord fixtures shared by rule, alert and action integration tests.
 *******************************************************************************/
class B1Fixture
{
   static final Instant NOW = Instant.parse("2026-10-15T15:00:00Z");
   static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
   final DbPerfRuntime runtime;
   final String backend;
   final QRecord settings;
   final QRecord snapshot;
   final QRecord table;
   final QRecord statement;


   /*******************************************************************************
    ** Evidence crossing every seeded threshold without a live statistics probe.
    *******************************************************************************/
   B1Fixture() throws Exception
   {
      runtime = DbPerfRuntime.of(DbPerfTestSupport.instance());
      backend = runtime.getConfig().getBackendName();
      settings = new DbPerfSettingsService(runtime).ensure(backend);
      snapshot = insert("snapshot", new QRecord().withValue("backendName", backend).withValue("capturedAt", NOW).withValue("status", "OK")
         .withValue("rawStatsReset", NOW.minusSeconds(30 * 86400L)).withValue("cacheHitRatio", 90).withValue("deadlocks", 2L)
         .withValue("connectionsTotal", 90).withValue("connectionsIdle", 60).withValue("maxConnections", 100).withValue("tempBytes", 200 * 1048576L));
      table = insert("table_stat", new QRecord().withValue("backendName", backend).withValue("schemaName", "public").withValue("tableName", "audit_orders")
         .withValue("lastSeenAt", NOW).withValue("seqScanShare", 80).withValue("seqScan", 800L).withValue("idxScan", 200L)
         .withValue("nTupIns", 200000L).withValue("nDeadTup", 30000L).withValue("nLiveTup", 70000L).withValue("deadRatio", 30)
         .withValue("lastVacuum", NOW.minusSeconds(10 * 86400L)).withValue("freezeAge", 160000000L));
      insert("table_snapshot", new QRecord().withValue("snapshotId", snapshot.getValueInteger("id")).withValue("tableStatId", table.getValueInteger("id"))
         .withValue("capturedAt", NOW).withValue("seqTupRead", 100000L).withValue("nTupIns", 10000L));
      statement = insert("statement", new QRecord().withValue("backendName", backend).withValue("queryId", 1L).withValue("dbUser", "app").withValue("toplevel", true)
         .withValue("fingerprint", "a".repeat(64)).withValue("queryText", "select * from audit_orders where customer_id = ? and status in (?)")
         .withValue("mainTable", "audit_orders").withValue("lastSeenAt", NOW).withValue("calls24h", 100L).withValue("meanMs24h", 600)
         .withValue("totalMs24h", 60000).withValue("shareOfDbTime24h", 25));
      insert("query_snapshot", new QRecord().withValue("snapshotId", snapshot.getValueInteger("id")).withValue("statementId", statement.getValueInteger("id"))
         .withValue("capturedAt", NOW).withValue("totalMs", 10000).withValue("tempBlksWritten", 150 * 128L));
      index("idx_customer", "customer_id", false, 0);
      index("idx_customer_copy", "customer_id", false, 0);
      index("idx_customer_wide", "customer_id, created_at", false, 20);
      insert("repeat_pattern", new QRecord().withValue("windowStart", NOW.minusSeconds(60)).withValue("windowEnd", NOW).withValue("unitKind", "PROCESS")
         .withValue("unitName", "shipOrders").withValue("tableName", "orders").withValue("fingerprint", "a".repeat(64))
         .withValue("maxRepeats", 20).withValue("unitsAffected", 3L).withValue("totalRepeats", 60L));
      insert("repeat_pattern", new QRecord().withValue("windowStart", NOW.minusSeconds(60)).withValue("windowEnd", NOW).withValue("unitKind", "PROCESS")
         .withValue("unitName", "shipOrders").withValue("tableName", "orders").withValue("fingerprint", "a".repeat(64))
         .withValue("maxRepeats", 10).withValue("unitsAffected", 3L).withValue("totalRepeats", 30L));
      insert("attribution", new QRecord().withValue("windowStart", NOW.minusSeconds(60)).withValue("windowEnd", NOW).withValue("unitKind", "PROCESS")
         .withValue("unitName", "shipOrders").withValue("tableName", "audit_orders").withValue("operation", "INSERT").withValue("totalMs", 1000).withValue("statements", 300L));
      insert("connection_sample", new QRecord().withValue("snapshotId", snapshot.getValueInteger("id")).withValue("capturedAt", NOW)
         .withValue("applicationName", "orders-app").withValue("state", "active").withValue("connectionCount", 30));
      insert("connection_sample", new QRecord().withValue("snapshotId", snapshot.getValueInteger("id")).withValue("capturedAt", NOW)
         .withValue("applicationName", "orders-app").withValue("state", "idle").withValue("connectionCount", 60));
      insert("activity_sample", new QRecord().withValue("snapshotId", snapshot.getValueInteger("id")).withValue("capturedAt", NOW)
         .withValue("applicationName", "orders-app").withValue("reason", "IDLE_IN_TRANSACTION").withValue("xactSeconds", 120));
   }


   /*******************************************************************************
    ** Consistent index metadata with collected definition.
    *******************************************************************************/
   QRecord index(String name, String columns, boolean unique, long scans) throws Exception
   {
      return (insert("index_stat", new QRecord().withValue("backendName", backend).withValue("schemaName", "public").withValue("tableName", "audit_orders")
         .withValue("tableStatId", table.getValueInteger("id")).withValue("indexName", name).withValue("columns", columns)
         .withValue("definition", "CREATE " + (unique ? "UNIQUE " : "") + "INDEX " + name + " ON public.audit_orders USING btree (" + columns + ")")
         .withValue("isValid", true).withValue("isUnique", unique).withValue("isPrimary", false).withValue("idxScan", scans).withValue("sizeBytes", 2 * 1048576L).withValue("lastSeenAt", NOW)));
   }


   /*******************************************************************************
    ** A fresh context observes edits to threshold rows on every call.
    *******************************************************************************/
   RuleContext context(StoredRule rule) throws Exception
   {
      return (new RuleContext(runtime, backend, DbPerfStore.first("db_perf_rule", DbPerfStore.eq("code", rule.code())), settings, NOW));
   }


   /*******************************************************************************
    ** Use normal QQQ inserts and return the generated identity.
    *******************************************************************************/
   static QRecord insert(String suffix, QRecord row) throws Exception
   {
      return (DbPerfStore.insert("db_perf_" + suffix, List.of(row)).getFirst());
   }
}
