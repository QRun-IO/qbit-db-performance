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
 ** Suggestion Rules persisted through QQQ table actions.
 *******************************************************************************/
public class DbPerfRule extends QRecordEntity
{
   public static final String TABLE_NAME = "db_perf_rule";

   @QField(isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(isRequired = true, maxLength = 40)
   private String code;

   @QField(maxLength = 100)
   private String label;

   @QField(maxLength = 1000)
   private String description;

   @QField
   private Boolean enabled;

   @QField(maxLength = 20)
   private String severity;

   @QField
   private BigDecimal threshold1;

   @QField
   private BigDecimal threshold2;

   @QField(maxLength = 100)
   private String threshold1Label;

   @QField(maxLength = 100)
   private String threshold2Label;

   @QField
   private Instant createDate;

   @QField
   private Instant modifyDate;


   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public DbPerfRule()
   {
   }


   /*******************************************************************************
    ** Populate from a QQQ record.
    *******************************************************************************/
   public DbPerfRule(QRecord record)
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
   public DbPerfRule withId(Integer id)
   {
      this.id = id;
      return (this);
   }


   /*******************************************************************************
    ** Getter for code.
    *******************************************************************************/
   public String getCode()
   {
      return (this.code);
   }


   /*******************************************************************************
    ** Setter for code.
    *******************************************************************************/
   public void setCode(String code)
   {
      this.code = code;
   }


   /*******************************************************************************
    ** Fluent setter for code.
    *******************************************************************************/
   public DbPerfRule withCode(String code)
   {
      this.code = code;
      return (this);
   }


   /*******************************************************************************
    ** Getter for label.
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }


   /*******************************************************************************
    ** Setter for label.
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }


   /*******************************************************************************
    ** Fluent setter for label.
    *******************************************************************************/
   public DbPerfRule withLabel(String label)
   {
      this.label = label;
      return (this);
   }


   /*******************************************************************************
    ** Getter for description.
    *******************************************************************************/
   public String getDescription()
   {
      return (this.description);
   }


   /*******************************************************************************
    ** Setter for description.
    *******************************************************************************/
   public void setDescription(String description)
   {
      this.description = description;
   }


   /*******************************************************************************
    ** Fluent setter for description.
    *******************************************************************************/
   public DbPerfRule withDescription(String description)
   {
      this.description = description;
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
   public DbPerfRule withEnabled(Boolean enabled)
   {
      this.enabled = enabled;
      return (this);
   }


   /*******************************************************************************
    ** Getter for severity.
    *******************************************************************************/
   public String getSeverity()
   {
      return (this.severity);
   }


   /*******************************************************************************
    ** Setter for severity.
    *******************************************************************************/
   public void setSeverity(String severity)
   {
      this.severity = severity;
   }


   /*******************************************************************************
    ** Fluent setter for severity.
    *******************************************************************************/
   public DbPerfRule withSeverity(String severity)
   {
      this.severity = severity;
      return (this);
   }


   /*******************************************************************************
    ** Getter for threshold1.
    *******************************************************************************/
   public BigDecimal getThreshold1()
   {
      return (this.threshold1);
   }


   /*******************************************************************************
    ** Setter for threshold1.
    *******************************************************************************/
   public void setThreshold1(BigDecimal threshold1)
   {
      this.threshold1 = threshold1;
   }


   /*******************************************************************************
    ** Fluent setter for threshold1.
    *******************************************************************************/
   public DbPerfRule withThreshold1(BigDecimal threshold1)
   {
      this.threshold1 = threshold1;
      return (this);
   }


   /*******************************************************************************
    ** Getter for threshold2.
    *******************************************************************************/
   public BigDecimal getThreshold2()
   {
      return (this.threshold2);
   }


   /*******************************************************************************
    ** Setter for threshold2.
    *******************************************************************************/
   public void setThreshold2(BigDecimal threshold2)
   {
      this.threshold2 = threshold2;
   }


   /*******************************************************************************
    ** Fluent setter for threshold2.
    *******************************************************************************/
   public DbPerfRule withThreshold2(BigDecimal threshold2)
   {
      this.threshold2 = threshold2;
      return (this);
   }


   /*******************************************************************************
    ** Getter for threshold1Label.
    *******************************************************************************/
   public String getThreshold1Label()
   {
      return (this.threshold1Label);
   }


   /*******************************************************************************
    ** Setter for threshold1Label.
    *******************************************************************************/
   public void setThreshold1Label(String threshold1Label)
   {
      this.threshold1Label = threshold1Label;
   }


   /*******************************************************************************
    ** Fluent setter for threshold1Label.
    *******************************************************************************/
   public DbPerfRule withThreshold1Label(String threshold1Label)
   {
      this.threshold1Label = threshold1Label;
      return (this);
   }


   /*******************************************************************************
    ** Getter for threshold2Label.
    *******************************************************************************/
   public String getThreshold2Label()
   {
      return (this.threshold2Label);
   }


   /*******************************************************************************
    ** Setter for threshold2Label.
    *******************************************************************************/
   public void setThreshold2Label(String threshold2Label)
   {
      this.threshold2Label = threshold2Label;
   }


   /*******************************************************************************
    ** Fluent setter for threshold2Label.
    *******************************************************************************/
   public DbPerfRule withThreshold2Label(String threshold2Label)
   {
      this.threshold2Label = threshold2Label;
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
   public DbPerfRule withCreateDate(Instant createDate)
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
   public DbPerfRule withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }
}
