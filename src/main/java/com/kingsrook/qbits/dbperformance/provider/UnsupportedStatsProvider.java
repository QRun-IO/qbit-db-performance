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
import java.util.List;
import java.util.Map;


/*******************************************************************************
 ** Unsupported vendors report reduced capability without reading vendor views.
 *******************************************************************************/
public class UnsupportedStatsProvider implements DbStatsProvider
{
   /*******************************************************************************
    ** Provider name.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return ("UNSUPPORTED");
   }


   /*******************************************************************************
    ** Empty unsupported result.
    *******************************************************************************/
   @Override
   public ProviderCapabilities capabilities(Connection connection)
   {
      return (new ProviderCapabilities(false, 0, false, false, "UNSUPPORTED", "This database vendor is not supported; PostgreSQL 13+ and H2 2.x are (see docs/PROVIDERS.md)."));
   }


   /*******************************************************************************
    ** Empty unsupported result.
    *******************************************************************************/
   @Override
   public DatabaseStats readDatabase(Connection connection)
   {
      return (new DatabaseStats(Map.of(), null, 0, 0));
   }


   /*******************************************************************************
    ** Empty unsupported result.
    *******************************************************************************/
   @Override
   public List<StatementStats> readStatements(Connection connection)
   {
      return (List.of());
   }


   /*******************************************************************************
    ** Empty unsupported result.
    *******************************************************************************/
   @Override
   public StatementResetInfo readStatementResetInfo(Connection connection)
   {
      return (new StatementResetInfo(null, 0));
   }


   /*******************************************************************************
    ** Empty unsupported result.
    *******************************************************************************/
   @Override
   public List<TableStats> readTables(Connection connection)
   {
      return (List.of());
   }


   /*******************************************************************************
    ** Empty unsupported result.
    *******************************************************************************/
   @Override
   public List<IndexStats> readIndexes(Connection connection)
   {
      return (List.of());
   }


   /*******************************************************************************
    ** Empty unsupported result.
    *******************************************************************************/
   @Override
   public List<SessionInfo> readSessions(Connection connection)
   {
      return (List.of());
   }


   /*******************************************************************************
    ** Empty unsupported result.
    *******************************************************************************/
   @Override
   public List<ConnectionCount> readConnections(Connection connection)
   {
      return (List.of());
   }


   /*******************************************************************************
    ** Empty unsupported result.
    *******************************************************************************/
   @Override
   public Map<String, String> readSettings(Connection connection)
   {
      return (Map.of());
   }
}
