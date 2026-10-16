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


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qbits.dbperformance.analyze.RuleContext;
import com.kingsrook.qbits.dbperformance.collect.DbPerfStore;
import com.kingsrook.qbits.dbperformance.collect.RollupService;
import com.kingsrook.qbits.dbperformance.widgets.DbPerfWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.MultiTableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Attribution detail grows by hundreds of thousands of rows a day. On
 ** 2026-10-16 the home widget loaded a whole day of it into the heap and ran
 ** the kof22 web pod out of memory, and the hourly analysis filled the worker
 ** heap the same way. These tests count every attribution detail record QQQ
 ** materializes, so the widget, the analysis and the daily rollup cannot go
 ** back to reading the table in one unbounded query.
 *******************************************************************************/
class AttributionBoundedReadTest
{
   private static final AtomicInteger LOADED   = new AtomicInteger();
   private static final AtomicInteger MAX_PAGE = new AtomicInteger();

   private DbPerfRuntime runtime;



   /*******************************************************************************
    ** Counts the detail records each QueryAction hands back.
    *******************************************************************************/
   public static class CountingCustomizer implements TableCustomizerInterface
   {
      /***************************************************************************
       ** Record the page size, change nothing.
       ***************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface queryInput, List<QRecord> records)
      {
         LOADED.addAndGet(records.size());
         MAX_PAGE.accumulateAndGet(records.size(), Math::max);
         return (records);
      }
   }



   /*******************************************************************************
    ** Fresh H2 instance with the counter on the detail table.
    *******************************************************************************/
   @BeforeEach
   void setup() throws Exception
   {
      runtime = DbPerfRuntime.of(DbPerfTestSupport.instance());
      runtime.getInstance().getTable("db_perf_attribution").withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(CountingCustomizer.class));
      LOADED.set(0);
      MAX_PAGE.set(0);
   }



