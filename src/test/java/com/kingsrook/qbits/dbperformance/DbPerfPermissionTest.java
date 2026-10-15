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


import java.util.List;
import com.kingsrook.qbits.dbperformance.collect.DbPerfSettingsService;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Exact read/write permissions enforced by core actions and write customizers.
 *******************************************************************************/
class DbPerfPermissionTest
{
   /*******************************************************************************
    ** Viewers can read configuration but cannot mutate it or runtime tables.
    *******************************************************************************/
   @Test
   void viewAndManageAreDistinct() throws Exception
   {
      QInstance instance = DbPerfTestSupport.instance();
      instance.setEnforcePermissionsInCoreActions(true);
      DbPerfRuntime runtime = DbPerfRuntime.of(instance);
      QRecord settings = new DbPerfSettingsService(runtime).ensure(runtime.getConfig().getBackendName());
      QContext.setQSession(new QSession().withPermissions(DbPerfPermissions.VIEW));
      assertThat(DbPerfStore.query("db_perf_settings", new QQueryFilter())).hasSize(1);
      QRecord change = new QRecord().withValue("id", settings.getValueInteger("id")).withValue("topStatementsPerSnapshot", 7);
      assertThatThrownBy(() -> DbPerfStore.update("db_perf_settings", List.of(change))).hasMessageContaining("permission");
      QContext.setQSession(new QSession().withPermissions(DbPerfPermissions.VIEW, DbPerfPermissions.MANAGE_SETTINGS));
      DbPerfStore.update("db_perf_settings", List.of(change));
      assertThat(new DbPerfSettingsService(runtime).ensure(runtime.getConfig().getBackendName()).getValueInteger("topStatementsPerSnapshot")).isEqualTo(7);
      assertThatThrownBy(() -> DbPerfStore.insert("db_perf_settings", List.of(new QRecord().withValue("backendName", "other")))).hasMessageContaining("permission");
      assertThatThrownBy(() -> DbPerfStore.insert("db_perf_snapshot", List.of(new QRecord().withValue("capturedAt", java.time.Instant.now())))).hasMessageContaining("permission");
      QRecord rule = DbPerfStore.insert("db_perf_rule", List.of(new QRecord().withValue("code", "CUSTOM").withValue("label", "Custom rule"))).getFirst();
      DbPerfStore.update("db_perf_rule", List.of(new QRecord().withValue("id", rule.getValueInteger("id")).withValue("enabled", false)));
      DbPerfStore.delete("db_perf_rule", DbPerfStore.eq("id", rule.getValueInteger("id")));
      assertThat(PermissionsHelper.getAllAvailablePermissions(instance)).extracting(permission -> permission.getName()).contains(DbPerfPermissions.VIEW, DbPerfPermissions.MANAGE_SETTINGS)
         .doesNotContain("db_performance.view.hasAccess", "db_performance.read", "db_performance.write");
      QContext.setQSession(new QSession());
      assertThatThrownBy(() -> DbPerfStore.query("db_perf_statement", new QQueryFilter())).hasMessageContaining("Permission");
   }


   /*******************************************************************************
    ** Runtime-only writes stay guarded when hosts disable core enforcement.
    *******************************************************************************/
   @Test
   void guardWithoutCoreEnforcement() throws Exception
   {
      QInstance instance = DbPerfTestSupport.instance();
      instance.setEnforcePermissionsInCoreActions(false);
      DbPerfRuntime runtime = DbPerfRuntime.of(instance);
      QRecord settings = new DbPerfSettingsService(runtime).ensure(runtime.getConfig().getBackendName());
      QContext.setQSession(new QSession().withPermissions(DbPerfPermissions.VIEW));
      assertThatThrownBy(() -> DbPerfStore.update("db_perf_settings", List.of(new QRecord().withValue("id", settings.getValueInteger("id")).withValue("enabled", false))))
         .hasMessageContaining("record write failed");
      assertThatThrownBy(() -> DbPerfStore.insert("db_perf_statement", List.of(new QRecord().withValue("queryId", 1L)))).hasMessageContaining("record write failed");
   }
}
