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


import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import com.kingsrook.qbits.dbperformance.DbPerfQBitConfig;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.normalize.SqlNormalizer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instrumentation.StatementExecution;
import com.kingsrook.qqq.backend.core.instrumentation.StatementExecutionListener;
import com.kingsrook.qqq.backend.core.instrumentation.UnitOfWork;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Bounded, in-memory attribution. No table actions run on statement threads.
 ** Completed units belong to the window in which they end, including long units.
 *******************************************************************************/
public class AttributionListener implements StatementExecutionListener
{
   private static final String OTHER = "(other)";
   private static final Key OVERFLOW = new Key("OTHER", OTHER, OTHER, OTHER, "OTHER", SqlNormalizer.fingerprint(OTHER), -1);
   private static final ThreadLocal<Boolean> SUPPRESSED = ThreadLocal.withInitial(() -> false);
   private final DbPerfRuntime runtime;
   private final ConcurrentHashMap<String, Shape> shapes = new ConcurrentHashMap<>();
   private final ThreadLocal<Buffer> current = new ThreadLocal<>();
   private volatile Settings settings = new Settings(false, 1.0, 60, 10000, 10);
   private Window window = new Window(Instant.now(), new HashMap<>(), new HashMap<>());


   /*******************************************************************************
    ** Bind to one instance and monitored backend.
    *******************************************************************************/
   public AttributionListener(DbPerfRuntime runtime)
   {
      this.runtime = runtime;
   }


   /*******************************************************************************
    ** Ignore self-observation, then sample and count without database access.
    *******************************************************************************/
   @Override
   public void onStatement(StatementExecution execution)
   {
      Settings copy = settings;
      if(!copy.enabled() || !Boolean.TRUE.equals(runtime.getConfig().getAttributionEnabled()) || SUPPRESSED.get()
         || !runtime.getConfig().getBackendName().equals(execution.backendName())
         || (execution.tableName() != null && execution.tableName().startsWith(DbPerfQBitConfig.TABLE_NAME_PREFIX))
         || (QContext.getQInstance() != null && QContext.getQInstance() != runtime.getInstance()))
      {
         return;
      }
      UnitOfWork unit = execution.unitOfWork();
      Buffer buffer = current.get();
      if(buffer != null && (unit == null || buffer.unit.getId() != unit.getId()))
      {
         merge(buffer);
         current.remove();
         buffer = null;
      }
      if(unit != null && buffer == null)
      {
         buffer = new Buffer(unit, copy, sampled(unit.getId(), copy.sampleRate()));
         current.set(buffer);
      }
      if(buffer == null ? !sampled(ThreadLocalRandom.current().nextLong(), copy.sampleRate()) : !buffer.sampled)
      {
         return;
      }
      Shape shape = shape(execution.sql());
      Key key = new Key(unit == null ? "OTHER" : unit.getKind().name(), unit == null ? name(execution.processName()) : "",
         execution.processName(), execution.tableName(), execution.operation() == null ? "OTHER" : execution.operation().name(), shape.fingerprint(), buffer == null ? copy.sampleRate() : buffer.settings.sampleRate());
      if(buffer == null)
      {
         synchronized(this)
         {
            Key target = bounded(window.attribution(), key, copy.maxKeys());
            Counters counts = window.attribution().computeIfAbsent(target, ignored -> new Counters(target == OVERFLOW ? OTHER : shape.sql(), key.sampleRate()));
            counts.sampleRate(key.sampleRate());
            counts.add(execution);
            counts.units.increment();
         }
      }
      else
      {
         Key target = bounded(buffer.counts, key, Math.max(2, buffer.settings.maxKeys()));
         buffer.counts.computeIfAbsent(target, ignored -> new Counters(target == OVERFLOW ? OTHER : shape.sql(), key.sampleRate())).add(execution);
      }
   }


   /*******************************************************************************
    ** Read the final route name, then release all per-thread counters.
    *******************************************************************************/
   @Override
   public void onUnitOfWorkEnd(UnitOfWork unit)
   {
      Buffer buffer = current.get();
      if(buffer != null && buffer.unit.getId() == unit.getId())
      {
         current.remove();
         if(settings.enabled() && Boolean.TRUE.equals(runtime.getConfig().getAttributionEnabled()))
         {
            merge(buffer);
         }
      }
   }


