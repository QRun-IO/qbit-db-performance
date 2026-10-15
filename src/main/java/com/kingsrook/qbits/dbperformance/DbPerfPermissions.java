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
import com.kingsrook.qqq.backend.core.actions.permissions.UseOtherPermissionNameCustomPermissionChecker;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;


/*******************************************************************************
 ** Stable public permission names for host role configuration.
 *******************************************************************************/
public final class DbPerfPermissions
{
   public static final String VIEW = "db_performance.view";
   public static final String VIEW_LIVE_TEXT = "db_performance.view_live_text";
   public static final String EXPLAIN = "db_performance.explain";
   public static final String EXPLAIN_ANALYZE = "db_performance.explain_analyze";
   public static final String MANAGE_FINDINGS = "db_performance.manage_findings";
   public static final String MANAGE_SETTINGS = "db_performance.manage_settings";
   public static final List<String> ALL = List.of(VIEW, VIEW_LIVE_TEXT, EXPLAIN, EXPLAIN_ANALYZE, MANAGE_FINDINGS, MANAGE_SETTINGS);


   /*******************************************************************************
    ** Constants only.
    *******************************************************************************/
   private DbPerfPermissions()
   {
   }


   /*******************************************************************************
    ** Require an exact permission name, without a generated suffix.
    *******************************************************************************/
   public static QPermissionRules rules(String permission)
   {
      return (new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION).withPermissionBaseName(permission)
         .withCustomPermissionChecker(UseOtherPermissionNameCustomPermissionChecker.build(permission)));
   }
}
