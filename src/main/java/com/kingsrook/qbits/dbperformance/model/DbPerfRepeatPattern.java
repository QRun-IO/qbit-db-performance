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
 ** Repeated Statements persisted through QQQ table actions.
 *******************************************************************************/
public class DbPerfRepeatPattern extends QRecordEntity
{
   public static final String TABLE_NAME = "db_perf_repeat_pattern";

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

   @QField(maxLength = 64)
   private String fingerprint;

   @QField(maxLength = 4000)
   private String sqlText;

   @QField
   private Long unitsAffected;

   @QField
   private Integer maxRepeats;

   @QField
   private Long totalRepeats;


   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public DbPerfRepeatPattern()
   {
   }


   /*******************************************************************************
    ** Populate from a QQQ record.
    *******************************************************************************/
   public DbPerfRepeatPattern(QRecord record)
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
   public DbPerfRepeatPattern withId(Integer id)
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
   public DbPerfRepeatPattern withWindowStart(Instant windowStart)
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
   public DbPerfRepeatPattern withWindowEnd(Instant windowEnd)
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
   public DbPerfRepeatPattern withPodName(String podName)
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
   public DbPerfRepeatPattern withUnitKind(String unitKind)
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
   public DbPerfRepeatPattern withUnitName(String unitName)
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
   public DbPerfRepeatPattern withProcessName(String processName)
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
   public DbPerfRepeatPattern withTableName(String tableName)
   {
      this.tableName = tableName;
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
   public DbPerfRepeatPattern withFingerprint(String fingerprint)
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
   public DbPerfRepeatPattern withSqlText(String sqlText)
   {
      this.sqlText = sqlText;
      return (this);
   }


   /*******************************************************************************
    ** Getter for unitsAffected.
    *******************************************************************************/
   public Long getUnitsAffected()
   {
      return (this.unitsAffected);
   }


   /*******************************************************************************
    ** Setter for unitsAffected.
    *******************************************************************************/
   public void setUnitsAffected(Long unitsAffected)
   {
      this.unitsAffected = unitsAffected;
   }


   /*******************************************************************************
    ** Fluent setter for unitsAffected.
    *******************************************************************************/
   public DbPerfRepeatPattern withUnitsAffected(Long unitsAffected)
   {
      this.unitsAffected = unitsAffected;
      return (this);
   }


   /*******************************************************************************
    ** Getter for maxRepeats.
    *******************************************************************************/
   public Integer getMaxRepeats()
   {
      return (this.maxRepeats);
   }


   /*******************************************************************************
    ** Setter for maxRepeats.
    *******************************************************************************/
   public void setMaxRepeats(Integer maxRepeats)
   {
      this.maxRepeats = maxRepeats;
   }


   /*******************************************************************************
    ** Fluent setter for maxRepeats.
    *******************************************************************************/
   public DbPerfRepeatPattern withMaxRepeats(Integer maxRepeats)
   {
      this.maxRepeats = maxRepeats;
      return (this);
   }


   /*******************************************************************************
    ** Getter for totalRepeats.
    *******************************************************************************/
   public Long getTotalRepeats()
   {
      return (this.totalRepeats);
   }


   /*******************************************************************************
    ** Setter for totalRepeats.
    *******************************************************************************/
   public void setTotalRepeats(Long totalRepeats)
   {
      this.totalRepeats = totalRepeats;
   }


   /*******************************************************************************
    ** Fluent setter for totalRepeats.
    *******************************************************************************/
   public DbPerfRepeatPattern withTotalRepeats(Long totalRepeats)
   {
      this.totalRepeats = totalRepeats;
      return (this);
   }
}
