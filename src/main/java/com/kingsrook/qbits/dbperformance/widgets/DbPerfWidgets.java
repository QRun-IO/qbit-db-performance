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


import java.util.List;
import com.kingsrook.qbits.dbperformance.DbPerfPermissions;
import com.kingsrook.qbits.dbperformance.DbPerfQBitConfig;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;


/*******************************************************************************
 ** Full-width typed home panels, with one panel per row and bounded refresh rates.
 *******************************************************************************/
public final class DbPerfWidgets
{
   public static final List<String> HOME = List.of("dbPerfSetup", "dbPerfDatabaseTime", "dbPerfTopQueries", "dbPerfFindings",
      "dbPerfLiveActivity", "dbPerfConnections", "dbPerfAttribution", "dbPerfTables");



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DbPerfWidgets()
   {
   }



   /*******************************************************************************
    ** Additive registration, preserving phase A record-child widgets and app sections.
    *******************************************************************************/
   public static void register(QInstance instance, DbPerfQBitConfig config)
   {
      add(instance, config, HOME.get(0), "Setup", WidgetType.TABLE, 60);
      add(instance, config, HOME.get(1), "Database time", WidgetType.LINE_CHART, 60);
      add(instance, config, HOME.get(2), "Top queries", WidgetType.TABLE, 60);
      add(instance, config, HOME.get(3), "Findings", WidgetType.TABLE, 60);
      add(instance, config, HOME.get(4), "Live activity", WidgetType.TABLE, 30);
      add(instance, config, HOME.get(5), "Connections", WidgetType.PARENT_WIDGET, 30);
      add(instance, config, "dbPerfConnectionStatistics", "Connection capacity", WidgetType.MULTI_STATISTICS, 30);
      add(instance, config, "dbPerfConnectionsByApplication", "Connections by application and user", WidgetType.TABLE, 30);
      add(instance, config, HOME.get(6), "QQQ attribution", WidgetType.MULTI_TABLE, 60);
      add(instance, config, HOME.get(7), "Tables", WidgetType.MULTI_TABLE, 60);
      instance.getApp(config.getAppName()).withWidgets(HOME);
   }



   /*******************************************************************************
    ** Permission and responsive width also apply when a widget is requested directly.
    *******************************************************************************/
   private static void add(QInstance instance, DbPerfQBitConfig config, String name, String label, WidgetType type, int refresh)
   {
      instance.addWidget(new QWidgetMetaData().withName(name).withLabel(label).withType(type.getType()).withGridColumns(12)
         .withCodeReference(new QCodeReference(DbPerfWidgetRenderer.class)).withDefaultValue("refreshSeconds", refresh)
         .withPermissionRules(DbPerfPermissions.rules(config.getPermissionBaseName() + ".view")));
   }
}
