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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;


/*******************************************************************************
 ** Provider registry keyed by JDBC {@code DatabaseMetaData.getDatabaseProductName()}.
 ** Adding a vendor is one implementation of {@link DbStatsProvider} plus one
 ** {@link #register} call (see docs/PROVIDERS.md); anything unregistered gets
 ** {@link UnsupportedStatsProvider}.
 *******************************************************************************/
public final class DbStatsProviders
{
   //////////////////////////////////////////////////////////////////////////
   // JDBC product name (lower case) -> provider. MySQL reports "MySQL",   //
   // MariaDB "MariaDB" (Connector/J 3) and SQLite "SQLite"; none are      //
   // registered yet: docs/PROVIDERS.md says what each could supply.       //
   //////////////////////////////////////////////////////////////////////////
   private static final Map<String, Supplier<DbStatsProvider>> BY_PRODUCT = new ConcurrentHashMap<>(Map.of(
      "postgresql", PostgresStatsProvider::new,
      "h2", H2StatsProvider::new));



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DbStatsProviders()
   {
   }



   /*******************************************************************************
    ** Provider for the connection's JDBC product.
    *******************************************************************************/
   public static DbStatsProvider forConnection(Connection connection) throws SQLException
   {
      return (forProductName(connection.getMetaData().getDatabaseProductName()));
   }



   /*******************************************************************************
    ** Provider for a JDBC product name; unsupported when not registered.
    *******************************************************************************/
   public static DbStatsProvider forProductName(String productName)
   {
      Supplier<DbStatsProvider> supplier = productName == null ? null : BY_PRODUCT.get(productName.toLowerCase(Locale.ROOT));
      return (supplier == null ? new UnsupportedStatsProvider() : supplier.get());
   }



   /*******************************************************************************
    ** Provider by its saved name (db_perf_settings.provider), for code that has
    ** settings but no connection; unsupported for null or unknown names.
    *******************************************************************************/
   public static DbStatsProvider forProviderName(String providerName)
   {
      for(Supplier<DbStatsProvider> supplier : BY_PRODUCT.values())
      {
         DbStatsProvider provider = supplier.get();
         if(provider.getName().equalsIgnoreCase(providerName))
         {
            return (provider);
         }
      }
      return (new UnsupportedStatsProvider());
   }



   /*******************************************************************************
    ** Register (or replace) the provider for a JDBC product name. Hosts may add
    ** vendors before the qbit collects; the name must be a Provider possible value.
    *******************************************************************************/
   public static void register(String productName, Supplier<DbStatsProvider> provider)
   {
      BY_PRODUCT.put(productName.toLowerCase(Locale.ROOT), provider);
   }
}
