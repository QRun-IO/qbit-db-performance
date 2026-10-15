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


import com.kingsrook.qbits.dbperformance.normalize.SqlNormalizer;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Privacy and stable-shape regressions for literal normalization.
 *******************************************************************************/
class SqlNormalizerTest
{
   /*******************************************************************************
    ** Different bind syntaxes and literals must have the same fingerprint.
    *******************************************************************************/
   @Test
   void equivalentParameters()
   {
      String qqq = SqlNormalizer.normalize("SELECT * FROM account WHERE id=? AND name=?");
      assertThat(SqlNormalizer.normalize("select * from account where id=$1 and name=$2")).isEqualTo(qqq);
      assertThat(SqlNormalizer.fingerprint(SqlNormalizer.normalize("SELECT * FROM account WHERE id=123 AND name='secret'")))
         .isEqualTo(SqlNormalizer.fingerprint(qqq)).hasSize(64);
   }


   /*******************************************************************************
    ** Comments, escaped and dollar-quoted strings never leave literal content.
    *******************************************************************************/
   @Test
   void literalsAndComments()
   {
      for(String literal : new String[] {"'private''name'", "E'private\\'name'", "$$private name$$", "$body$private 'name'$body$", "'unterminated private", "$x$unterminated private"})
      {
         assertThat(SqlNormalizer.normalize("select " + literal)).isEqualTo("select ?");
      }
      assertThat(SqlNormalizer.normalize("SELECT /* outer /* nested */ done */ col12 -- private\nFROM t WHERE n=-1.2e+3"))
         .isEqualTo("select col12 from t where n = - ?");
      assertThat(SqlNormalizer.normalize("select '/* literal */', 0xff, .5, 2e-4")).isEqualTo("select ? , ? , ? , ?");
      assertThat(SqlNormalizer.normalize("SELECT \"My Table\".\"Field2\" FROM \"My Table\""))
         .isEqualTo("select \"My Table\" . \"Field2\" from \"My Table\"");
   }


   /*******************************************************************************
    ** Repeated list shapes collapse while column arity remains meaningful.
    *******************************************************************************/
   @Test
   void listShapesAndClassification()
   {
      assertThat(SqlNormalizer.normalize("select * from t where id IN (1,2,3)"))
         .isEqualTo(SqlNormalizer.normalize("select * from t where id in (?)"));
      assertThat(SqlNormalizer.normalize("insert into t values (1,'a'),(2,'b')"))
         .isEqualTo(SqlNormalizer.normalize("insert into t values (?,?)"));
      for(String operation : new String[] {"select", "insert", "update", "delete", "with"})
      {
         assertThat(SqlNormalizer.operation(operation)).isEqualTo(operation.toUpperCase(java.util.Locale.ROOT));
      }
      assertThat(SqlNormalizer.normalize("SELECT id::bigint FROM t WHERE n>=1 AND name||'secret'=$1"))
         .isEqualTo("select id :: bigint from t where n >= ? and name || ? = ?");
      assertThat(SqlNormalizer.normalize("select payload->>'secret', n<>10 from t"))
         .isEqualTo("select payload ->> ? , n <> ? from t");
      assertThat(SqlNormalizer.operation("create table t(a int)")).isEqualTo("OTHER");
      assertThat(SqlNormalizer.mainTable("select x from public.\"Account\" where x=1")).isEqualTo("Account");
      assertThat(SqlNormalizer.mainTable("update public.account set x=1")).isEqualTo("account");
      assertThat(SqlNormalizer.mainTable("select 1")).isNull();
      assertThat(SqlNormalizer.normalize("select \"in ( ? , ? )\" from t")).isEqualTo("select \"in ( ? , ? )\" from t");
      assertThat(SqlNormalizer.normalize(null)).isEmpty();
      assertThat(SqlNormalizer.normalize(" ")).isEmpty();
      assertThat(SqlNormalizer.normalize("select " + "field, ".repeat(5000))).hasSize(10000);
   }
}
