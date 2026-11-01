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

package com.kingsrook.qbits.dbperformance.provider;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


/*******************************************************************************
 ** Bounded-time JDBC reads shared by statistics providers.
 *******************************************************************************/
final class JdbcReads
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private JdbcReads()
   {
   }



   /*******************************************************************************
    ** Execute one bounded-time read and close JDBC resources.
    *******************************************************************************/
   static <T> List<T> read(Connection connection, String sql, RowReader<T> mapper) throws SQLException
   {
      List<T> result = new ArrayList<>();
      try(Statement statement = connection.createStatement())
      {
         statement.setQueryTimeout(30);
         try(ResultSet rows = statement.executeQuery(sql))
         {
            while(rows.next())
            {
               result.add(mapper.read(rows));
            }
         }
      }
      return (result);
   }



   /*******************************************************************************
    ** A single numeric read.
    *******************************************************************************/
   static long scalar(Connection connection, String sql) throws SQLException
   {
      return (read(connection, sql, row -> row.getLong(1)).getFirst());
   }



   /*******************************************************************************
    ** Timestamp conversion preserves null values.
    *******************************************************************************/
   static Instant instant(ResultSet row, String column) throws SQLException
   {
      Timestamp timestamp = row.getTimestamp(column);
      return (timestamp == null ? null : timestamp.toInstant());
   }



   /*******************************************************************************
    ** Checked JDBC row mapping.
    *******************************************************************************/
   interface RowReader<T>
   {
      /*******************************************************************************
       ** Map one row.
       *******************************************************************************/
      T read(ResultSet row) throws SQLException;
   }
}
