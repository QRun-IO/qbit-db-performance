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

package com.kingsrook.qbits.dbperformance.normalize;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*******************************************************************************
 ** Literal-free SQL shapes shared by PostgreSQL snapshots and QQQ attribution.
 *******************************************************************************/
public final class SqlNormalizer
{
   private static final Pattern NUMBER = Pattern.compile("(?:0[xX][0-9a-fA-F]+|(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?)");
   private static final Pattern OPERATOR = Pattern.compile("(?:->>|#>>|!~\\*|\\|\\|/|::|>=|<=|<>|!=|\\|\\||->|#>|!~|~\\*|@>|<@|&&|\\|/|:=|=>|\\?\\||\\?&)");
   private static final Pattern DOLLAR = Pattern.compile("\\$(?:[A-Za-z_][A-Za-z_0-9]*)?\\$");
   private static final Pattern TABLE = Pattern.compile("(?:^| )(?:(?:from|into|update) )((?:\"(?:[^\"]|\"\")*\"|[a-z_][a-z_0-9$]*)(?: \\. (?:\"(?:[^\"]|\"\")*\"|[a-z_][a-z_0-9$]*))?)");


   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private SqlNormalizer()
   {
   }


   /*******************************************************************************
    ** Lex SQL before folding whitespace, so quoted identifiers stay intact.
    ** Unterminated literals and comments consume their remainder and never leak it.
    *******************************************************************************/
   public static String normalize(String sql)
   {
      if(sql == null || sql.isBlank())
      {
         return ("");
      }
      StringBuilder out = new StringBuilder();
      int i = 0;
      while(i < sql.length())
      {
         char c = sql.charAt(i);
         if(Character.isWhitespace(c))
         {
            i++;
            continue;
         }
         if(sql.startsWith("--", i))
         {
            int end = sql.indexOf('\n', i);
            i = end < 0 ? sql.length() : end + 1;
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
            continue;
         }
         boolean escape = (c == 'e' || c == 'E') && i + 1 < sql.length() && sql.charAt(i + 1) == '\'';
         if(c == '\'' || escape)
         {
            i = quotedEnd(sql, i + (escape ? 1 : 0), '\'', escape);
            out.append("? ");
            continue;
         }
         if(c == '"')
         {
            int end = quotedEnd(sql, i, '"', false);
            out.append(sql, i, end).append(' ');
            i = end;
            continue;
         }
         if(c == '$')
         {
            Matcher dollar = DOLLAR.matcher(sql).region(i, sql.length());
            if(dollar.lookingAt())
            {
               int end = sql.indexOf(dollar.group(), dollar.end());
               i = end < 0 ? sql.length() : end + dollar.group().length();
               out.append("? ");
               continue;
            }
            int end = i + 1;
            while(end < sql.length() && Character.isDigit(sql.charAt(end)))
            {
               end++;
            }
            if(end > i + 1)
            {
               out.append("? ");
               i = end;
               continue;
            }
         }
         if(Character.isDigit(c) || c == '.' && i + 1 < sql.length() && Character.isDigit(sql.charAt(i + 1)))
         {
            Matcher number = NUMBER.matcher(sql).region(i, sql.length());
            if(number.lookingAt())
            {
               i = number.end();
               out.append("? ");
               continue;
            }
         }
         if(Character.isLetter(c) || c == '_')
         {
            int end = i + 1;
            while(end < sql.length() && (Character.isLetterOrDigit(sql.charAt(end)) || sql.charAt(end) == '_' || sql.charAt(end) == '$'))
            {
               end++;
            }
            out.append(sql.substring(i, end).toLowerCase(Locale.ROOT)).append(' ');
            i = end;
            continue;
         }
         Matcher operator = OPERATOR.matcher(sql).region(i, sql.length());
         if(operator.lookingAt())
         {
            out.append(operator.group()).append(' ');
            i = operator.end();
            continue;
         }
         out.append(c).append(' ');
         i++;
      }
      String normalized = collapseOutsideIdentifiers(out.toString().trim());
      return (normalized.substring(0, Math.min(10000, normalized.length())));
   }


   /*******************************************************************************
    ** List folding applies only outside quoted identifiers, whose contents may
    ** themselves resemble SQL. Escaped identifier quotes remain untouched.
    *******************************************************************************/
   private static String collapseOutsideIdentifiers(String sql)
   {
      StringBuilder result = new StringBuilder();
      int start = 0;
      while(start < sql.length())
      {
         int quote = sql.indexOf('"', start);
         if(quote < 0)
         {
            result.append(collapseLists(sql.substring(start)));
            break;
         }
         result.append(collapseLists(sql.substring(start, quote)));
         int end = quotedEnd(sql, quote, '"', false);
         result.append(sql, quote, end);
         start = end;
      }
      return (result.toString());
   }


