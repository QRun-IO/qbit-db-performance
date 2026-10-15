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
import java.sql.DriverManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qbits.dbperformance.DbPerfTestSupport;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.collect.RollupService;
import com.kingsrook.qbits.dbperformance.metadata.DbPerfWriteGuard;
import com.kingsrook.qbits.dbperformance.normalize.SqlNormalizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instrumentation.StatementExecution;
import com.kingsrook.qqq.backend.core.instrumentation.StatementExecutionListener;
import com.kingsrook.qqq.backend.core.instrumentation.StatementExecutionListeners;
import com.kingsrook.qqq.backend.core.instrumentation.StatementOperation;
import com.kingsrook.qqq.backend.core.instrumentation.UnitOfWork;
import com.kingsrook.qqq.backend.core.instrumentation.UnitOfWorkKind;
import com.kingsrook.qqq.backend.core.instrumentation.UnitsOfWork;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Real QQQ RDBMS hooks, persistence, bounded counters, and lifecycle contracts.
 *******************************************************************************/
class AttributionTest
{
   private DbPerfRuntime runtime;
   private AttributionFlusher flusher;
   private AttributionListener listener;
   private QRecord settings;


   /*******************************************************************************
    ** A real business table, separate from the self-suppressed db_perf tables.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      QInstance instance = DbPerfTestSupport.instance();
      runtime = DbPerfRuntime.of(instance);
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) instance.getBackend(runtime.getConfig().getBackendName());
      instance.addTable(new QTableMetaData().withName("business").withBackendName(backend.getName()).withPrimaryKeyField("id")
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("business"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)).withField(new QFieldMetaData("name", QFieldType.STRING)));
      try(var connection = DriverManager.getConnection(backend.getJdbcUrl(), backend.getUsername(), backend.getPassword()); var statement = connection.createStatement())
      {
         statement.execute("CREATE TABLE business (id INT PRIMARY KEY, name VARCHAR(100))");
         statement.execute("INSERT INTO business VALUES (1, 'example')");
      }
      flusher = new AttributionFlusher(runtime);
      flusher.refreshSettings();
      listener = flusher.getListener();
      settings = DbPerfStore.query("db_perf_settings", new QQueryFilter()).getFirst();
      StatementExecutionListeners.register(listener);
   }


   /*******************************************************************************
    ** No listener, scope, suppression or runtime may escape a test.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      StatementExecutionListeners.unregister(listener);
      flusher.close();
      runtime.stopAttribution();
      UnitsOfWork.reset();
      AttributionListener.suppress(false);
      QContext.clear();
   }


   /*******************************************************************************
    ** The core hook attributes real queries and reads final HTTP route names.
    *******************************************************************************/
   @Test
   void realQueriesAndFinalNames() throws Exception
   {
      try(var scope = UnitsOfWork.begin(UnitOfWorkKind.SCHEDULED_JOB, "job"))
      {
         query(2);
      }
      try(var scope = UnitsOfWork.begin(UnitOfWorkKind.HTTP_REQUEST, "unresolved"))
      {
         query(1);
         scope.getUnitOfWork().setName("GET /things/{id}");
      }
      query(1);
      flusher.flushNow();
      List<QRecord> rows = rows("db_perf_attribution");
      assertThat(rows).hasSize(3);
      assertThat(rows).extracting(row -> row.getValueString("unitName")).containsExactlyInAnyOrder("job", "GET /things/{id}", "(none)");
      QRecord job = named(rows, "job");
      assertThat(job.getValueString("unitKind")).isEqualTo("SCHEDULED_JOB");
      assertThat(job.getValueLong("units")).isEqualTo(1);
      assertThat(job.getValueLong("statements")).isEqualTo(2);
      assertThat(job.getValueLong("rowsCount")).isEqualTo(2);
      assertThat(job.getValueString("operation")).isEqualTo("QUERY");
      assertThat(job.getValueString("tableName")).isEqualTo("business");
      assertThat(job.getValueString("fingerprint")).isEqualTo(SqlNormalizer.fingerprint(job.getValueString("sqlText")));
      assertThat(named(rows, "(none)").getValueString("unitKind")).isEqualTo("OTHER");
   }


