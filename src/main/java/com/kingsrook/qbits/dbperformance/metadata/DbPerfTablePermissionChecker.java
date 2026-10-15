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


import com.kingsrook.qqq.backend.core.actions.permissions.AvailablePermission;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionSubType;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.permissions.UseOtherPermissionNameCustomPermissionChecker;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithName;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;


/*******************************************************************************
 ** Exact view and settings-write permissions without READ_WRITE permission names.
 *******************************************************************************/
public class DbPerfTablePermissionChecker extends UseOtherPermissionNameCustomPermissionChecker
{
   /*******************************************************************************
    ** Reads require view; only settings/rules have human write permissions.
    *******************************************************************************/
   @Override
   public void checkPermissionsThrowing(AbstractActionInput input, MetaDataWithPermissionRules metadata) throws QPermissionDeniedException
   {
      if(QContext.getQSession() instanceof QSystemUserSession)
      {
         return;
      }
      if(input instanceof InsertInput || input instanceof UpdateInput || input instanceof DeleteInput)
      {
         boolean settings = "db_perf_settings".equals(metadata.getName());
         boolean rule = "db_perf_rule".equals(metadata.getName());
         if((!settings && !rule) || settings && !(input instanceof UpdateInput)
            || QContext.getQSession() == null || !QContext.getQSession().hasPermission(managePermission()))
         {
            throw (new QPermissionDeniedException("Database Performance write permission denied"));
         }
         return;
      }
      super.checkPermissionsThrowing(input, metadata);
   }


   /*******************************************************************************
    ** Publish the exact write permission in host role editors.
    *******************************************************************************/
   @Override
   public AvailablePermission buildAvailablePermission(QPermissionRules rules, PermissionSubType subtype, String base, MetaDataWithName metadata, String objectType)
   {
      AvailablePermission permission = super.buildAvailablePermission(rules, subtype, base, metadata, objectType);
      if(permission != null && (subtype == TablePermissionSubType.INSERT || subtype == TablePermissionSubType.EDIT || subtype == TablePermissionSubType.DELETE))
      {
         permission.setName(managePermission());
      }
      return (permission);
   }


   /*******************************************************************************
    ** The configurable base is shared with the view permission.
    *******************************************************************************/
   private String managePermission()
   {
      String view = getPermissionName();
      return (view.substring(0, view.length() - "view".length()) + "manage_settings");
   }
}
