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


import java.time.Instant;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;


/*******************************************************************************
 ** Table Snapshots persisted through QQQ table actions.
 *******************************************************************************/
public class DbPerfTableSnapshot extends QRecordEntity
{
   public static final String TABLE_NAME = "db_perf_table_snapshot";

   @QField(isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField
   private Integer snapshotId;

   @QField
   private Integer tableStatId;

   @QField
   private Instant capturedAt;

   @QField
   private Long seqScan;

   @QField
   private Long seqTupRead;

   @QField
   private Long idxScan;

   @QField
   private Long idxTupFetch;

   @QField
   private Long nTupIns;

   @QField
   private Long nTupUpd;

   @QField
   private Long nTupDel;

   @QField
   private Long nTupHotUpd;

   @QField
   private Long nLiveTup;

   @QField
   private Long nDeadTup;

   @QField
   private Long totalBytes;

   @QField
   private Long tableBytes;

   @QField
   private Long indexBytes;

   @QField
   private Long autovacuumCount;

   @QField
   private Long autoanalyzeCount;

   @QField
   private Long freezeAge;


   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public DbPerfTableSnapshot()
   {
   }


   /*******************************************************************************
    ** Populate from a QQQ record.
    *******************************************************************************/
   public DbPerfTableSnapshot(QRecord record)
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
   public DbPerfTableSnapshot withId(Integer id)
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
   public DbPerfTableSnapshot withSnapshotId(Integer snapshotId)
   {
      this.snapshotId = snapshotId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for tableStatId.
    *******************************************************************************/
   public Integer getTableStatId()
   {
      return (this.tableStatId);
   }


   /*******************************************************************************
    ** Setter for tableStatId.
    *******************************************************************************/
   public void setTableStatId(Integer tableStatId)
   {
      this.tableStatId = tableStatId;
   }


   /*******************************************************************************
    ** Fluent setter for tableStatId.
    *******************************************************************************/
   public DbPerfTableSnapshot withTableStatId(Integer tableStatId)
   {
      this.tableStatId = tableStatId;
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
   public DbPerfTableSnapshot withCapturedAt(Instant capturedAt)
   {
      this.capturedAt = capturedAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for seqScan.
    *******************************************************************************/
   public Long getSeqScan()
   {
      return (this.seqScan);
   }


   /*******************************************************************************
    ** Setter for seqScan.
    *******************************************************************************/
   public void setSeqScan(Long seqScan)
   {
      this.seqScan = seqScan;
   }


   /*******************************************************************************
    ** Fluent setter for seqScan.
    *******************************************************************************/
   public DbPerfTableSnapshot withSeqScan(Long seqScan)
   {
      this.seqScan = seqScan;
      return (this);
   }


   /*******************************************************************************
    ** Getter for seqTupRead.
    *******************************************************************************/
   public Long getSeqTupRead()
   {
      return (this.seqTupRead);
   }


   /*******************************************************************************
    ** Setter for seqTupRead.
    *******************************************************************************/
   public void setSeqTupRead(Long seqTupRead)
   {
      this.seqTupRead = seqTupRead;
   }


   /*******************************************************************************
    ** Fluent setter for seqTupRead.
    *******************************************************************************/
   public DbPerfTableSnapshot withSeqTupRead(Long seqTupRead)
   {
      this.seqTupRead = seqTupRead;
      return (this);
   }


   /*******************************************************************************
    ** Getter for idxScan.
    *******************************************************************************/
   public Long getIdxScan()
   {
      return (this.idxScan);
   }


   /*******************************************************************************
    ** Setter for idxScan.
    *******************************************************************************/
   public void setIdxScan(Long idxScan)
   {
      this.idxScan = idxScan;
   }


   /*******************************************************************************
    ** Fluent setter for idxScan.
    *******************************************************************************/
   public DbPerfTableSnapshot withIdxScan(Long idxScan)
   {
      this.idxScan = idxScan;
      return (this);
   }


   /*******************************************************************************
    ** Getter for idxTupFetch.
    *******************************************************************************/
   public Long getIdxTupFetch()
   {
      return (this.idxTupFetch);
   }


   /*******************************************************************************
    ** Setter for idxTupFetch.
    *******************************************************************************/
   public void setIdxTupFetch(Long idxTupFetch)
   {
      this.idxTupFetch = idxTupFetch;
   }


   /*******************************************************************************
    ** Fluent setter for idxTupFetch.
    *******************************************************************************/
   public DbPerfTableSnapshot withIdxTupFetch(Long idxTupFetch)
   {
      this.idxTupFetch = idxTupFetch;
      return (this);
   }


   /*******************************************************************************
    ** Getter for nTupIns.
    *******************************************************************************/
   public Long getNTupIns()
   {
      return (this.nTupIns);
   }


   /*******************************************************************************
    ** Setter for nTupIns.
    *******************************************************************************/
   public void setNTupIns(Long nTupIns)
   {
      this.nTupIns = nTupIns;
   }


   /*******************************************************************************
    ** Fluent setter for nTupIns.
    *******************************************************************************/
   public DbPerfTableSnapshot withNTupIns(Long nTupIns)
   {
      this.nTupIns = nTupIns;
      return (this);
   }


   /*******************************************************************************
    ** Getter for nTupUpd.
    *******************************************************************************/
   public Long getNTupUpd()
   {
      return (this.nTupUpd);
   }


   /*******************************************************************************
    ** Setter for nTupUpd.
    *******************************************************************************/
   public void setNTupUpd(Long nTupUpd)
   {
      this.nTupUpd = nTupUpd;
   }


   /*******************************************************************************
    ** Fluent setter for nTupUpd.
    *******************************************************************************/
   public DbPerfTableSnapshot withNTupUpd(Long nTupUpd)
   {
      this.nTupUpd = nTupUpd;
      return (this);
   }


   /*******************************************************************************
    ** Getter for nTupDel.
    *******************************************************************************/
   public Long getNTupDel()
   {
      return (this.nTupDel);
   }


   /*******************************************************************************
    ** Setter for nTupDel.
    *******************************************************************************/
   public void setNTupDel(Long nTupDel)
   {
      this.nTupDel = nTupDel;
   }


   /*******************************************************************************
    ** Fluent setter for nTupDel.
    *******************************************************************************/
   public DbPerfTableSnapshot withNTupDel(Long nTupDel)
   {
      this.nTupDel = nTupDel;
      return (this);
   }


   /*******************************************************************************
    ** Getter for nTupHotUpd.
    *******************************************************************************/
   public Long getNTupHotUpd()
   {
      return (this.nTupHotUpd);
   }


   /*******************************************************************************
    ** Setter for nTupHotUpd.
    *******************************************************************************/
   public void setNTupHotUpd(Long nTupHotUpd)
   {
      this.nTupHotUpd = nTupHotUpd;
   }


   /*******************************************************************************
    ** Fluent setter for nTupHotUpd.
    *******************************************************************************/
   public DbPerfTableSnapshot withNTupHotUpd(Long nTupHotUpd)
   {
      this.nTupHotUpd = nTupHotUpd;
      return (this);
   }


   /*******************************************************************************
    ** Getter for nLiveTup.
    *******************************************************************************/
   public Long getNLiveTup()
   {
      return (this.nLiveTup);
   }


   /*******************************************************************************
    ** Setter for nLiveTup.
    *******************************************************************************/
   public void setNLiveTup(Long nLiveTup)
   {
      this.nLiveTup = nLiveTup;
   }


   /*******************************************************************************
    ** Fluent setter for nLiveTup.
    *******************************************************************************/
   public DbPerfTableSnapshot withNLiveTup(Long nLiveTup)
   {
      this.nLiveTup = nLiveTup;
      return (this);
   }


   /*******************************************************************************
    ** Getter for nDeadTup.
    *******************************************************************************/
   public Long getNDeadTup()
   {
      return (this.nDeadTup);
   }


   /*******************************************************************************
    ** Setter for nDeadTup.
    *******************************************************************************/
   public void setNDeadTup(Long nDeadTup)
   {
      this.nDeadTup = nDeadTup;
   }


   /*******************************************************************************
    ** Fluent setter for nDeadTup.
    *******************************************************************************/
   public DbPerfTableSnapshot withNDeadTup(Long nDeadTup)
   {
      this.nDeadTup = nDeadTup;
      return (this);
   }


   /*******************************************************************************
    ** Getter for totalBytes.
    *******************************************************************************/
   public Long getTotalBytes()
   {
      return (this.totalBytes);
   }


   /*******************************************************************************
    ** Setter for totalBytes.
    *******************************************************************************/
   public void setTotalBytes(Long totalBytes)
   {
      this.totalBytes = totalBytes;
   }


   /*******************************************************************************
    ** Fluent setter for totalBytes.
    *******************************************************************************/
   public DbPerfTableSnapshot withTotalBytes(Long totalBytes)
   {
      this.totalBytes = totalBytes;
      return (this);
   }


   /*******************************************************************************
    ** Getter for tableBytes.
    *******************************************************************************/
   public Long getTableBytes()
   {
      return (this.tableBytes);
   }


   /*******************************************************************************
    ** Setter for tableBytes.
    *******************************************************************************/
   public void setTableBytes(Long tableBytes)
   {
      this.tableBytes = tableBytes;
   }


   /*******************************************************************************
    ** Fluent setter for tableBytes.
    *******************************************************************************/
   public DbPerfTableSnapshot withTableBytes(Long tableBytes)
   {
      this.tableBytes = tableBytes;
      return (this);
   }


   /*******************************************************************************
    ** Getter for indexBytes.
    *******************************************************************************/
   public Long getIndexBytes()
   {
      return (this.indexBytes);
   }


   /*******************************************************************************
    ** Setter for indexBytes.
    *******************************************************************************/
   public void setIndexBytes(Long indexBytes)
   {
      this.indexBytes = indexBytes;
   }


   /*******************************************************************************
    ** Fluent setter for indexBytes.
    *******************************************************************************/
   public DbPerfTableSnapshot withIndexBytes(Long indexBytes)
   {
      this.indexBytes = indexBytes;
      return (this);
   }


   /*******************************************************************************
    ** Getter for autovacuumCount.
    *******************************************************************************/
   public Long getAutovacuumCount()
   {
      return (this.autovacuumCount);
   }


   /*******************************************************************************
    ** Setter for autovacuumCount.
    *******************************************************************************/
   public void setAutovacuumCount(Long autovacuumCount)
   {
      this.autovacuumCount = autovacuumCount;
   }


   /*******************************************************************************
    ** Fluent setter for autovacuumCount.
    *******************************************************************************/
   public DbPerfTableSnapshot withAutovacuumCount(Long autovacuumCount)
   {
      this.autovacuumCount = autovacuumCount;
      return (this);
   }


   /*******************************************************************************
    ** Getter for autoanalyzeCount.
    *******************************************************************************/
   public Long getAutoanalyzeCount()
   {
      return (this.autoanalyzeCount);
   }


   /*******************************************************************************
    ** Setter for autoanalyzeCount.
    *******************************************************************************/
   public void setAutoanalyzeCount(Long autoanalyzeCount)
   {
      this.autoanalyzeCount = autoanalyzeCount;
   }


   /*******************************************************************************
    ** Fluent setter for autoanalyzeCount.
    *******************************************************************************/
   public DbPerfTableSnapshot withAutoanalyzeCount(Long autoanalyzeCount)
   {
      this.autoanalyzeCount = autoanalyzeCount;
      return (this);
   }


   /*******************************************************************************
    ** Getter for freezeAge.
    *******************************************************************************/
   public Long getFreezeAge()
   {
      return (this.freezeAge);
   }


   /*******************************************************************************
    ** Setter for freezeAge.
    *******************************************************************************/
   public void setFreezeAge(Long freezeAge)
   {
      this.freezeAge = freezeAge;
   }


   /*******************************************************************************
    ** Fluent setter for freezeAge.
    *******************************************************************************/
   public DbPerfTableSnapshot withFreezeAge(Long freezeAge)
   {
      this.freezeAge = freezeAge;
      return (this);
   }
}