   /*******************************************************************************
    ** The threshold applies to each unit, and each window uses one batch/table.
    *******************************************************************************/
   @Test
   void repeatPatternsBatchesAndSelfSuppression() throws Exception
   {
      AtomicInteger attributionInserts = new AtomicInteger();
      AtomicInteger repeatInserts = new AtomicInteger();
      StatementExecutionListener observer = execution ->
      {
         if(execution.operation() == StatementOperation.INSERT)
         {
            if("db_perf_attribution".equals(execution.tableName()))
            {
               attributionInserts.incrementAndGet();
            }
            if("db_perf_repeat_pattern".equals(execution.tableName()))
            {
               repeatInserts.incrementAndGet();
            }
         }
      };
      StatementExecutionListeners.register(observer);
      try
      {
         for(int count : List.of(12, 5))
         {
            try(var scope = UnitsOfWork.begin(UnitOfWorkKind.SCHEDULED_JOB, "job"))
            {
               query(count);
            }
         }
         rows("db_perf_settings");
         flusher.flushNow();
         QRecord repeated = rows("db_perf_repeat_pattern").getFirst();
         assertThat(repeated.getValueLong("unitsAffected")).isEqualTo(1);
         assertThat(repeated.getValueInteger("maxRepeats")).isEqualTo(12);
         assertThat(repeated.getValueLong("totalRepeats")).isEqualTo(12);
         assertThat(repeated.getValueBigDecimal("sampleRate")).isEqualByComparingTo("1");
         assertThat(rows("db_perf_repeat_pattern")).hasSize(1);
         assertThat(rows("db_perf_attribution")).singleElement().satisfies(row ->
         {
            assertThat(row.getValueLong("units")).isEqualTo(2);
            assertThat(row.getValueLong("statements")).isEqualTo(17);
         });
         flusher.flushNow();
         assertThat(attributionInserts).hasValue(1);
         assertThat(repeatInserts).hasValue(1);
         assertThat(listener.drain(Instant.now()).attribution()).isEmpty();
      }
      finally
      {
         StatementExecutionListeners.unregister(observer);
      }
   }


   /*******************************************************************************
    ** Both runtime switches and sample zero take effect through cached settings.
    *******************************************************************************/
   @Test
   void disabledSamplingAndFilters() throws Exception
   {
      settings.withValue("attributionSampleRate", BigDecimal.ZERO);
      refresh();
      query(1);
      try(var scope = UnitsOfWork.begin(UnitOfWorkKind.PROCESS, "zero"))
      {
         query(1);
      }
      assertThat(listener.drain(Instant.now()).attribution()).isEmpty();
      settings.withValue("attributionSampleRate", BigDecimal.ONE).withValue("attributionEnabled", false);
      refresh();
      query(1);
      assertThat(listener.drain(Instant.now()).attribution()).isEmpty();
      settings.withValue("attributionEnabled", true);
      refresh();
      runtime.getConfig().setAttributionEnabled(false);
      query(1);
      runtime.getConfig().setAttributionEnabled(true);
      listener.onStatement(new StatementExecution("wrong", "business", StatementOperation.QUERY, "select ?", 1, 1, true, null, null));
      AttributionListener.suppress(true);
      query(1);
      AttributionListener.suppress(false);
      rows("db_perf_settings");
      assertThat(listener.drain(Instant.now()).attribution()).isEmpty();
      query(1);
      assertThat(listener.drain(Instant.now()).attribution()).hasSize(1);
   }


   /*******************************************************************************
    ** The cap includes the overflow bucket; a unit counts only once in it.
    *******************************************************************************/
   @Test
   void overflowAndRepeatBounds() throws Exception
   {
      settings.withValue("attributionMaxKeys", 2).withValue("nPlusOneThreshold", 2);
      refresh();
      try(var scope = UnitsOfWork.begin(UnitOfWorkKind.PROCESS, "many-shapes"))
      {
         for(int i = 0; i < 8; i++)
         {
            synthetic("select column_" + i + " from business", 1, true, scope.getUnitOfWork());
         }
      }
      for(int i = 0; i < 8; i++)
      {
         try(var scope = UnitsOfWork.begin(UnitOfWorkKind.PROCESS, "job_" + i))
         {
            synthetic("select column_" + i + " from business", 1, true, scope.getUnitOfWork());
            synthetic("select column_" + i + " from business", 1, true, scope.getUnitOfWork());
         }
      }
      flusher.flushNow();
      assertThat(rows("db_perf_attribution")).hasSize(2);
      assertThat(named(rows("db_perf_attribution"), "(other)").getValueLong("units")).isEqualTo(9);
      assertThat(rows("db_perf_attribution").stream().mapToLong(row -> row.getValueLong("statements")).sum()).isEqualTo(24);
      assertThat(rows("db_perf_repeat_pattern")).hasSize(2);
      assertThat(named(rows("db_perf_repeat_pattern"), "(other)").getValueString("fingerprint")).isEqualTo(SqlNormalizer.fingerprint("(other)"));
   }