   /*******************************************************************************
    ** Merge each unit once per destination, even when several shapes overflow.
    ** Repeat counts are combined across operations before threshold evaluation.
    *******************************************************************************/
   private synchronized void merge(Buffer buffer)
   {
      Set<Key> touched = new HashSet<>();
      Map<Key, Counters> repeats = new HashMap<>();
      for(Map.Entry<Key, Counters> entry : buffer.counts.entrySet())
      {
         Key local = entry.getKey();
         Key key = local == OVERFLOW ? OVERFLOW : new Key(local.kind(), name(buffer.unit.getName()), local.process(), local.table(), local.operation(), local.fingerprint(), buffer.settings.sampleRate());
         Key target = bounded(window.attribution(), key, settings.maxKeys());
         Counters counts = window.attribution().computeIfAbsent(target, ignored -> new Counters(target == OVERFLOW ? OTHER : entry.getValue().sql, entry.getValue().sampleRate));
         counts.merge(entry.getValue());
         if(touched.add(target))
         {
            counts.units.increment();
         }
         // Collapsed shapes cannot prove repetition of any one fingerprint.
         if(local != OVERFLOW)
         {
            Key repeat = new Key(key.kind(), key.name(), key.process(), key.table(), null, key.fingerprint(), key.sampleRate());
            repeats.computeIfAbsent(repeat, ignored -> new Counters(entry.getValue().sql, entry.getValue().sampleRate)).merge(entry.getValue());
         }
      }
      Map<Key, Long> totals = new HashMap<>();
      Map<Key, Long> maxima = new HashMap<>();
      for(Map.Entry<Key, Counters> entry : repeats.entrySet())
      {
         long count = entry.getValue().statements.sum();
         if(count >= buffer.settings.threshold())
         {
            Key target = bounded(window.repeats(), entry.getKey(), settings.maxKeys());
            window.repeats().computeIfAbsent(target, ignored -> new Counters(target == OVERFLOW ? OTHER : entry.getValue().sql, entry.getValue().sampleRate));
            window.repeats().get(target).sampleRate(entry.getValue().sampleRate);
            totals.merge(target, count, Long::sum);
            maxima.merge(target, count, Math::max);
         }
      }
      for(Map.Entry<Key, Long> entry : totals.entrySet())
      {
         Counters counts = window.repeats().get(entry.getKey());
         counts.units.increment();
         counts.statements.add(entry.getValue());
         counts.max.accumulateAndGet(maxima.get(entry.getKey()), Math::max);
      }
   }


   /*******************************************************************************
    ** Reserve one slot for a universal overflow key, preserving the strict cap.
    *******************************************************************************/
   private static Key bounded(Map<Key, Counters> counts, Key key, int limit)
   {
      return (counts.containsKey(key) || (key != OVERFLOW && counts.size() < limit - (counts.containsKey(OVERFLOW) ? 0 : 1)) ? key : OVERFLOW);
   }


   /*******************************************************************************
    ** Normalize once per raw SQL shape; cold-cache admission alone is serialized.
    *******************************************************************************/
   private Shape shape(String sql)
   {
      String raw = sql == null ? "" : sql;
      Shape cached = shapes.get(raw);
      if(cached != null)
      {
         return (cached);
      }
      String normalized = SqlNormalizer.normalize(raw);
      Shape created = new Shape(normalized, SqlNormalizer.fingerprint(normalized));
      synchronized(shapes)
      {
         if(shapes.size() >= 5000)
         {
            shapes.clear();
         }
         shapes.put(raw, created);
      }
      return (created);
   }


   /*******************************************************************************
    ** SplitMix64 finalizer distributes sequential unit IDs without allocations.
    *******************************************************************************/
   private static boolean sampled(long id, double rate)
   {
      long hash = (id ^ (id >>> 30)) * 0xbf58476d1ce4e5b9L;
      hash = (hash ^ (hash >>> 27)) * 0x94d049bb133111ebL;
      hash ^= hash >>> 31;
      return ((hash >>> 11) * 0x1.0p-53 < rate);
   }


   /*******************************************************************************
    ** Use a stable label for units without a name.
    *******************************************************************************/
   private static String name(String value)
   {
      return (value == null || value.isBlank() ? "(none)" : value);
   }


   /*******************************************************************************
    ** Detach an immutable-in-practice window; no writers retain its counters.
    *******************************************************************************/
   synchronized Window drain(Instant end)
   {
      Window previous = window;
      window = new Window(end, new HashMap<>(), new HashMap<>());
      return (previous);
   }


