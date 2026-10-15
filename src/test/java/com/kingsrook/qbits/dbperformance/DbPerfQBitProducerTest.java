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


import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.kingsrook.qbits.dbperformance.metadata.DbPerfMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.AiExposure;
import com.kingsrook.qqq.backend.core.actions.ai.AiTableMetaData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfigValidationException;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Metadata and entity storage contract tests.
 *******************************************************************************/
class DbPerfQBitProducerTest
{
   /*******************************************************************************
    ** All tables use the exact storage names and protected navigation.
    *******************************************************************************/
   @Test
   void metadata() throws Exception
   {
      QInstance instance = DbPerfTestSupport.instance();
      assertThat(instance.getTables()).hasSize(17);
      assertThat(instance.getQBits()).hasSize(1);
      for(String name : DbPerfMetaData.TABLES)
      {
         QTableMetaData table = instance.getTable(name);
         assertThat(((RDBMSTableBackendDetails) table.getBackendDetails()).getTableName()).isEqualTo(name);
         assertThat(table.getPermissionRules().getPermissionBaseName()).isEqualTo(DbPerfPermissions.VIEW);
         table.getFields().values().forEach(field -> assertThat(field.getBackendName()).matches("[a-z][a-z0-9_]*"));
         if(!List.of("db_perf_settings", "db_perf_rule").contains(name))
         {
            assertThat(table.getDisabledCapabilities()).contains(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
         }
      }
      assertThat(AiTableMetaData.of(instance.getTable("db_perf_settings")).getExposure()).isEqualTo(AiExposure.NONE);
      assertThat(AiTableMetaData.of(instance.getTable("db_perf_rule")).getExposure()).isEqualTo(AiExposure.NONE);
      assertThat(instance.getApp("dbPerformance").getSections().getFirst().getTables()).containsExactly("db_perf_finding", "db_perf_statement", "db_perf_table_stat", "db_perf_index_stat", "db_perf_snapshot", "db_perf_settings", "db_perf_rule");
      assertThat(instance.getJoins()).hasSize(13);
   }


   /*******************************************************************************
    ** Fail before modifying the instance when configuration conflicts.
    *******************************************************************************/
   @Test
   void configuration() throws Exception
   {
      assertThat(new DbPerfQBitProducer().isEnabled()).isFalse();
      assertThatThrownBy(() -> new DbPerfQBitProducer().produce(new QInstance())).hasMessageContaining("configuration");
      assertThatThrownBy(() -> new DbPerfQBitProducer().withQBitConfig(new DbPerfQBitConfig()).produce(new QInstance())).isInstanceOf(QBitConfigValidationException.class);
      QInstance instance = DbPerfTestSupport.instance();
      DbPerfQBitConfig config = new DbPerfQBitConfig().withBackendName(instance.getBackends().keySet().iterator().next());
      DbPerfQBitProducer producer = new DbPerfQBitProducer().withQBitConfig(config).withNamespace("db_perf");
      assertThat(producer.getNamespace()).isEqualTo("db_perf");
      assertThat(producer.getQBitConfig()).isSameAs(config);
      assertThat(producer.isEnabled()).isTrue();
      assertThat(config.getDefaultBackendNameForTables()).isEqualTo(config.getBackendName());
      assertThatThrownBy(() -> producer.produce(instance)).isInstanceOf(QBitConfigValidationException.class);
      for(Method method : DbPerfQBitConfig.class.getDeclaredMethods())
      {
         if(method.getName().startsWith("with"))
         {
            Object value = method.getParameterTypes()[0] == Boolean.class ? false : "configured";
            assertThat(method.invoke(config, value)).isSameAs(config);
            assertThat(DbPerfQBitConfig.class.getMethod("get" + method.getName().substring(4)).invoke(config)).isEqualTo(value);
            DbPerfQBitConfig.class.getMethod("set" + method.getName().substring(4), method.getParameterTypes()).invoke(config, value);
         }
      }
   }


   /*******************************************************************************
    ** Every annotated entity round-trips through QRecord without type loss.
    *******************************************************************************/
   @Test
   void entityStorageContract() throws Exception
   {
      for(String name : DbPerfMetaData.TABLES)
      {
         StringBuilder typeName = new StringBuilder("com.kingsrook.qbits.dbperformance.model.DbPerf");
         for(String part : name.substring("db_perf_".length()).split("_"))
         {
            typeName.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
         }
         Class<?> type = Class.forName(typeName.toString());
         QRecordEntity entity = (QRecordEntity) type.getConstructor().newInstance();
         for(Method method : type.getDeclaredMethods())
         {
            if(method.getName().startsWith("with"))
            {
               Class<?> fieldType = method.getParameterTypes()[0];
               Object value = fieldType == Integer.class ? 12 : fieldType == Long.class ? 9000000000L : fieldType == Boolean.class ? true : fieldType == Instant.class ? Instant.parse("2026-01-01T00:00:00Z") : fieldType == LocalDate.class ? LocalDate.of(2026, 1, 1) : fieldType == BigDecimal.class ? new BigDecimal("12.345") : "value";
               assertThat(method.invoke(entity, value)).isSameAs(entity);
               type.getMethod("set" + method.getName().substring(4), fieldType).invoke(entity, value);
               Object copy = type.getConstructor(QRecord.class).newInstance(entity.toQRecord());
               assertThat(type.getMethod("get" + method.getName().substring(4)).invoke(copy)).isEqualTo(value);
            }
         }
      }
   }
}
