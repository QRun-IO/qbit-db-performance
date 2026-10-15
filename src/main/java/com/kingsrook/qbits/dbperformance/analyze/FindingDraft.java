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

package com.kingsrook.qbits.dbperformance.analyze;


import java.math.BigDecimal;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Immutable proposal. The lifecycle service owns identity, timestamps and state.
 *******************************************************************************/
public record FindingDraft(String ruleCode, String dedupeKey, String severity, double impact,
                           String title, String summary, String evidence, String recommendation,
                           String migrationSnippet, String subjectTable, Integer statementId,
                           Integer tableStatId, Integer indexStatId, String unitName)
{
   /*******************************************************************************
    ** Rank even small findings, with impact clamped to a finite fraction.
    *******************************************************************************/
   public BigDecimal score()
   {
      int weight = switch(severity)
      {
         case "INFO" -> 1;
         case "WARNING" -> 2;
         case "ERROR" -> 3;
         case "CRITICAL" -> 4;
         default -> throw new IllegalArgumentException("Unknown finding severity");
      };
      if(!Double.isFinite(impact))
      {
         throw (new IllegalArgumentException("Impact must be finite"));
      }
      return (BigDecimal.valueOf(weight * (0.1 + Math.max(0, Math.min(1, impact)))));
   }


   /*******************************************************************************
    ** Persist only the generated evidence; do not overwrite human status fields.
    *******************************************************************************/
   public QRecord record()
   {
      return (new QRecord().withValue("ruleCode", ruleCode).withValue("dedupeKey", dedupeKey)
         .withValue("severity", severity).withValue("score", score()).withValue("title", title)
         .withValue("summary", summary).withValue("evidence", evidence).withValue("recommendation", recommendation)
         .withValue("migrationSnippet", migrationSnippet).withValue("subjectTable", subjectTable)
         .withValue("statementId", statementId).withValue("tableStatId", tableStatId).withValue("indexStatId", indexStatId).withValue("unitName", unitName));
   }
}
