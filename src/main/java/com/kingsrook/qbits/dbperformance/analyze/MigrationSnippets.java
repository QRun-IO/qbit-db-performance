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


import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Reviewable Liquibase text only. These suggestions are never executed.
 *******************************************************************************/
public final class MigrationSnippets
{
   private static final Set<String> RESERVED = Set.of("user", "order", "group", "select", "table", "index", "where", "from", "to", "primary", "constraint", "limit", "offset", "references", "column", "default", "check", "unique", "all", "and", "or", "not", "null", "true", "false", "case", "when", "end", "join", "on", "in", "as", "by", "values", "grant", "current_user", "authorization", "returning",
      "analyse", "analyze", "array", "asc", "asymmetric", "both", "cast", "collate", "collation", "concurrently", "current_catalog", "current_date", "current_role", "current_schema", "current_time", "current_timestamp",
      "desc", "distinct", "do", "else", "except", "fetch", "for", "foreign", "freeze", "ilike", "initially", "intersect", "into", "is", "isnull", "lateral", "leading", "like", "localtime", "localtimestamp", "natural", "notnull",
      "only", "placing", "session_user", "similar", "some", "symmetric", "system_user", "tablesample", "then", "trailing", "union", "using", "variadic", "verbose", "window", "with");


   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private MigrationSnippets()
   {
   }


   /*******************************************************************************
    ** Quote identifiers with punctuation, mixed case, or reserved spelling.
    *******************************************************************************/
   public static String quote(String name)
   {
      return (name.matches("[a-z_][a-z0-9_]*") && !RESERVED.contains(name) ? name : "\"" + name.replace("\"", "\"\"") + "\"");
   }


   /*******************************************************************************
    ** ASCII names are byte-bounded as well as character-bounded for PostgreSQL.
    *******************************************************************************/
   public static String indexName(String table, List<String> columns)
   {
      String raw = "idx_" + table + "_" + String.join("_", columns);
      String safe = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
      return (safe.length() <= 63 && safe.equals(raw) ? safe : safe.substring(0, Math.min(54, safe.length())) + "_" + String.format("%08x", raw.hashCode()));
   }


   /*******************************************************************************
    ** Create concurrently, with a matching schema-qualified rollback.
    *******************************************************************************/
   public static String create(Instant now, QRecord table, List<String> columns, String reason)
   {
      String name = indexName(table.getValueString("tableName"), columns);
      String schema = quote(table.getValueString("schemaName"));
      return (header(now, name, "MISSING_INDEX", reason)
         + "CREATE INDEX CONCURRENTLY IF NOT EXISTS " + quote(name) + " ON " + schema + "." + quote(table.getValueString("tableName"))
         + " (" + columns.stream().map(MigrationSnippets::quote).collect(Collectors.joining(", ")) + ");\n"
         + "-- rollback DROP INDEX CONCURRENTLY IF EXISTS " + schema + "." + quote(name) + ";\n");
   }


   /*******************************************************************************
    ** Preserve the collected definition in rollback; no guessed index structure.
    *******************************************************************************/
   public static String drop(Instant now, QRecord index, String rule, String reason)
   {
      String definition = index.getValueString("definition");
      if(definition == null || !definition.matches("(?is)^CREATE\\s+(UNIQUE\\s+)?INDEX\\s+.*"))
      {
         return (null);
      }
      String rollback = definition.replaceFirst("(?i)^(CREATE\\s+(?:UNIQUE\\s+)?INDEX)\\s+(?:CONCURRENTLY\\s+)?", "$1 CONCURRENTLY ").replaceAll("[\\r\\n]+", " ").replaceAll(";+$", "");
      return (header(now, index.getValueString("indexName"), rule, reason)
         + "DROP INDEX CONCURRENTLY IF EXISTS " + quote(index.getValueString("schemaName")) + "." + quote(index.getValueString("indexName")) + ";\n"
         + "-- rollback " + rollback + ";\n");
   }


   /*******************************************************************************
    ** Optional tuning suggestion; rollback resets the override to server defaults.
    *******************************************************************************/
   public static String vacuum(Instant now, QRecord table, String reason)
   {
      String target = quote(table.getValueString("schemaName")) + "." + quote(table.getValueString("tableName"));
      return (header(now, table.getValueString("tableName"), "BLOAT", reason)
         + "ALTER TABLE " + target + " SET (autovacuum_vacuum_scale_factor = 0.05);\n"
         + "-- rollback ALTER TABLE " + target + " RESET (autovacuum_vacuum_scale_factor);\n");
   }


   /*******************************************************************************
    ** A stable daily changeset identity and a single-line evidence comment.
    *******************************************************************************/
   private static String header(Instant now, String subject, String rule, String reason)
   {
      String date = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC).format(now);
      return ("-- liquibase formatted sql\n-- changeset db-performance:" + date + "-" + rule.toLowerCase(Locale.ROOT) + "-" + subject.replaceAll("[^a-zA-Z0-9_-]", "_")
         + " runInTransaction:false\n-- comment: Suggested by Database Performance (" + rule + "): " + reason.replaceAll("[\\r\\n]+", " ") + "\n");
   }
}
