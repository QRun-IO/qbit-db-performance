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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.kingsrook.qbits.dbperformance.normalize.SqlNormalizer;


/*******************************************************************************
 ** Conservative tokenizer for simple WHERE equality/IN and equijoins. Complex
 ** subqueries and expressions are deliberately left for the Query plan action.
 *******************************************************************************/
public final class PredicateParser
{
   private static final Pattern TOKEN = Pattern.compile("\"(?:\"\"|[^\"])*\"|[a-zA-Z_][a-zA-Z_0-9$]*|\\?|[^\\s]");
   private static final Set<String> KEYWORDS = Set.of("where", "join", "inner", "outer", "left", "right", "full", "cross", "on", "group", "order", "limit", "having", "set", "returning", "union");


   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private PredicateParser()
   {
   }


   /*******************************************************************************
    ** Decode SQL identifiers; unquoted names fold to lower case.
    *******************************************************************************/
   public static String identifier(String token)
   {
      return (token.startsWith("\"") ? token.substring(1, token.length() - 1).replace("\"\"", "\"") : token.toLowerCase(Locale.ROOT));
   }


   /*******************************************************************************
    ** Only simple column indexes can be compared as prefixes.
    *******************************************************************************/
   public static List<String> indexColumns(String columns)
   {
      if(columns == null || columns.isBlank())
      {
         return (List.of());
      }
      List<String> result = new ArrayList<>();
      Matcher matcher = TOKEN.matcher(columns);
      boolean wantColumn = true;
      while(matcher.find())
      {
         String token = matcher.group();
         if(wantColumn && isIdentifier(token))
         {
            result.add(identifier(token));
         }
         else if(wantColumn || !token.equals(","))
         {
            return (List.of());
         }
         wantColumn = !wantColumn;
      }
      return (wantColumn ? List.of() : result);
   }


   /*******************************************************************************
    ** Resolve aliases before collecting predicates so joined tables do not lend
    ** their columns to the main table's suggested index.
    *******************************************************************************/
   public static List<String> candidates(String sql, String schema, String table)
   {
      List<String> tokens = new ArrayList<>();
      Matcher matcher = TOKEN.matcher(SqlNormalizer.normalize(sql));
      while(matcher.find())
      {
         tokens.add(matcher.group());
      }
      if(tokens.stream().filter(t -> t.equals("select")).count() > 1 || tokens.contains("union"))
      {
         return (List.of());
      }
      Map<String, Boolean> aliases = new LinkedHashMap<>();
      for(int i = 0; i + 1 < tokens.size(); i++)
      {
         if(Set.of("from", "join", "update").contains(tokens.get(i)) && isIdentifier(tokens.get(i + 1)))
         {
            int end = i + 2;
            String relation = identifier(tokens.get(i + 1));
            boolean schemaMatches = true;
            if(end + 1 < tokens.size() && tokens.get(end).equals("."))
            {
               schemaMatches = relation.equals(schema);
               relation = identifier(tokens.get(end + 1));
               end += 2;
            }
            boolean target = schemaMatches && relation.equals(table);
            aliases.put(relation, target);
            if(end < tokens.size() && tokens.get(end).equals("as"))
            {
               end++;
            }
            if(end < tokens.size() && isIdentifier(tokens.get(end)) && !KEYWORDS.contains(tokens.get(end)))
            {
               aliases.put(identifier(tokens.get(end)), target);
            }
         }
      }
      Set<String> result = new LinkedHashSet<>();
      boolean predicate = false;
      for(int i = 0; i < tokens.size(); i++)
      {
         String token = tokens.get(i);
         if(token.equals("where") || token.equals("on"))
         {
            predicate = true;
            continue;
         }
         if(KEYWORDS.contains(token) && !token.equals("on"))
         {
            predicate = false;
         }
         if(!predicate || !isIdentifier(token) || i > 0 && tokens.get(i - 1).equals("."))
         {
            continue;
         }
         int end = i + 1;
         String column = identifier(token);
         boolean belongs = !aliases.isEmpty() && aliases.values().stream().allMatch(Boolean::booleanValue);
         if(end + 1 < tokens.size() && tokens.get(end).equals("."))
         {
            belongs = Boolean.TRUE.equals(aliases.get(column));
            column = identifier(tokens.get(end + 1));
            end += 2;
         }
         if(end + 1 >= tokens.size())
         {
            continue;
         }
         String operator = tokens.get(end);
         String right = tokens.get(end + 1);
         boolean joinedColumn = end + 3 < tokens.size() && tokens.get(end + 2).equals(".") && aliases.containsKey(identifier(right));
         if(belongs && (operator.equals("in") && right.equals("(") || operator.equals("=") && (right.equals("?") || joinedColumn)))
         {
            result.add(column);
         }
         if(operator.equals("=") && end + 3 < tokens.size() && tokens.get(end + 2).equals(".")
            && Boolean.TRUE.equals(aliases.get(identifier(right))))
         {
            result.add(identifier(tokens.get(end + 3)));
         }
      }
      return (List.copyOf(result));
   }


   /*******************************************************************************
    ** Tokens are already balanced quoted identifiers or simple words.
    *******************************************************************************/
   private static boolean isIdentifier(String token)
   {
      return (token.startsWith("\"") || token.matches("[a-zA-Z_][a-zA-Z_0-9$]*"));
   }
}
