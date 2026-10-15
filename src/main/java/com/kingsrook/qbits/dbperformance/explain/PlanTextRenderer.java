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

package com.kingsrook.qbits.dbperformance.explain;


import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.dbperformance.normalize.SqlNormalizer;


/*******************************************************************************
 ** Readable PostgreSQL JSON plan tree, with a literal-free prompt variant.
 *******************************************************************************/
public final class PlanTextRenderer
{
   private static final ObjectMapper MAPPER = new ObjectMapper();



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private PlanTextRenderer()
   {
   }



   /*******************************************************************************
    ** A parsed plan and its top-level measures.
    *******************************************************************************/
   public record Plan(String text, String safeText, BigDecimal totalCost, BigDecimal actualTotalMs)
   {
   }



   /*******************************************************************************
    ** PostgreSQL emits an array containing the plan and optional execution timing.
    *******************************************************************************/
   public static Plan render(String json) throws Exception
   {
      JsonNode document = MAPPER.readTree(json);
      JsonNode root = document.isArray() ? document.path(0) : document;
      JsonNode plan = root.path("Plan");
      if(!plan.isObject())
      {
         throw (new IllegalArgumentException("PostgreSQL did not return a plan tree."));
      }
      StringBuilder text = new StringBuilder();
      StringBuilder safe = new StringBuilder();
      append(plan, 0, text, false);
      append(plan, 0, safe, true);
      JsonNode actual = root.has("Execution Time") ? root.get("Execution Time") : plan.get("Actual Total Time");
      return (new Plan(text.toString().stripTrailing(), safe.toString().stripTrailing(),
         plan.has("Total Cost") ? plan.get("Total Cost").decimalValue() : null, actual == null ? null : actual.decimalValue()));
   }



   /*******************************************************************************
    ** Preserve tree structure and planner metrics; redact expression literals for AI.
    *******************************************************************************/
   private static void append(JsonNode node, int depth, StringBuilder out, boolean redact)
   {
      out.append("  ".repeat(depth)).append(node.path("Node Type").asText("Unknown"));
      for(String field : List.of("Relation Name", "Index Name", "Startup Cost", "Total Cost", "Plan Rows", "Actual Total Time", "Actual Rows", "Filter", "Index Cond", "Hash Cond", "Join Filter", "Recheck Cond"))
      {
         if(node.has(field))
         {
            String value = node.get(field).asText();
            boolean expression = field.endsWith("Cond") || field.endsWith("Filter");
            out.append(" | ").append(field).append(": ").append(redact && expression ? SqlNormalizer.normalize(value) : value);
         }
      }
      out.append('\n');
      for(JsonNode child : node.path("Plans"))
      {
         append(child, depth + 1, out, redact);
      }
   }
}