   /*******************************************************************************
    ** Counters, normalization, null rows, process fallback, and text bounds.
    *******************************************************************************/
   @Test
   void syntheticCountersAndSqlSafety()
   {
      String sql = "select 'secret', 123 from business where id in (?, ?, ?)";
      synthetic(sql, 2000000, true, null);
      synthetic(sql, 5000000, false, null);
      listener.onStatement(new StatementExecution(runtime.getConfig().getBackendName(), null, null, null, 10, null, true, null, "process"));
      flusher.flushNow();
      List<QRecord> rows = rows("db_perf_attribution");
      QRecord row = named(rows, "(none)");
      assertThat(row.getValueBigDecimal("totalMs")).isEqualByComparingTo("7");
      assertThat(row.getValueBigDecimal("maxMs")).isEqualByComparingTo("5");
      assertThat(row.getValueLong("errors")).isEqualTo(1);
      assertThat(row.getValueLong("rowsCount")).isEqualTo(2);
      assertThat(row.getValueString("sqlText")).doesNotContain("secret", "123");
      assertThat(named(rows, "process").getValueString("unitKind")).isEqualTo("OTHER");
      synthetic("select " + "identifier".repeat(600) + " from business", 1, true, null);
      flusher.flushNow();
      assertThat(rows("db_perf_attribution")).anySatisfy(value -> assertThat(value.getValueString("sqlText")).hasSize(4000));
   }


   /*******************************************************************************
    ** An abandoned unit is merged when another unit arrives on that worker.
    *******************************************************************************/
   @Test
   void defensiveUnitTransitionAndWindowBoundary() throws Exception
   {
      var abandoned = UnitsOfWork.begin(UnitOfWorkKind.PROCESS, "abandoned");
      query(1);
      UnitsOfWork.reset();
      try(var scope = UnitsOfWork.begin(UnitOfWorkKind.HTTP_REQUEST, "initial"))
      {
         query(1);
         flusher.flushNow();
         assertThat(rows("db_perf_attribution")).singleElement().satisfies(row -> assertThat(row.getValueString("unitName")).isEqualTo("abandoned"));
         scope.getUnitOfWork().setName("final");
         query(1);
      }
      abandoned.close();
      flusher.flushNow();
      assertThat(named(rows("db_perf_attribution"), "final").getValueLong("statements")).isEqualTo(2);
   }


   /*******************************************************************************
    ** Each sampled unit contributes all statements, including across a refresh.
    *******************************************************************************/
   @Test
   void samplingIsPerUnit() throws Exception
   {
      settings.withValue("attributionSampleRate", new BigDecimal("0.5"));
      refresh();
      for(int i = 0; i < 100; i++)
      {
         try(var scope = UnitsOfWork.begin(UnitOfWorkKind.PROCESS, "sample_" + i))
         {
            for(int j = 0; j < 3; j++)
            {
               synthetic("select ?", 1, true, scope.getUnitOfWork());
            }
         }
      }
      var window = listener.drain(Instant.now());
      assertThat(window.attribution().size()).isBetween(20, 80);
      assertThat(window.attribution().values()).allSatisfy(counts -> assertThat(counts.statements.sum()).isEqualTo(3));
   }


   /*******************************************************************************
    ** An in-flight unit keeps its sampling decision and original rate annotation.
    *******************************************************************************/
   @Test
   void samplingRefreshDuringUnit() throws Exception
   {
      try(var scope = UnitsOfWork.begin(UnitOfWorkKind.PROCESS, "in-flight"))
      {
         query(1);
         settings.withValue("attributionSampleRate", BigDecimal.ZERO);
         refresh();
         query(1);
      }
      query(1);
      flusher.flushNow();
      assertThat(rows("db_perf_attribution")).singleElement().satisfies(row ->
      {
         assertThat(row.getValueLong("statements")).isEqualTo(2);
         assertThat(row.getValueBigDecimal("sampleRate")).isEqualByComparingTo("1");
      });
   }


   /*******************************************************************************
    ** Atomic drains cannot lose or double-count concurrent statement delivery.
    *******************************************************************************/
   @Test
   void concurrentDrains() throws Exception
   {
      var pool = Executors.newFixedThreadPool(4);
      try
      {
         List<java.util.concurrent.Future<?>> tasks = new ArrayList<>();
         for(int i = 0; i < 4; i++)
         {
            tasks.add(pool.submit(() ->
            {
               for(int j = 0; j < 5000; j++)
               {
                  synthetic("select ?", 1, true, null);
               }
            }));
         }
         long total = 0;
         while(tasks.stream().anyMatch(task -> !task.isDone()))
         {
            total += listener.drain(Instant.now()).attribution().values().stream().mapToLong(counts -> counts.statements.sum()).sum();
            Thread.yield();
         }
         for(var task : tasks)
         {
            task.get();
         }
         total += listener.drain(Instant.now()).attribution().values().stream().mapToLong(counts -> counts.statements.sum()).sum();
         assertThat(total).isEqualTo(20000);
      }
      finally
      {
         pool.shutdownNow();
      }
   }