   /*******************************************************************************
    ** Collapse placeholder repetitions, preserving VALUES column arity.
    *******************************************************************************/
   private static String collapseLists(String normalized)
   {
      ///////////////////////////////////////////////////////////////////////////
      // a linear scan over the space-separated tokens: regular expressions    //
      // with nested repetition overflow the stack on very long IN and VALUES  //
      // lists (seen with 60 KB statements in production)                      //
      ///////////////////////////////////////////////////////////////////////////
      ////////////////////////////////////////////////////////////////////////
      // segments between quoted identifiers keep their edge spaces          //
      ////////////////////////////////////////////////////////////////////////
      String trimmed = normalized.strip();
      if(trimmed.isEmpty())
      {
         return (normalized);
      }
      String       leading  = normalized.substring(0, normalized.indexOf(trimmed.charAt(0)));
      String       trailing = normalized.substring(leading.length() + trimmed.length());
      String[]     tokens   = trimmed.split(" ");
      List<String> out    = new ArrayList<>(tokens.length);
      int          i      = 0;
      while(i < tokens.length)
      {
         String token = tokens[i];
         if("in".equals(token))
         {
            int end = placeholderGroupEnd(tokens, i + 1);
            if(end > 0)
            {
               out.addAll(List.of("in", "(", "?", ")"));
               i = end;
               continue;
            }
         }
         if("values".equals(token))
         {
            int first = placeholderGroupEnd(tokens, i + 1);
            if(first > 0)
            {
               int end = first;
               while(end + 1 < tokens.length && ",".equals(tokens[end]))
               {
                  int next = placeholderGroupEnd(tokens, end + 1);
                  if(next < 0)
                  {
                     break;
                  }
                  end = next;
               }
               out.add("values");
               out.addAll(Arrays.asList(tokens).subList(i + 1, first));
               i = end;
               continue;
            }
         }
         out.add(token);
         i++;
      }
      return (leading + String.join(" ", out) + trailing);
   }



   /*******************************************************************************
    ** If tokens[start] opens "( ? , ? ... )" made only of placeholders, the index
    ** just after its ")"; otherwise -1.
    *******************************************************************************/
   private static int placeholderGroupEnd(String[] tokens, int start)
   {
      if(start + 2 >= tokens.length || !"(".equals(tokens[start]) || !"?".equals(tokens[start + 1]))
      {
         return (-1);
      }
      int i = start + 2;
      while(i + 1 < tokens.length && ",".equals(tokens[i]) && "?".equals(tokens[i + 1]))
      {
         i += 2;
      }
      return (i < tokens.length && ")".equals(tokens[i]) ? i + 1 : -1);
   }



   /*******************************************************************************
    ** Consume a quote with SQL doubled-quote and optional escape handling.
    *******************************************************************************/
   private static int quotedEnd(String sql, int start, char quote, boolean escapes)
   {
      int i = start + 1;
      while(i < sql.length())
      {
         if(escapes && sql.charAt(i) == '\\')
         {
            i = Math.min(i + 2, sql.length());
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
      return (i);
   }


   /*******************************************************************************
    ** Stable hexadecimal SHA-256; callers supply the normalized shape.
    *******************************************************************************/
   public static String fingerprint(String normalized)
   {
      try
      {
         return (HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8))));
      }
      catch(NoSuchAlgorithmException e)
      {
         throw (new IllegalStateException("SHA-256 is required by Java", e));
      }
   }


   /*******************************************************************************
    ** Leading operation, including WITH as its own kind.
    *******************************************************************************/
   public static String operation(String sql)
   {
      String first = normalize(sql).split(" ", 2)[0].toUpperCase(Locale.ROOT);
      return (switch(first)
      {
         case "SELECT", "INSERT", "UPDATE", "DELETE", "WITH" -> first;
         default -> "OTHER";
      });
   }


   /*******************************************************************************
    ** Best-effort first target table, without schema or identifier quotes.
    *******************************************************************************/
   public static String mainTable(String sql)
   {
      Matcher match = TABLE.matcher(normalize(sql));
      if(!match.find())
      {
         return (null);
      }
      String[] parts = match.group(1).split(" \\. ");
      return (parts[parts.length - 1].replace("\"", ""));
   }
}
