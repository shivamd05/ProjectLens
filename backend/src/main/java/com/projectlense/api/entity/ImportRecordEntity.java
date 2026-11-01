package com.projectlense.api.entity;

import jakarta.persistence.*;

@Entity @Table(name = "excel_import_records", uniqueConstraints = @UniqueConstraint(name="uq_import_job_source_row", columnNames={"job_id","sheet_name","source_row"}))
public class ImportRecordEntity {
    @Id @Column(name="record_id", length=36) private String id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="job_id", nullable=false) private ImportJobEntity job;
    @Column(name="sheet_name", nullable=false, length=100) private String sheetName;
    @Column(name="source_row", nullable=false) private int sourceRow;
    @Column(name="record_json", nullable=false, columnDefinition="LONGTEXT") private String recordJson;
    @Column(name="validation_json", nullable=false, columnDefinition="LONGTEXT") private String validationJson;
    @Column(name="warnings_json", nullable=false, columnDefinition="LONGTEXT") private String warningsJson;
    @Column(name="record_status", nullable=false, length=32) private String status;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="imported_project_id") private ProjectEntity importedProject;
    protected ImportRecordEntity() {}
    public ImportRecordEntity(String id, ImportJobEntity job, String sheetName, int sourceRow, String recordJson, String validationJson, String warningsJson, String status) {
        this.id=id; this.job=job; this.sheetName=sheetName; this.sourceRow=sourceRow; this.recordJson=recordJson;
        this.validationJson=validationJson; this.warningsJson=warningsJson; this.status=status;
    }
    public String getId(){return id;} public ImportJobEntity getJob(){return job;} public String getSheetName(){return sheetName;}
    public int getSourceRow(){return sourceRow;} public String getRecordJson(){return recordJson;} public void setRecordJson(String v){recordJson=v;}
    public String getValidationJson(){return validationJson;} public void setValidationJson(String v){validationJson=v;}
    public String getWarningsJson(){return warningsJson;} public void setWarningsJson(String v){warningsJson=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public ProjectEntity getImportedProject(){return importedProject;} public void setImportedProject(ProjectEntity v){importedProject=v;}
}
