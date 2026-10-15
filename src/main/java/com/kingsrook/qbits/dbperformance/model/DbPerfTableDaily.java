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


import java.time.LocalDate;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;


/*******************************************************************************
 ** Table Daily persisted through QQQ table actions.
 *******************************************************************************/
public class DbPerfTableDaily extends QRecordEntity
{
   public static final String TABLE_NAME = "db_perf_table_daily";

   @QField(isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField
   private Integer tableStatId;

   @QField
   private LocalDate day;

   @QField
   private Long seqScan;

   @QField
   private Long idxScan;

   @QField
   private Long seqTupRead;

   @QField
   private Long nTupIns;

   @QField
   private Long nTupUpd;

   @QField
   private Long nTupDel;

   @QField
   private Long nLiveTup;

   @QField
   private Long nDeadTup;

   @QField
   private Long totalBytes;


   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public DbPerfTableDaily()
   {
   }


   /*******************************************************************************
    ** Populate from a QQQ record.
    *******************************************************************************/
   public DbPerfTableDaily(QRecord record)
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
   public DbPerfTableDaily withId(Integer id)
   {
      this.id = id;
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
   public DbPerfTableDaily withTableStatId(Integer tableStatId)
   {
      this.tableStatId = tableStatId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for day.
    *******************************************************************************/
   public LocalDate getDay()
   {
      return (this.day);
   }


   /*******************************************************************************
    ** Setter for day.
    *******************************************************************************/
   public void setDay(LocalDate day)
   {
      this.day = day;
   }


   /*******************************************************************************
    ** Fluent setter for day.
    *******************************************************************************/
   public DbPerfTableDaily withDay(LocalDate day)
   {
      this.day = day;
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
   public DbPerfTableDaily withSeqScan(Long seqScan)
   {
      this.seqScan = seqScan;
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
   public DbPerfTableDaily withIdxScan(Long idxScan)
   {
      this.idxScan = idxScan;
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
   public DbPerfTableDaily withSeqTupRead(Long seqTupRead)
   {
      this.seqTupRead = seqTupRead;
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
   public DbPerfTableDaily withNTupIns(Long nTupIns)
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
   public DbPerfTableDaily withNTupUpd(Long nTupUpd)
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
   public DbPerfTableDaily withNTupDel(Long nTupDel)
   {
      this.nTupDel = nTupDel;
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
   public DbPerfTableDaily withNLiveTup(Long nLiveTup)
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
   public DbPerfTableDaily withNDeadTup(Long nDeadTup)
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
   public DbPerfTableDaily withTotalBytes(Long totalBytes)
   {
      this.totalBytes = totalBytes;
      return (this);
   }
}