   /*******************************************************************************
    ** Cached hot-path overhead stays below a generous two-second budget.
    *******************************************************************************/
   @Test
   void overhead()
   {
      StatementExecution execution = new StatementExecution(runtime.getConfig().getBackendName(), "business", StatementOperation.QUERY, "select ?", 1, 1, true, null, null);
      listener.onStatement(execution);
      long start = System.nanoTime();
      for(int i = 0; i < 100000; i++)
      {
         listener.onStatement(execution);
      }
      assertThat(System.nanoTime() - start).isLessThan(TimeUnit.SECONDS.toNanos(2));
      assertThat(listener.drain(Instant.now()).attribution().values().iterator().next().statements.sum()).isEqualTo(100001);
   }


   /*******************************************************************************
    ** Launcher lifecycle registers once, flushes on stop, and respects config.
    *******************************************************************************/
   @Test
   void runtimeLifecycle() throws Exception
   {
      flusher.close();
      assertThat(StatementExecutionListeners.isActive()).isFalse();
      AttributionRuntimeService service = new AttributionRuntimeService();
      assertThat(service.getName()).isEqualTo("dbPerformanceAttribution");
      service.stop();
      service.start(runtime.getInstance());
      runtime.startAttribution();
      assertThat(StatementExecutionListeners.isActive()).isTrue();
      query(1);
      service.stop();
      service.stop();
      assertThat(StatementExecutionListeners.isActive()).isFalse();
      assertThat(rows("db_perf_attribution")).hasSize(1);
      runtime.getConfig().setAttributionEnabled(false);
      service.start(runtime.getInstance());
      assertThat(StatementExecutionListeners.isActive()).isFalse();
      service.stop();
   }


   /*******************************************************************************
    ** Real flusher rows roll up across pods, preserving max and additive totals.
    *******************************************************************************/
   @Test
   void rollupMergesPods() throws Exception
   {
      for(int i = 1; i <= 2; i++)
      {
         runtime.getConfig().setPodName("pod_" + i);
         try(var scope = UnitsOfWork.begin(UnitOfWorkKind.PROCESS, "job"))
         {
            synthetic("select ?", i * 1000000L, true, scope.getUnitOfWork());
            synthetic("select ?", i * 1000000L, false, scope.getUnitOfWork());
         }
         flusher.flushNow();
      }
      new RollupService(runtime).rollupDay(LocalDate.now(ZoneOffset.UTC));
      assertThat(rows("db_perf_attribution_daily")).singleElement().satisfies(row ->
      {
         assertThat(row.getValueLong("units")).isEqualTo(2);
         assertThat(row.getValueLong("statements")).isEqualTo(4);
         assertThat(row.getValueBigDecimal("totalMs")).isEqualByComparingTo("6");
         assertThat(row.getValueBigDecimal("maxMs")).isEqualByComparingTo("2");
         assertThat(row.getValueLong("rowsCount")).isEqualTo(4);
         assertThat(row.getValueLong("errors")).isEqualTo(2);
      });
   }


   /*******************************************************************************
    ** Large windows use one QQQ insert action even when the backend pages SQL.
    *******************************************************************************/
   @Test
   void largeWindowUsesOneInsertAction() throws Exception
   {
      CountingInsert.inputs.clear();
      for(String table : List.of("db_perf_attribution", "db_perf_repeat_pattern"))
      {
         runtime.getInstance().getTable(table).getCustomizers().put(TableCustomizers.PRE_INSERT_RECORD.getRole(), new QCodeReference(CountingInsert.class));
      }
      settings.withValue("nPlusOneThreshold", 2);
      refresh();
      for(int i = 0; i < 1005; i++)
      {
         try(var scope = UnitsOfWork.begin(UnitOfWorkKind.PROCESS, "job_" + i))
         {
            synthetic("select ?", 1, true, scope.getUnitOfWork());
            synthetic("select ?", 1, true, scope.getUnitOfWork());
         }
      }
      flusher.flushNow();
      assertThat(rows("db_perf_attribution")).hasSize(1005);
      assertThat(rows("db_perf_repeat_pattern")).hasSize(1005);
      assertThat(CountingInsert.inputs).extracting(InsertInput::getTableName).containsExactlyInAnyOrder("db_perf_attribution", "db_perf_repeat_pattern");
      assertThat(CountingInsert.inputs).allSatisfy(input -> assertThat(input.getRecords()).hasSize(1005));
   }


