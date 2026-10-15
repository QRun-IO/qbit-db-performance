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
 ** Snapshots persisted through QQQ table actions.
 *******************************************************************************/
public class DbPerfSnapshot extends QRecordEntity
{
   public static final String TABLE_NAME = "db_perf_snapshot";

   @QField(isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(maxLength = 100)
   private String backendName;

   @QField(isRequired = true)
   private Instant capturedAt;

   @QField
   private Integer intervalSeconds;

   @QField(maxLength = 20)
   private String status;

   @QField
   private Integer durationMs;

   @QField(maxLength = 2000)
   private String errorMessage;

   @QField
   private Boolean statsResetDetected;

   @QField
   private Boolean includesTables;

   @QField
   private Long xactCommit;

   @QField
   private Long xactRollback;

   @QField
   private Long blksHit;

   @QField
   private Long blksRead;

   @QField
   private BigDecimal cacheHitRatio;

   @QField
   private Long tempFiles;

   @QField
   private Long tempBytes;

   @QField
   private Long deadlocks;

   @QField
   private Long conflicts;

   @QField
   private Integer connectionsTotal;

   @QField
   private Integer connectionsActive;

   @QField
   private Integer connectionsIdle;

   @QField
   private Integer connectionsIdleInTx;

   @QField
   private Integer maxConnections;

   @QField
   private Long databaseSizeBytes;

   @QField
   private Integer statementCount;

   @QField
   private Long statementCalls;

   @QField
   private BigDecimal statementTotalMs;

   @QField
   private Integer storedStatementCount;

   @QField
   private Instant createDate;

   @QField
   private Long rawXactCommit;

   @QField
   private Long rawXactRollback;

   @QField
   private Long rawBlksHit;

   @QField
   private Long rawBlksRead;

   @QField
   private Long rawTempFiles;

   @QField
   private Long rawTempBytes;

   @QField
   private Long rawDeadlocks;

   @QField
   private Long rawConflicts;

   @QField
   private Instant rawStatsReset;

   @QField
   private Instant rawStatementStatsReset;


   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public DbPerfSnapshot()
   {
   }


   /*******************************************************************************
    ** Populate from a QQQ record.
    *******************************************************************************/
   public DbPerfSnapshot(QRecord record)
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
   public DbPerfSnapshot withId(Integer id)
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
   public DbPerfSnapshot withBackendName(String backendName)
   {
      this.backendName = backendName;
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
   public DbPerfSnapshot withCapturedAt(Instant capturedAt)
   {
      this.capturedAt = capturedAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for intervalSeconds.
    *******************************************************************************/
   public Integer getIntervalSeconds()
   {
      return (this.intervalSeconds);
   }


   /*******************************************************************************
    ** Setter for intervalSeconds.
    *******************************************************************************/
   public void setIntervalSeconds(Integer intervalSeconds)
   {
      this.intervalSeconds = intervalSeconds;
   }


   /*******************************************************************************
    ** Fluent setter for intervalSeconds.
    *******************************************************************************/
   public DbPerfSnapshot withIntervalSeconds(Integer intervalSeconds)
   {
      this.intervalSeconds = intervalSeconds;
      return (this);
   }


   /*******************************************************************************
    ** Getter for status.
    *******************************************************************************/
   public String getStatus()
   {
      return (this.status);
   }


   /*******************************************************************************
    ** Setter for status.
    *******************************************************************************/
   public void setStatus(String status)
   {
      this.status = status;
   }


   /*******************************************************************************
    ** Fluent setter for status.
    *******************************************************************************/
   public DbPerfSnapshot withStatus(String status)
   {
      this.status = status;
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
   public DbPerfSnapshot withDurationMs(Integer durationMs)
   {
      this.durationMs = durationMs;
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
   public DbPerfSnapshot withErrorMessage(String errorMessage)
   {
      this.errorMessage = errorMessage;
      return (this);
   }


   /*******************************************************************************
    ** Getter for statsResetDetected.
    *******************************************************************************/
   public Boolean getStatsResetDetected()
   {
      return (this.statsResetDetected);
   }


   /*******************************************************************************
    ** Setter for statsResetDetected.
    *******************************************************************************/
   public void setStatsResetDetected(Boolean statsResetDetected)
   {
      this.statsResetDetected = statsResetDetected;
   }


   /*******************************************************************************
    ** Fluent setter for statsResetDetected.
    *******************************************************************************/
   public DbPerfSnapshot withStatsResetDetected(Boolean statsResetDetected)
   {
      this.statsResetDetected = statsResetDetected;
      return (this);
   }


   /*******************************************************************************
    ** Getter for includesTables.
    *******************************************************************************/
   public Boolean getIncludesTables()
   {
      return (this.includesTables);
   }


   /*******************************************************************************
    ** Setter for includesTables.
    *******************************************************************************/
   public void setIncludesTables(Boolean includesTables)
   {
      this.includesTables = includesTables;
   }


   /*******************************************************************************
    ** Fluent setter for includesTables.
    *******************************************************************************/
   public DbPerfSnapshot withIncludesTables(Boolean includesTables)
   {
      this.includesTables = includesTables;
      return (this);
   }


   /*******************************************************************************
    ** Getter for xactCommit.
    *******************************************************************************/
   public Long getXactCommit()
   {
      return (this.xactCommit);
   }


   /*******************************************************************************
    ** Setter for xactCommit.
    *******************************************************************************/
   public void setXactCommit(Long xactCommit)
   {
      this.xactCommit = xactCommit;
   }


   /*******************************************************************************
    ** Fluent setter for xactCommit.
    *******************************************************************************/
   public DbPerfSnapshot withXactCommit(Long xactCommit)
   {
      this.xactCommit = xactCommit;
      return (this);
   }


   /*******************************************************************************
    ** Getter for xactRollback.
    *******************************************************************************/
   public Long getXactRollback()
   {
      return (this.xactRollback);
   }


   /*******************************************************************************
    ** Setter for xactRollback.
    *******************************************************************************/
   public void setXactRollback(Long xactRollback)
   {
      this.xactRollback = xactRollback;
   }


   /*******************************************************************************
    ** Fluent setter for xactRollback.
    *******************************************************************************/
   public DbPerfSnapshot withXactRollback(Long xactRollback)
   {
      this.xactRollback = xactRollback;
      return (this);
   }


   /*******************************************************************************
    ** Getter for blksHit.
    *******************************************************************************/
   public Long getBlksHit()
   {
      return (this.blksHit);
   }


   /*******************************************************************************
    ** Setter for blksHit.
    *******************************************************************************/
   public void setBlksHit(Long blksHit)
   {
      this.blksHit = blksHit;
   }


   /*******************************************************************************
    ** Fluent setter for blksHit.
    *******************************************************************************/
   public DbPerfSnapshot withBlksHit(Long blksHit)
   {
      this.blksHit = blksHit;
      return (this);
   }


   /*******************************************************************************
    ** Getter for blksRead.
    *******************************************************************************/
   public Long getBlksRead()
   {
      return (this.blksRead);
   }


   /*******************************************************************************
    ** Setter for blksRead.
    *******************************************************************************/
   public void setBlksRead(Long blksRead)
   {
      this.blksRead = blksRead;
   }


   /*******************************************************************************
    ** Fluent setter for blksRead.
    *******************************************************************************/
   public DbPerfSnapshot withBlksRead(Long blksRead)
   {
      this.blksRead = blksRead;
      return (this);
   }


   /*******************************************************************************
    ** Getter for cacheHitRatio.
    *******************************************************************************/
   public BigDecimal getCacheHitRatio()
   {
      return (this.cacheHitRatio);
   }


   /*******************************************************************************
    ** Setter for cacheHitRatio.
    *******************************************************************************/
   public void setCacheHitRatio(BigDecimal cacheHitRatio)
   {
      this.cacheHitRatio = cacheHitRatio;
   }


   /*******************************************************************************
    ** Fluent setter for cacheHitRatio.
    *******************************************************************************/
   public DbPerfSnapshot withCacheHitRatio(BigDecimal cacheHitRatio)
   {
      this.cacheHitRatio = cacheHitRatio;
      return (this);
   }


   /*******************************************************************************
    ** Getter for tempFiles.
    *******************************************************************************/
   public Long getTempFiles()
   {
      return (this.tempFiles);
   }


   /*******************************************************************************
    ** Setter for tempFiles.
    *******************************************************************************/
   public void setTempFiles(Long tempFiles)
   {
      this.tempFiles = tempFiles;
   }


   /*******************************************************************************
    ** Fluent setter for tempFiles.
    *******************************************************************************/
   public DbPerfSnapshot withTempFiles(Long tempFiles)
   {
      this.tempFiles = tempFiles;
      return (this);
   }


   /*******************************************************************************
    ** Getter for tempBytes.
    *******************************************************************************/
   public Long getTempBytes()
   {
      return (this.tempBytes);
   }


   /*******************************************************************************
    ** Setter for tempBytes.
    *******************************************************************************/
   public void setTempBytes(Long tempBytes)
   {
      this.tempBytes = tempBytes;
   }


   /*******************************************************************************
    ** Fluent setter for tempBytes.
    *******************************************************************************/
   public DbPerfSnapshot withTempBytes(Long tempBytes)
   {
      this.tempBytes = tempBytes;
      return (this);
   }


   /*******************************************************************************
    ** Getter for deadlocks.
    *******************************************************************************/
   public Long getDeadlocks()
   {
      return (this.deadlocks);
   }


   /*******************************************************************************
    ** Setter for deadlocks.
    *******************************************************************************/
   public void setDeadlocks(Long deadlocks)
   {
      this.deadlocks = deadlocks;
   }


   /*******************************************************************************
    ** Fluent setter for deadlocks.
    *******************************************************************************/
   public DbPerfSnapshot withDeadlocks(Long deadlocks)
   {
      this.deadlocks = deadlocks;
      return (this);
   }


   /*******************************************************************************
    ** Getter for conflicts.
    *******************************************************************************/
   public Long getConflicts()
   {
      return (this.conflicts);
   }


   /*******************************************************************************
    ** Setter for conflicts.
    *******************************************************************************/
   public void setConflicts(Long conflicts)
   {
      this.conflicts = conflicts;
   }


   /*******************************************************************************
    ** Fluent setter for conflicts.
    *******************************************************************************/
   public DbPerfSnapshot withConflicts(Long conflicts)
   {
      this.conflicts = conflicts;
      return (this);
   }


   /*******************************************************************************
    ** Getter for connectionsTotal.
    *******************************************************************************/
   public Integer getConnectionsTotal()
   {
      return (this.connectionsTotal);
   }


   /*******************************************************************************
    ** Setter for connectionsTotal.
    *******************************************************************************/
   public void setConnectionsTotal(Integer connectionsTotal)
   {
      this.connectionsTotal = connectionsTotal;
   }


   /*******************************************************************************
    ** Fluent setter for connectionsTotal.
    *******************************************************************************/
   public DbPerfSnapshot withConnectionsTotal(Integer connectionsTotal)
   {
      this.connectionsTotal = connectionsTotal;
      return (this);
   }


   /*******************************************************************************
    ** Getter for connectionsActive.
    *******************************************************************************/
   public Integer getConnectionsActive()
   {
      return (this.connectionsActive);
   }


   /*******************************************************************************
    ** Setter for connectionsActive.
    *******************************************************************************/
   public void setConnectionsActive(Integer connectionsActive)
   {
      this.connectionsActive = connectionsActive;
   }


   /*******************************************************************************
    ** Fluent setter for connectionsActive.
    *******************************************************************************/
   public DbPerfSnapshot withConnectionsActive(Integer connectionsActive)
   {
      this.connectionsActive = connectionsActive;
      return (this);
   }


   /*******************************************************************************
    ** Getter for connectionsIdle.
    *******************************************************************************/
   public Integer getConnectionsIdle()
   {
      return (this.connectionsIdle);
   }


   /*******************************************************************************
    ** Setter for connectionsIdle.
    *******************************************************************************/
   public void setConnectionsIdle(Integer connectionsIdle)
   {
      this.connectionsIdle = connectionsIdle;
   }


   /*******************************************************************************
    ** Fluent setter for connectionsIdle.
    *******************************************************************************/
   public DbPerfSnapshot withConnectionsIdle(Integer connectionsIdle)
   {
      this.connectionsIdle = connectionsIdle;
      return (this);
   }


   /*******************************************************************************
    ** Getter for connectionsIdleInTx.
    *******************************************************************************/
   public Integer getConnectionsIdleInTx()
   {
      return (this.connectionsIdleInTx);
   }


   /*******************************************************************************
    ** Setter for connectionsIdleInTx.
    *******************************************************************************/
   public void setConnectionsIdleInTx(Integer connectionsIdleInTx)
   {
      this.connectionsIdleInTx = connectionsIdleInTx;
   }


   /*******************************************************************************
    ** Fluent setter for connectionsIdleInTx.
    *******************************************************************************/
   public DbPerfSnapshot withConnectionsIdleInTx(Integer connectionsIdleInTx)
   {
      this.connectionsIdleInTx = connectionsIdleInTx;
      return (this);
   }


   /*******************************************************************************
    ** Getter for maxConnections.
    *******************************************************************************/
   public Integer getMaxConnections()
   {
      return (this.maxConnections);
   }


   /*******************************************************************************
    ** Setter for maxConnections.
    *******************************************************************************/
   public void setMaxConnections(Integer maxConnections)
   {
      this.maxConnections = maxConnections;
   }


   /*******************************************************************************
    ** Fluent setter for maxConnections.
    *******************************************************************************/
   public DbPerfSnapshot withMaxConnections(Integer maxConnections)
   {
      this.maxConnections = maxConnections;
      return (this);
   }


   /*******************************************************************************
    ** Getter for databaseSizeBytes.
    *******************************************************************************/
   public Long getDatabaseSizeBytes()
   {
      return (this.databaseSizeBytes);
   }


   /*******************************************************************************
    ** Setter for databaseSizeBytes.
    *******************************************************************************/
   public void setDatabaseSizeBytes(Long databaseSizeBytes)
   {
      this.databaseSizeBytes = databaseSizeBytes;
   }


   /*******************************************************************************
    ** Fluent setter for databaseSizeBytes.
    *******************************************************************************/
   public DbPerfSnapshot withDatabaseSizeBytes(Long databaseSizeBytes)
   {
      this.databaseSizeBytes = databaseSizeBytes;
      return (this);
   }


   /*******************************************************************************
    ** Getter for statementCount.
    *******************************************************************************/
   public Integer getStatementCount()
   {
      return (this.statementCount);
   }


   /*******************************************************************************
    ** Setter for statementCount.
    *******************************************************************************/
   public void setStatementCount(Integer statementCount)
   {
      this.statementCount = statementCount;
   }


   /*******************************************************************************
    ** Fluent setter for statementCount.
    *******************************************************************************/
   public DbPerfSnapshot withStatementCount(Integer statementCount)
   {
      this.statementCount = statementCount;
      return (this);
   }


   /*******************************************************************************
    ** Getter for statementCalls.
    *******************************************************************************/
   public Long getStatementCalls()
   {
      return (this.statementCalls);
   }


   /*******************************************************************************
    ** Setter for statementCalls.
    *******************************************************************************/
   public void setStatementCalls(Long statementCalls)
   {
      this.statementCalls = statementCalls;
   }


   /*******************************************************************************
    ** Fluent setter for statementCalls.
    *******************************************************************************/
   public DbPerfSnapshot withStatementCalls(Long statementCalls)
   {
      this.statementCalls = statementCalls;
      return (this);
   }


   /*******************************************************************************
    ** Getter for statementTotalMs.
    *******************************************************************************/
   public BigDecimal getStatementTotalMs()
   {
      return (this.statementTotalMs);
   }


   /*******************************************************************************
    ** Setter for statementTotalMs.
    *******************************************************************************/
   public void setStatementTotalMs(BigDecimal statementTotalMs)
   {
      this.statementTotalMs = statementTotalMs;
   }


   /*******************************************************************************
    ** Fluent setter for statementTotalMs.
    *******************************************************************************/
   public DbPerfSnapshot withStatementTotalMs(BigDecimal statementTotalMs)
   {
      this.statementTotalMs = statementTotalMs;
      return (this);
   }


   /*******************************************************************************
    ** Getter for storedStatementCount.
    *******************************************************************************/
   public Integer getStoredStatementCount()
   {
      return (this.storedStatementCount);
   }


   /*******************************************************************************
    ** Setter for storedStatementCount.
    *******************************************************************************/
   public void setStoredStatementCount(Integer storedStatementCount)
   {
      this.storedStatementCount = storedStatementCount;
   }


   /*******************************************************************************
    ** Fluent setter for storedStatementCount.
    *******************************************************************************/
   public DbPerfSnapshot withStoredStatementCount(Integer storedStatementCount)
   {
      this.storedStatementCount = storedStatementCount;
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
   public DbPerfSnapshot withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }


   /*******************************************************************************
    ** Getter for rawXactCommit.
    *******************************************************************************/
   public Long getRawXactCommit()
   {
      return (this.rawXactCommit);
   }


   /*******************************************************************************
    ** Setter for rawXactCommit.
    *******************************************************************************/
   public void setRawXactCommit(Long rawXactCommit)
   {
      this.rawXactCommit = rawXactCommit;
   }


   /*******************************************************************************
    ** Fluent setter for rawXactCommit.
    *******************************************************************************/
   public DbPerfSnapshot withRawXactCommit(Long rawXactCommit)
   {
      this.rawXactCommit = rawXactCommit;
      return (this);
   }


   /*******************************************************************************
    ** Getter for rawXactRollback.
    *******************************************************************************/
   public Long getRawXactRollback()
   {
      return (this.rawXactRollback);
   }


   /*******************************************************************************
    ** Setter for rawXactRollback.
    *******************************************************************************/
   public void setRawXactRollback(Long rawXactRollback)
   {
      this.rawXactRollback = rawXactRollback;
   }


   /*******************************************************************************
    ** Fluent setter for rawXactRollback.
    *******************************************************************************/
   public DbPerfSnapshot withRawXactRollback(Long rawXactRollback)
   {
      this.rawXactRollback = rawXactRollback;
      return (this);
   }


   /*******************************************************************************
    ** Getter for rawBlksHit.
    *******************************************************************************/
   public Long getRawBlksHit()
   {
      return (this.rawBlksHit);
   }


   /*******************************************************************************
    ** Setter for rawBlksHit.
    *******************************************************************************/
   public void setRawBlksHit(Long rawBlksHit)
   {
      this.rawBlksHit = rawBlksHit;
   }


   /*******************************************************************************
    ** Fluent setter for rawBlksHit.
    *******************************************************************************/
   public DbPerfSnapshot withRawBlksHit(Long rawBlksHit)
   {
      this.rawBlksHit = rawBlksHit;
      return (this);
   }


   /*******************************************************************************
    ** Getter for rawBlksRead.
    *******************************************************************************/
   public Long getRawBlksRead()
   {
      return (this.rawBlksRead);
   }


   /*******************************************************************************
    ** Setter for rawBlksRead.
    *******************************************************************************/
   public void setRawBlksRead(Long rawBlksRead)
   {
      this.rawBlksRead = rawBlksRead;
   }


   /*******************************************************************************
    ** Fluent setter for rawBlksRead.
    *******************************************************************************/
   public DbPerfSnapshot withRawBlksRead(Long rawBlksRead)
   {
      this.rawBlksRead = rawBlksRead;
      return (this);
   }


   /*******************************************************************************
    ** Getter for rawTempFiles.
    *******************************************************************************/
   public Long getRawTempFiles()
   {
      return (this.rawTempFiles);
   }


   /*******************************************************************************
    ** Setter for rawTempFiles.
    *******************************************************************************/
   public void setRawTempFiles(Long rawTempFiles)
   {
      this.rawTempFiles = rawTempFiles;
   }


   /*******************************************************************************
    ** Fluent setter for rawTempFiles.
    *******************************************************************************/
   public DbPerfSnapshot withRawTempFiles(Long rawTempFiles)
   {
      this.rawTempFiles = rawTempFiles;
      return (this);
   }


   /*******************************************************************************
    ** Getter for rawTempBytes.
    *******************************************************************************/
   public Long getRawTempBytes()
   {
      return (this.rawTempBytes);
   }


   /*******************************************************************************
    ** Setter for rawTempBytes.
    *******************************************************************************/
   public void setRawTempBytes(Long rawTempBytes)
   {
      this.rawTempBytes = rawTempBytes;
   }


   /*******************************************************************************
    ** Fluent setter for rawTempBytes.
    *******************************************************************************/
   public DbPerfSnapshot withRawTempBytes(Long rawTempBytes)
   {
      this.rawTempBytes = rawTempBytes;
      return (this);
   }


   /*******************************************************************************
    ** Getter for rawDeadlocks.
    *******************************************************************************/
   public Long getRawDeadlocks()
   {
      return (this.rawDeadlocks);
   }


   /*******************************************************************************
    ** Setter for rawDeadlocks.
    *******************************************************************************/
   public void setRawDeadlocks(Long rawDeadlocks)
   {
      this.rawDeadlocks = rawDeadlocks;
   }


   /*******************************************************************************
    ** Fluent setter for rawDeadlocks.
    *******************************************************************************/
   public DbPerfSnapshot withRawDeadlocks(Long rawDeadlocks)
   {
      this.rawDeadlocks = rawDeadlocks;
      return (this);
   }


   /*******************************************************************************
    ** Getter for rawConflicts.
    *******************************************************************************/
   public Long getRawConflicts()
   {
      return (this.rawConflicts);
   }


   /*******************************************************************************
    ** Setter for rawConflicts.
    *******************************************************************************/
   public void setRawConflicts(Long rawConflicts)
   {
      this.rawConflicts = rawConflicts;
   }


   /*******************************************************************************
    ** Fluent setter for rawConflicts.
    *******************************************************************************/
   public DbPerfSnapshot withRawConflicts(Long rawConflicts)
   {
      this.rawConflicts = rawConflicts;
      return (this);
   }


   /*******************************************************************************
    ** Getter for rawStatsReset.
    *******************************************************************************/
   public Instant getRawStatsReset()
   {
      return (this.rawStatsReset);
   }


   /*******************************************************************************
    ** Setter for rawStatsReset.
    *******************************************************************************/
   public void setRawStatsReset(Instant rawStatsReset)
   {
      this.rawStatsReset = rawStatsReset;
   }


   /*******************************************************************************
    ** Fluent setter for rawStatsReset.
    *******************************************************************************/
   public DbPerfSnapshot withRawStatsReset(Instant rawStatsReset)
   {
      this.rawStatsReset = rawStatsReset;
      return (this);
   }


   /*******************************************************************************
    ** Getter for rawStatementStatsReset.
    *******************************************************************************/
   public Instant getRawStatementStatsReset()
   {
      return (this.rawStatementStatsReset);
   }


   /*******************************************************************************
    ** Setter for rawStatementStatsReset.
    *******************************************************************************/
   public void setRawStatementStatsReset(Instant rawStatementStatsReset)
   {
      this.rawStatementStatsReset = rawStatementStatsReset;
   }


   /*******************************************************************************
    ** Fluent setter for rawStatementStatsReset.
    *******************************************************************************/
   public DbPerfSnapshot withRawStatementStatsReset(Instant rawStatementStatsReset)
   {
      this.rawStatementStatsReset = rawStatementStatsReset;
      return (this);
   }
}
