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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Small QQQ action adapter. All mutations preserve audit/customizer semantics.
 *******************************************************************************/
public final class DbPerfStore
{
   public static final int BATCH_SIZE = 1000;


   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DbPerfStore()
   {
   }


   /*******************************************************************************
    ** Read rows using the ambient system transaction.
    *******************************************************************************/
   public static List<QRecord> query(String table, QQueryFilter filter) throws QException
   {
      return (new QueryAction().execute(new QueryInput(table).withFilter(filter).withTransaction(QContext.getQBackendTransaction())).getRecords());
   }


   /*******************************************************************************
    ** Keyset-paged read for tables that can hold more rows than fit in the heap
    ** (attribution writes hundreds of thousands per day). At most pageSize
    ** records are held at once; the consumer must not keep the page.
    *******************************************************************************/
   public static void forEachPage(String table, QQueryFilter filter, int pageSize, PageConsumer consumer) throws Exception
   {
      Serializable lastId = 0;
      while(true)
      {
         QQueryFilter page = new QQueryFilter().withSubFilter(filter).withCriteria(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, lastId))
            .withOrderBy(new QFilterOrderBy("id", true)).withLimit(pageSize);
         List<QRecord> rows = query(table, page);
         if(rows.isEmpty())
         {
            return;
         }
         consumer.accept(rows);
         if(rows.size() < pageSize)
         {
            return;
         }
         lastId = rows.getLast().getValue("id");
      }
   }



   /*******************************************************************************
    ** Receives one bounded page from forEachPage.
    *******************************************************************************/
   @FunctionalInterface
   public interface PageConsumer
   {
      /***************************************************************************
       ** Handle one page.
       ***************************************************************************/
      void accept(List<QRecord> page) throws Exception;
   }



   /*******************************************************************************
    ** First matching record.
    *******************************************************************************/
   public static QRecord first(String table, QQueryFilter filter) throws QException
   {
      List<QRecord> rows = query(table, filter.withLimit(1));
      return (rows.isEmpty() ? null : rows.getFirst());
   }


   /*******************************************************************************
    ** A field equality predicate.
    *******************************************************************************/
   public static QQueryFilter eq(String field, Serializable value)
   {
      return (new QQueryFilter(new QFilterCriteria(field, QCriteriaOperator.EQUALS, value)));
   }


   /*******************************************************************************
    ** Bounded batch insertion, returning generated IDs and rejecting row errors.
    *******************************************************************************/
   public static List<QRecord> insert(String table, List<QRecord> rows) throws QException
   {
      List<QRecord> result = new ArrayList<>();
      for(int i = 0; i < rows.size(); i += BATCH_SIZE)
      {
         List<QRecord> inserted = InsertAction.executeForRecords(new InsertInput(table).withRecords(new ArrayList<>(rows.subList(i, Math.min(rows.size(), i + BATCH_SIZE)))).withTransaction(QContext.getQBackendTransaction()));
         check(inserted);
         result.addAll(inserted);
      }
      return (result);
   }


   /*******************************************************************************
    ** Batch updates, throwing on row errors rather than silently losing data.
    *******************************************************************************/
   public static void update(String table, List<QRecord> rows) throws QException
   {
      for(int i = 0; i < rows.size(); i += BATCH_SIZE)
      {
         check(UpdateAction.executeForRecords(new UpdateInput(table).withRecords(new ArrayList<>(rows.subList(i, Math.min(rows.size(), i + BATCH_SIZE)))).withTransaction(QContext.getQBackendTransaction())));
      }
   }


   /*******************************************************************************
    ** Keyset deletion: select at most 1000 matching IDs, delete through QQQ,
    ** then advance the key. No OFFSET skips and no unbounded ID collection.
    *******************************************************************************/
   public static int delete(String table, QQueryFilter filter) throws QException
   {
      int count = 0;
      int lastId = 0;
      while(true)
      {
         QQueryFilter page = new QQueryFilter().withSubFilter(filter).withCriteria(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, lastId))
            .withOrderBy(new QFilterOrderBy("id", true)).withLimit(BATCH_SIZE);
         List<QRecord> rows = query(table, page);
         if(rows.isEmpty())
         {
            return (count);
         }
         List<Serializable> ids = rows.stream().map(row -> (Serializable) row.getValueInteger("id")).toList();
         var deleted = new DeleteAction().execute(new DeleteInput(table).withQueryFilter(new QQueryFilter().withSubFilter(filter).withCriteria(new QFilterCriteria("id", QCriteriaOperator.IN, ids))).withTransaction(QContext.getQBackendTransaction()));
         if(deleted.getRecordsWithErrors() != null)
         {
            check(deleted.getRecordsWithErrors());
         }
         if(deleted.getDeletedRecordCount() != ids.size())
         {
            throw (new QException("Database Performance deletion did not remove all selected records"));
         }
         lastId = rows.getLast().getValueInteger("id");
         count += rows.size();
      }
   }


   /*******************************************************************************
    ** Reject action-level record errors without persisting potentially raw SQL.
    *******************************************************************************/
   private static void check(List<QRecord> rows) throws QException
   {
      for(QRecord row : rows)
      {
         if(row.getErrors() != null && !row.getErrors().isEmpty())
         {
            throw (new QException("Database Performance record write failed: " + row.getErrors().getFirst().getClass().getSimpleName()));
         }
      }
   }
}
