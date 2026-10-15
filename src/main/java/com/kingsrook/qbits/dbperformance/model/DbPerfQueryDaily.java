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
import java.time.LocalDate;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;


/*******************************************************************************
 ** Query Daily persisted through QQQ table actions.
 *******************************************************************************/
public class DbPerfQueryDaily extends QRecordEntity
{
   public static final String TABLE_NAME = "db_perf_query_daily";

   @QField(isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField
   private Integer statementId;

   @QField
   private LocalDate day;

   @QField
   private Long calls;

   @QField
   private BigDecimal totalMs;

   @QField
   private BigDecimal meanMs;

   @QField
   private BigDecimal maxMs;

   @QField
   private Long rowsCount;

   @QField
   private BigDecimal hitRatio;

   @QField
   private BigDecimal shareOfDbTime;

   @QField
   private Integer snapshots;


   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public DbPerfQueryDaily()
   {
   }


   /*******************************************************************************
    ** Populate from a QQQ record.
    *******************************************************************************/
   public DbPerfQueryDaily(QRecord record)
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
   public DbPerfQueryDaily withId(Integer id)
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
   public DbPerfQueryDaily withStatementId(Integer statementId)
   {
      this.statementId = statementId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for day.
    *******************************************************************************/
   public LocalDate getDay()
   {
      return (this.day);
   }


   /*******************************************************************************
    ** Setter for day.
    *******************************************************************************/
   public void setDay(LocalDate day)
   {
      this.day = day;
   }


   /*******************************************************************************
    ** Fluent setter for day.
    *******************************************************************************/
   public DbPerfQueryDaily withDay(LocalDate day)
   {
      this.day = day;
      return (this);
   }


   /*******************************************************************************
    ** Getter for calls.
    *******************************************************************************/
   public Long getCalls()
   {
      return (this.calls);
   }


   /*******************************************************************************
    ** Setter for calls.
    *******************************************************************************/
   public void setCalls(Long calls)
   {
      this.calls = calls;
   }


   /*******************************************************************************
    ** Fluent setter for calls.
    *******************************************************************************/
   public DbPerfQueryDaily withCalls(Long calls)
   {
      this.calls = calls;
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
   public DbPerfQueryDaily withTotalMs(BigDecimal totalMs)
   {
      this.totalMs = totalMs;
      return (this);
   }


   /*******************************************************************************
    ** Getter for meanMs.
    *******************************************************************************/
   public BigDecimal getMeanMs()
   {
      return (this.meanMs);
   }


   /*******************************************************************************
    ** Setter for meanMs.
    *******************************************************************************/
   public void setMeanMs(BigDecimal meanMs)
   {
      this.meanMs = meanMs;
   }


   /*******************************************************************************
    ** Fluent setter for meanMs.
    *******************************************************************************/
   public DbPerfQueryDaily withMeanMs(BigDecimal meanMs)
   {
      this.meanMs = meanMs;
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
   public DbPerfQueryDaily withMaxMs(BigDecimal maxMs)
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
   public DbPerfQueryDaily withRowsCount(Long rowsCount)
   {
      this.rowsCount = rowsCount;
      return (this);
   }


   /*******************************************************************************
    ** Getter for hitRatio.
    *******************************************************************************/
   public BigDecimal getHitRatio()
   {
      return (this.hitRatio);
   }


   /*******************************************************************************
    ** Setter for hitRatio.
    *******************************************************************************/
   public void setHitRatio(BigDecimal hitRatio)
   {
      this.hitRatio = hitRatio;
   }


   /*******************************************************************************
    ** Fluent setter for hitRatio.
    *******************************************************************************/
   public DbPerfQueryDaily withHitRatio(BigDecimal hitRatio)
   {
      this.hitRatio = hitRatio;
      return (this);
   }


   /*******************************************************************************
    ** Getter for shareOfDbTime.
    *******************************************************************************/
   public BigDecimal getShareOfDbTime()
   {
      return (this.shareOfDbTime);
   }


   /*******************************************************************************
    ** Setter for shareOfDbTime.
    *******************************************************************************/
   public void setShareOfDbTime(BigDecimal shareOfDbTime)
   {
      this.shareOfDbTime = shareOfDbTime;
   }


   /*******************************************************************************
    ** Fluent setter for shareOfDbTime.
    *******************************************************************************/
   public DbPerfQueryDaily withShareOfDbTime(BigDecimal shareOfDbTime)
   {
      this.shareOfDbTime = shareOfDbTime;
      return (this);
   }


   /*******************************************************************************
    ** Getter for snapshots.
    *******************************************************************************/
   public Integer getSnapshots()
   {
      return (this.snapshots);
   }


   /*******************************************************************************
    ** Setter for snapshots.
    *******************************************************************************/
   public void setSnapshots(Integer snapshots)
   {
      this.snapshots = snapshots;
   }


   /*******************************************************************************
    ** Fluent setter for snapshots.
    *******************************************************************************/
   public DbPerfQueryDaily withSnapshots(Integer snapshots)
   {
      this.snapshots = snapshots;
      return (this);
   }
}
