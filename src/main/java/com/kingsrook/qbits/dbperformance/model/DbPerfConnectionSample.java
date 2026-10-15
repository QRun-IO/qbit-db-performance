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
 ** Connection Samples persisted through QQQ table actions.
 *******************************************************************************/
public class DbPerfConnectionSample extends QRecordEntity
{
   public static final String TABLE_NAME = "db_perf_connection_sample";

   @QField(isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField
   private Integer snapshotId;

   @QField
   private Instant capturedAt;

   @QField(maxLength = 100)
   private String dbUser;

   @QField(maxLength = 200)
   private String applicationName;

   @QField(maxLength = 40)
   private String state;

   @QField
   private Integer connectionCount;


   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public DbPerfConnectionSample()
   {
   }


   /*******************************************************************************
    ** Populate from a QQQ record.
    *******************************************************************************/
   public DbPerfConnectionSample(QRecord record)
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
   public DbPerfConnectionSample withId(Integer id)
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
   public DbPerfConnectionSample withSnapshotId(Integer snapshotId)
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
   public DbPerfConnectionSample withCapturedAt(Instant capturedAt)
   {
      this.capturedAt = capturedAt;
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
   public DbPerfConnectionSample withDbUser(String dbUser)
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
   public DbPerfConnectionSample withApplicationName(String applicationName)
   {
      this.applicationName = applicationName;
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
   public DbPerfConnectionSample withState(String state)
   {
      this.state = state;
      return (this);
   }


   /*******************************************************************************
    ** Getter for connectionCount.
    *******************************************************************************/
   public Integer getConnectionCount()
   {
      return (this.connectionCount);
   }


   /*******************************************************************************
    ** Setter for connectionCount.
    *******************************************************************************/
   public void setConnectionCount(Integer connectionCount)
   {
      this.connectionCount = connectionCount;
   }


   /*******************************************************************************
    ** Fluent setter for connectionCount.
    *******************************************************************************/
   public DbPerfConnectionSample withConnectionCount(Integer connectionCount)
   {
      this.connectionCount = connectionCount;
      return (this);
   }
}
