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
 ** Settings persisted through QQQ table actions.
 *******************************************************************************/
public class DbPerfSettings extends QRecordEntity
{
   public static final String TABLE_NAME = "db_perf_settings";

   @QField(isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(isRequired = true, maxLength = 100)
   private String backendName;

   @QField
   private Boolean enabled;

   @QField(maxLength = 20)
   private String provider;

   @QField
   private Integer snapshotIntervalMinutes;

   @QField
   private Integer tableSnapshotMinutes;

   @QField
   private Integer detailRetentionDays;

   @QField
   private Integer rollupRetentionDays;

   @QField
   private Integer topStatementsPerSnapshot;

   @QField
   private Integer activityLongRunningSeconds;

   @QField
   private Integer idleInTransactionSeconds;

   @QField
   private Boolean attributionEnabled;

   @QField
   private BigDecimal attributionSampleRate;

   @QField
   private Integer attributionFlushSeconds;

   @QField
   private Integer attributionMaxKeys;

   @QField
   private Integer nPlusOneThreshold;

   @QField
   private Integer explainTimeoutMs;

   @QField
   private Boolean alertsEnabled;

   @QField
   private BigDecimal spikeFactor;

   @QField
   private Integer spikeMinTotalMs;

   @QField
   private Integer connectionAlertPercent;

   @QField
   private Integer blockedAlertSeconds;

   @QField
   private Instant lastSnapshotAt;

   @QField
   private Instant lastTableSnapshotAt;

   @QField(maxLength = 30)
   private String statsAccess;

   @QField(maxLength = 500)
   private String statsAccessMessage;

   @QField
   private Instant createDate;

   @QField
   private Instant modifyDate;


   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public DbPerfSettings()
   {
   }


   /*******************************************************************************
    ** Populate from a QQQ record.
    *******************************************************************************/
   public DbPerfSettings(QRecord record)
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
   public DbPerfSettings withId(Integer id)
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
   public DbPerfSettings withBackendName(String backendName)
   {
      this.backendName = backendName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for enabled.
    *******************************************************************************/
   public Boolean getEnabled()
   {
      return (this.enabled);
   }


   /*******************************************************************************
    ** Setter for enabled.
    *******************************************************************************/
   public void setEnabled(Boolean enabled)
   {
      this.enabled = enabled;
   }


   /*******************************************************************************
    ** Fluent setter for enabled.
    *******************************************************************************/
   public DbPerfSettings withEnabled(Boolean enabled)
   {
      this.enabled = enabled;
      return (this);
   }


   /*******************************************************************************
    ** Getter for provider.
    *******************************************************************************/
   public String getProvider()
   {
      return (this.provider);
   }


   /*******************************************************************************
    ** Setter for provider.
    *******************************************************************************/
   public void setProvider(String provider)
   {
      this.provider = provider;
   }


   /*******************************************************************************
    ** Fluent setter for provider.
    *******************************************************************************/
   public DbPerfSettings withProvider(String provider)
   {
      this.provider = provider;
      return (this);
   }


   /*******************************************************************************
    ** Getter for snapshotIntervalMinutes.
    *******************************************************************************/
   public Integer getSnapshotIntervalMinutes()
   {
      return (this.snapshotIntervalMinutes);
   }


   /*******************************************************************************
    ** Setter for snapshotIntervalMinutes.
    *******************************************************************************/
   public void setSnapshotIntervalMinutes(Integer snapshotIntervalMinutes)
   {
      this.snapshotIntervalMinutes = snapshotIntervalMinutes;
   }


   /*******************************************************************************
    ** Fluent setter for snapshotIntervalMinutes.
    *******************************************************************************/
   public DbPerfSettings withSnapshotIntervalMinutes(Integer snapshotIntervalMinutes)
   {
      this.snapshotIntervalMinutes = snapshotIntervalMinutes;
      return (this);
   }


   /*******************************************************************************
    ** Getter for tableSnapshotMinutes.
    *******************************************************************************/
   public Integer getTableSnapshotMinutes()
   {
      return (this.tableSnapshotMinutes);
   }


   /*******************************************************************************
    ** Setter for tableSnapshotMinutes.
    *******************************************************************************/
   public void setTableSnapshotMinutes(Integer tableSnapshotMinutes)
   {
      this.tableSnapshotMinutes = tableSnapshotMinutes;
   }


   /*******************************************************************************
    ** Fluent setter for tableSnapshotMinutes.
    *******************************************************************************/
   public DbPerfSettings withTableSnapshotMinutes(Integer tableSnapshotMinutes)
   {
      this.tableSnapshotMinutes = tableSnapshotMinutes;
      return (this);
   }


   /*******************************************************************************
    ** Getter for detailRetentionDays.
    *******************************************************************************/
   public Integer getDetailRetentionDays()
   {
      return (this.detailRetentionDays);
   }


   /*******************************************************************************
    ** Setter for detailRetentionDays.
    *******************************************************************************/
   public void setDetailRetentionDays(Integer detailRetentionDays)
   {
      this.detailRetentionDays = detailRetentionDays;
   }


   /*******************************************************************************
    ** Fluent setter for detailRetentionDays.
    *******************************************************************************/
   public DbPerfSettings withDetailRetentionDays(Integer detailRetentionDays)
   {
      this.detailRetentionDays = detailRetentionDays;
      return (this);
   }


   /*******************************************************************************
    ** Getter for rollupRetentionDays.
    *******************************************************************************/
   public Integer getRollupRetentionDays()
   {
      return (this.rollupRetentionDays);
   }


   /*******************************************************************************
    ** Setter for rollupRetentionDays.
    *******************************************************************************/
   public void setRollupRetentionDays(Integer rollupRetentionDays)
   {
      this.rollupRetentionDays = rollupRetentionDays;
   }


   /*******************************************************************************
    ** Fluent setter for rollupRetentionDays.
    *******************************************************************************/
   public DbPerfSettings withRollupRetentionDays(Integer rollupRetentionDays)
   {
      this.rollupRetentionDays = rollupRetentionDays;
      return (this);
   }


   /*******************************************************************************
    ** Getter for topStatementsPerSnapshot.
    *******************************************************************************/
   public Integer getTopStatementsPerSnapshot()
   {
      return (this.topStatementsPerSnapshot);
   }


   /*******************************************************************************
    ** Setter for topStatementsPerSnapshot.
    *******************************************************************************/
   public void setTopStatementsPerSnapshot(Integer topStatementsPerSnapshot)
   {
      this.topStatementsPerSnapshot = topStatementsPerSnapshot;
   }


   /*******************************************************************************
    ** Fluent setter for topStatementsPerSnapshot.
    *******************************************************************************/
   public DbPerfSettings withTopStatementsPerSnapshot(Integer topStatementsPerSnapshot)
   {
      this.topStatementsPerSnapshot = topStatementsPerSnapshot;
      return (this);
   }


   /*******************************************************************************
    ** Getter for activityLongRunningSeconds.
    *******************************************************************************/
   public Integer getActivityLongRunningSeconds()
   {
      return (this.activityLongRunningSeconds);
   }


   /*******************************************************************************
    ** Setter for activityLongRunningSeconds.
    *******************************************************************************/
   public void setActivityLongRunningSeconds(Integer activityLongRunningSeconds)
   {
      this.activityLongRunningSeconds = activityLongRunningSeconds;
   }


   /*******************************************************************************
    ** Fluent setter for activityLongRunningSeconds.
    *******************************************************************************/
   public DbPerfSettings withActivityLongRunningSeconds(Integer activityLongRunningSeconds)
   {
      this.activityLongRunningSeconds = activityLongRunningSeconds;
      return (this);
   }


   /*******************************************************************************
    ** Getter for idleInTransactionSeconds.
    *******************************************************************************/
   public Integer getIdleInTransactionSeconds()
   {
      return (this.idleInTransactionSeconds);
   }


   /*******************************************************************************
    ** Setter for idleInTransactionSeconds.
    *******************************************************************************/
   public void setIdleInTransactionSeconds(Integer idleInTransactionSeconds)
   {
      this.idleInTransactionSeconds = idleInTransactionSeconds;
   }


   /*******************************************************************************
    ** Fluent setter for idleInTransactionSeconds.
    *******************************************************************************/
   public DbPerfSettings withIdleInTransactionSeconds(Integer idleInTransactionSeconds)
   {
      this.idleInTransactionSeconds = idleInTransactionSeconds;
      return (this);
   }


   /*******************************************************************************
    ** Getter for attributionEnabled.
    *******************************************************************************/
   public Boolean getAttributionEnabled()
   {
      return (this.attributionEnabled);
   }


   /*******************************************************************************
    ** Setter for attributionEnabled.
    *******************************************************************************/
   public void setAttributionEnabled(Boolean attributionEnabled)
   {
      this.attributionEnabled = attributionEnabled;
   }


   /*******************************************************************************
    ** Fluent setter for attributionEnabled.
    *******************************************************************************/
   public DbPerfSettings withAttributionEnabled(Boolean attributionEnabled)
   {
      this.attributionEnabled = attributionEnabled;
      return (this);
   }


   /*******************************************************************************
    ** Getter for attributionSampleRate.
    *******************************************************************************/
   public BigDecimal getAttributionSampleRate()
   {
      return (this.attributionSampleRate);
   }


   /*******************************************************************************
    ** Setter for attributionSampleRate.
    *******************************************************************************/
   public void setAttributionSampleRate(BigDecimal attributionSampleRate)
   {
      this.attributionSampleRate = attributionSampleRate;
   }


   /*******************************************************************************
    ** Fluent setter for attributionSampleRate.
    *******************************************************************************/
   public DbPerfSettings withAttributionSampleRate(BigDecimal attributionSampleRate)
   {
      this.attributionSampleRate = attributionSampleRate;
      return (this);
   }


   /*******************************************************************************
    ** Getter for attributionFlushSeconds.
    *******************************************************************************/
   public Integer getAttributionFlushSeconds()
   {
      return (this.attributionFlushSeconds);
   }


   /*******************************************************************************
    ** Setter for attributionFlushSeconds.
    *******************************************************************************/
   public void setAttributionFlushSeconds(Integer attributionFlushSeconds)
   {
      this.attributionFlushSeconds = attributionFlushSeconds;
   }


   /*******************************************************************************
    ** Fluent setter for attributionFlushSeconds.
    *******************************************************************************/
   public DbPerfSettings withAttributionFlushSeconds(Integer attributionFlushSeconds)
   {
      this.attributionFlushSeconds = attributionFlushSeconds;
      return (this);
   }


   /*******************************************************************************
    ** Getter for attributionMaxKeys.
    *******************************************************************************/
   public Integer getAttributionMaxKeys()
   {
      return (this.attributionMaxKeys);
   }


   /*******************************************************************************
    ** Setter for attributionMaxKeys.
    *******************************************************************************/
   public void setAttributionMaxKeys(Integer attributionMaxKeys)
   {
      this.attributionMaxKeys = attributionMaxKeys;
   }


   /*******************************************************************************
    ** Fluent setter for attributionMaxKeys.
    *******************************************************************************/
   public DbPerfSettings withAttributionMaxKeys(Integer attributionMaxKeys)
   {
      this.attributionMaxKeys = attributionMaxKeys;
      return (this);
   }


   /*******************************************************************************
    ** Getter for nPlusOneThreshold.
    *******************************************************************************/
   public Integer getNPlusOneThreshold()
   {
      return (this.nPlusOneThreshold);
   }


   /*******************************************************************************
    ** Setter for nPlusOneThreshold.
    *******************************************************************************/
   public void setNPlusOneThreshold(Integer nPlusOneThreshold)
   {
      this.nPlusOneThreshold = nPlusOneThreshold;
   }


   /*******************************************************************************
    ** Fluent setter for nPlusOneThreshold.
    *******************************************************************************/
   public DbPerfSettings withNPlusOneThreshold(Integer nPlusOneThreshold)
   {
      this.nPlusOneThreshold = nPlusOneThreshold;
      return (this);
   }


   /*******************************************************************************
    ** Getter for explainTimeoutMs.
    *******************************************************************************/
   public Integer getExplainTimeoutMs()
   {
      return (this.explainTimeoutMs);
   }


   /*******************************************************************************
    ** Setter for explainTimeoutMs.
    *******************************************************************************/
   public void setExplainTimeoutMs(Integer explainTimeoutMs)
   {
      this.explainTimeoutMs = explainTimeoutMs;
   }


   /*******************************************************************************
    ** Fluent setter for explainTimeoutMs.
    *******************************************************************************/
   public DbPerfSettings withExplainTimeoutMs(Integer explainTimeoutMs)
   {
      this.explainTimeoutMs = explainTimeoutMs;
      return (this);
   }


   /*******************************************************************************
    ** Getter for alertsEnabled.
    *******************************************************************************/
   public Boolean getAlertsEnabled()
   {
      return (this.alertsEnabled);
   }


   /*******************************************************************************
    ** Setter for alertsEnabled.
    *******************************************************************************/
   public void setAlertsEnabled(Boolean alertsEnabled)
   {
      this.alertsEnabled = alertsEnabled;
   }


   /*******************************************************************************
    ** Fluent setter for alertsEnabled.
    *******************************************************************************/
   public DbPerfSettings withAlertsEnabled(Boolean alertsEnabled)
   {
      this.alertsEnabled = alertsEnabled;
      return (this);
   }


   /*******************************************************************************
    ** Getter for spikeFactor.
    *******************************************************************************/
   public BigDecimal getSpikeFactor()
   {
      return (this.spikeFactor);
   }


   /*******************************************************************************
    ** Setter for spikeFactor.
    *******************************************************************************/
   public void setSpikeFactor(BigDecimal spikeFactor)
   {
      this.spikeFactor = spikeFactor;
   }


   /*******************************************************************************
    ** Fluent setter for spikeFactor.
    *******************************************************************************/
   public DbPerfSettings withSpikeFactor(BigDecimal spikeFactor)
   {
      this.spikeFactor = spikeFactor;
      return (this);
   }


   /*******************************************************************************
    ** Getter for spikeMinTotalMs.
    *******************************************************************************/
   public Integer getSpikeMinTotalMs()
   {
      return (this.spikeMinTotalMs);
   }


   /*******************************************************************************
    ** Setter for spikeMinTotalMs.
    *******************************************************************************/
   public void setSpikeMinTotalMs(Integer spikeMinTotalMs)
   {
      this.spikeMinTotalMs = spikeMinTotalMs;
   }


   /*******************************************************************************
    ** Fluent setter for spikeMinTotalMs.
    *******************************************************************************/
   public DbPerfSettings withSpikeMinTotalMs(Integer spikeMinTotalMs)
   {
      this.spikeMinTotalMs = spikeMinTotalMs;
      return (this);
   }


   /*******************************************************************************
    ** Getter for connectionAlertPercent.
    *******************************************************************************/
   public Integer getConnectionAlertPercent()
   {
      return (this.connectionAlertPercent);
   }


   /*******************************************************************************
    ** Setter for connectionAlertPercent.
    *******************************************************************************/
   public void setConnectionAlertPercent(Integer connectionAlertPercent)
   {
      this.connectionAlertPercent = connectionAlertPercent;
   }


   /*******************************************************************************
    ** Fluent setter for connectionAlertPercent.
    *******************************************************************************/
   public DbPerfSettings withConnectionAlertPercent(Integer connectionAlertPercent)
   {
      this.connectionAlertPercent = connectionAlertPercent;
      return (this);
   }


   /*******************************************************************************
    ** Getter for blockedAlertSeconds.
    *******************************************************************************/
   public Integer getBlockedAlertSeconds()
   {
      return (this.blockedAlertSeconds);
   }


   /*******************************************************************************
    ** Setter for blockedAlertSeconds.
    *******************************************************************************/
   public void setBlockedAlertSeconds(Integer blockedAlertSeconds)
   {
      this.blockedAlertSeconds = blockedAlertSeconds;
   }


   /*******************************************************************************
    ** Fluent setter for blockedAlertSeconds.
    *******************************************************************************/
   public DbPerfSettings withBlockedAlertSeconds(Integer blockedAlertSeconds)
   {
      this.blockedAlertSeconds = blockedAlertSeconds;
      return (this);
   }


   /*******************************************************************************
    ** Getter for lastSnapshotAt.
    *******************************************************************************/
   public Instant getLastSnapshotAt()
   {
      return (this.lastSnapshotAt);
   }


   /*******************************************************************************
    ** Setter for lastSnapshotAt.
    *******************************************************************************/
   public void setLastSnapshotAt(Instant lastSnapshotAt)
   {
      this.lastSnapshotAt = lastSnapshotAt;
   }


   /*******************************************************************************
    ** Fluent setter for lastSnapshotAt.
    *******************************************************************************/
   public DbPerfSettings withLastSnapshotAt(Instant lastSnapshotAt)
   {
      this.lastSnapshotAt = lastSnapshotAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for lastTableSnapshotAt.
    *******************************************************************************/
   public Instant getLastTableSnapshotAt()
   {
      return (this.lastTableSnapshotAt);
   }


   /*******************************************************************************
    ** Setter for lastTableSnapshotAt.
    *******************************************************************************/
   public void setLastTableSnapshotAt(Instant lastTableSnapshotAt)
   {
      this.lastTableSnapshotAt = lastTableSnapshotAt;
   }


   /*******************************************************************************
    ** Fluent setter for lastTableSnapshotAt.
    *******************************************************************************/
   public DbPerfSettings withLastTableSnapshotAt(Instant lastTableSnapshotAt)
   {
      this.lastTableSnapshotAt = lastTableSnapshotAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for statsAccess.
    *******************************************************************************/
   public String getStatsAccess()
   {
      return (this.statsAccess);
   }


   /*******************************************************************************
    ** Setter for statsAccess.
    *******************************************************************************/
   public void setStatsAccess(String statsAccess)
   {
      this.statsAccess = statsAccess;
   }


   /*******************************************************************************
    ** Fluent setter for statsAccess.
    *******************************************************************************/
   public DbPerfSettings withStatsAccess(String statsAccess)
   {
      this.statsAccess = statsAccess;
      return (this);
   }


   /*******************************************************************************
    ** Getter for statsAccessMessage.
    *******************************************************************************/
   public String getStatsAccessMessage()
   {
      return (this.statsAccessMessage);
   }


   /*******************************************************************************
    ** Setter for statsAccessMessage.
    *******************************************************************************/
   public void setStatsAccessMessage(String statsAccessMessage)
   {
      this.statsAccessMessage = statsAccessMessage;
   }


   /*******************************************************************************
    ** Fluent setter for statsAccessMessage.
    *******************************************************************************/
   public DbPerfSettings withStatsAccessMessage(String statsAccessMessage)
   {
      this.statsAccessMessage = statsAccessMessage;
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
   public DbPerfSettings withCreateDate(Instant createDate)
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
   public DbPerfSettings withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }
}
