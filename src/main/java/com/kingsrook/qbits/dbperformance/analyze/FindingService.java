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


import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryLockMode;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;


/*******************************************************************************
 ** Atomic reconciliation of one rule. Human acknowledgement and ignore survive
 ** recurrence; only resolved findings reopen. Disabled rules are entirely inert.
 *******************************************************************************/
public class FindingService
{
   private final DbPerfRuntime runtime;
   private final Clock clock;


   /*******************************************************************************
    ** Production clock.
    *******************************************************************************/
   public FindingService(DbPerfRuntime runtime)
   {
      this(runtime, Clock.systemUTC());
   }


   /*******************************************************************************
    ** Testable observation time.
    *******************************************************************************/
   public FindingService(DbPerfRuntime runtime, Clock clock)
   {
      this.runtime = runtime;
      this.clock = clock;
   }


   /*******************************************************************************
    ** Counts describe actual writes, not the size of the input list.
    *******************************************************************************/
   public record Counts(int created, int updated, int resolved)
   {
   }


   /*******************************************************************************
    ** Validate scope before any mutations; a failed rule never clears findings.
    *******************************************************************************/
   public Counts apply(String backend, String ruleCode, List<FindingDraft> drafts) throws Exception
   {
      if(!runtime.getConfig().getBackendName().equals(backend))
      {
         throw (new IllegalArgumentException("Backend is not configured for this runtime"));
      }
      synchronized(runtime)
      {
         return (runtime.transaction(() -> reconcile(backend, ruleCode, drafts)));
      }
   }


   /*******************************************************************************
    ** Serialize analyzer and human status writes across host instances. A busy
    ** rule reports a retryable error instead of overwriting a concurrent Ignore.
    ** Call inside the runtime transaction and synchronized(runtime) boundary.
    *******************************************************************************/
   public static QRecord lockRule(DbPerfRuntime runtime, String code) throws Exception
   {
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) runtime.getInstance().getBackend(runtime.getConfig().getBackendName());
      if(backend.getActionStrategy().supportsForUpdateSkipLocked())
      {
         List<QRecord> rows = new QueryAction().execute(new QueryInput("db_perf_rule").withFilter(DbPerfStore.eq("code", code))
            .withTransaction(QContext.getQBackendTransaction()).withLockMode(QueryLockMode.FOR_UPDATE_SKIP_LOCKED)).getRecords();
         if(rows.isEmpty() && DbPerfStore.first("db_perf_rule", DbPerfStore.eq("code", code)) != null)
         {
            throw (new IllegalStateException("Rule is being updated; retry analysis or the finding action"));
         }
         return (rows.isEmpty() ? null : rows.getFirst());
      }
      return (DbPerfStore.first("db_perf_rule", DbPerfStore.eq("code", code)));
   }


   /*******************************************************************************
    ** Re-read the enabled flag in the transaction, preserving administrator edits.
    *******************************************************************************/
   private Counts reconcile(String backend, String ruleCode, List<FindingDraft> drafts) throws Exception
   {
      QRecord rule = lockRule(runtime, ruleCode);
      if(rule == null || !Boolean.TRUE.equals(rule.getValueBoolean("enabled")))
      {
         return (new Counts(0, 0, 0));
      }
      Map<String, QRecord> existing = new HashMap<>();
      for(QRecord row : RuleContext.read("db_perf_finding", DbPerfStore.eq("backendName", backend)
         .withCriteria(new QFilterCriteria("ruleCode", QCriteriaOperator.EQUALS, ruleCode))))
      {
         existing.put(row.getValueString("dedupeKey"), row);
      }
      Instant now = clock.instant();
      Set<String> seen = new HashSet<>();
      int created = 0;
      int updated = 0;
      int resolved = 0;
      for(FindingDraft draft : drafts)
      {
         if(!ruleCode.equals(draft.ruleCode()) || !draft.dedupeKey().startsWith(ruleCode + ":" + backend + ":") || !seen.add(draft.dedupeKey()))
         {
            throw (new IllegalArgumentException("Invalid or duplicate finding identity"));
         }
         QRecord old = existing.get(draft.dedupeKey());
         QRecord row = draft.record().withValue("backendName", backend).withValue("ruleId", rule.getValueInteger("id"))
            .withValue("lastSeenAt", now);
         if(old == null)
         {
            row.withValue("status", "OPEN").withValue("firstSeenAt", now).withValue("occurrences", 1);
            DbPerfStore.insert("db_perf_finding", List.of(row));
            created++;
         }
         else
         {
            row.withValue("id", old.getValueInteger("id")).withValue("occurrences", (int) RuleContext.number(old, "occurrences") + 1);
            if("RESOLVED".equals(old.getValueString("status")))
            {
               row.withValue("status", "OPEN").withValue("resolvedAt", null).withValue("statusNote", "Detected again").withValue("statusChangedBy", null);
            }
            DbPerfStore.update("db_perf_finding", List.of(row));
            updated++;
         }
      }
      for(QRecord old : existing.values())
      {
         if(!seen.contains(old.getValueString("dedupeKey")) && List.of("OPEN", "ACKNOWLEDGED").contains(old.getValueString("status")))
         {
            DbPerfStore.update("db_perf_finding", List.of(new QRecord().withValue("id", old.getValueInteger("id"))
               .withValue("status", "RESOLVED").withValue("statusNote", "No longer detected").withValue("resolvedAt", now).withValue("statusChangedBy", null)));
            resolved++;
         }
      }
      return (new Counts(created, updated, resolved));
   }
}
