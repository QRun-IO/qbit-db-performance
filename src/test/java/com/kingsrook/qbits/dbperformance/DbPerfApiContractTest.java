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


import java.nio.file.Files;
import java.nio.file.Path;
import com.kingsrook.qbits.dbperformance.api.DbPerformanceUiApi;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtensionsMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import com.kingsrook.qqq.middleware.javalin.tools.UiApiContract;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** The committed fragment is generated from exactly the registered UI specs.
 *******************************************************************************/
class DbPerfApiContractTest
{



   /*******************************************************************************
    ** Explicit regeneration flag; normal builds detect contract drift.
    *******************************************************************************/
   @Test
   void contract() throws Exception
   {
      var extension = DbPerformanceUiApi.extension();
      var version = new MiddlewareVersionV1();
      String generated = UiApiContract.generateExtensionYaml(version, extension);
      Path path = Path.of("src/main/resources/openapi/qqq-v1/db-performance.yaml");
      if(Boolean.getBoolean("dbperf.openapi.update"))
      {
         Files.createDirectories(path.getParent());
         Files.writeString(path, generated);
      }
      assertThat(UiApiContract.check(generated, Files.readString(path), null, 1, "mvnw -Dtest=DbPerfApiContractTest -Ddbperf.openapi.update=true test")).isEmpty();
      var model = version.generateExtensionOpenAPIModel("qqq", extension);
      assertThat(model.getComponents().getSchemas().keySet()).allMatch(name -> name.startsWith("DbPerformance") && name.endsWith("V1"));
      assertThat(model.getPaths().keySet()).containsExactlyInAnyOrder("/qqq/v1/qbit/db-performance/overview", "/qqq/v1/qbit/db-performance/activity",
         "/qqq/v1/qbit/db-performance/explain", "/qqq/v1/qbit/db-performance/statements/lookup");
      assertThat(extension.getSemanticVersion()).isEqualTo("1.0.0");
      assertThat(MiddlewareSpecExtensionsMetaData.of(DbPerfTestSupport.instance()).getExtensions()).containsOnlyKeys("db-performance");
      assertThat(generated).contains("liveTextRedacted", "parameters", "assistantPrompt", "403");
   }
}
