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
import java.time.ZoneOffset;
import java.util.List;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.health.DbPerfHealthChecks;
import com.kingsrook.qqq.backend.core.health.HealthCategory;
import com.kingsrook.qqq.backend.core.health.HealthCheckKind;
import com.kingsrook.qqq.backend.core.health.HealthChecksMetaData;
import com.kingsrook.qqq.backend.core.health.HealthStatus;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Passive health boundaries, metadata registration and typed detail guarantees.
 *******************************************************************************/
class DbPerfHealthTest
{
   /*******************************************************************************
    ** Collector boundaries are inclusive at three and six intervals.
    *******************************************************************************/
   @Test
   void collectorAgeAndDisabled() throws Exception
   {
      B1Fixture f = new B1Fixture();
      assertThat(checks(f, 900).check("collector").getStatus()).isEqualTo(HealthStatus.UP);
      assertThat(checks(f, 901).check("collector").getStatus()).isEqualTo(HealthStatus.DEGRADED);
      assertThat(checks(f, 1800).check("collector").getStatus()).isEqualTo(HealthStatus.DEGRADED);
      assertThat(checks(f, 1801).check("collector").getStatus()).isEqualTo(HealthStatus.DOWN);
      DbPerfStore.update("db_perf_snapshot", List.of(new QRecord().withValue("id", f.snapshot.getValueInteger("id")).withValue("status", "FAILED")));
      assertThat(checks(f, 0).check("collector").getStatus()).isEqualTo(HealthStatus.DOWN);
      DbPerfStore.update("db_perf_settings", List.of(new QRecord().withValue("id", f.settings.getValueInteger("id")).withValue("enabled", false)));
      assertThat(checks(f, 0).check("collector").getStatus()).isEqualTo(HealthStatus.UNKNOWN);
      assertThat(HealthChecksMetaData.ofOrWithNew(f.runtime.getInstance()).getChecks()).hasSize(3).allSatisfy(metadata ->
      {
         assertThat(metadata.getComponent()).isEqualTo("db_performance");
         assertThat(metadata.getCategory()).isEqualTo(HealthCategory.DATABASE);
         assertThat(metadata.getKind()).isEqualTo(HealthCheckKind.DEPENDENCY);
         assertThat(metadata.getIntervalSeconds()).isBetween(60, 300);
         assertThat(metadata.getCheckInstance().run(null).getStatus()).isEqualTo(HealthStatus.UNKNOWN);
      });
   }


   /*******************************************************************************
    ** Access remedies and connection capacity reflect persisted data only.
    *******************************************************************************/
   @Test
   void accessAndCapacity() throws Exception
   {
      B1Fixture f = new B1Fixture();
      DbPerfHealthChecks checks = checks(f, 0);
      for(String access : List.of("FULL", "NO_READ_ALL_STATS", "NO_PG_STAT_STATEMENTS", "UNSUPPORTED", "UNKNOWN"))
      {
         DbPerfStore.update("db_perf_settings", List.of(new QRecord().withValue("id", f.settings.getValueInteger("id")).withValue("statsAccess", access)));
         var outcome = checks.check("stats_access");
         assertThat(outcome.getDetails().getValues()).containsEntry("statsAccess", access).hasSize(1);
         assertThat(outcome.getStatus()).isEqualTo(switch(access)
         {
            case "FULL" -> HealthStatus.UP;
            case "NO_READ_ALL_STATS" -> HealthStatus.DEGRADED;
            case "UNKNOWN" -> HealthStatus.UNKNOWN;
            default -> HealthStatus.DOWN;
         });
         if(access.equals("NO_READ_ALL_STATS"))
         {
            assertThat(outcome.getMessage()).isEqualTo("grant pg_read_all_stats to sa");
         }
      }
      for(int total : List.of(79, 80, 94, 95))
      {
         DbPerfStore.update("db_perf_snapshot", List.of(new QRecord().withValue("id", f.snapshot.getValueInteger("id")).withValue("connectionsTotal", total)));
         assertThat(checks.check("connections").getStatus()).isEqualTo(total < 80 ? HealthStatus.UP : total < 95 ? HealthStatus.DEGRADED : HealthStatus.DOWN);
         assertThat(checks.check("connections").getDetails().getValues().values()).allSatisfy(v -> assertThat(v).isInstanceOf(Number.class));
      }
      DbPerfStore.update("db_perf_snapshot", List.of(new QRecord().withValue("id", f.snapshot.getValueInteger("id")).withValue("maxConnections", null)));
      assertThat(checks.check("connections").getStatus()).isEqualTo(HealthStatus.UNKNOWN);
   }


   /*******************************************************************************
    ** A controllable clock avoids waiting for collection intervals in tests.
    *******************************************************************************/
   private static DbPerfHealthChecks checks(B1Fixture f, int seconds)
   {
      return (new DbPerfHealthChecks(f.runtime, Clock.fixed(B1Fixture.NOW.plusSeconds(seconds), ZoneOffset.UTC)));
   }
}
