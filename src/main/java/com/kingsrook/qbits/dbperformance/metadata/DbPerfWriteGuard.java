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

package com.kingsrook.qbits.dbperformance.metadata;


import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.PermissionDeniedMessage;


/*******************************************************************************
 ** Enforces, in the core actions, what a table's capabilities hide in the UI
 ** and API: an insert, update or delete the table does not offer is rejected
 ** unless the qbit itself (the system user) makes it. So performance records only change through the engine and record actions,
 ** whoever calls the table actions (admins included).
 *******************************************************************************/
public class DbPerfWriteGuard implements TableCustomizerInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preInsert(InsertInput insertInput, List<QRecord> records, boolean isPreview) throws QException
   {
      return (deny(insertInput.getTableName(), Capability.TABLE_INSERT, records));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preUpdate(UpdateInput updateInput, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecordList) throws QException
   {
      return (deny(updateInput.getTableName(), Capability.TABLE_UPDATE, records));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preDelete(DeleteInput deleteInput, List<QRecord> records, boolean isPreview) throws QException
   {
      return (deny(deleteInput.getTableName(), Capability.TABLE_DELETE, records));
   }



   /*******************************************************************************
    ** Add an error to every record when the table lacks the capability and the
    ** caller is not the system user.
    *******************************************************************************/
   static List<QRecord> deny(String tableName, Capability capability, List<QRecord> records)
   {
      if(QContext.getQSession() instanceof QSystemUserSession)
      {
         return (records);
      }

      QTableMetaData table = QContext.getQInstance().getTable(tableName);
      if(table != null && table.getDisabledCapabilities() != null && table.getDisabledCapabilities().contains(capability))
      {
         for(QRecord record : records)
         {
            record.addError(new PermissionDeniedMessage("Only the Database Performance runtime changes this table."));
         }
      }
      if(table != null && (tableName.equals("db_perf_settings") || tableName.equals("db_perf_rule")))
      {
         String view = table.getPermissionRules().getPermissionBaseName();
         String manage = view.substring(0, view.length() - "view".length()) + "manage_settings";
         if(QContext.getQSession() == null || !QContext.getQSession().hasPermission(manage))
         {
            for(QRecord record : records)
            {
               record.addError(new PermissionDeniedMessage("Managing settings requires " + manage));
            }
         }
      }
      return (records);
   }
}
