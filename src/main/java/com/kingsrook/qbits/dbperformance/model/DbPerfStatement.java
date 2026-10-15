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
 ** Queries persisted through QQQ table actions.
 *******************************************************************************/
public class DbPerfStatement extends QRecordEntity
{
   public static final String TABLE_NAME = "db_perf_statement";

   @QField(isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(maxLength = 100)
   private String backendName;

   @QField
   private Long queryId;

   @QField(maxLength = 100)
   private String dbUser;

   @QField
   private Boolean toplevel;

   @QField(maxLength = 64)
   private String fingerprint;

   @QField
   private String queryText;

   @QField(maxLength = 20)
   private String operation;

   @QField(maxLength = 200)
   private String mainTable;

   @QField
   private Instant firstSeenAt;

   @QField
   private Instant lastSeenAt;

   @QField
   private Long lastCalls;

   @QField
   private BigDecimal lastTotalExecMs;

   @QField
   private Long lastRows;

   @QField
   private Long lastSharedBlksHit;

   @QField
   private Long lastSharedBlksRead;

   @QField
   private Long lastTempBlksWritten;

   @QField
   private BigDecimal lastMaxExecMs;

   @QField
   private Instant lastStatsSince;

   @QField
   private Long calls24h;

   @QField
   private BigDecimal totalMs24h;

   @QField
   private BigDecimal meanMs24h;

   @QField
   private BigDecimal shareOfDbTime24h;

   @QField
   private Instant createDate;

   @QField
   private Instant modifyDate;


   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public DbPerfStatement()
   {
   }


   /*******************************************************************************
    ** Populate from a QQQ record.
    *******************************************************************************/
   public DbPerfStatement(QRecord record)
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
   public DbPerfStatement withId(Integer id)
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
   public DbPerfStatement withBackendName(String backendName)
   {
      this.backendName = backendName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for queryId.
    *******************************************************************************/
   public Long getQueryId()
   {
      return (this.queryId);
   }


   /*******************************************************************************
    ** Setter for queryId.
    *******************************************************************************/
   public void setQueryId(Long queryId)
   {
      this.queryId = queryId;
   }


   /*******************************************************************************
    ** Fluent setter for queryId.
    *******************************************************************************/
   public DbPerfStatement withQueryId(Long queryId)
   {
      this.queryId = queryId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for dbUser.
    *******************************************************************************/
   public String getDbUser()
   {
      return (this.dbUser);
   }


   /*******************************************************************************
    ** Setter for dbUser.
    *******************************************************************************/
   public void setDbUser(String dbUser)
   {
      this.dbUser = dbUser;
   }


   /*******************************************************************************
    ** Fluent setter for dbUser.
    *******************************************************************************/
   public DbPerfStatement withDbUser(String dbUser)
   {
      this.dbUser = dbUser;
      return (this);
   }


   /*******************************************************************************
    ** Getter for toplevel.
    *******************************************************************************/
   public Boolean getToplevel()
   {
      return (this.toplevel);
   }


   /*******************************************************************************
    ** Setter for toplevel.
    *******************************************************************************/
   public void setToplevel(Boolean toplevel)
   {
      this.toplevel = toplevel;
   }


   /*******************************************************************************
    ** Fluent setter for toplevel.
    *******************************************************************************/
   public DbPerfStatement withToplevel(Boolean toplevel)
   {
      this.toplevel = toplevel;
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
   public DbPerfStatement withFingerprint(String fingerprint)
   {
      this.fingerprint = fingerprint;
      return (this);
   }


   /*******************************************************************************
    ** Getter for queryText.
    *******************************************************************************/
   public String getQueryText()
   {
      return (this.queryText);
   }


   /*******************************************************************************
    ** Setter for queryText.
    *******************************************************************************/
   public void setQueryText(String queryText)
   {
      this.queryText = queryText;
   }


   /*******************************************************************************
    ** Fluent setter for queryText.
    *******************************************************************************/
   public DbPerfStatement withQueryText(String queryText)
   {
      this.queryText = queryText;
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
   public DbPerfStatement withOperation(String operation)
   {
      this.operation = operation;
      return (this);
   }


   /*******************************************************************************
    ** Getter for mainTable.
    *******************************************************************************/
   public String getMainTable()
   {
      return (this.mainTable);
   }


   /*******************************************************************************
    ** Setter for mainTable.
    *******************************************************************************/
   public void setMainTable(String mainTable)
   {
      this.mainTable = mainTable;
   }


   /*******************************************************************************
    ** Fluent setter for mainTable.
    *******************************************************************************/
   public DbPerfStatement withMainTable(String mainTable)
   {
      this.mainTable = mainTable;
      return (this);
   }


   /*******************************************************************************
    ** Getter for firstSeenAt.
    *******************************************************************************/
   public Instant getFirstSeenAt()
   {
      return (this.firstSeenAt);
   }


   /*******************************************************************************
    ** Setter for firstSeenAt.
    *******************************************************************************/
   public void setFirstSeenAt(Instant firstSeenAt)
   {
      this.firstSeenAt = firstSeenAt;
   }


   /*******************************************************************************
    ** Fluent setter for firstSeenAt.
    *******************************************************************************/
   public DbPerfStatement withFirstSeenAt(Instant firstSeenAt)
   {
      this.firstSeenAt = firstSeenAt;
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
   public DbPerfStatement withLastSeenAt(Instant lastSeenAt)
   {
      this.lastSeenAt = lastSeenAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for lastCalls.
    *******************************************************************************/
   public Long getLastCalls()
   {
      return (this.lastCalls);
   }


   /*******************************************************************************
    ** Setter for lastCalls.
    *******************************************************************************/
   public void setLastCalls(Long lastCalls)
   {
      this.lastCalls = lastCalls;
   }


   /*******************************************************************************
    ** Fluent setter for lastCalls.
    *******************************************************************************/
   public DbPerfStatement withLastCalls(Long lastCalls)
   {
      this.lastCalls = lastCalls;
      return (this);
   }


   /*******************************************************************************
    ** Getter for lastTotalExecMs.
    *******************************************************************************/
   public BigDecimal getLastTotalExecMs()
   {
      return (this.lastTotalExecMs);
   }


   /*******************************************************************************
    ** Setter for lastTotalExecMs.
    *******************************************************************************/
   public void setLastTotalExecMs(BigDecimal lastTotalExecMs)
   {
      this.lastTotalExecMs = lastTotalExecMs;
   }


   /*******************************************************************************
    ** Fluent setter for lastTotalExecMs.
    *******************************************************************************/
   public DbPerfStatement withLastTotalExecMs(BigDecimal lastTotalExecMs)
   {
      this.lastTotalExecMs = lastTotalExecMs;
      return (this);
   }


   /*******************************************************************************
    ** Getter for lastRows.
    *******************************************************************************/
   public Long getLastRows()
   {
      return (this.lastRows);
   }


   /*******************************************************************************
    ** Setter for lastRows.
    *******************************************************************************/
   public void setLastRows(Long lastRows)
   {
      this.lastRows = lastRows;
   }


   /*******************************************************************************
    ** Fluent setter for lastRows.
    *******************************************************************************/
   public DbPerfStatement withLastRows(Long lastRows)
   {
      this.lastRows = lastRows;
      return (this);
   }


   /*******************************************************************************
    ** Getter for lastSharedBlksHit.
    *******************************************************************************/
   public Long getLastSharedBlksHit()
   {
      return (this.lastSharedBlksHit);
   }


   /*******************************************************************************
    ** Setter for lastSharedBlksHit.
    *******************************************************************************/
   public void setLastSharedBlksHit(Long lastSharedBlksHit)
   {
      this.lastSharedBlksHit = lastSharedBlksHit;
   }


   /*******************************************************************************
    ** Fluent setter for lastSharedBlksHit.
    *******************************************************************************/
   public DbPerfStatement withLastSharedBlksHit(Long lastSharedBlksHit)
   {
      this.lastSharedBlksHit = lastSharedBlksHit;
      return (this);
   }


   /*******************************************************************************
    ** Getter for lastSharedBlksRead.
    *******************************************************************************/
   public Long getLastSharedBlksRead()
   {
      return (this.lastSharedBlksRead);
   }


   /*******************************************************************************
    ** Setter for lastSharedBlksRead.
    *******************************************************************************/
   public void setLastSharedBlksRead(Long lastSharedBlksRead)
   {
      this.lastSharedBlksRead = lastSharedBlksRead;
   }


   /*******************************************************************************
    ** Fluent setter for lastSharedBlksRead.
    *******************************************************************************/
   public DbPerfStatement withLastSharedBlksRead(Long lastSharedBlksRead)
   {
      this.lastSharedBlksRead = lastSharedBlksRead;
      return (this);
   }


   /*******************************************************************************
    ** Getter for lastTempBlksWritten.
    *******************************************************************************/
   public Long getLastTempBlksWritten()
   {
      return (this.lastTempBlksWritten);
   }


   /*******************************************************************************
    ** Setter for lastTempBlksWritten.
    *******************************************************************************/
   public void setLastTempBlksWritten(Long lastTempBlksWritten)
   {
      this.lastTempBlksWritten = lastTempBlksWritten;
   }


   /*******************************************************************************
    ** Fluent setter for lastTempBlksWritten.
    *******************************************************************************/
   public DbPerfStatement withLastTempBlksWritten(Long lastTempBlksWritten)
   {
      this.lastTempBlksWritten = lastTempBlksWritten;
      return (this);
   }


   /*******************************************************************************
    ** Getter for lastMaxExecMs.
    *******************************************************************************/
   public BigDecimal getLastMaxExecMs()
   {
      return (this.lastMaxExecMs);
   }


   /*******************************************************************************
    ** Setter for lastMaxExecMs.
    *******************************************************************************/
   public void setLastMaxExecMs(BigDecimal lastMaxExecMs)
   {
      this.lastMaxExecMs = lastMaxExecMs;
   }


   /*******************************************************************************
    ** Fluent setter for lastMaxExecMs.
    *******************************************************************************/
   public DbPerfStatement withLastMaxExecMs(BigDecimal lastMaxExecMs)
   {
      this.lastMaxExecMs = lastMaxExecMs;
      return (this);
   }


   /*******************************************************************************
    ** Getter for lastStatsSince.
    *******************************************************************************/
   public Instant getLastStatsSince()
   {
      return (this.lastStatsSince);
   }


   /*******************************************************************************
    ** Setter for lastStatsSince.
    *******************************************************************************/
   public void setLastStatsSince(Instant lastStatsSince)
   {
      this.lastStatsSince = lastStatsSince;
   }


   /*******************************************************************************
    ** Fluent setter for lastStatsSince.
    *******************************************************************************/
   public DbPerfStatement withLastStatsSince(Instant lastStatsSince)
   {
      this.lastStatsSince = lastStatsSince;
      return (this);
   }


   /*******************************************************************************
    ** Getter for calls24h.
    *******************************************************************************/
   public Long getCalls24h()
   {
      return (this.calls24h);
   }


   /*******************************************************************************
    ** Setter for calls24h.
    *******************************************************************************/
   public void setCalls24h(Long calls24h)
   {
      this.calls24h = calls24h;
   }


   /*******************************************************************************
    ** Fluent setter for calls24h.
    *******************************************************************************/
   public DbPerfStatement withCalls24h(Long calls24h)
   {
      this.calls24h = calls24h;
      return (this);
   }


   /*******************************************************************************
    ** Getter for totalMs24h.
    *******************************************************************************/
   public BigDecimal getTotalMs24h()
   {
      return (this.totalMs24h);
   }


   /*******************************************************************************
    ** Setter for totalMs24h.
    *******************************************************************************/
   public void setTotalMs24h(BigDecimal totalMs24h)
   {
      this.totalMs24h = totalMs24h;
   }


   /*******************************************************************************
    ** Fluent setter for totalMs24h.
    *******************************************************************************/
   public DbPerfStatement withTotalMs24h(BigDecimal totalMs24h)
   {
      this.totalMs24h = totalMs24h;
      return (this);
   }


   /*******************************************************************************
    ** Getter for meanMs24h.
    *******************************************************************************/
   public BigDecimal getMeanMs24h()
   {
      return (this.meanMs24h);
   }


   /*******************************************************************************
    ** Setter for meanMs24h.
    *******************************************************************************/
   public void setMeanMs24h(BigDecimal meanMs24h)
   {
      this.meanMs24h = meanMs24h;
   }


   /*******************************************************************************
    ** Fluent setter for meanMs24h.
    *******************************************************************************/
   public DbPerfStatement withMeanMs24h(BigDecimal meanMs24h)
   {
      this.meanMs24h = meanMs24h;
      return (this);
   }


   /*******************************************************************************
    ** Getter for shareOfDbTime24h.
    *******************************************************************************/
   public BigDecimal getShareOfDbTime24h()
   {
      return (this.shareOfDbTime24h);
   }


   /*******************************************************************************
    ** Setter for shareOfDbTime24h.
    *******************************************************************************/
   public void setShareOfDbTime24h(BigDecimal shareOfDbTime24h)
   {
      this.shareOfDbTime24h = shareOfDbTime24h;
   }


   /*******************************************************************************
    ** Fluent setter for shareOfDbTime24h.
    *******************************************************************************/
   public DbPerfStatement withShareOfDbTime24h(BigDecimal shareOfDbTime24h)
   {
      this.shareOfDbTime24h = shareOfDbTime24h;
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
   public DbPerfStatement withCreateDate(Instant createDate)
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
   public DbPerfStatement withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }
}
