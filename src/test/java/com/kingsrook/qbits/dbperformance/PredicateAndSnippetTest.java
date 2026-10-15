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


import java.util.List;
import com.kingsrook.qbits.dbperformance.analyze.MigrationSnippets;
import com.kingsrook.qbits.dbperformance.analyze.PredicateParser;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Best-effort SQL parsing and ready-to-review PostgreSQL migration text.
 *******************************************************************************/
class PredicateAndSnippetTest
{
   /*******************************************************************************
    ** Bind values, comments, aliases and quoted columns retain table ownership.
    *******************************************************************************/
   @Test
   void predicates()
   {
      assertThat(PredicateParser.candidates("SELECT * FROM public.orders o JOIN customers c ON c.id = o.customer_id WHERE o.status IN ($1, $2) AND c.name = $3", "public", "orders"))
         .containsExactly("customer_id", "status");
      assertThat(PredicateParser.candidates("select * from orders where id = ? and status in (?)", "public", "orders")).containsExactly("id", "status");
      assertThat(PredicateParser.candidates("select * from orders where lower(name) = ? order by created_at", "public", "orders")).isEmpty();
      assertThat(PredicateParser.candidates("select * from orders where id in (select id from customers)", "public", "orders")).isEmpty();
      assertThat(PredicateParser.candidates("select * from other.orders where id = ?", "public", "orders")).isEmpty();
      assertThat(PredicateParser.candidates("select * from \"Order\" where \"Customer ID\" = 'private' /* where fake = ? */", "public", "Order")).containsExactly("Customer ID");
      assertThat(PredicateParser.indexColumns("\"last,name\", id")).containsExactly("last,name", "id");
      assertThat(PredicateParser.indexColumns("lower(name)")).isEmpty();
      assertThat(PredicateParser.indexColumns("id,")).isEmpty();
      assertThat(PredicateParser.indexColumns(null)).isEmpty();
   }


   /*******************************************************************************
    ** Concurrent DDL has a transaction opt-out and schema-qualified rollback.
    *******************************************************************************/
   @Test
   void snippets()
   {
      QRecord table = new QRecord().withValue("schemaName", "public").withValue("tableName", "order");
      String create = MigrationSnippets.create(B1Fixture.NOW, table, List.of("Customer ID", "status"), "1000000 reads/day\nreview first");
      assertThat(create).startsWith("-- liquibase formatted sql\n-- changeset db-performance:20261015-")
         .contains("runInTransaction:false", "CREATE INDEX CONCURRENTLY IF NOT EXISTS", "ON public.\"order\" (\"Customer ID\", status)", "-- rollback DROP INDEX CONCURRENTLY IF EXISTS public.")
         .contains("1000000 reads/day review first");
      String name = MigrationSnippets.indexName("really_long_table_".repeat(8), List.of("one", "two"));
      assertThat(name.length()).isLessThanOrEqualTo(63);
      assertThat(name).matches("[a-z_][a-z0-9_]*");
      assertThat(MigrationSnippets.indexName("orders", List.of("id"))).isEqualTo("idx_orders_id");
      QRecord index = new QRecord().withValue("schemaName", "sales").withValue("indexName", "idx_orders_id")
         .withValue("definition", "CREATE INDEX idx_orders_id ON sales.orders USING btree (id)");
      assertThat(MigrationSnippets.drop(B1Fixture.NOW, index, "UNUSED_INDEX", "0 scans"))
         .contains("DROP INDEX CONCURRENTLY IF EXISTS sales.idx_orders_id;", "-- rollback CREATE INDEX CONCURRENTLY idx_orders_id ON sales.orders USING btree (id);");
      assertThat(MigrationSnippets.drop(B1Fixture.NOW, index.withValue("definition", null), "UNUSED_INDEX", "0 scans")).isNull();
      assertThat(MigrationSnippets.vacuum(B1Fixture.NOW, table, "30% dead"))
         .contains("SET (autovacuum_vacuum_scale_factor = 0.05)", "-- rollback ALTER TABLE public.\"order\" RESET");
      assertThat(MigrationSnippets.quote("a\"b")).isEqualTo("\"a\"\"b\"");
   }
}
