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

package com.kingsrook.qbits.dbperformance.attribution;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.collect.DbPerfSettingsService;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instrumentation.StatementExecutionListeners;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** One daemon per runtime. Windows are detached before any database operation;
 ** failed windows are dropped and later windows continue without retry queues.
 *******************************************************************************/
public class AttributionFlusher implements AutoCloseable
{
   private static final QLogger LOG = QLogger.getLogger(AttributionFlusher.class);
   private final DbPerfRuntime runtime;
   private final AttributionListener listener;
   private ScheduledExecutorService executor;
   private Thread shutdownHook;
   private boolean closed;
   private long failures;


   /*******************************************************************************
    ** Construct without starting threads, for hosts and deterministic tests.
    *******************************************************************************/
   public AttributionFlusher(DbPerfRuntime runtime)
   {
      this.runtime = runtime;
      this.listener = new AttributionListener(runtime);
   }


   /*******************************************************************************
    ** Register once after the instance and schema are ready.
    *******************************************************************************/
   public synchronized void start()
   {
      if(executor != null || closed || !Boolean.TRUE.equals(runtime.getConfig().getAttributionEnabled()))
      {
         return;
      }
      refreshSettings();
      StatementExecutionListeners.register(listener);
      executor = Executors.newSingleThreadScheduledExecutor(task ->
      {
         Thread thread = new Thread(task, "db-perf-attribution");
         thread.setDaemon(true);
         return (thread);
      });
      shutdownHook = new Thread(this::close, "db-perf-attribution-shutdown");
      Runtime.getRuntime().addShutdownHook(shutdownHook);
      schedule();
   }


   /*******************************************************************************
    ** Reschedule after each pass so administrative interval changes take effect.
    *******************************************************************************/
   private synchronized void schedule()
   {
      if(!closed)
      {
         executor.schedule(() ->
         {
            flushNow();
            schedule();
         }, listener.settings().flushSeconds(), TimeUnit.SECONDS);
      }
   }


   /*******************************************************************************
    ** Exactly one InsertAction per nonempty table, with all rows in the window.
    ** Serialize flushers only; statement threads never acquire this monitor.
    *******************************************************************************/
   public synchronized void flushNow()
   {
      boolean previous = AttributionListener.suppress(true);
      try
      {
         Instant end = Instant.now();
         AttributionListener.Window window = listener.drain(end);
         List<QRecord> attribution = records(window.attribution(), window.start(), end, false);
         List<QRecord> repeats = records(window.repeats(), window.start(), end, true);
         if(!attribution.isEmpty() || !repeats.isEmpty())
         {
            runtime.transaction(() ->
            {
               insert("db_perf_attribution", attribution);
               insert("db_perf_repeat_pattern", repeats);
               return (null);
            });
         }
      }
      catch(Exception e)
      {
         failed();
      }
      finally
      {
         refreshSettings();
         AttributionListener.suppress(previous);
      }
   }


   /*******************************************************************************
    ** Refresh even after failed writes, including while attribution is disabled.
    *******************************************************************************/
   public synchronized void refreshSettings()
   {
      boolean previous = AttributionListener.suppress(true);
      try
      {
         listener.refresh(new DbPerfSettingsService(runtime).ensure(runtime.getConfig().getBackendName()));
      }
      catch(Exception e)
      {
         failed();
      }
      finally
      {
         AttributionListener.suppress(previous);
      }
   }


   /*******************************************************************************
    ** Rate-limited fixed text; database errors may contain sensitive SQL.
    *******************************************************************************/
   private void failed()
   {
      failures++;
      if(failures == 1 || failures % 1000 == 0)
      {
         LOG.warn("Database Performance attribution write or settings refresh failed; the failed window is dropped");
      }
   }


   /*******************************************************************************
    ** Convert nanoseconds to persisted milliseconds without floating point loss.
    *******************************************************************************/
   private List<QRecord> records(Map<AttributionListener.Key, AttributionListener.Counters> values, Instant start, Instant end, boolean repeat)
   {
      List<QRecord> records = new ArrayList<>(values.size());
      for(Map.Entry<AttributionListener.Key, AttributionListener.Counters> entry : values.entrySet())
      {
         AttributionListener.Key key = entry.getKey();
         AttributionListener.Counters counts = entry.getValue();
         QRecord row = new QRecord().withValue("windowStart", start).withValue("windowEnd", end)
            .withValue("podName", truncate(runtime.getConfig().getPodName(), 200)).withValue("unitKind", key.kind())
            .withValue("unitName", truncate(key.name(), 300)).withValue("processName", truncate(key.process(), 200))
            .withValue("tableName", truncate(key.table(), 200)).withValue("fingerprint", key.fingerprint())
            .withValue("sqlText", truncate(counts.sql, 4000)).withValue("sampleRate", counts.sampleRate < 0 ? null : BigDecimal.valueOf(counts.sampleRate));
         if(repeat)
         {
            row.withValue("unitsAffected", counts.units.sum()).withValue("totalRepeats", counts.statements.sum())
               .withValue("maxRepeats", (int) Math.min(Integer.MAX_VALUE, counts.max.get()));
         }
         else
         {
            row.withValue("operation", key.operation()).withValue("units", counts.units.sum()).withValue("statements", counts.statements.sum())
               .withValue("totalMs", BigDecimal.valueOf(counts.nanos.sum(), 6)).withValue("maxMs", BigDecimal.valueOf(counts.max.get(), 6))
               .withValue("rowsCount", counts.rows.sum()).withValue("errors", counts.errors.sum());
         }
         records.add(row);
      }
      return (records);
   }


   /*******************************************************************************
    ** Bound text at the persistence boundary after normalization.
    *******************************************************************************/
   private static String truncate(String value, int length)
   {
      return (value == null || value.length() <= length ? value : value.substring(0, length));
   }


   /*******************************************************************************
    ** Reject record-level failures so the whole window transaction rolls back.
    *******************************************************************************/
   private static void insert(String table, List<QRecord> rows) throws Exception
   {
      if(!rows.isEmpty())
      {
         List<QRecord> inserted = InsertAction.executeForRecords(new InsertInput(table).withRecords(rows).withTransaction(QContext.getQBackendTransaction()));
         if(inserted.size() != rows.size() || inserted.stream().anyMatch(row -> row.getErrors() != null && !row.getErrors().isEmpty()))
         {
            throw (new IllegalStateException("Attribution insert failed"));
         }
      }
   }


   /*******************************************************************************
    ** Listener exposed for deterministic delivery and registration tests.
    *******************************************************************************/
   public AttributionListener getListener()
   {
      return (listener);
   }


   /*******************************************************************************
    ** Unregister first, stop scheduling, and flush one final detached window.
    *******************************************************************************/
   @Override
   public synchronized void close()
   {
      if(closed)
      {
         return;
      }
      closed = true;
      StatementExecutionListeners.unregister(listener);
      if(executor != null)
      {
         executor.shutdownNow();
      }
      if(shutdownHook != null && Thread.currentThread() != shutdownHook)
      {
         try
         {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
         }
         catch(IllegalStateException expected)
         {
            // JVM shutdown is already running the hook.
         }
      }
      flushNow();
   }
}
