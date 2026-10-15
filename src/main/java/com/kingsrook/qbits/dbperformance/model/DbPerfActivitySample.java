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
 ** Activity Samples persisted through QQQ table actions.
 *******************************************************************************/
public class DbPerfActivitySample extends QRecordEntity
{
   public static final String TABLE_NAME = "db_perf_activity_sample";

   @QField(isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField
   private Integer snapshotId;

   @QField
   private Instant capturedAt;

   @QField
   private Integer pid;

   @QField(maxLength = 100)
   private String dbUser;

   @QField(maxLength = 200)
   private String applicationName;

   @QField(maxLength = 100)
   private String clientAddr;

   @QField(maxLength = 40)
   private String state;

   @QField(maxLength = 60)
   private String waitEventType;

   @QField(maxLength = 100)
   private String waitEvent;

   @QField(maxLength = 60)
   private String backendType;

   @QField
   private Instant xactStart;

   @QField
   private Instant queryStart;

   @QField
   private Instant stateChange;

   @QField
   private BigDecimal querySeconds;

   @QField
   private BigDecimal xactSeconds;

   @QField(maxLength = 500)
   private String blockedBy;

   @QField
   private Integer blockingCount;

   @QField(maxLength = 30)
   private String reason;

   @QField
   private Long queryId;

   @QField
   private Integer statementId;

   @QField(maxLength = 64)
   private String queryFingerprint;

   @QField(maxLength = 4000)
   private String queryTextNormalized;


   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public DbPerfActivitySample()
   {
   }


   /*******************************************************************************
    ** Populate from a QQQ record.
    *******************************************************************************/
   public DbPerfActivitySample(QRecord record)
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
   public DbPerfActivitySample withId(Integer id)
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
   public DbPerfActivitySample withSnapshotId(Integer snapshotId)
   {
      this.snapshotId = snapshotId;
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
   public DbPerfActivitySample withCapturedAt(Instant capturedAt)
   {
      this.capturedAt = capturedAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for pid.
    *******************************************************************************/
   public Integer getPid()
   {
      return (this.pid);
   }


   /*******************************************************************************
    ** Setter for pid.
    *******************************************************************************/
   public void setPid(Integer pid)
   {
      this.pid = pid;
   }


   /*******************************************************************************
    ** Fluent setter for pid.
    *******************************************************************************/
   public DbPerfActivitySample withPid(Integer pid)
   {
      this.pid = pid;
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
   public DbPerfActivitySample withDbUser(String dbUser)
   {
      this.dbUser = dbUser;
      return (this);
   }


   /*******************************************************************************
    ** Getter for applicationName.
    *******************************************************************************/
   public String getApplicationName()
   {
      return (this.applicationName);
   }


   /*******************************************************************************
    ** Setter for applicationName.
    *******************************************************************************/
   public void setApplicationName(String applicationName)
   {
      this.applicationName = applicationName;
   }


   /*******************************************************************************
    ** Fluent setter for applicationName.
    *******************************************************************************/
   public DbPerfActivitySample withApplicationName(String applicationName)
   {
      this.applicationName = applicationName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for clientAddr.
    *******************************************************************************/
   public String getClientAddr()
   {
      return (this.clientAddr);
   }


   /*******************************************************************************
    ** Setter for clientAddr.
    *******************************************************************************/
   public void setClientAddr(String clientAddr)
   {
      this.clientAddr = clientAddr;
   }


   /*******************************************************************************
    ** Fluent setter for clientAddr.
    *******************************************************************************/
   public DbPerfActivitySample withClientAddr(String clientAddr)
   {
      this.clientAddr = clientAddr;
      return (this);
   }


   /*******************************************************************************
    ** Getter for state.
    *******************************************************************************/
   public String getState()
   {
      return (this.state);
   }


   /*******************************************************************************
    ** Setter for state.
    *******************************************************************************/
   public void setState(String state)
   {
      this.state = state;
   }


   /*******************************************************************************
    ** Fluent setter for state.
    *******************************************************************************/
   public DbPerfActivitySample withState(String state)
   {
      this.state = state;
      return (this);
   }


   /*******************************************************************************
    ** Getter for waitEventType.
    *******************************************************************************/
   public String getWaitEventType()
   {
      return (this.waitEventType);
   }


   /*******************************************************************************
    ** Setter for waitEventType.
    *******************************************************************************/
   public void setWaitEventType(String waitEventType)
   {
      this.waitEventType = waitEventType;
   }


   /*******************************************************************************
    ** Fluent setter for waitEventType.
    *******************************************************************************/
   public DbPerfActivitySample withWaitEventType(String waitEventType)
   {
      this.waitEventType = waitEventType;
      return (this);
   }


   /*******************************************************************************
    ** Getter for waitEvent.
    *******************************************************************************/
   public String getWaitEvent()
   {
      return (this.waitEvent);
   }


   /*******************************************************************************
    ** Setter for waitEvent.
    *******************************************************************************/
   public void setWaitEvent(String waitEvent)
   {
      this.waitEvent = waitEvent;
   }


   /*******************************************************************************
    ** Fluent setter for waitEvent.
    *******************************************************************************/
   public DbPerfActivitySample withWaitEvent(String waitEvent)
   {
      this.waitEvent = waitEvent;
      return (this);
   }


   /*******************************************************************************
    ** Getter for backendType.
    *******************************************************************************/
   public String getBackendType()
   {
      return (this.backendType);
   }


   /*******************************************************************************
    ** Setter for backendType.
    *******************************************************************************/
   public void setBackendType(String backendType)
   {
      this.backendType = backendType;
   }


   /*******************************************************************************
    ** Fluent setter for backendType.
    *******************************************************************************/
   public DbPerfActivitySample withBackendType(String backendType)
   {
      this.backendType = backendType;
      return (this);
   }


   /*******************************************************************************
    ** Getter for xactStart.
    *******************************************************************************/
   public Instant getXactStart()
   {
      return (this.xactStart);
   }


   /*******************************************************************************
    ** Setter for xactStart.
    *******************************************************************************/
   public void setXactStart(Instant xactStart)
   {
      this.xactStart = xactStart;
   }


   /*******************************************************************************
    ** Fluent setter for xactStart.
    *******************************************************************************/
   public DbPerfActivitySample withXactStart(Instant xactStart)
   {
      this.xactStart = xactStart;
      return (this);
   }


   /*******************************************************************************
    ** Getter for queryStart.
    *******************************************************************************/
   public Instant getQueryStart()
   {
      return (this.queryStart);
   }


   /*******************************************************************************
    ** Setter for queryStart.
    *******************************************************************************/
   public void setQueryStart(Instant queryStart)
   {
      this.queryStart = queryStart;
   }


   /*******************************************************************************
    ** Fluent setter for queryStart.
    *******************************************************************************/
   public DbPerfActivitySample withQueryStart(Instant queryStart)
   {
      this.queryStart = queryStart;
      return (this);
   }


   /*******************************************************************************
    ** Getter for stateChange.
    *******************************************************************************/
   public Instant getStateChange()
   {
      return (this.stateChange);
   }


   /*******************************************************************************
    ** Setter for stateChange.
    *******************************************************************************/
   public void setStateChange(Instant stateChange)
   {
      this.stateChange = stateChange;
   }


   /*******************************************************************************
    ** Fluent setter for stateChange.
    *******************************************************************************/
   public DbPerfActivitySample withStateChange(Instant stateChange)
   {
      this.stateChange = stateChange;
      return (this);
   }


   /*******************************************************************************
    ** Getter for querySeconds.
    *******************************************************************************/
   public BigDecimal getQuerySeconds()
   {
      return (this.querySeconds);
   }


   /*******************************************************************************
    ** Setter for querySeconds.
    *******************************************************************************/
   public void setQuerySeconds(BigDecimal querySeconds)
   {
      this.querySeconds = querySeconds;
   }


   /*******************************************************************************
    ** Fluent setter for querySeconds.
    *******************************************************************************/
   public DbPerfActivitySample withQuerySeconds(BigDecimal querySeconds)
   {
      this.querySeconds = querySeconds;
      return (this);
   }


   /*******************************************************************************
    ** Getter for xactSeconds.
    *******************************************************************************/
   public BigDecimal getXactSeconds()
   {
      return (this.xactSeconds);
   }


   /*******************************************************************************
    ** Setter for xactSeconds.
    *******************************************************************************/
   public void setXactSeconds(BigDecimal xactSeconds)
   {
      this.xactSeconds = xactSeconds;
   }


   /*******************************************************************************
    ** Fluent setter for xactSeconds.
    *******************************************************************************/
   public DbPerfActivitySample withXactSeconds(BigDecimal xactSeconds)
   {
      this.xactSeconds = xactSeconds;
      return (this);
   }


   /*******************************************************************************
    ** Getter for blockedBy.
    *******************************************************************************/
   public String getBlockedBy()
   {
      return (this.blockedBy);
   }


   /*******************************************************************************
    ** Setter for blockedBy.
    *******************************************************************************/
   public void setBlockedBy(String blockedBy)
   {
      this.blockedBy = blockedBy;
   }


   /*******************************************************************************
    ** Fluent setter for blockedBy.
    *******************************************************************************/
   public DbPerfActivitySample withBlockedBy(String blockedBy)
   {
      this.blockedBy = blockedBy;
      return (this);
   }


   /*******************************************************************************
    ** Getter for blockingCount.
    *******************************************************************************/
   public Integer getBlockingCount()
   {
      return (this.blockingCount);
   }


   /*******************************************************************************
    ** Setter for blockingCount.
    *******************************************************************************/
   public void setBlockingCount(Integer blockingCount)
   {
      this.blockingCount = blockingCount;
   }


   /*******************************************************************************
    ** Fluent setter for blockingCount.
    *******************************************************************************/
   public DbPerfActivitySample withBlockingCount(Integer blockingCount)
   {
      this.blockingCount = blockingCount;
      return (this);
   }


   /*******************************************************************************
    ** Getter for reason.
    *******************************************************************************/
   public String getReason()
   {
      return (this.reason);
   }


   /*******************************************************************************
    ** Setter for reason.
    *******************************************************************************/
   public void setReason(String reason)
   {
      this.reason = reason;
   }


   /*******************************************************************************
    ** Fluent setter for reason.
    *******************************************************************************/
   public DbPerfActivitySample withReason(String reason)
   {
      this.reason = reason;
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
   public DbPerfActivitySample withQueryId(Long queryId)
   {
      this.queryId = queryId;
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
   public DbPerfActivitySample withStatementId(Integer statementId)
   {
      this.statementId = statementId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for queryFingerprint.
    *******************************************************************************/
   public String getQueryFingerprint()
   {
      return (this.queryFingerprint);
   }


   /*******************************************************************************
    ** Setter for queryFingerprint.
    *******************************************************************************/
   public void setQueryFingerprint(String queryFingerprint)
   {
      this.queryFingerprint = queryFingerprint;
   }


   /*******************************************************************************
    ** Fluent setter for queryFingerprint.
    *******************************************************************************/
   public DbPerfActivitySample withQueryFingerprint(String queryFingerprint)
   {
      this.queryFingerprint = queryFingerprint;
      return (this);
   }


   /*******************************************************************************
    ** Getter for queryTextNormalized.
    *******************************************************************************/
   public String getQueryTextNormalized()
   {
      return (this.queryTextNormalized);
   }


   /*******************************************************************************
    ** Setter for queryTextNormalized.
    *******************************************************************************/
   public void setQueryTextNormalized(String queryTextNormalized)
   {
      this.queryTextNormalized = queryTextNormalized;
   }


   /*******************************************************************************
    ** Fluent setter for queryTextNormalized.
    *******************************************************************************/
   public DbPerfActivitySample withQueryTextNormalized(String queryTextNormalized)
   {
      this.queryTextNormalized = queryTextNormalized;
      return (this);
   }
}
