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
import java.util.TreeMap;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** Explicit versioned schemas; every public property is described for generated clients.
 *******************************************************************************/
public final class DbPerformanceSchemas
{



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DbPerformanceSchemas()
   {
   }



   /*******************************************************************************
    ** Shared schema components referenced by the four independently versioned operations.
    *******************************************************************************/
   public static Map<String, Schema> all()
   {
      Map<String, Schema> schemas = new TreeMap<>();
      schemas.put("DbPerformanceErrorV1", object("error", text()));
      schemas.put("DbPerformanceSettingsV1", object("provider", text(), "statsAccess", text(), "statsAccessMessage", text(), "lastSnapshotAt", text()));
      schemas.put("DbPerformanceSnapshotV1", object("id", integer(), "capturedAt", text(), "status", text(), "connectionsTotal", integer(), "maxConnections", integer(),
         "cacheHitRatio", number(), "tempBytes", integer(), "deadlocks", integer(), "statementTotalMs", number(), "statementCalls", integer()));
      schemas.put("DbPerformanceStatementV1", object("id", integer(), "queryText", text().withDescription("Normalized SQL, truncated to 300 characters."),
         "calls24h", integer(), "meanMs24h", number(), "totalMs24h", number(), "shareOfDbTime24h", number(), "hitRatio", number(), "historyPath", text()));
      schemas.put("DbPerformanceFindingV1", object("id", integer(), "title", text(), "severity", text(), "score", number()));
      schemas.put("DbPerformanceOverviewV1", object("settings", ref("Settings"), "snapshot", ref("Snapshot"), "topStatements", array(ref("Statement")),
         "findingsBySeverity", new Schema().withType(Type.OBJECT).withAdditionalProperties(integer()), "topFindings", array(ref("Finding"))));
      schemas.put("DbPerformanceSessionV1", object("pid", integer(), "user", text(), "application", text(), "state", text(), "waitEvent", text(),
         "querySeconds", number(), "xactSeconds", number(), "blockedBy", array(integer()), "blockingCount", integer(), "fingerprint", text(), "normalizedText", text(),
         "query", text().withDescription("Raw live SQL only with view_live_text; never persisted."), "liveTextRedacted", bool()));
      schemas.put("DbPerformanceBlockingChainV1", object("rootPid", integer(), "blockedPids", array(integer())));
      schemas.put("DbPerformanceActivityV1", object("sessions", array(ref("Session")), "blockingChains", array(ref("BlockingChain")), "liveTextRedacted", bool()));
      schemas.put("DbPerformanceLookupV1", object("normalizedSql", text(), "fingerprint", text(), "matches", array(ref("Statement"))));
      schemas.put("DbPerformanceExplainRequestV1", object("backend", text(), "statementId", integer(), "sql", text(),
         "mode", new Schema().withType(Type.STRING).withEnumValues(List.of("PLAN", "ANALYZE")), "parameters", array(text()))
         .withDescription("Supply exactly one of statementId or sql. Parameters are strings, accepted only for ANALYZE and never stored."));
      schemas.put("DbPerformanceExplainResultV1", object("mode", text(), "planJson", text(), "planText", text(), "totalCost", number(), "actualTotalMs", number(),
         "warnings", array(text()), "explainId", integer(), "sqlWorkbenchUrl", text(), "assistantPrompt", text()));
      return (schemas);
   }



   /*******************************************************************************
    ** Nullable scalar for metrics that have not yet been collected.
    *******************************************************************************/
   private static Schema text()
   {
      return (new Schema().withType(Type.STRING).withNullable(true));
   }



   /*******************************************************************************
    ** Integral count or identifier.
    *******************************************************************************/
   private static Schema integer()
   {
      return (new Schema().withType(Type.INTEGER).withNullable(true));
   }



   /*******************************************************************************
    ** Fractional duration, cost or ratio.
    *******************************************************************************/
   private static Schema number()
   {
      return (new Schema().withType(Type.NUMBER).withNullable(true));
   }



   /*******************************************************************************
    ** Required boolean flag.
    *******************************************************************************/
   private static Schema bool()
   {
      return (new Schema().withType(Type.BOOLEAN));
   }



   /*******************************************************************************
    ** Stable component reference.
    *******************************************************************************/
   public static Schema ref(String suffix)
   {
      return (new Schema().withRefToSchema("DbPerformance" + suffix + "V1"));
   }



   /*******************************************************************************
    ** A list with an explicitly typed item.
    *******************************************************************************/
   private static Schema array(Schema item)
   {
      return (new Schema().withType(Type.ARRAY).withItems(item));
   }



   /*******************************************************************************
    ** Deterministically ordered properties for stable OpenAPI generation.
    *******************************************************************************/
   private static Schema object(Object... pairs)
   {
      Map<String, Schema> properties = new TreeMap<>();
      for(int i = 0; i < pairs.length; i += 2)
      {
         properties.put((String) pairs[i], (Schema) pairs[i + 1]);
      }
      return (new Schema().withType(Type.OBJECT).withProperties(properties));
   }
}