   /*******************************************************************************
    ** An insert failure drops the window and never poisons a later good window.
    *******************************************************************************/
   @Test
   void failedWindowIsDropped() throws Exception
   {
      RDBMSTableBackendDetails details = (RDBMSTableBackendDetails) runtime.getInstance().getTable("db_perf_attribution").getBackendDetails();
      query(1);
      details.setTableName("missing_attribution_table");
      flusher.flushNow();
      details.setTableName("db_perf_attribution");
      flusher.flushNow();
      assertThat(rows("db_perf_attribution")).isEmpty();
      query(1);
      flusher.flushNow();
      assertThat(rows("db_perf_attribution")).singleElement().satisfies(row -> assertThat(row.getValueLong("statements")).isEqualTo(1));
   }


   /*******************************************************************************
    ** Cold-cache overflow remains bounded and later hits keep the right shape.
    *******************************************************************************/
   @Test
   void normalizationCacheOverflow()
   {
      for(int i = 0; i < 5010; i++)
      {
         synthetic("select column_" + i + " from business", 1, true, null);
      }
      synthetic("select column_0 from business", 1, true, null);
      var window = listener.drain(Instant.now());
      assertThat(window.attribution()).hasSize(5010);
      String fingerprint = SqlNormalizer.fingerprint(SqlNormalizer.normalize("select column_0 from business"));
      assertThat(window.attribution().entrySet().stream().filter(entry -> entry.getKey().fingerprint().equals(fingerprint)).findFirst().orElseThrow().getValue().statements.sum()).isEqualTo(2);
   }


   /*******************************************************************************
    ** The daemon executes on its own system context and honors a short interval.
    *******************************************************************************/
   @Test
   void scheduledFlush() throws Exception
   {
      settings.withValue("attributionFlushSeconds", 1);
      refresh();
      flusher.start();
      flusher.start();
      query(1);
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      while(rows("db_perf_attribution").isEmpty() && System.nanoTime() < deadline)
      {
         Thread.sleep(25);
      }
      assertThat(rows("db_perf_attribution")).hasSize(1);
      assertThat(QContext.getQInstance()).isSameAs(runtime.getInstance());
      flusher.close();
      flusher.start();
      assertThat(StatementExecutionListeners.isActive()).isFalse();
   }


   /*******************************************************************************
    ** Deliver a statement with exact counter values.
    *******************************************************************************/
   private void synthetic(String sql, long nanos, boolean succeeded, UnitOfWork unit)
   {
      listener.onStatement(new StatementExecution(runtime.getConfig().getBackendName(), "business", StatementOperation.QUERY, sql, nanos, 1, succeeded, unit, null));
   }


   /*******************************************************************************
    ** Execute real RDBMS-backed QQQ queries.
    *******************************************************************************/
   private void query(int count) throws Exception
   {
      for(int i = 0; i < count; i++)
      {
         new QueryAction().execute(new QueryInput("business").withFilter(new QQueryFilter()));
      }
   }


   /*******************************************************************************
    ** Persist administrator settings, then explicitly refresh the cached copy.
    *******************************************************************************/
   private void refresh() throws Exception
   {
      DbPerfStore.update("db_perf_settings", List.of(settings));
      flusher.refreshSettings();
   }


   /*******************************************************************************
    ** Read persisted evidence with a concise test failure on action errors.
    *******************************************************************************/
   private List<QRecord> rows(String table)
   {
      try
      {
         return (DbPerfStore.query(table, new QQueryFilter()));
      }
      catch(Exception e)
      {
         throw (new AssertionError(e));
      }
   }


   /*******************************************************************************
    ** Find one named aggregate.
    *******************************************************************************/
   private static QRecord named(List<QRecord> rows, String name)
   {
      return (rows.stream().filter(row -> name.equals(row.getValueString("unitName"))).findFirst().orElseThrow());
   }

   /*******************************************************************************
    ** Observe whole QQQ actions while preserving the production write guard.
    *******************************************************************************/
   public static class CountingInsert extends DbPerfWriteGuard
   {
      private static final List<InsertInput> inputs = new ArrayList<>();


      /*******************************************************************************
       ** Capture each action's original input before the backend pages its SQL.
       *******************************************************************************/
      @Override
      public List<QRecord> preInsert(InsertInput input, List<QRecord> records, boolean preview) throws QException
      {
         inputs.add(input);
         return (super.preInsert(input, records, preview));
      }
   }
}
