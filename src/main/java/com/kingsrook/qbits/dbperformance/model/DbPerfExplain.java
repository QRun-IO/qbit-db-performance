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

package com.kingsrook.qbits.dbperformance.model;


import java.math.BigDecimal;
import java.time.Instant;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;


/*******************************************************************************
 ** Query Plans persisted through QQQ table actions.
 *******************************************************************************/
public class DbPerfExplain extends QRecordEntity
{
   public static final String TABLE_NAME = "db_perf_explain";

   @QField(isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField
   private Integer statementId;

   @QField(maxLength = 100)
   private String requestedBy;

   @QField
   private Instant requestedAt;

   @QField(maxLength = 20)
   private String mode;

   @QField(maxLength = 64)
   private String sqlFingerprint;

   @QField
   private String sqlText;

   @QField
   private Integer parameterCount;

   @QField
   private Integer durationMs;

   @QField(maxLength = 20)
   private String outcome;

   @QField(maxLength = 2000)
   private String errorMessage;

   @QField
   private String planJson;

   @QField
   private String planText;

   @QField
   private BigDecimal totalCost;


   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public DbPerfExplain()
   {
   }


   /*******************************************************************************
    ** Populate from a QQQ record.
    *******************************************************************************/
   public DbPerfExplain(QRecord record)
   {
      populateFromQRecord(record);
   }


   /*******************************************************************************
    ** Getter for id.
    *******************************************************************************/
   public Integer getId()
   {
      return (this.id);
   }


   /*******************************************************************************
    ** Setter for id.
    *******************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }


   /*******************************************************************************
    ** Fluent setter for id.
    *******************************************************************************/
   public DbPerfExplain withId(Integer id)
   {
      this.id = id;
      return (this);
   }


   /*******************************************************************************
    ** Getter for statementId.
    *******************************************************************************/
   public Integer getStatementId()
   {
      return (this.statementId);
   }


   /*******************************************************************************
    ** Setter for statementId.
    *******************************************************************************/
   public void setStatementId(Integer statementId)
   {
      this.statementId = statementId;
   }


   /*******************************************************************************
    ** Fluent setter for statementId.
    *******************************************************************************/
   public DbPerfExplain withStatementId(Integer statementId)
   {
      this.statementId = statementId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for requestedBy.
    *******************************************************************************/
   public String getRequestedBy()
   {
      return (this.requestedBy);
   }


   /*******************************************************************************
    ** Setter for requestedBy.
    *******************************************************************************/
   public void setRequestedBy(String requestedBy)
   {
      this.requestedBy = requestedBy;
   }


   /*******************************************************************************
    ** Fluent setter for requestedBy.
    *******************************************************************************/
   public DbPerfExplain withRequestedBy(String requestedBy)
   {
      this.requestedBy = requestedBy;
      return (this);
   }


   /*******************************************************************************
    ** Getter for requestedAt.
    *******************************************************************************/
   public Instant getRequestedAt()
   {
      return (this.requestedAt);
   }


   /*******************************************************************************
    ** Setter for requestedAt.
    *******************************************************************************/
   public void setRequestedAt(Instant requestedAt)
   {
      this.requestedAt = requestedAt;
   }


   /*******************************************************************************
    ** Fluent setter for requestedAt.
    *******************************************************************************/
   public DbPerfExplain withRequestedAt(Instant requestedAt)
   {
      this.requestedAt = requestedAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for mode.
    *******************************************************************************/
   public String getMode()
   {
      return (this.mode);
   }


   /*******************************************************************************
    ** Setter for mode.
    *******************************************************************************/
   public void setMode(String mode)
   {
      this.mode = mode;
   }


   /*******************************************************************************
    ** Fluent setter for mode.
    *******************************************************************************/
   public DbPerfExplain withMode(String mode)
   {
      this.mode = mode;
      return (this);
   }


   /*******************************************************************************
    ** Getter for sqlFingerprint.
    *******************************************************************************/
   public String getSqlFingerprint()
   {
      return (this.sqlFingerprint);
   }


   /*******************************************************************************
    ** Setter for sqlFingerprint.
    *******************************************************************************/
   public void setSqlFingerprint(String sqlFingerprint)
   {
      this.sqlFingerprint = sqlFingerprint;
   }


   /*******************************************************************************
    ** Fluent setter for sqlFingerprint.
    *******************************************************************************/
   public DbPerfExplain withSqlFingerprint(String sqlFingerprint)
   {
      this.sqlFingerprint = sqlFingerprint;
      return (this);
   }


   /*******************************************************************************
    ** Getter for sqlText.
    *******************************************************************************/
   public String getSqlText()
   {
      return (this.sqlText);
   }


   /*******************************************************************************
    ** Setter for sqlText.
    *******************************************************************************/
   public void setSqlText(String sqlText)
   {
      this.sqlText = sqlText;
   }


   /*******************************************************************************
    ** Fluent setter for sqlText.
    *******************************************************************************/
   public DbPerfExplain withSqlText(String sqlText)
   {
      this.sqlText = sqlText;
      return (this);
   }


   /*******************************************************************************
    ** Getter for parameterCount.
    *******************************************************************************/
   public Integer getParameterCount()
   {
      return (this.parameterCount);
   }


   /*******************************************************************************
    ** Setter for parameterCount.
    *******************************************************************************/
   public void setParameterCount(Integer parameterCount)
   {
      this.parameterCount = parameterCount;
   }


   /*******************************************************************************
    ** Fluent setter for parameterCount.
    *******************************************************************************/
   public DbPerfExplain withParameterCount(Integer parameterCount)
   {
      this.parameterCount = parameterCount;
      return (this);
   }


   /*******************************************************************************
    ** Getter for durationMs.
    *******************************************************************************/
   public Integer getDurationMs()
   {
      return (this.durationMs);
   }


   /*******************************************************************************
    ** Setter for durationMs.
    *******************************************************************************/
   public void setDurationMs(Integer durationMs)
   {
      this.durationMs = durationMs;
   }


   /*******************************************************************************
    ** Fluent setter for durationMs.
    *******************************************************************************/
   public DbPerfExplain withDurationMs(Integer durationMs)
   {
      this.durationMs = durationMs;
      return (this);
   }


   /*******************************************************************************
    ** Getter for outcome.
    *******************************************************************************/
   public String getOutcome()
   {
      return (this.outcome);
   }


   /*******************************************************************************
    ** Setter for outcome.
    *******************************************************************************/
   public void setOutcome(String outcome)
   {
      this.outcome = outcome;
   }


   /*******************************************************************************
    ** Fluent setter for outcome.
    *******************************************************************************/
   public DbPerfExplain withOutcome(String outcome)
   {
      this.outcome = outcome;
      return (this);
   }


   /*******************************************************************************
    ** Getter for errorMessage.
    *******************************************************************************/
   public String getErrorMessage()
   {
      return (this.errorMessage);
   }


   /*******************************************************************************
    ** Setter for errorMessage.
    *******************************************************************************/
   public void setErrorMessage(String errorMessage)
   {
      this.errorMessage = errorMessage;
   }


   /*******************************************************************************
    ** Fluent setter for errorMessage.
    *******************************************************************************/
   public DbPerfExplain withErrorMessage(String errorMessage)
   {
      this.errorMessage = errorMessage;
      return (this);
   }


   /*******************************************************************************
    ** Getter for planJson.
    *******************************************************************************/
   public String getPlanJson()
   {
      return (this.planJson);
   }


   /*******************************************************************************
    ** Setter for planJson.
    *******************************************************************************/
   public void setPlanJson(String planJson)
   {
      this.planJson = planJson;
   }


   /*******************************************************************************
    ** Fluent setter for planJson.
    *******************************************************************************/
   public DbPerfExplain withPlanJson(String planJson)
   {
      this.planJson = planJson;
      return (this);
   }


   /*******************************************************************************
    ** Getter for planText.
    *******************************************************************************/
   public String getPlanText()
   {
      return (this.planText);
   }


   /*******************************************************************************
    ** Setter for planText.
    *******************************************************************************/
   public void setPlanText(String planText)
   {
      this.planText = planText;
   }


   /*******************************************************************************
    ** Fluent setter for planText.
    *******************************************************************************/
   public DbPerfExplain withPlanText(String planText)
   {
      this.planText = planText;
      return (this);
   }


   /*******************************************************************************
    ** Getter for totalCost.
    *******************************************************************************/
   public BigDecimal getTotalCost()
   {
      return (this.totalCost);
   }


   /*******************************************************************************
    ** Setter for totalCost.
    *******************************************************************************/
   public void setTotalCost(BigDecimal totalCost)
   {
      this.totalCost = totalCost;
   }


   /*******************************************************************************
    ** Fluent setter for totalCost.
    *******************************************************************************/
   public DbPerfExplain withTotalCost(BigDecimal totalCost)
   {
      this.totalCost = totalCost;
      return (this);
   }
}
