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

package com.kingsrook.qbits.dbperformance.health;


import java.time.Clock;
import java.time.Duration;
import java.util.List;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.analyze.RuleContext;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.provider.DbStatsProviders;
import com.kingsrook.qqq.backend.core.health.HealthCategory;
import com.kingsrook.qqq.backend.core.health.HealthCheckKind;
import com.kingsrook.qqq.backend.core.health.HealthCheckMetaData;
import com.kingsrook.qqq.backend.core.health.HealthCheckOutcome;
import com.kingsrook.qqq.backend.core.health.HealthChecksMetaData;
import com.kingsrook.qqq.backend.core.health.HealthDetails;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;


/*******************************************************************************
 ** Passive core health checks. No live database probes and no SQL in details.
 *******************************************************************************/
public class DbPerfHealthChecks
{
   private final DbPerfRuntime runtime;
   private final Clock clock;


   /*******************************************************************************
    ** Bind observations to this runtime and clock.
    *******************************************************************************/
   public DbPerfHealthChecks(DbPerfRuntime runtime, Clock clock)
   {
      this.runtime = runtime;
      this.clock = clock;
   }


   /*******************************************************************************
    ** Register the three central database dependency checks.
    *******************************************************************************/
   public static void register(QInstance instance)
   {
      DbPerfHealthChecks checks = new DbPerfHealthChecks(DbPerfRuntime.of(instance), Clock.systemUTC());
      for(String name : List.of("collector", "stats_access", "connections"))
      {
         HealthChecksMetaData.ofOrWithNew(instance).withCheck(new HealthCheckMetaData().withName("db_performance." + name)
            .withLabel("Database performance " + name.replace('_', ' ')).withComponent("db_performance")
            .withCategory(HealthCategory.DATABASE).withKind(HealthCheckKind.DEPENDENCY).withIntervalSeconds(60).withTimeoutMs(5000)
            .withCheckInstance(input -> checks.check(name)));
      }
   }


   /*******************************************************************************
    ** Errors expose only a remedy, never a JDBC exception or query text.
    *******************************************************************************/
   public HealthCheckOutcome check(String name)
   {
      try
      {
         return (runtime.asSystem(() -> evaluate(name)));
      }
      catch(Exception e)
      {
         return (HealthCheckOutcome.down("Unable to read Database Performance health; check connectivity and migrations"));
      }
   }


   /*******************************************************************************
    ** Missing settings never cause a write during a health request.
    *******************************************************************************/
   private HealthCheckOutcome evaluate(String name) throws Exception
   {
      String backend = runtime.getConfig().getBackendName();
      QRecord settings = DbPerfStore.first("db_perf_settings", DbPerfStore.eq("backendName", backend));
      if(settings == null || !Boolean.TRUE.equals(settings.getValueBoolean("enabled")))
      {
         return (HealthCheckOutcome.unknown("Database performance collection is disabled or not configured"));
      }
      if(name.equals("stats_access"))
      {
         String access = settings.getValueString("statsAccess");
         if(access == null)
         {
            return (HealthCheckOutcome.unknown("No statistics access observation yet"));
         }
         String user = ((RDBMSBackendMetaData) runtime.getInstance().getBackend(backend)).getUsername();
         String remedy = DbStatsProviders.forProviderName(settings.getValueString("provider")).remedy(access, user);
         HealthCheckOutcome outcome = switch(access)
         {
            case "FULL" -> HealthCheckOutcome.up("Full statistics access");
            case "NO_READ_ALL_STATS", "NOT_ADMIN", "NO_QUERY_STATISTICS" -> HealthCheckOutcome.degraded(remedy);
            case "NO_PG_STAT_STATEMENTS", "UNSUPPORTED" -> HealthCheckOutcome.down(remedy);
            default -> HealthCheckOutcome.unknown("Statistics access has not been classified");
         };
         String safe = List.of("FULL", "NO_READ_ALL_STATS", "NO_PG_STAT_STATEMENTS", "NO_QUERY_STATISTICS", "NOT_ADMIN", "UNSUPPORTED").contains(access) ? access : "UNKNOWN";
         return (outcome.withDetails(new HealthDetails().withValue("statsAccess", safe)));
      }
      var filter = DbPerfStore.eq("backendName", backend).withOrderBy(new QFilterOrderBy("capturedAt", false));
      if(name.equals("collector"))
      {
         filter.withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, "OK"));
      }
      QRecord snapshot = DbPerfStore.first("db_perf_snapshot", filter);
      if(snapshot == null)
      {
         return (name.equals("collector") ? HealthCheckOutcome.down("No successful snapshot; run Collect now and check statistics access") : HealthCheckOutcome.unknown("No connection sample yet"));
      }
      if(name.equals("collector"))
      {
         long age = Math.max(0, Duration.between(snapshot.getValueInstant("capturedAt"), clock.instant()).getSeconds());
         double interval = Math.max(1, RuleContext.number(settings, "snapshotIntervalMinutes")) * 60;
         HealthCheckOutcome outcome = age <= 3 * interval ? HealthCheckOutcome.up("Collector is current")
            : age <= 6 * interval ? HealthCheckOutcome.degraded("Collector is delayed") : HealthCheckOutcome.down("Collector is stale; check the collection schedule");
         return (outcome.withDetails(new HealthDetails().withValue("ageSeconds", age).withValue("intervalSeconds", interval)));
      }
      double maximum = RuleContext.number(snapshot, "maxConnections");
      if(maximum <= 0 || snapshot.getValue("connectionsTotal") == null)
      {
         return (HealthCheckOutcome.unknown("Latest snapshot has no connection capacity measurement"));
      }
      double used = 100 * RuleContext.number(snapshot, "connectionsTotal") / maximum;
      HealthCheckOutcome outcome = used >= 95 ? HealthCheckOutcome.down("Connection usage is at least 95%")
         : used >= RuleContext.number(settings, "connectionAlertPercent") ? HealthCheckOutcome.degraded("Connection usage reached the alert threshold") : HealthCheckOutcome.up("Connection capacity is available");
      return (outcome.withDetails(new HealthDetails().withValue("usedPercent", used).withValue("connectionsTotal", snapshot.getValueInteger("connectionsTotal")).withValue("maxConnections", snapshot.getValueInteger("maxConnections"))));
   }
}
