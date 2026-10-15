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
 ** Tables persisted through QQQ table actions.
 *******************************************************************************/
public class DbPerfTableStat extends QRecordEntity
{
   public static final String TABLE_NAME = "db_perf_table_stat";

   @QField(isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(maxLength = 100)
   private String backendName;

   @QField(maxLength = 100)
   private String schemaName;

   @QField(maxLength = 200)
   private String tableName;

   @QField
   private Long relid;

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
   private BigDecimal deadRatio;

   @QField
   private Long totalBytes;

   @QField
   private Long tableBytes;

   @QField
   private Long indexBytes;

   @QField
   private Long toastBytes;

   @QField
   private Long growthBytes7d;

   @QField
   private Instant lastVacuum;

   @QField
   private Instant lastAutovacuum;

   @QField
   private Instant lastAnalyze;

   @QField
   private Instant lastAutoanalyze;

   @QField
   private Long vacuumCount;

   @QField
   private Long autovacuumCount;

   @QField
   private Long analyzeCount;

   @QField
   private Long autoanalyzeCount;

   @QField
   private Long freezeAge;

   @QField
   private BigDecimal seqScanShare;

   @QField
   private Instant lastSeenAt;

   @QField
   private Instant createDate;

   @QField
   private Instant modifyDate;


   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public DbPerfTableStat()
   {
   }


   /*******************************************************************************
    ** Populate from a QQQ record.
    *******************************************************************************/
   public DbPerfTableStat(QRecord record)
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
   public DbPerfTableStat withId(Integer id)
   {
      this.id = id;
      return (this);
   }


   /*******************************************************************************
    ** Getter for backendName.
    *******************************************************************************/
   public String getBackendName()
   {
      return (this.backendName);
   }


   /*******************************************************************************
    ** Setter for backendName.
    *******************************************************************************/
   public void setBackendName(String backendName)
   {
      this.backendName = backendName;
   }


   /*******************************************************************************
    ** Fluent setter for backendName.
    *******************************************************************************/
   public DbPerfTableStat withBackendName(String backendName)
   {
      this.backendName = backendName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for schemaName.
    *******************************************************************************/
   public String getSchemaName()
   {
      return (this.schemaName);
   }


   /*******************************************************************************
    ** Setter for schemaName.
    *******************************************************************************/
   public void setSchemaName(String schemaName)
   {
      this.schemaName = schemaName;
   }


   /*******************************************************************************
    ** Fluent setter for schemaName.
    *******************************************************************************/
   public DbPerfTableStat withSchemaName(String schemaName)
   {
      this.schemaName = schemaName;
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
   public DbPerfTableStat withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for relid.
    *******************************************************************************/
   public Long getRelid()
   {
      return (this.relid);
   }


   /*******************************************************************************
    ** Setter for relid.
    *******************************************************************************/
   public void setRelid(Long relid)
   {
      this.relid = relid;
   }


   /*******************************************************************************
    ** Fluent setter for relid.
    *******************************************************************************/
   public DbPerfTableStat withRelid(Long relid)
   {
      this.relid = relid;
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
   public DbPerfTableStat withSeqScan(Long seqScan)
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
   public DbPerfTableStat withSeqTupRead(Long seqTupRead)
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
   public DbPerfTableStat withIdxScan(Long idxScan)
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
   public DbPerfTableStat withIdxTupFetch(Long idxTupFetch)
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
   public DbPerfTableStat withNTupIns(Long nTupIns)
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
   public DbPerfTableStat withNTupUpd(Long nTupUpd)
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
   public DbPerfTableStat withNTupDel(Long nTupDel)
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
   public DbPerfTableStat withNTupHotUpd(Long nTupHotUpd)
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
   public DbPerfTableStat withNLiveTup(Long nLiveTup)
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
   public DbPerfTableStat withNDeadTup(Long nDeadTup)
   {
      this.nDeadTup = nDeadTup;
      return (this);
   }


   /*******************************************************************************
    ** Getter for deadRatio.
    *******************************************************************************/
   public BigDecimal getDeadRatio()
   {
      return (this.deadRatio);
   }


   /*******************************************************************************
    ** Setter for deadRatio.
    *******************************************************************************/
   public void setDeadRatio(BigDecimal deadRatio)
   {
      this.deadRatio = deadRatio;
   }


   /*******************************************************************************
    ** Fluent setter for deadRatio.
    *******************************************************************************/
   public DbPerfTableStat withDeadRatio(BigDecimal deadRatio)
   {
      this.deadRatio = deadRatio;
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
   public DbPerfTableStat withTotalBytes(Long totalBytes)
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
   public DbPerfTableStat withTableBytes(Long tableBytes)
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
   public DbPerfTableStat withIndexBytes(Long indexBytes)
   {
      this.indexBytes = indexBytes;
      return (this);
   }


   /*******************************************************************************
    ** Getter for toastBytes.
    *******************************************************************************/
   public Long getToastBytes()
   {
      return (this.toastBytes);
   }


   /*******************************************************************************
    ** Setter for toastBytes.
    *******************************************************************************/
   public void setToastBytes(Long toastBytes)
   {
      this.toastBytes = toastBytes;
   }


   /*******************************************************************************
    ** Fluent setter for toastBytes.
    *******************************************************************************/
   public DbPerfTableStat withToastBytes(Long toastBytes)
   {
      this.toastBytes = toastBytes;
      return (this);
   }


   /*******************************************************************************
    ** Getter for growthBytes7d.
    *******************************************************************************/
   public Long getGrowthBytes7d()
   {
      return (this.growthBytes7d);
   }


   /*******************************************************************************
    ** Setter for growthBytes7d.
    *******************************************************************************/
   public void setGrowthBytes7d(Long growthBytes7d)
   {
      this.growthBytes7d = growthBytes7d;
   }


   /*******************************************************************************
    ** Fluent setter for growthBytes7d.
    *******************************************************************************/
   public DbPerfTableStat withGrowthBytes7d(Long growthBytes7d)
   {
      this.growthBytes7d = growthBytes7d;
      return (this);
   }


   /*******************************************************************************
    ** Getter for lastVacuum.
    *******************************************************************************/
   public Instant getLastVacuum()
   {
      return (this.lastVacuum);
   }


   /*******************************************************************************
    ** Setter for lastVacuum.
    *******************************************************************************/
   public void setLastVacuum(Instant lastVacuum)
   {
      this.lastVacuum = lastVacuum;
   }


   /*******************************************************************************
    ** Fluent setter for lastVacuum.
    *******************************************************************************/
   public DbPerfTableStat withLastVacuum(Instant lastVacuum)
   {
      this.lastVacuum = lastVacuum;
      return (this);
   }


   /*******************************************************************************
    ** Getter for lastAutovacuum.
    *******************************************************************************/
   public Instant getLastAutovacuum()
   {
      return (this.lastAutovacuum);
   }


   /*******************************************************************************
    ** Setter for lastAutovacuum.
    *******************************************************************************/
   public void setLastAutovacuum(Instant lastAutovacuum)
   {
      this.lastAutovacuum = lastAutovacuum;
   }


   /*******************************************************************************
    ** Fluent setter for lastAutovacuum.
    *******************************************************************************/
   public DbPerfTableStat withLastAutovacuum(Instant lastAutovacuum)
   {
      this.lastAutovacuum = lastAutovacuum;
      return (this);
   }


   /*******************************************************************************
    ** Getter for lastAnalyze.
    *******************************************************************************/
   public Instant getLastAnalyze()
   {
      return (this.lastAnalyze);
   }


   /*******************************************************************************
    ** Setter for lastAnalyze.
    *******************************************************************************/
   public void setLastAnalyze(Instant lastAnalyze)
   {
      this.lastAnalyze = lastAnalyze;
   }


   /*******************************************************************************
    ** Fluent setter for lastAnalyze.
    *******************************************************************************/
   public DbPerfTableStat withLastAnalyze(Instant lastAnalyze)
   {
      this.lastAnalyze = lastAnalyze;
      return (this);
   }


   /*******************************************************************************
    ** Getter for lastAutoanalyze.
    *******************************************************************************/
   public Instant getLastAutoanalyze()
   {
      return (this.lastAutoanalyze);
   }


   /*******************************************************************************
    ** Setter for lastAutoanalyze.
    *******************************************************************************/
   public void setLastAutoanalyze(Instant lastAutoanalyze)
   {
      this.lastAutoanalyze = lastAutoanalyze;
   }


   /*******************************************************************************
    ** Fluent setter for lastAutoanalyze.
    *******************************************************************************/
   public DbPerfTableStat withLastAutoanalyze(Instant lastAutoanalyze)
   {
      this.lastAutoanalyze = lastAutoanalyze;
      return (this);
   }


   /*******************************************************************************
    ** Getter for vacuumCount.
    *******************************************************************************/
   public Long getVacuumCount()
   {
      return (this.vacuumCount);
   }


   /*******************************************************************************
    ** Setter for vacuumCount.
    *******************************************************************************/
   public void setVacuumCount(Long vacuumCount)
   {
      this.vacuumCount = vacuumCount;
   }


   /*******************************************************************************
    ** Fluent setter for vacuumCount.
    *******************************************************************************/
   public DbPerfTableStat withVacuumCount(Long vacuumCount)
   {
      this.vacuumCount = vacuumCount;
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
   public DbPerfTableStat withAutovacuumCount(Long autovacuumCount)
   {
      this.autovacuumCount = autovacuumCount;
      return (this);
   }


   /*******************************************************************************
    ** Getter for analyzeCount.
    *******************************************************************************/
   public Long getAnalyzeCount()
   {
      return (this.analyzeCount);
   }


   /*******************************************************************************
    ** Setter for analyzeCount.
    *******************************************************************************/
   public void setAnalyzeCount(Long analyzeCount)
   {
      this.analyzeCount = analyzeCount;
   }


   /*******************************************************************************
    ** Fluent setter for analyzeCount.
    *******************************************************************************/
   public DbPerfTableStat withAnalyzeCount(Long analyzeCount)
   {
      this.analyzeCount = analyzeCount;
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
   public DbPerfTableStat withAutoanalyzeCount(Long autoanalyzeCount)
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
   public DbPerfTableStat withFreezeAge(Long freezeAge)
   {
      this.freezeAge = freezeAge;
      return (this);
   }


   /*******************************************************************************
    ** Getter for seqScanShare.
    *******************************************************************************/
   public BigDecimal getSeqScanShare()
   {
      return (this.seqScanShare);
   }


   /*******************************************************************************
    ** Setter for seqScanShare.
    *******************************************************************************/
   public void setSeqScanShare(BigDecimal seqScanShare)
   {
      this.seqScanShare = seqScanShare;
   }


   /*******************************************************************************
    ** Fluent setter for seqScanShare.
    *******************************************************************************/
   public DbPerfTableStat withSeqScanShare(BigDecimal seqScanShare)
   {
      this.seqScanShare = seqScanShare;
      return (this);
   }


   /*******************************************************************************
    ** Getter for lastSeenAt.
    *******************************************************************************/
   public Instant getLastSeenAt()
   {
      return (this.lastSeenAt);
   }


   /*******************************************************************************
    ** Setter for lastSeenAt.
    *******************************************************************************/
   public void setLastSeenAt(Instant lastSeenAt)
   {
      this.lastSeenAt = lastSeenAt;
   }


   /*******************************************************************************
    ** Fluent setter for lastSeenAt.
    *******************************************************************************/
   public DbPerfTableStat withLastSeenAt(Instant lastSeenAt)
   {
      this.lastSeenAt = lastSeenAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for createDate.
    *******************************************************************************/
   public Instant getCreateDate()
   {
      return (this.createDate);
   }


   /*******************************************************************************
    ** Setter for createDate.
    *******************************************************************************/
   public void setCreateDate(Instant createDate)
   {
      this.createDate = createDate;
   }


   /*******************************************************************************
    ** Fluent setter for createDate.
    *******************************************************************************/
   public DbPerfTableStat withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }


   /*******************************************************************************
    ** Getter for modifyDate.
    *******************************************************************************/
   public Instant getModifyDate()
   {
      return (this.modifyDate);
   }


   /*******************************************************************************
    ** Setter for modifyDate.
    *******************************************************************************/
   public void setModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
   }


   /*******************************************************************************
    ** Fluent setter for modifyDate.
    *******************************************************************************/
   public DbPerfTableStat withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }
}
