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


import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.openapi.model.HttpMethod;


/*******************************************************************************
 ** Database Performance activity under the authenticated UI API.
 *******************************************************************************/
public class ActivitySpec extends DbPerformanceJsonSpec
{



   /*******************************************************************************
    ** Method and path relative to /qqq/v1.
    *******************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation().withHttpMethod(HttpMethod.GET).withPath("/qbit/db-performance/activity")
         .withShortSummary("Database Performance activity").withLongDescription("Requires db_performance.view and uses the caller's session."));
   }



   /*******************************************************************************
    ** Versioned response shape.
    *******************************************************************************/
   @Override
   protected String getResponseSchemaName()
   {
      return ("DbPerformanceActivityV1");
   }



   /*******************************************************************************
    ** Execute in the caller's context; also directly testable without transport.
    *******************************************************************************/
   @Override
   public Object handle(JsonInput input) throws Exception
   {
      return (data().activity(input.getQueryParam("backend"), true));
   }
}
