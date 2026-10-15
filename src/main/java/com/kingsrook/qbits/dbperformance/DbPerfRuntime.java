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

package com.kingsrook.qbits.dbperformance;


import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qbits.dbperformance.attribution.AttributionFlusher;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;


/*******************************************************************************
 ** Per-instance configuration and service boundary; no global singleton or filesystem.
 *******************************************************************************/
public class DbPerfRuntime implements QSupplementalInstanceMetaData
{
   public static final String NAME = "dbPerformance";
   private final QInstance instance;
   private final DbPerfQBitConfig config;
   private AttributionFlusher attributionFlusher;


   /*******************************************************************************
    ** Runtime bound to a single host instance.
    *******************************************************************************/
   public DbPerfRuntime(QInstance instance, DbPerfQBitConfig config)
   {
      this.instance = instance;
      this.config = config;
   }


   /*******************************************************************************
    ** Find the registered runtime.
    *******************************************************************************/
   public static DbPerfRuntime of(QInstance instance)
   {
      DbPerfRuntime runtime = QSupplementalInstanceMetaData.of(instance, NAME);
      if(runtime == null)
      {
         throw (new IllegalStateException("Database Performance is not registered"));
      }
      return (runtime);
   }


   /*******************************************************************************
    ** Supplemental metadata name.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }


   /*******************************************************************************
    ** Register on the owning instance.
    *******************************************************************************/
   @Override
   public void addSelfToInstance(QInstance instance)
   {
      instance.withSupplementalMetaData(this);
   }


   /*******************************************************************************
    ** Host configuration.
    *******************************************************************************/
   public DbPerfQBitConfig getConfig()
   {
      return (config);
   }


   /*******************************************************************************
    ** Owning instance.
    *******************************************************************************/
   public QInstance getInstance()
   {
      return (instance);
   }


   /*******************************************************************************
    ** Execute as the system user, always restoring the caller context.
    *******************************************************************************/
   public <T> T asSystem(Work<T> work) throws Exception
   {
      AtomicReference<T> result = new AtomicReference<>();
      QContext.withTemporaryContext(new CapturedContext(instance, new QSystemUserSession(instance)), () -> result.set(work.run()));
      return (result.get());
   }


   /*******************************************************************************
    ** Atomic QQQ action transaction, rolled back on failure.
    *******************************************************************************/
   public <T> T transaction(Work<T> work) throws Exception
   {
      return (asSystem(() ->
      {
         try(QBackendTransaction transaction = QBackendTransaction.openFor(new QueryInput("db_perf_snapshot")))
         {
            AtomicReference<T> result = new AtomicReference<>();
            try
            {
               QContext.withTemporaryContext(new CapturedContext(instance, new QSystemUserSession(instance), transaction, null), () -> result.set(work.run()));
               transaction.commit();
               return (result.get());
            }
            catch(Exception e)
            {
               transaction.rollback();
               throw (e);
            }
         }
      }));
   }


   /*******************************************************************************
    ** Called by the launcher, or explicitly by hosts without a launcher.
    *******************************************************************************/
   public synchronized void startAttribution()
   {
      if(attributionFlusher == null && Boolean.TRUE.equals(config.getAttributionEnabled()))
      {
         attributionFlusher = new AttributionFlusher(this);
         attributionFlusher.start();
      }
   }


   /*******************************************************************************
    ** Stop the per-instance listener and its daemon; safe to call repeatedly.
    *******************************************************************************/
   public synchronized void stopAttribution()
   {
      if(attributionFlusher != null)
      {
         attributionFlusher.close();
         attributionFlusher = null;
      }
   }


   /*******************************************************************************
    ** Checked service operation.
    *******************************************************************************/
   public interface Work<T>
   {
      /*******************************************************************************
       ** Execute the operation.
       *******************************************************************************/
      T run() throws Exception;
   }
}
