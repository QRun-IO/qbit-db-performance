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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.collect.DbPerfSettingsService;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.provider.DbStatsProviders;
import com.kingsrook.qbits.dbperformance.provider.StatsFeature;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Each rule has its own failure boundary and atomic finding reconciliation.
 *******************************************************************************/
public class AnalyzeService
{
   private final DbPerfRuntime runtime;
   private final Clock clock;
   private final List<DbPerfRule> rules;


   /*******************************************************************************
    ** All seeded rules, using the production clock.
    *******************************************************************************/
   public AnalyzeService(DbPerfRuntime runtime)
   {
      this(runtime, Clock.systemUTC(), Arrays.asList(StoredRule.values()));
   }


   /*******************************************************************************
    ** Explicit rules permit isolation tests and future provider-independent rules.
    *******************************************************************************/
   public AnalyzeService(DbPerfRuntime runtime, Clock clock, List<DbPerfRule> rules)
   {
      this.runtime = runtime;
      this.clock = clock;
      this.rules = List.copyOf(rules);
   }


   /*******************************************************************************
    ** Error text is sanitized so no database exception leaks SQL or literals.
    *******************************************************************************/
   public record Result(String ruleCode, FindingService.Counts counts, String error)
   {
   }


   /*******************************************************************************
    ** Disabled rules, and rules whose evidence the backend's provider cannot
    ** supply (unknown before the first collection), are neither evaluated nor reconciled.
    *******************************************************************************/
   public List<Result> run(String backend) throws Exception
   {
      if(!runtime.getConfig().getBackendName().equals(backend))
      {
         throw (new IllegalArgumentException("Backend is not configured for this runtime"));
      }
      QRecord settings = new DbPerfSettingsService(runtime).ensure(backend);
      String provider = settings.getValueString("provider");
      Set<StatsFeature> available = provider == null ? null : DbStatsProviders.forProviderName(provider).features();
      List<Result> results = new ArrayList<>();
      for(DbPerfRule rule : rules)
      {
         if(available != null && !available.containsAll(rule.requires()))
         {
            continue;
         }
         try
         {
            QRecord row = runtime.asSystem(() -> DbPerfStore.first("db_perf_rule", DbPerfStore.eq("code", rule.code())));
            if(row == null || !Boolean.TRUE.equals(row.getValueBoolean("enabled")))
            {
               continue;
            }
            List<FindingDraft> drafts = runtime.asSystem(() -> rule.evaluate(new RuleContext(runtime, backend, row, settings, clock.instant())));
            results.add(new Result(rule.code(), new FindingService(runtime, clock).apply(backend, rule.code(), drafts), null));
         }
         catch(Exception e)
         {
            results.add(new Result(rule.code(), new FindingService.Counts(0, 0, 0), "Evaluation failed (" + e.getClass().getSimpleName() + ")"));
         }
      }
      return (results);
   }
}
