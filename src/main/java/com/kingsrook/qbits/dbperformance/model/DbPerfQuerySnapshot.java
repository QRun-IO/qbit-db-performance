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
 ** Query Snapshots persisted through QQQ table actions.
 *******************************************************************************/
public class DbPerfQuerySnapshot extends QRecordEntity
{
   public static final String TABLE_NAME = "db_perf_query_snapshot";

   @QField(isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField
   private Integer snapshotId;

   @QField
   private Integer statementId;

   @QField
   private Instant capturedAt;

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
   private Long sharedBlksHit;

   @QField
   private Long sharedBlksRead;

   @QField
   private BigDecimal hitRatio;

   @QField
   private Long tempBlksWritten;

   @QField
   private BigDecimal shareOfDbTime;


   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public DbPerfQuerySnapshot()
   {
   }


   /*******************************************************************************
    ** Populate from a QQQ record.
    *******************************************************************************/
   public DbPerfQuerySnapshot(QRecord record)
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
   public DbPerfQuerySnapshot withId(Integer id)
   {
      this.id = id;
      return (this);
   }


   /*******************************************************************************
    ** Getter for snapshotId.
    *******************************************************************************/
   public Integer getSnapshotId()
   {
      return (this.snapshotId);
   }


   /*******************************************************************************
    ** Setter for snapshotId.
    *******************************************************************************/
   public void setSnapshotId(Integer snapshotId)
   {
      this.snapshotId = snapshotId;
   }


   /*******************************************************************************
    ** Fluent setter for snapshotId.
    *******************************************************************************/
   public DbPerfQuerySnapshot withSnapshotId(Integer snapshotId)
   {
      this.snapshotId = snapshotId;
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
   public DbPerfQuerySnapshot withStatementId(Integer statementId)
   {
      this.statementId = statementId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for capturedAt.
    *******************************************************************************/
   public Instant getCapturedAt()
   {
      return (this.capturedAt);
   }


   /*******************************************************************************
    ** Setter for capturedAt.
    *******************************************************************************/
   public void setCapturedAt(Instant capturedAt)
   {
      this.capturedAt = capturedAt;
   }


   /*******************************************************************************
    ** Fluent setter for capturedAt.
    *******************************************************************************/
   public DbPerfQuerySnapshot withCapturedAt(Instant capturedAt)
   {
      this.capturedAt = capturedAt;
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
   public DbPerfQuerySnapshot withCalls(Long calls)
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
   public DbPerfQuerySnapshot withTotalMs(BigDecimal totalMs)
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
   public DbPerfQuerySnapshot withMeanMs(BigDecimal meanMs)
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
   public DbPerfQuerySnapshot withMaxMs(BigDecimal maxMs)
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
   public DbPerfQuerySnapshot withRowsCount(Long rowsCount)
   {
      this.rowsCount = rowsCount;
      return (this);
   }


   /*******************************************************************************
    ** Getter for sharedBlksHit.
    *******************************************************************************/
   public Long getSharedBlksHit()
   {
      return (this.sharedBlksHit);
   }


   /*******************************************************************************
    ** Setter for sharedBlksHit.
    *******************************************************************************/
   public void setSharedBlksHit(Long sharedBlksHit)
   {
      this.sharedBlksHit = sharedBlksHit;
   }


   /*******************************************************************************
    ** Fluent setter for sharedBlksHit.
    *******************************************************************************/
   public DbPerfQuerySnapshot withSharedBlksHit(Long sharedBlksHit)
   {
      this.sharedBlksHit = sharedBlksHit;
      return (this);
   }


   /*******************************************************************************
    ** Getter for sharedBlksRead.
    *******************************************************************************/
   public Long getSharedBlksRead()
   {
      return (this.sharedBlksRead);
   }


   /*******************************************************************************
    ** Setter for sharedBlksRead.
    *******************************************************************************/
   public void setSharedBlksRead(Long sharedBlksRead)
   {
      this.sharedBlksRead = sharedBlksRead;
   }


   /*******************************************************************************
    ** Fluent setter for sharedBlksRead.
    *******************************************************************************/
   public DbPerfQuerySnapshot withSharedBlksRead(Long sharedBlksRead)
   {
      this.sharedBlksRead = sharedBlksRead;
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
   public DbPerfQuerySnapshot withHitRatio(BigDecimal hitRatio)
   {
      this.hitRatio = hitRatio;
      return (this);
   }


   /*******************************************************************************
    ** Getter for tempBlksWritten.
    *******************************************************************************/
   public Long getTempBlksWritten()
   {
      return (this.tempBlksWritten);
   }


   /*******************************************************************************
    ** Setter for tempBlksWritten.
    *******************************************************************************/
   public void setTempBlksWritten(Long tempBlksWritten)
   {
      this.tempBlksWritten = tempBlksWritten;
   }


   /*******************************************************************************
    ** Fluent setter for tempBlksWritten.
    *******************************************************************************/
   public DbPerfQuerySnapshot withTempBlksWritten(Long tempBlksWritten)
   {
      this.tempBlksWritten = tempBlksWritten;
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
   public DbPerfQuerySnapshot withShareOfDbTime(BigDecimal shareOfDbTime)
   {
      this.shareOfDbTime = shareOfDbTime;
      return (this);
   }
}