   /*******************************************************************************
    ** Publish a validated immutable settings copy only from the flusher.
    *******************************************************************************/
   void refresh(QRecord row)
   {
      double rate = row.getValueBigDecimal("attributionSampleRate") == null ? 1.0 : row.getValueBigDecimal("attributionSampleRate").doubleValue();
      settings = new Settings(!Boolean.FALSE.equals(row.getValueBoolean("attributionEnabled")), Math.max(0, Math.min(1, rate)),
         positive(row, "attributionFlushSeconds", 60), positive(row, "attributionMaxKeys", 10000), positive(row, "nPlusOneThreshold", 10));
   }


   /*******************************************************************************
    ** Invalid administrative values use defaults.
    *******************************************************************************/
   private static int positive(QRecord row, String field, int fallback)
   {
      Integer value = row.getValueInteger(field);
      return (value == null || value <= 0 ? fallback : value);
   }


   /*******************************************************************************
    ** Current settings for scheduling and window annotations.
    *******************************************************************************/
   Settings settings()
   {
      return (settings);
   }


   /*******************************************************************************
    ** Set thread suppression, returning the previous state for nested callers.
    *******************************************************************************/
   static boolean suppress(boolean value)
   {
      boolean previous = SUPPRESSED.get();
      if(value)
      {
         SUPPRESSED.set(true);
      }
      else
      {
         SUPPRESSED.remove();
      }
      return (previous);
   }


   /*******************************************************************************
    ** Immutable administrative settings.
    *******************************************************************************/
   record Settings(boolean enabled, double sampleRate, int flushSeconds, int maxKeys, int threshold)
   {
   }


   /*******************************************************************************
    ** Attribution dimensions; sampling transitions remain separately interpretable.
    ** Null operation denotes a repeat pattern.
    *******************************************************************************/
   record Key(String kind, String name, String process, String table, String operation, String fingerprint, double sampleRate)
   {
   }


   /*******************************************************************************
    ** Cached literal-free text and its digest.
    *******************************************************************************/
   private record Shape(String sql, String fingerprint)
   {
   }


   /*******************************************************************************
    ** Detached window transferred to the flusher.
    *******************************************************************************/
   record Window(Instant start, Map<Key, Counters> attribution, Map<Key, Counters> repeats)
   {
   }


   /*******************************************************************************
    ** Thread-confined unit, including its once-only sampling decision.
    *******************************************************************************/
   private static class Buffer
   {
      private final UnitOfWork unit;
      private final Settings settings;
      private final boolean sampled;
      private final Map<Key, Counters> counts = new HashMap<>();


      /*******************************************************************************
       ** Capture unit settings so refresh cannot partially sample a unit.
       *******************************************************************************/
      private Buffer(UnitOfWork unit, Settings settings, boolean sampled)
      {
         this.unit = unit;
         this.settings = settings;
         this.sampled = sampled;
      }
   }


   /*******************************************************************************
    ** Additive counters plus an atomic maximum, shared by both window tables.
    *******************************************************************************/
   static class Counters
   {
      final String sql;
      double sampleRate;
      final LongAdder units = new LongAdder();
      final LongAdder statements = new LongAdder();
      final LongAdder nanos = new LongAdder();
      final AtomicLong max = new AtomicLong();
      final LongAdder rows = new LongAdder();
      final LongAdder errors = new LongAdder();


      /*******************************************************************************
       ** Retain only normalized SQL.
       *******************************************************************************/
      Counters(String sql, double sampleRate)
      {
         this.sql = sql;
         this.sampleRate = sampleRate;
      }


      /*******************************************************************************
       ** Count one completed statement, including failures and unknown row counts.
       *******************************************************************************/
      void add(StatementExecution execution)
      {
         statements.increment();
         nanos.add(execution.elapsedNanos());
         max.accumulateAndGet(execution.elapsedNanos(), Math::max);
         rows.add(execution.rowCount() == null ? 0 : execution.rowCount());
         if(!execution.succeeded())
         {
            errors.increment();
         }
      }


      /*******************************************************************************
       ** An overflow bucket spanning sampling rates has no single valid rate.
       *******************************************************************************/
      void sampleRate(double rate)
      {
         if(sampleRate != rate)
         {
            sampleRate = -1;
         }
      }


      /*******************************************************************************
       ** Fold statement totals; distinct units are managed by the caller.
       *******************************************************************************/
      void merge(Counters other)
      {
         sampleRate(other.sampleRate);
         statements.add(other.statements.sum());
         nanos.add(other.nanos.sum());
         max.accumulateAndGet(other.max.get(), Math::max);
         rows.add(other.rows.sum());
         errors.add(other.errors.sum());
      }
   }
}
