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


import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.AbstractJsonEndpointSpec;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Method;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.HttpStatus;


/*******************************************************************************
 ** Common schemas and error contract, with no alternate route or permission pipeline.
 *******************************************************************************/
public abstract class DbPerformanceJsonSpec extends AbstractJsonEndpointSpec
{



   /*******************************************************************************
    ** Stable operation identifiers for generated clients.
    *******************************************************************************/
   @Override
   public Method defineMethod()
   {
      return (super.defineMethod().withOperationId("dbPerformance" + getClass().getSimpleName().replace("Spec", "")));
   }



   /*******************************************************************************
    ** Publish all nested response and request component schemas.
    *******************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return (DbPerformanceSchemas.all());
   }



   /*******************************************************************************
    ** Backend defaults to the qbit's single configured monitored backend for reads.
    *******************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return (List.of(new Parameter().withName("backend").withIn(In.QUERY).withRequired(false)
         .withSchema(new Schema().withType(Type.STRING)).withDescription("Configured monitored backend; defaults to the qbit backend.")));
   }



   /*******************************************************************************
    ** Framework exceptions map to the same status in every UI client.
    *******************************************************************************/
   @Override
   public List<BasicResponse> defineAdditionalBasicResponses()
   {
      return (List.of(new BasicResponse(HttpStatus.BAD_REQUEST, "Invalid request", "DbPerformanceErrorV1"),
         new BasicResponse(HttpStatus.UNAUTHORIZED, "Sign in required", "DbPerformanceErrorV1"),
         new BasicResponse(HttpStatus.FORBIDDEN, "Permission denied", "DbPerformanceErrorV1")));
   }



   /*******************************************************************************
    ** Resolve per-request runtime from the authenticated QContext.
    *******************************************************************************/
   protected DbPerformanceData data()
   {
      return (new DbPerformanceData(DbPerfRuntime.of(QContext.getQInstance())));
   }
}
