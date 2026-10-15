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


import java.util.Locale;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;


/*******************************************************************************
 ** Enum-backed possible values with readable labels.
 *******************************************************************************/
public final class DbPerfPossibleValues
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DbPerfPossibleValues()
   {
   }


   /*******************************************************************************
    ** Values for Provider.
    *******************************************************************************/
   public enum Provider implements PossibleValueEnum<String>
   {
      POSTGRES, MYSQL, UNSUPPORTED;


      /*******************************************************************************
       ** Stored value.
       *******************************************************************************/
      @Override
      public String getPossibleValueId()
      {
         return (name());
      }


      /*******************************************************************************
       ** Human-readable label.
       *******************************************************************************/
      @Override
      public String getPossibleValueLabel()
      {
         String text = name().toLowerCase(Locale.ROOT).replace('_', ' ');
         return (Character.toUpperCase(text.charAt(0)) + text.substring(1));
      }
   }


   /*******************************************************************************
    ** Values for StatsAccess.
    *******************************************************************************/
   public enum StatsAccess implements PossibleValueEnum<String>
   {
      FULL, NO_READ_ALL_STATS, NO_PG_STAT_STATEMENTS, UNSUPPORTED;


      /*******************************************************************************
       ** Stored value.
       *******************************************************************************/
      @Override
      public String getPossibleValueId()
      {
         return (name());
      }


      /*******************************************************************************
       ** Human-readable label.
       *******************************************************************************/
      @Override
      public String getPossibleValueLabel()
      {
         String text = name().toLowerCase(Locale.ROOT).replace('_', ' ');
         return (Character.toUpperCase(text.charAt(0)) + text.substring(1));
      }
   }


   /*******************************************************************************
    ** Values for SnapshotStatus.
    *******************************************************************************/
   public enum SnapshotStatus implements PossibleValueEnum<String>
   {
      RUNNING, OK, PARTIAL, FAILED;


      /*******************************************************************************
       ** Stored value.
       *******************************************************************************/
      @Override
      public String getPossibleValueId()
      {
         return (name());
      }


      /*******************************************************************************
       ** Human-readable label.
       *******************************************************************************/
      @Override
      public String getPossibleValueLabel()
      {
         String text = name().toLowerCase(Locale.ROOT).replace('_', ' ');
         return (Character.toUpperCase(text.charAt(0)) + text.substring(1));
      }
   }


   /*******************************************************************************
    ** Values for Severity.
    *******************************************************************************/
   public enum Severity implements PossibleValueEnum<String>
   {
      INFO, WARNING, ERROR, CRITICAL;


      /*******************************************************************************
       ** Stored value.
       *******************************************************************************/
      @Override
      public String getPossibleValueId()
      {
         return (name());
      }


      /*******************************************************************************
       ** Human-readable label.
       *******************************************************************************/
      @Override
      public String getPossibleValueLabel()
      {
         String text = name().toLowerCase(Locale.ROOT).replace('_', ' ');
         return (Character.toUpperCase(text.charAt(0)) + text.substring(1));
      }
   }


   /*******************************************************************************
    ** Values for Operation.
    *******************************************************************************/
   public enum Operation implements PossibleValueEnum<String>
   {
      SELECT, INSERT, UPDATE, DELETE, WITH, OTHER;


      /*******************************************************************************
       ** Stored value.
       *******************************************************************************/
      @Override
      public String getPossibleValueId()
      {
         return (name());
      }


      /*******************************************************************************
       ** Human-readable label.
       *******************************************************************************/
      @Override
      public String getPossibleValueLabel()
      {
         String text = name().toLowerCase(Locale.ROOT).replace('_', ' ');
         return (Character.toUpperCase(text.charAt(0)) + text.substring(1));
      }
   }


   /*******************************************************************************
    ** Values for UnitKind.
    *******************************************************************************/
   public enum UnitKind implements PossibleValueEnum<String>
   {
      HTTP_REQUEST, PROCESS, SCHEDULED_JOB, OTHER;


      /*******************************************************************************
       ** Stored value.
       *******************************************************************************/
      @Override
      public String getPossibleValueId()
      {
         return (name());
      }


      /*******************************************************************************
       ** Human-readable label.
       *******************************************************************************/
      @Override
      public String getPossibleValueLabel()
      {
         String text = name().toLowerCase(Locale.ROOT).replace('_', ' ');
         return (Character.toUpperCase(text.charAt(0)) + text.substring(1));
      }
   }


   /*******************************************************************************
    ** Values for FindingStatus.
    *******************************************************************************/
   public enum FindingStatus implements PossibleValueEnum<String>
   {
      OPEN, ACKNOWLEDGED, RESOLVED, IGNORED;


      /*******************************************************************************
       ** Stored value.
       *******************************************************************************/
      @Override
      public String getPossibleValueId()
      {
         return (name());
      }


      /*******************************************************************************
       ** Human-readable label.
       *******************************************************************************/
      @Override
      public String getPossibleValueLabel()
      {
         String text = name().toLowerCase(Locale.ROOT).replace('_', ' ');
         return (Character.toUpperCase(text.charAt(0)) + text.substring(1));
      }
   }


   /*******************************************************************************
    ** Values for ExplainMode.
    *******************************************************************************/
   public enum ExplainMode implements PossibleValueEnum<String>
   {
      PLAN, GENERIC, ANALYZE;


      /*******************************************************************************
       ** Stored value.
       *******************************************************************************/
      @Override
      public String getPossibleValueId()
      {
         return (name());
      }


      /*******************************************************************************
       ** Human-readable label.
       *******************************************************************************/
      @Override
      public String getPossibleValueLabel()
      {
         String text = name().toLowerCase(Locale.ROOT).replace('_', ' ');
         return (Character.toUpperCase(text.charAt(0)) + text.substring(1));
      }
   }


   /*******************************************************************************
    ** Values for Outcome.
    *******************************************************************************/
   public enum Outcome implements PossibleValueEnum<String>
   {
      OK, ERROR, DENIED, TIMEOUT;


      /*******************************************************************************
       ** Stored value.
       *******************************************************************************/
      @Override
      public String getPossibleValueId()
      {
         return (name());
      }


      /*******************************************************************************
       ** Human-readable label.
       *******************************************************************************/
      @Override
      public String getPossibleValueLabel()
      {
         String text = name().toLowerCase(Locale.ROOT).replace('_', ' ');
         return (Character.toUpperCase(text.charAt(0)) + text.substring(1));
      }
   }


   /*******************************************************************************
    ** Values for Reason.
    *******************************************************************************/
   public enum Reason implements PossibleValueEnum<String>
   {
      LONG_RUNNING, IDLE_IN_TRANSACTION, BLOCKED, BLOCKING;


      /*******************************************************************************
       ** Stored value.
       *******************************************************************************/
      @Override
      public String getPossibleValueId()
      {
         return (name());
      }


      /*******************************************************************************
       ** Human-readable label.
       *******************************************************************************/
      @Override
      public String getPossibleValueLabel()
      {
         String text = name().toLowerCase(Locale.ROOT).replace('_', ' ');
         return (Character.toUpperCase(text.charAt(0)) + text.substring(1));
      }
   }


   /*******************************************************************************
    ** Values for State.
    *******************************************************************************/
   public enum State implements PossibleValueEnum<String>
   {
      active, idle, idle_in_transaction, idle_in_transaction_aborted, fastpath_function_call, disabled;


      /*******************************************************************************
       ** Stored value.
       *******************************************************************************/
      @Override
      public String getPossibleValueId()
      {
         return (name().replace('_', ' '));
      }


      /*******************************************************************************
       ** Human-readable label.
       *******************************************************************************/
      @Override
      public String getPossibleValueLabel()
      {
         String text = name().toLowerCase(Locale.ROOT).replace('_', ' ');
         return (Character.toUpperCase(text.charAt(0)) + text.substring(1));
      }
   }
}
