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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;


/*******************************************************************************
 ** Lex executable SQL without changing literals, identifiers or parameter positions.
 ** This is a conservative SELECT guard; PostgreSQL READ ONLY is the write boundary.
 *******************************************************************************/
public final class ExplainSql
{
   private static final Pattern DOLLAR_QUOTE = Pattern.compile("\\$(?:[A-Za-z_][A-Za-z_0-9]*)?\\$");



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private ExplainSql()
   {
   }



   /*******************************************************************************
    ** Executable SQL with contiguous PostgreSQL bind slots and unquoted tokens.
    *******************************************************************************/
   public record Parsed(String sql, List<String> tokens, int parameterCount)
   {
      /*******************************************************************************
       ** Reject writes, SELECT INTO and every locking SELECT, including nested CTEs.
       *******************************************************************************/
      public void requireSelect() throws QBadRequestException
      {
         if(tokens.isEmpty() || !Set.of("SELECT", "WITH").contains(tokens.getFirst())
            || tokens.stream().anyMatch(Set.of("INSERT", "UPDATE", "DELETE", "MERGE", "INTO", "SHARE")::contains))
         {
            throw (new QBadRequestException("ANALYZE requires a single SELECT without writes, INTO or row locks."));
         }
      }
   }



   /*******************************************************************************
    ** Skip SQL comments and quoted regions, reject multiple statements, and convert ?.
    ** Mixing JDBC and PostgreSQL parameter styles is rejected as ambiguous.
    *******************************************************************************/
   public static Parsed parse(String sql) throws QBadRequestException
   {
      if(sql == null || sql.isBlank() || sql.length() > 100000)
      {
         throw (new QBadRequestException("Supply one SQL statement of at most 100000 characters."));
      }
      StringBuilder result = new StringBuilder();
      List<String> tokens = new ArrayList<>();
      Set<Integer> positions = new HashSet<>();
      int jdbc = 0;
      int i = 0;
      boolean ended = false;
      while(i < sql.length())
      {
         int start = i;
         char c = sql.charAt(i);
         if(Character.isWhitespace(c))
         {
            result.append(c);
            i++;
            continue;
         }
         if(sql.startsWith("--", i))
         {
            int end = sql.indexOf('\n', i);
            i = end < 0 ? sql.length() : end;
            result.append(' ');
            continue;
         }
         if(sql.startsWith("/*", i))
         {
            int depth = 1;
            i += 2;
            while(i < sql.length() && depth > 0)
            {
               if(sql.startsWith("/*", i))
               {
                  depth++;
                  i += 2;
               }
               else if(sql.startsWith("*/", i))
               {
                  depth--;
                  i += 2;
               }
               else
               {
                  i++;
               }
            }
            if(depth != 0)
            {
               throw (new QBadRequestException("Unterminated SQL comment."));
            }
            result.append(' ');
            continue;
         }
         if(ended)
         {
            throw (new QBadRequestException("Only one SQL statement is allowed."));
         }
         if(c == ';')
         {
            ended = true;
            i++;
            continue;
         }
         boolean escaped = (c == 'e' || c == 'E') && i + 1 < sql.length() && sql.charAt(i + 1) == '\'';
         if(c == '\'' || c == '"' || escaped)
         {
            char quote = escaped ? '\'' : c;
            i = quotedEnd(sql, i + (escaped ? 1 : 0), quote, escaped);
            result.append(sql, start, i);
            tokens.add("<QUOTED>");
            continue;
         }
         if(c == '$')
         {
            Matcher delimiter = DOLLAR_QUOTE.matcher(sql).region(i, sql.length());
            if(delimiter.lookingAt())
            {
               int end = sql.indexOf(delimiter.group(), delimiter.end());
               if(end < 0)
               {
                  throw (new QBadRequestException("Unterminated dollar-quoted SQL string."));
               }
               i = end + delimiter.group().length();
               result.append(sql, start, i);
               tokens.add("<QUOTED>");
               continue;
            }
            i++;
            while(i < sql.length() && Character.isDigit(sql.charAt(i)))
            {
               i++;
            }
            if(i > start + 1)
            {
               try
               {
                  int position = Integer.parseInt(sql.substring(start + 1, i));
                  if(position < 1 || position > 65535)
                  {
                     throw (new NumberFormatException());
                  }
                  positions.add(position);
               }
               catch(NumberFormatException e)
               {
                  throw (new QBadRequestException("SQL parameter positions must be between 1 and 65535."));
               }
            }
            result.append(sql, start, i);
            continue;
         }
         if(c == '?')
         {
            // PostgreSQL's multi-character JSON operators are not JDBC bind slots.
            if(i + 1 < sql.length() && "|&?".indexOf(sql.charAt(i + 1)) >= 0)
            {
               result.append(sql, i, i + 2);
               i += 2;
            }
            else
            {
               result.append('$').append(++jdbc);
               i++;
            }
            continue;
         }
         if(Character.isLetter(c) || c == '_')
         {
            i++;
            while(i < sql.length() && (Character.isLetterOrDigit(sql.charAt(i)) || "_$".indexOf(sql.charAt(i)) >= 0))
            {
               i++;
            }
            tokens.add(sql.substring(start, i).toUpperCase(Locale.ROOT));
            result.append(sql, start, i);
            continue;
         }
         result.append(c);
         i++;
      }
      int maximum = positions.stream().mapToInt(Integer::intValue).max().orElse(0);
      if(jdbc > 0 && maximum > 0 || maximum != positions.size())
      {
         throw (new QBadRequestException("Use either ? parameters or contiguous $1 through $n parameters."));
      }
      return (new Parsed(result.toString().trim(), List.copyOf(tokens), Math.max(jdbc, maximum)));
   }



   /*******************************************************************************
    ** A quoted region cannot introduce executable keywords or statement delimiters.
    *******************************************************************************/
   private static int quotedEnd(String sql, int start, char quote, boolean escaped) throws QBadRequestException
   {
      int i = start + 1;
      while(i < sql.length())
      {
         if(escaped && sql.charAt(i) == '\\')
         {
            i += 2;
         }
         else if(sql.charAt(i++) == quote)
         {
            if(i < sql.length() && sql.charAt(i) == quote)
            {
               i++;
            }
            else
            {
               return (i);
            }
         }
      }
      throw (new QBadRequestException("Unterminated SQL quote."));
   }
}