   /*******************************************************************************
    ** The widget sums in the database: no detail rows reach the heap, and the
    ** totals still match the detail.
    *******************************************************************************/
   @Test
   void widgetSumsInTheDatabase() throws Exception
   {
      Instant now = Instant.now();
      insert(3000, i -> row(now.minusSeconds(60), now, i));
      LOADED.set(0);

      MultiTableData attribution = new DbPerfWidgetRenderer().attribution();

      assertThat(LOADED.get()).as("attribution detail rows loaded by the widget").isZero();
      List<Map<String, Object>> units = attribution.getTableDataList().getFirst().getRows();
      assertThat(units).hasSize(3);
      // unit-0 holds i = 0, 3, 6 ... (1000 rows, 2 statements each), mostly against "patient"
      assertThat(((Number) units.getFirst().get("Statements")).doubleValue() + ((Number) units.get(1).get("Statements")).doubleValue()
         + ((Number) units.get(2).get("Statements")).doubleValue()).isEqualTo(6000.0);
      assertThat(units).allSatisfy(unit -> assertThat(((Number) unit.get("Statements")).doubleValue()).isEqualTo(2000.0));
      assertThat(JsonUtils.toJson(units.getFirst())).contains("top table patient");
      assertThat(units.stream().mapToDouble(unit -> ((Number) unit.get("Share %")).doubleValue()).sum()).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.001));
   }



   /*******************************************************************************
    ** Detail older than 24 hours is left out of the sums.
    *******************************************************************************/
   @Test
   void widgetKeepsTheDayWindow() throws Exception
   {
      Instant now = Instant.now();
      insert(3, i -> row(now.minusSeconds(60), now, i));
      insert(3, i -> row(now.minusSeconds(3 * 86400), now.minusSeconds(2 * 86400), i));
      List<Map<String, Object>> units = new DbPerfWidgetRenderer().attribution().getTableDataList().getFirst().getRows();
      assertThat(units).hasSize(3).allSatisfy(unit -> assertThat(((Number) unit.get("Statements")).doubleValue()).isEqualTo(2.0));
   }



   /*******************************************************************************
    ** The hourly analysis reads attribution as database sums per unit, table
    ** and operation: no detail rows, same totals.
    *******************************************************************************/
   @Test
   void analysisSumsInTheDatabase() throws Exception
   {
      Instant now = Instant.now();
      insert(3000, i -> row(now.minusSeconds(60), now, i));
      LOADED.set(0);
      RuleContext context = new RuleContext(runtime, runtime.getConfig().getBackendName(), new QRecord(), new QRecord(), now.plusSeconds(1));

      List<QRecord> rows = context.window("attribution", 1);

      assertThat(LOADED.get()).as("attribution detail rows loaded by the analysis").isZero();
      assertThat(rows).hasSize(6);
      assertThat(rows.stream().mapToDouble(row -> RuleContext.number(row, "statements")).sum()).isEqualTo(6000.0);
      assertThat(rows.stream().mapToDouble(row -> RuleContext.number(row, "totalMs")).sum()).isEqualTo(1500 * 10.0 + 1500 * 1.0);
      assertThat(rows).allSatisfy(row -> assertThat(row.getValueString("operation")).isEqualTo("SELECT"));
   }



   /*******************************************************************************
    ** Keyset pages never exceed the page size, and every row is seen once.
    *******************************************************************************/
   @Test
   void forEachPageIsBounded() throws Exception
   {
      Instant now = Instant.now();
      insert(1050, i -> row(now.minusSeconds(60), now, i));
      LOADED.set(0);
      List<Integer> sizes = new ArrayList<>();
      List<Object> ids = new ArrayList<>();
      DbPerfStore.forEachPage("db_perf_attribution", new QQueryFilter(), 100, page ->
      {
         sizes.add(page.size());
         page.forEach(row -> ids.add(row.getValue("id")));
      });
      assertThat(sizes).hasSize(11).allSatisfy(size -> assertThat(size).isLessThanOrEqualTo(100));
      assertThat(ids).hasSize(1050).doesNotHaveDuplicates();
      assertThat(MAX_PAGE.get()).isLessThanOrEqualTo(100);
   }



   /*******************************************************************************
    ** The daily rollup reads the day in BATCH_SIZE pages and its sums still
    ** cover every detail row across page boundaries.
    *******************************************************************************/
   @Test
   void rollupReadsTheDayInPages() throws Exception
   {
      ZoneId zone = ZoneId.of(runtime.getConfig().getTimeZone());
      LocalDate day = LocalDate.now(zone).minusDays(1);
      Instant start = day.atStartOfDay(zone).toInstant().plusSeconds(3600);
      int rows = DbPerfStore.BATCH_SIZE * 2 + 500;
      insert(rows, i -> row(start, start.plusSeconds(60), i));
      MAX_PAGE.set(0);
      LOADED.set(0);

      new RollupService(runtime).rollupDay(day);

      assertThat(MAX_PAGE.get()).as("largest page of attribution detail held at once").isLessThanOrEqualTo(DbPerfStore.BATCH_SIZE);
      assertThat(LOADED.get()).isEqualTo(rows);
      List<QRecord> daily = DbPerfStore.query("db_perf_attribution_daily", new QQueryFilter());
      assertThat(daily).hasSize(6);
      assertThat(daily.stream().mapToLong(row -> row.getValueLong("statements")).sum()).isEqualTo(2L * rows);
   }



   /*******************************************************************************
    ** Insert generated rows in batches.
    *******************************************************************************/
   private static void insert(int count, java.util.function.IntFunction<QRecord> factory) throws Exception
   {
      List<QRecord> rows = new ArrayList<>();
      for(int i = 0; i < count; i++)
      {
         rows.add(factory.apply(i));
      }
      DbPerfStore.insert("db_perf_attribution", rows);
   }



   /*******************************************************************************
    ** Three units; each unit spends most of its time on "patient".
    *******************************************************************************/
   private static QRecord row(Instant windowStart, Instant windowEnd, int i)
   {
      boolean patient = i % 6 < 3;
      return (new QRecord().withValue("windowStart", windowStart).withValue("windowEnd", windowEnd).withValue("podName", "pod-" + (i % 2))
         .withValue("unitKind", "PROCESS").withValue("unitName", "unit-" + (i % 3)).withValue("tableName", patient ? "patient" : "visit")
         .withValue("operation", "SELECT").withValue("fingerprint", "f".repeat(64)).withValue("sqlText", "select 1")
         .withValue("units", 1L).withValue("statements", 2L).withValue("rowsCount", 1L).withValue("errors", 0L)
         .withValue("totalMs", patient ? BigDecimal.TEN : BigDecimal.ONE).withValue("maxMs", BigDecimal.ONE));
   }
}
