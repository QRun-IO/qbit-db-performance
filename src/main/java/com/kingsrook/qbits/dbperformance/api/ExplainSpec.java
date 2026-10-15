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


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.explain.ExplainRequest;
import com.kingsrook.qbits.dbperformance.explain.ExplainService;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;


/*******************************************************************************
 ** Audited plan execution. Service denial is a core permission exception (HTTP 403).
 *******************************************************************************/
public class ExplainSpec extends DbPerformanceJsonSpec
{



   /*******************************************************************************
    ** JSON-only POST, with stronger permission for ANALYZE.
    *******************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation().withHttpMethod(HttpMethod.POST).withPath("/qbit/db-performance/explain")
         .withShortSummary("Explain a PostgreSQL query").withLongDescription("PLAN requires db_performance.explain; ANALYZE requires db_performance.explain_analyze. Values and ANALYZE plans are never persisted."));
   }



   /*******************************************************************************
    ** All inputs are in the body.
    *******************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return (List.of());
   }



   /*******************************************************************************
    ** Versioned response shape.
    *******************************************************************************/
   @Override
   protected String getResponseSchemaName()
   {
      return ("DbPerformanceExplainResultV1");
   }



   /*******************************************************************************
    ** Versioned request shape.
    *******************************************************************************/
   @Override
   protected Schema defineRequestBodySchema()
   {
      return (DbPerformanceSchemas.ref("ExplainRequest"));
   }



   /*******************************************************************************
    ** Validate JSON types without copying arbitrary values to errors or logs.
    *******************************************************************************/
   @Override
   public Object handle(JsonInput input) throws Exception
   {
      Map<String, Object> body = input.getBody();
      ExplainRequest.Mode mode = null;
      try
      {
         mode = ExplainRequest.Mode.valueOf(string(body, "mode"));
      }
      catch(IllegalArgumentException | NullPointerException e)
      {
         // The service audits invalid modes before returning a user-facing error.
      }
      Integer id = null;
      if(body.get("statementId") != null)
      {
         try
         {
            id = new java.math.BigDecimal(body.get("statementId").toString()).intValueExact();
         }
         catch(ArithmeticException | NumberFormatException e)
         {
            throw (new QBadRequestException("statementId must be an integer."));
         }
      }
      List<String> parameters = new ArrayList<>();
      if(body.get("parameters") != null)
      {
         if(!(body.get("parameters") instanceof List<?> values))
         {
            throw (new QBadRequestException("parameters must be an array of strings."));
         }
         for(Object value : values)
         {
            if(!(value instanceof String))
            {
               throw (new QBadRequestException("parameters must be an array of strings."));
            }
            parameters.add((String) value);
         }
      }
      return (new ExplainService(DbPerfRuntime.of(QContext.getQInstance())).explain(new ExplainRequest(string(body, "backend"), id, string(body, "sql"), mode, parameters)));
   }



   /*******************************************************************************
    ** Accept strings only, rather than coercing objects into executable SQL.
    *******************************************************************************/
   private static String string(Map<String, Object> body, String key) throws QBadRequestException
   {
      Object value = body.get(key);
      if(value != null && !(value instanceof String))
      {
         throw (new QBadRequestException(key + " must be a string."));
      }
      return ((String) value);
   }
}
