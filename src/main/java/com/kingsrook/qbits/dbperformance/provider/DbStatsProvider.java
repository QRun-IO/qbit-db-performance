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
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.Map;
import java.util.Set;


/*******************************************************************************
 ** Database statistics SPI. Every read is read-only and never changes the
 ** database; {@link #enableStatementStatistics} is the one explicit, opt-in
 ** change, run only from the settings action by a user who manages settings.
 ** A provider is chosen per JDBC product name by {@link DbStatsProviders}.
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



   /*******************************************************************************
    ** Statistics areas this vendor can supply at best, independent of a
    ** connection. Analysis rules that need an absent area are skipped.
    *******************************************************************************/
   default Set<StatsFeature> features()
   {
      return (Set.of());
   }



   /*******************************************************************************
    ** Operator next step for a persisted statsAccess code (health and setup).
    *******************************************************************************/
   default String remedy(String statsAccess, String databaseUser)
   {
      return ("FULL".equals(statsAccess) ? "Full statistics access" : "Use a supported database vendor and statistics provider");
   }



   /*******************************************************************************
    ** Vendor prerequisite rows for the guided setup panel, from saved settings.
    *******************************************************************************/
   default List<SetupCheck> setupChecks(String statsAccess, String message)
   {
      return (List.of(new SetupCheck("Statistics access", statsAccess, message == null ? remedy(statsAccess, null) : message)));
   }



   /*******************************************************************************
    ** Turn on statement statistics where the vendor allows it at runtime, and
    ** return what was done. The default explains why it is not possible.
    *******************************************************************************/
   default String enableStatementStatistics(Connection connection) throws SQLException
   {
      throw (new SQLFeatureNotSupportedException("Statement statistics cannot be enabled at runtime for this database; see the setup panel."));
   }



   /*******************************************************************************
    ** Vendor text plan for providers without structured JSON plans. The SQL uses
    ** JDBC ?n slots; parameters are bound only for ANALYZE.
    *******************************************************************************/
   default String explainText(Connection connection, String sql, List<String> parameters, boolean analyze, int timeoutMs) throws SQLException
   {
      throw (new SQLFeatureNotSupportedException("Query plans are not available for this database."));
   }
}
