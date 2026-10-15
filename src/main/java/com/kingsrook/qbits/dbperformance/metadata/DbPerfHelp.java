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


import com.kingsrook.qbits.dbperformance.DbPerfQBitConfig;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundle;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundleEntry;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles;


/*******************************************************************************
 ** Data-driven app/table help, also visible when the host has no help-content tables.
 *******************************************************************************/
public final class DbPerfHelp
{
   public static final String RESOURCE = "help/qbit-db-performance.yaml";



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DbPerfHelp()
   {
   }



   /*******************************************************************************
    ** Register the Maven-versioned bundle and fill only unclaimed help slots.
    *******************************************************************************/
   public static void register(QInstance instance, DbPerfQBitConfig config)
   {
      HelpBundles.register(instance, RESOURCE);
      HelpBundle bundle = HelpBundles.of(instance).getBundles().getLast();
      for(HelpBundleEntry entry : bundle.getEntries())
      {
         String key = entry.getKey();
         if(key.startsWith("app:dbPerformance;"))
         {
            entry.setKey(key.replace("app:dbPerformance;", "app:" + config.getAppName() + ";"));
         }
         if(key.startsWith("table:") && key.contains(";slot:"))
         {
            String name = key.substring(6, key.indexOf(';'));
            String slot = key.substring(key.indexOf(";slot:") + 6);
            var table = instance.getTable(name);
            if(table != null && (table.getHelpContent() == null || !table.getHelpContent().containsKey(slot)))
            {
               table.withHelpContent(slot, content(entry));
            }
         }
         else if(key.equals("app:dbPerformance;slot:help"))
         {
            var app = instance.getApp(config.getAppName());
            if(app.getHelpContent() == null || !app.getHelpContent().containsKey("help"))
            {
               app.withHelpContent("help", content(entry));
            }
         }
      }
   }



   /*******************************************************************************
    ** Render a concise summary followed by the full data-driven markdown explanation.
    *******************************************************************************/
   private static QHelpContent content(HelpBundleEntry entry)
   {
      return (new QHelpContent().withContentAsMarkdown(entry.getSummary() + "\n\n" + (entry.getContent() == null ? "" : entry.getContent())));
   }
}
