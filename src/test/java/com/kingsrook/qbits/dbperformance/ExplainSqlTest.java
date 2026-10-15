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


import com.kingsrook.qbits.dbperformance.explain.ExplainSql;
import com.kingsrook.qbits.dbperformance.explain.PlanTextRenderer;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Lexical security, bind-slot identity and readable plan rendering without JDBC.
 *******************************************************************************/
class ExplainSqlTest
{



   /*******************************************************************************
    ** Quotes, nested comments and repeated parameter references retain their meaning.
    *******************************************************************************/
   @Test
   void lexicalBoundaries() throws Exception
   {
      var parsed = ExplainSql.parse("/* UPDATE /* DELETE */ */ SELECT ?, '?', E'foo\\'UPDATE', $$DELETE;$$, \"INTO\" -- DELETE\n ;");
      parsed.requireSelect();
      assertThat(parsed.parameterCount()).isEqualTo(1);
      assertThat(parsed.sql()).contains("SELECT $1", "'?'").doesNotContain("/*");
      assertThat(ExplainSql.parse("SELECT $1::int + $1, $2").parameterCount()).isEqualTo(2);
      assertThat(ExplainSql.parse("SELECT col ?| array['a'], col ?& array['b'] FROM example").parameterCount()).isZero();
      for(String sql : new String[] {"select $0", "select $999999999999999999", "select $2", "select ?, $1", "select 'bad", "select $$bad", "/*bad", "select 1; select 2", "", " ", "select 1;;"})
      {
         assertThatThrownBy(() -> ExplainSql.parse(sql)).isInstanceOf(Exception.class);
      }
      assertThatThrownBy(() -> ExplainSql.parse(null)).hasMessageContaining("Supply");
      assertThatThrownBy(() -> ExplainSql.parse("x".repeat(100001))).hasMessageContaining("100000");
      for(String sql : new String[] {"delete from x", "with x as (insert into t values (1)) select * from x", "select * from t for no key update", "select * from t for key share", "select * into t from x"})
      {
         assertThatThrownBy(() -> ExplainSql.parse(sql).requireSelect()).hasMessageContaining("single SELECT");
      }
      ExplainSql.parse("with x as (select 'UPDATE') select * from x").requireSelect();
   }



   /*******************************************************************************
    ** Render hierarchy and index detail; sanitize plan expressions in assistant prompts.
    *******************************************************************************/
   @Test
   void planTree() throws Exception
   {
      var plan = PlanTextRenderer.render("""
         [{"Plan":{"Node Type":"Nested Loop","Startup Cost":0.1,"Total Cost":42.5,"Plan Rows":2,
         "Plans":[{"Node Type":"Index Scan","Relation Name":"patient","Index Name":"patient_pk",
         "Index Cond":"(id = 987654)","Filter":"(name = 'private-name'::text)","Actual Rows":1}]},"Execution Time":1.25}]
         """);
      assertThat(plan.text()).contains("Nested Loop", "\n  Index Scan", "patient_pk", "Total Cost: 42.5", "Plan Rows: 2", "private-name");
      assertThat(plan.safeText()).doesNotContain("private-name", "987654").contains("id = ?", "name = ?");
      assertThat(plan.totalCost()).isEqualByComparingTo("42.5");
      assertThat(plan.actualTotalMs()).isEqualByComparingTo("1.25");
      assertThat(PlanTextRenderer.render("{\"Plan\":{\"Node Type\":\"Result\"}}").actualTotalMs()).isNull();
      assertThat(PlanTextRenderer.render("{\"Plan\":{\"Actual Total Time\":2}}").actualTotalMs()).isEqualByComparingTo("2");
      assertThatThrownBy(() -> PlanTextRenderer.render("[]")).hasMessageContaining("plan tree");
      assertThatThrownBy(() -> PlanTextRenderer.render("broken")).isInstanceOf(Exception.class);
   }
}
