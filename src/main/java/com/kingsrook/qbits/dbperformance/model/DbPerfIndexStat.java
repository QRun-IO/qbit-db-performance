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
 ** Indexes persisted through QQQ table actions.
 *******************************************************************************/
public class DbPerfIndexStat extends QRecordEntity
{
   public static final String TABLE_NAME = "db_perf_index_stat";

   @QField(isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(maxLength = 100)
   private String backendName;

   @QField
   private Integer tableStatId;

   @QField(maxLength = 100)
   private String schemaName;

   @QField(maxLength = 200)
   private String tableName;

   @QField(maxLength = 200)
   private String indexName;

   @QField
   private Long relid;

   @QField(maxLength = 2000)
   private String definition;

   @QField(maxLength = 1000)
   private String columns;

   @QField
   private Boolean isUnique;

   @QField
   private Boolean isPrimary;

   @QField
   private Boolean isValid;

   @QField
   private Long idxScan;

   @QField
   private Long idxTupRead;

   @QField
   private Long idxTupFetch;

   @QField
   private Long sizeBytes;

   @QField
   private Long writeCost;

   @QField
   private Long scansSinceReset;

   @QField
   private Instant lastSeenAt;

   @QField
   private Instant createDate;

   @QField
   private Instant modifyDate;


   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public DbPerfIndexStat()
   {
   }


   /*******************************************************************************
    ** Populate from a QQQ record.
    *******************************************************************************/
   public DbPerfIndexStat(QRecord record)
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
   public DbPerfIndexStat withId(Integer id)
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
   public DbPerfIndexStat withBackendName(String backendName)
   {
      this.backendName = backendName;
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
   public DbPerfIndexStat withTableStatId(Integer tableStatId)
   {
      this.tableStatId = tableStatId;
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
   public DbPerfIndexStat withSchemaName(String schemaName)
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
   public DbPerfIndexStat withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for indexName.
    *******************************************************************************/
   public String getIndexName()
   {
      return (this.indexName);
   }


   /*******************************************************************************
    ** Setter for indexName.
    *******************************************************************************/
   public void setIndexName(String indexName)
   {
      this.indexName = indexName;
   }


   /*******************************************************************************
    ** Fluent setter for indexName.
    *******************************************************************************/
   public DbPerfIndexStat withIndexName(String indexName)
   {
      this.indexName = indexName;
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
   public DbPerfIndexStat withRelid(Long relid)
   {
      this.relid = relid;
      return (this);
   }


   /*******************************************************************************
    ** Getter for definition.
    *******************************************************************************/
   public String getDefinition()
   {
      return (this.definition);
   }


   /*******************************************************************************
    ** Setter for definition.
    *******************************************************************************/
   public void setDefinition(String definition)
   {
      this.definition = definition;
   }


   /*******************************************************************************
    ** Fluent setter for definition.
    *******************************************************************************/
   public DbPerfIndexStat withDefinition(String definition)
   {
      this.definition = definition;
      return (this);
   }


   /*******************************************************************************
    ** Getter for columns.
    *******************************************************************************/
   public String getColumns()
   {
      return (this.columns);
   }


   /*******************************************************************************
    ** Setter for columns.
    *******************************************************************************/
   public void setColumns(String columns)
   {
      this.columns = columns;
   }


   /*******************************************************************************
    ** Fluent setter for columns.
    *******************************************************************************/
   public DbPerfIndexStat withColumns(String columns)
   {
      this.columns = columns;
      return (this);
   }


   /*******************************************************************************
    ** Getter for isUnique.
    *******************************************************************************/
   public Boolean getIsUnique()
   {
      return (this.isUnique);
   }


   /*******************************************************************************
    ** Setter for isUnique.
    *******************************************************************************/
   public void setIsUnique(Boolean isUnique)
   {
      this.isUnique = isUnique;
   }


   /*******************************************************************************
    ** Fluent setter for isUnique.
    *******************************************************************************/
   public DbPerfIndexStat withIsUnique(Boolean isUnique)
   {
      this.isUnique = isUnique;
      return (this);
   }


   /*******************************************************************************
    ** Getter for isPrimary.
    *******************************************************************************/
   public Boolean getIsPrimary()
   {
      return (this.isPrimary);
   }


   /*******************************************************************************
    ** Setter for isPrimary.
    *******************************************************************************/
   public void setIsPrimary(Boolean isPrimary)
   {
      this.isPrimary = isPrimary;
   }


   /*******************************************************************************
    ** Fluent setter for isPrimary.
    *******************************************************************************/
   public DbPerfIndexStat withIsPrimary(Boolean isPrimary)
   {
      this.isPrimary = isPrimary;
      return (this);
   }


   /*******************************************************************************
    ** Getter for isValid.
    *******************************************************************************/
   public Boolean getIsValid()
   {
      return (this.isValid);
   }


   /*******************************************************************************
    ** Setter for isValid.
    *******************************************************************************/
   public void setIsValid(Boolean isValid)
   {
      this.isValid = isValid;
   }


   /*******************************************************************************
    ** Fluent setter for isValid.
    *******************************************************************************/
   public DbPerfIndexStat withIsValid(Boolean isValid)
   {
      this.isValid = isValid;
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
   public DbPerfIndexStat withIdxScan(Long idxScan)
   {
      this.idxScan = idxScan;
      return (this);
   }


   /*******************************************************************************
    ** Getter for idxTupRead.
    *******************************************************************************/
   public Long getIdxTupRead()
   {
      return (this.idxTupRead);
   }


   /*******************************************************************************
    ** Setter for idxTupRead.
    *******************************************************************************/
   public void setIdxTupRead(Long idxTupRead)
   {
      this.idxTupRead = idxTupRead;
   }


   /*******************************************************************************
    ** Fluent setter for idxTupRead.
    *******************************************************************************/
   public DbPerfIndexStat withIdxTupRead(Long idxTupRead)
   {
      this.idxTupRead = idxTupRead;
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
   public DbPerfIndexStat withIdxTupFetch(Long idxTupFetch)
   {
      this.idxTupFetch = idxTupFetch;
      return (this);
   }


   /*******************************************************************************
    ** Getter for sizeBytes.
    *******************************************************************************/
   public Long getSizeBytes()
   {
      return (this.sizeBytes);
   }


   /*******************************************************************************
    ** Setter for sizeBytes.
    *******************************************************************************/
   public void setSizeBytes(Long sizeBytes)
   {
      this.sizeBytes = sizeBytes;
   }


   /*******************************************************************************
    ** Fluent setter for sizeBytes.
    *******************************************************************************/
   public DbPerfIndexStat withSizeBytes(Long sizeBytes)
   {
      this.sizeBytes = sizeBytes;
      return (this);
   }


   /*******************************************************************************
    ** Getter for writeCost.
    *******************************************************************************/
   public Long getWriteCost()
   {
      return (this.writeCost);
   }


   /*******************************************************************************
    ** Setter for writeCost.
    *******************************************************************************/
   public void setWriteCost(Long writeCost)
   {
      this.writeCost = writeCost;
   }


   /*******************************************************************************
    ** Fluent setter for writeCost.
    *******************************************************************************/
   public DbPerfIndexStat withWriteCost(Long writeCost)
   {
      this.writeCost = writeCost;
      return (this);
   }


   /*******************************************************************************
    ** Getter for scansSinceReset.
    *******************************************************************************/
   public Long getScansSinceReset()
   {
      return (this.scansSinceReset);
   }


   /*******************************************************************************
    ** Setter for scansSinceReset.
    *******************************************************************************/
   public void setScansSinceReset(Long scansSinceReset)
   {
      this.scansSinceReset = scansSinceReset;
   }


   /*******************************************************************************
    ** Fluent setter for scansSinceReset.
    *******************************************************************************/
   public DbPerfIndexStat withScansSinceReset(Long scansSinceReset)
   {
      this.scansSinceReset = scansSinceReset;
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
   public DbPerfIndexStat withLastSeenAt(Instant lastSeenAt)
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
   public DbPerfIndexStat withCreateDate(Instant createDate)
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
   public DbPerfIndexStat withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }
}
