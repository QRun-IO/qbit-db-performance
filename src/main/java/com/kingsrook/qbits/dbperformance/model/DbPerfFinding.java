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
 ** Findings persisted through QQQ table actions.
 *******************************************************************************/
public class DbPerfFinding extends QRecordEntity
{
   public static final String TABLE_NAME = "db_perf_finding";

   @QField(isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(maxLength = 100)
   private String backendName;

   @QField
   private Integer ruleId;

   @QField(maxLength = 40)
   private String ruleCode;

   @QField(maxLength = 300)
   private String dedupeKey;

   @QField(maxLength = 20)
   private String status;

   @QField(maxLength = 20)
   private String severity;

   @QField
   private BigDecimal score;

   @QField(maxLength = 300)
   private String title;

   @QField(maxLength = 2000)
   private String summary;

   @QField
   private String evidence;

   @QField
   private String recommendation;

   @QField
   private String migrationSnippet;

   @QField(maxLength = 200)
   private String subjectTable;

   @QField
   private Integer statementId;

   @QField
   private Integer tableStatId;

   @QField
   private Integer indexStatId;

   @QField(maxLength = 300)
   private String unitName;

   @QField
   private Instant firstSeenAt;

   @QField
   private Instant lastSeenAt;

   @QField
   private Integer occurrences;

   @QField
   private Instant resolvedAt;

   @QField(maxLength = 2000)
   private String statusNote;

   @QField(maxLength = 100)
   private String statusChangedBy;

   @QField
   private Instant createDate;

   @QField
   private Instant modifyDate;


   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public DbPerfFinding()
   {
   }


   /*******************************************************************************
    ** Populate from a QQQ record.
    *******************************************************************************/
   public DbPerfFinding(QRecord record)
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
   public DbPerfFinding withId(Integer id)
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
   public DbPerfFinding withBackendName(String backendName)
   {
      this.backendName = backendName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for ruleId.
    *******************************************************************************/
   public Integer getRuleId()
   {
      return (this.ruleId);
   }


   /*******************************************************************************
    ** Setter for ruleId.
    *******************************************************************************/
   public void setRuleId(Integer ruleId)
   {
      this.ruleId = ruleId;
   }


   /*******************************************************************************
    ** Fluent setter for ruleId.
    *******************************************************************************/
   public DbPerfFinding withRuleId(Integer ruleId)
   {
      this.ruleId = ruleId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for ruleCode.
    *******************************************************************************/
   public String getRuleCode()
   {
      return (this.ruleCode);
   }


   /*******************************************************************************
    ** Setter for ruleCode.
    *******************************************************************************/
   public void setRuleCode(String ruleCode)
   {
      this.ruleCode = ruleCode;
   }


   /*******************************************************************************
    ** Fluent setter for ruleCode.
    *******************************************************************************/
   public DbPerfFinding withRuleCode(String ruleCode)
   {
      this.ruleCode = ruleCode;
      return (this);
   }


   /*******************************************************************************
    ** Getter for dedupeKey.
    *******************************************************************************/
   public String getDedupeKey()
   {
      return (this.dedupeKey);
   }


   /*******************************************************************************
    ** Setter for dedupeKey.
    *******************************************************************************/
   public void setDedupeKey(String dedupeKey)
   {
      this.dedupeKey = dedupeKey;
   }


   /*******************************************************************************
    ** Fluent setter for dedupeKey.
    *******************************************************************************/
   public DbPerfFinding withDedupeKey(String dedupeKey)
   {
      this.dedupeKey = dedupeKey;
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
   public DbPerfFinding withStatus(String status)
   {
      this.status = status;
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
   public DbPerfFinding withSeverity(String severity)
   {
      this.severity = severity;
      return (this);
   }


   /*******************************************************************************
    ** Getter for score.
    *******************************************************************************/
   public BigDecimal getScore()
   {
      return (this.score);
   }


   /*******************************************************************************
    ** Setter for score.
    *******************************************************************************/
   public void setScore(BigDecimal score)
   {
      this.score = score;
   }


   /*******************************************************************************
    ** Fluent setter for score.
    *******************************************************************************/
   public DbPerfFinding withScore(BigDecimal score)
   {
      this.score = score;
      return (this);
   }


   /*******************************************************************************
    ** Getter for title.
    *******************************************************************************/
   public String getTitle()
   {
      return (this.title);
   }


   /*******************************************************************************
    ** Setter for title.
    *******************************************************************************/
   public void setTitle(String title)
   {
      this.title = title;
   }


   /*******************************************************************************
    ** Fluent setter for title.
    *******************************************************************************/
   public DbPerfFinding withTitle(String title)
   {
      this.title = title;
      return (this);
   }


   /*******************************************************************************
    ** Getter for summary.
    *******************************************************************************/
   public String getSummary()
   {
      return (this.summary);
   }


   /*******************************************************************************
    ** Setter for summary.
    *******************************************************************************/
   public void setSummary(String summary)
   {
      this.summary = summary;
   }


   /*******************************************************************************
    ** Fluent setter for summary.
    *******************************************************************************/
   public DbPerfFinding withSummary(String summary)
   {
      this.summary = summary;
      return (this);
   }


   /*******************************************************************************
    ** Getter for evidence.
    *******************************************************************************/
   public String getEvidence()
   {
      return (this.evidence);
   }


   /*******************************************************************************
    ** Setter for evidence.
    *******************************************************************************/
   public void setEvidence(String evidence)
   {
      this.evidence = evidence;
   }


   /*******************************************************************************
    ** Fluent setter for evidence.
    *******************************************************************************/
   public DbPerfFinding withEvidence(String evidence)
   {
      this.evidence = evidence;
      return (this);
   }


   /*******************************************************************************
    ** Getter for recommendation.
    *******************************************************************************/
   public String getRecommendation()
   {
      return (this.recommendation);
   }


   /*******************************************************************************
    ** Setter for recommendation.
    *******************************************************************************/
   public void setRecommendation(String recommendation)
   {
      this.recommendation = recommendation;
   }


   /*******************************************************************************
    ** Fluent setter for recommendation.
    *******************************************************************************/
   public DbPerfFinding withRecommendation(String recommendation)
   {
      this.recommendation = recommendation;
      return (this);
   }


   /*******************************************************************************
    ** Getter for migrationSnippet.
    *******************************************************************************/
   public String getMigrationSnippet()
   {
      return (this.migrationSnippet);
   }


   /*******************************************************************************
    ** Setter for migrationSnippet.
    *******************************************************************************/
   public void setMigrationSnippet(String migrationSnippet)
   {
      this.migrationSnippet = migrationSnippet;
   }


   /*******************************************************************************
    ** Fluent setter for migrationSnippet.
    *******************************************************************************/
   public DbPerfFinding withMigrationSnippet(String migrationSnippet)
   {
      this.migrationSnippet = migrationSnippet;
      return (this);
   }


   /*******************************************************************************
    ** Getter for subjectTable.
    *******************************************************************************/
   public String getSubjectTable()
   {
      return (this.subjectTable);
   }


   /*******************************************************************************
    ** Setter for subjectTable.
    *******************************************************************************/
   public void setSubjectTable(String subjectTable)
   {
      this.subjectTable = subjectTable;
   }


   /*******************************************************************************
    ** Fluent setter for subjectTable.
    *******************************************************************************/
   public DbPerfFinding withSubjectTable(String subjectTable)
   {
      this.subjectTable = subjectTable;
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
   public DbPerfFinding withStatementId(Integer statementId)
   {
      this.statementId = statementId;
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
   public DbPerfFinding withTableStatId(Integer tableStatId)
   {
      this.tableStatId = tableStatId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for indexStatId.
    *******************************************************************************/
   public Integer getIndexStatId()
   {
      return (this.indexStatId);
   }


   /*******************************************************************************
    ** Setter for indexStatId.
    *******************************************************************************/
   public void setIndexStatId(Integer indexStatId)
   {
      this.indexStatId = indexStatId;
   }


   /*******************************************************************************
    ** Fluent setter for indexStatId.
    *******************************************************************************/
   public DbPerfFinding withIndexStatId(Integer indexStatId)
   {
      this.indexStatId = indexStatId;
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
   public DbPerfFinding withUnitName(String unitName)
   {
      this.unitName = unitName;
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
   public DbPerfFinding withFirstSeenAt(Instant firstSeenAt)
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
   public DbPerfFinding withLastSeenAt(Instant lastSeenAt)
   {
      this.lastSeenAt = lastSeenAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for occurrences.
    *******************************************************************************/
   public Integer getOccurrences()
   {
      return (this.occurrences);
   }


   /*******************************************************************************
    ** Setter for occurrences.
    *******************************************************************************/
   public void setOccurrences(Integer occurrences)
   {
      this.occurrences = occurrences;
   }


   /*******************************************************************************
    ** Fluent setter for occurrences.
    *******************************************************************************/
   public DbPerfFinding withOccurrences(Integer occurrences)
   {
      this.occurrences = occurrences;
      return (this);
   }


   /*******************************************************************************
    ** Getter for resolvedAt.
    *******************************************************************************/
   public Instant getResolvedAt()
   {
      return (this.resolvedAt);
   }


   /*******************************************************************************
    ** Setter for resolvedAt.
    *******************************************************************************/
   public void setResolvedAt(Instant resolvedAt)
   {
      this.resolvedAt = resolvedAt;
   }


   /*******************************************************************************
    ** Fluent setter for resolvedAt.
    *******************************************************************************/
   public DbPerfFinding withResolvedAt(Instant resolvedAt)
   {
      this.resolvedAt = resolvedAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for statusNote.
    *******************************************************************************/
   public String getStatusNote()
   {
      return (this.statusNote);
   }


   /*******************************************************************************
    ** Setter for statusNote.
    *******************************************************************************/
   public void setStatusNote(String statusNote)
   {
      this.statusNote = statusNote;
   }


   /*******************************************************************************
    ** Fluent setter for statusNote.
    *******************************************************************************/
   public DbPerfFinding withStatusNote(String statusNote)
   {
      this.statusNote = statusNote;
      return (this);
   }


   /*******************************************************************************
    ** Getter for statusChangedBy.
    *******************************************************************************/
   public String getStatusChangedBy()
   {
      return (this.statusChangedBy);
   }


   /*******************************************************************************
    ** Setter for statusChangedBy.
    *******************************************************************************/
   public void setStatusChangedBy(String statusChangedBy)
   {
      this.statusChangedBy = statusChangedBy;
   }


   /*******************************************************************************
    ** Fluent setter for statusChangedBy.
    *******************************************************************************/
   public DbPerfFinding withStatusChangedBy(String statusChangedBy)
   {
      this.statusChangedBy = statusChangedBy;
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
   public DbPerfFinding withCreateDate(Instant createDate)
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
   public DbPerfFinding withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }
}
