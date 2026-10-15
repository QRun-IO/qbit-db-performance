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
import java.sql.SQLException;
import java.util.List;
import java.util.Map;


/*******************************************************************************
 ** Read-only database statistics SPI. Implementations never change the database.
 *******************************************************************************/
public interface DbStatsProvider
{
   /*******************************************************************************
    ** Provider name.
    *******************************************************************************/
   String getName();


   /*******************************************************************************
    ** capabilities using the supplied backend connection.
    *******************************************************************************/
   ProviderCapabilities capabilities(Connection connection) throws SQLException;


   /*******************************************************************************
    ** readDatabase using the supplied backend connection.
    *******************************************************************************/
   DatabaseStats readDatabase(Connection connection) throws SQLException;


   /*******************************************************************************
    ** readStatements using the supplied backend connection.
    *******************************************************************************/
   List<StatementStats> readStatements(Connection connection) throws SQLException;


   /*******************************************************************************
    ** readStatementResetInfo using the supplied backend connection.
    *******************************************************************************/
   StatementResetInfo readStatementResetInfo(Connection connection) throws SQLException;


   /*******************************************************************************
    ** readTables using the supplied backend connection.
    *******************************************************************************/
   List<TableStats> readTables(Connection connection) throws SQLException;


   /*******************************************************************************
    ** readIndexes using the supplied backend connection.
    *******************************************************************************/
   List<IndexStats> readIndexes(Connection connection) throws SQLException;


   /*******************************************************************************
    ** readSessions using the supplied backend connection.
    *******************************************************************************/
   List<SessionInfo> readSessions(Connection connection) throws SQLException;


   /*******************************************************************************
    ** readConnections using the supplied backend connection.
    *******************************************************************************/
   List<ConnectionCount> readConnections(Connection connection) throws SQLException;


   /*******************************************************************************
    ** readSettings using the supplied backend connection.
    *******************************************************************************/
   Map<String, String> readSettings(Connection connection) throws SQLException;
}
