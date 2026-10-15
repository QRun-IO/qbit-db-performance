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


import com.kingsrook.qbits.dbperformance.metadata.DbPerfHelp;
import com.kingsrook.qbits.dbperformance.metadata.DbPerfMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles;
import com.kingsrook.qqq.backend.core.testing.HelpBundleAssertions;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Version, live references, summary quality and complete owned metadata coverage.
 *******************************************************************************/
class DbPerfHelpBundleTest
{



   /*******************************************************************************
    ** No host Help tables are needed for registration or empty-state guidance.
    *******************************************************************************/
   @Test
   void bundleCoverage() throws Exception
   {
      var instance = DbPerfTestSupport.instance();
      HelpBundleAssertions.forInstance(instance).bundles(DbPerfHelp.RESOURCE)
         .owning(target -> target.name().startsWith("db_perf_") || target.name().startsWith("dbPerf")).assertComplete();
      var bundle = HelpBundles.of(instance).getBundles().getFirst();
      assertThat(bundle.getVersion()).isEqualTo(projectVersion());
      var keys = bundle.getEntries().stream().map(entry -> entry.getKey()).toList();
      instance.getWidgets().keySet().forEach(name -> assertThat(keys).contains("widget:" + name + ";slot:label"));
      for(String table : DbPerfMetaData.TABLES)
      {
         assertThat(instance.getTable(table).getHelpContent()).containsKeys("help", "emptyState");
      }
      QHelpContent host = new QHelpContent().withContentAsMarkdown("Host-specific help");
      instance.getTable("db_perf_statement").withHelpContent("help", host);
      DbPerfHelp.register(instance, DbPerfRuntime.of(instance).getConfig());
      assertThat(instance.getTable("db_perf_statement").getHelpContent().get("help")).containsExactly(host);
   }



   /*******************************************************************************
    ** Compare to the effective Maven project version supplied to this test's resource.
    *******************************************************************************/
   private static String projectVersion() throws Exception
   {
      try(var stream = DbPerfHelpBundleTest.class.getResourceAsStream("/help/project-version.txt"))
      {
         return (new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim());
      }
   }
}
