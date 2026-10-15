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
 ** QQQ Attribution persisted through QQQ table actions.
 *******************************************************************************/
public class DbPerfAttribution extends QRecordEntity
{
   public static final String TABLE_NAME = "db_perf_attribution";

   @QField(isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField
   private Instant windowStart;

   @QField
   private Instant windowEnd;

   @QField(maxLength = 200)
   private String podName;

   @QField(maxLength = 20)
   private String unitKind;

   @QField(maxLength = 300)
   private String unitName;

   @QField(maxLength = 200)
   private String processName;

   @QField(maxLength = 200)
   private String tableName;

   @QField(maxLength = 20)
   private String operation;

   @QField(maxLength = 64)
   private String fingerprint;

   @QField(maxLength = 4000)
   private String sqlText;

   @QField
   private Long units;

   @QField
   private Long statements;

   @QField
   private BigDecimal totalMs;

   @QField
   private BigDecimal maxMs;

   @QField
   private Long rowsCount;

   @QField
   private Long errors;

   @QField
   private BigDecimal sampleRate;


   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public DbPerfAttribution()
   {
   }


   /*******************************************************************************
    ** Populate from a QQQ record.
    *******************************************************************************/
   public DbPerfAttribution(QRecord record)
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
   public DbPerfAttribution withId(Integer id)
   {
      this.id = id;
      return (this);
   }


   /*******************************************************************************
    ** Getter for windowStart.
    *******************************************************************************/
   public Instant getWindowStart()
   {
      return (this.windowStart);
   }


   /*******************************************************************************
    ** Setter for windowStart.
    *******************************************************************************/
   public void setWindowStart(Instant windowStart)
   {
      this.windowStart = windowStart;
   }


   /*******************************************************************************
    ** Fluent setter for windowStart.
    *******************************************************************************/
   public DbPerfAttribution withWindowStart(Instant windowStart)
   {
      this.windowStart = windowStart;
      return (this);
   }


   /*******************************************************************************
    ** Getter for windowEnd.
    *******************************************************************************/
   public Instant getWindowEnd()
   {
      return (this.windowEnd);
   }


   /*******************************************************************************
    ** Setter for windowEnd.
    *******************************************************************************/
   public void setWindowEnd(Instant windowEnd)
   {
      this.windowEnd = windowEnd;
   }


   /*******************************************************************************
    ** Fluent setter for windowEnd.
    *******************************************************************************/
   public DbPerfAttribution withWindowEnd(Instant windowEnd)
   {
      this.windowEnd = windowEnd;
      return (this);
   }


   /*******************************************************************************
    ** Getter for podName.
    *******************************************************************************/
   public String getPodName()
   {
      return (this.podName);
   }


   /*******************************************************************************
    ** Setter for podName.
    *******************************************************************************/
   public void setPodName(String podName)
   {
      this.podName = podName;
   }


   /*******************************************************************************
    ** Fluent setter for podName.
    *******************************************************************************/
   public DbPerfAttribution withPodName(String podName)
   {
      this.podName = podName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for unitKind.
    *******************************************************************************/
   public String getUnitKind()
   {
      return (this.unitKind);
   }


   /*******************************************************************************
    ** Setter for unitKind.
    *******************************************************************************/
   public void setUnitKind(String unitKind)
   {
      this.unitKind = unitKind;
   }


   /*******************************************************************************
    ** Fluent setter for unitKind.
    *******************************************************************************/
   public DbPerfAttribution withUnitKind(String unitKind)
   {
      this.unitKind = unitKind;
      return (this);
   }


   /*******************************************************************************
    ** Getter for unitName.
    *******************************************************************************/
   public String getUnitName()
   {
      return (this.unitName);
   }


   /*******************************************************************************
    ** Setter for unitName.
    *******************************************************************************/
   public void setUnitName(String unitName)
   {
      this.unitName = unitName;
   }


   /*******************************************************************************
    ** Fluent setter for unitName.
    *******************************************************************************/
   public DbPerfAttribution withUnitName(String unitName)
   {
      this.unitName = unitName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for processName.
    *******************************************************************************/
   public String getProcessName()
   {
      return (this.processName);
   }


   /*******************************************************************************
    ** Setter for processName.
    *******************************************************************************/
   public void setProcessName(String processName)
   {
      this.processName = processName;
   }


   /*******************************************************************************
    ** Fluent setter for processName.
    *******************************************************************************/
   public DbPerfAttribution withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for tableName.
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }


   /*******************************************************************************
    ** Setter for tableName.
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }


   /*******************************************************************************
    ** Fluent setter for tableName.
    *******************************************************************************/
   public DbPerfAttribution withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for operation.
    *******************************************************************************/
   public String getOperation()
   {
      return (this.operation);
   }


   /*******************************************************************************
    ** Setter for operation.
    *******************************************************************************/
   public void setOperation(String operation)
   {
      this.operation = operation;
   }


   /*******************************************************************************
    ** Fluent setter for operation.
    *******************************************************************************/
   public DbPerfAttribution withOperation(String operation)
   {
      this.operation = operation;
      return (this);
   }


   /*******************************************************************************
    ** Getter for fingerprint.
    *******************************************************************************/
   public String getFingerprint()
   {
      return (this.fingerprint);
   }


   /*******************************************************************************
    ** Setter for fingerprint.
    *******************************************************************************/
   public void setFingerprint(String fingerprint)
   {
      this.fingerprint = fingerprint;
   }


   /*******************************************************************************
    ** Fluent setter for fingerprint.
    *******************************************************************************/
   public DbPerfAttribution withFingerprint(String fingerprint)
   {
      this.fingerprint = fingerprint;
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
   public DbPerfAttribution withSqlText(String sqlText)
   {
      this.sqlText = sqlText;
      return (this);
   }


   /*******************************************************************************
    ** Getter for units.
    *******************************************************************************/
   public Long getUnits()
   {
      return (this.units);
   }


   /*******************************************************************************
    ** Setter for units.
    *******************************************************************************/
   public void setUnits(Long units)
   {
      this.units = units;
   }


   /*******************************************************************************
    ** Fluent setter for units.
    *******************************************************************************/
   public DbPerfAttribution withUnits(Long units)
   {
      this.units = units;
      return (this);
   }


   /*******************************************************************************
    ** Getter for statements.
    *******************************************************************************/
   public Long getStatements()
   {
      return (this.statements);
   }


   /*******************************************************************************
    ** Setter for statements.
    *******************************************************************************/
   public void setStatements(Long statements)
   {
      this.statements = statements;
   }


   /*******************************************************************************
    ** Fluent setter for statements.
    *******************************************************************************/
   public DbPerfAttribution withStatements(Long statements)
   {
      this.statements = statements;
      return (this);
   }


   /*******************************************************************************
    ** Getter for totalMs.
    *******************************************************************************/
   public BigDecimal getTotalMs()
   {
      return (this.totalMs);
   }


   /*******************************************************************************
    ** Setter for totalMs.
    *******************************************************************************/
   public void setTotalMs(BigDecimal totalMs)
   {
      this.totalMs = totalMs;
   }


   /*******************************************************************************
    ** Fluent setter for totalMs.
    *******************************************************************************/
   public DbPerfAttribution withTotalMs(BigDecimal totalMs)
   {
      this.totalMs = totalMs;
      return (this);
   }


   /*******************************************************************************
    ** Getter for maxMs.
    *******************************************************************************/
   public BigDecimal getMaxMs()
   {
      return (this.maxMs);
   }


   /*******************************************************************************
    ** Setter for maxMs.
    *******************************************************************************/
   public void setMaxMs(BigDecimal maxMs)
   {
      this.maxMs = maxMs;
   }


   /*******************************************************************************
    ** Fluent setter for maxMs.
    *******************************************************************************/
   public DbPerfAttribution withMaxMs(BigDecimal maxMs)
   {
      this.maxMs = maxMs;
      return (this);
   }


   /*******************************************************************************
    ** Getter for rowsCount.
    *******************************************************************************/
   public Long getRowsCount()
   {
      return (this.rowsCount);
   }


   /*******************************************************************************
    ** Setter for rowsCount.
    *******************************************************************************/
   public void setRowsCount(Long rowsCount)
   {
      this.rowsCount = rowsCount;
   }


   /*******************************************************************************
    ** Fluent setter for rowsCount.
    *******************************************************************************/
   public DbPerfAttribution withRowsCount(Long rowsCount)
   {
      this.rowsCount = rowsCount;
      return (this);
   }


   /*******************************************************************************
    ** Getter for errors.
    *******************************************************************************/
   public Long getErrors()
   {
      return (this.errors);
   }


   /*******************************************************************************
    ** Setter for errors.
    *******************************************************************************/
   public void setErrors(Long errors)
   {
      this.errors = errors;
   }


   /*******************************************************************************
    ** Fluent setter for errors.
    *******************************************************************************/
   public DbPerfAttribution withErrors(Long errors)
   {
      this.errors = errors;
      return (this);
   }


   /*******************************************************************************
    ** Getter for sampleRate.
    *******************************************************************************/
   public BigDecimal getSampleRate()
   {
      return (this.sampleRate);
   }


   /*******************************************************************************
    ** Setter for sampleRate.
    *******************************************************************************/
   public void setSampleRate(BigDecimal sampleRate)
   {
      this.sampleRate = sampleRate;
   }


   /*******************************************************************************
    ** Fluent setter for sampleRate.
    *******************************************************************************/
   public DbPerfAttribution withSampleRate(BigDecimal sampleRate)
   {
      this.sampleRate = sampleRate;
      return (this);
   }
}
