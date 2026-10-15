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

package com.kingsrook.qbits.dbperformance.api;


import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtension;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtensionsMetaData;


/*******************************************************************************
 ** Discoverable UI API extension, never raw Javalin routes.
 *******************************************************************************/
public final class DbPerformanceUiApi
{



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DbPerformanceUiApi()
   {
   }



   /*******************************************************************************
    ** Add the versioned extension once per host instance.
    *******************************************************************************/
   public static void register(QInstance instance)
   {
      MiddlewareSpecExtensionsMetaData.ofOrWithNew(instance).withExtension(extension());
   }



   /*******************************************************************************
    ** Public contract used for routing, discovery and committed OpenAPI generation.
    *******************************************************************************/
   public static MiddlewareSpecExtension extension()
   {
      return (new MiddlewareSpecExtension().withName("db-performance").withSemanticVersion("1.0.0")
         .withDescription("Database performance, live activity and audited query plans")
         .withSpec(OverviewSpec.class).withSpec(ActivitySpec.class).withSpec(ExplainSpec.class).withSpec(LookupSpec.class));
   }
}
