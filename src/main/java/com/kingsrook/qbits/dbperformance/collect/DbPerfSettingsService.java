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

package com.kingsrook.qbits.dbperformance.collect;


import java.util.List;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Insert-only per-backend defaults; user edits are never overwritten.
 *******************************************************************************/
public class DbPerfSettingsService
{
   private final DbPerfRuntime runtime;


   /*******************************************************************************
    ** Runtime dependency.
    *******************************************************************************/
   public DbPerfSettingsService(DbPerfRuntime runtime)
   {
      this.runtime = runtime;
   }


   /*******************************************************************************
    ** Load or insert the settings row. A concurrent winner is re-read.
    *******************************************************************************/
   public QRecord ensure(String backendName) throws Exception
   {
      return (runtime.asSystem(() ->
      {
         QRecord existing = DbPerfStore.first("db_perf_settings", DbPerfStore.eq("backendName", backendName));
         if(existing != null)
         {
            return (existing);
         }
         try
         {
            return (DbPerfStore.insert("db_perf_settings", List.of(new QRecord().withValue("backendName", backendName)
               .withValue("attributionEnabled", runtime.getConfig().getAttributionEnabled()))).getFirst());
         }
         catch(Exception e)
         {
            QRecord winner = DbPerfStore.first("db_perf_settings", DbPerfStore.eq("backendName", backendName));
            if(winner != null)
            {
               return (winner);
            }
            throw (e);
         }
      }));
   }
}
