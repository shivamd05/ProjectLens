package com.projectlense.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "excel_import_jobs")
public class ImportJobEntity {
    @Id @Column(name = "job_id", length = 36) private String id;
    @Column(name = "file_name", nullable = false) private String fileName;
    @Column(name = "file_hash", nullable = false, length = 64) private String fileHash;
    @Column(nullable = false, length = 32) private String status;
    @Column(name = "total_records", nullable = false) private int totalRecords;
    @Column(name = "valid_records", nullable = false) private int validRecords;
    @Column(name = "invalid_records", nullable = false) private int invalidRecords;
    @Column(name = "duplicate_records", nullable = false) private int duplicateRecords;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "approved_at") private Instant approvedAt;
    @Column(name = "cancelled_at") private Instant cancelledAt;
    @Column(name = "error_message") private String errorMessage;
    @Column(name = "workbook_warnings_json", columnDefinition = "LONGTEXT") private String workbookWarningsJson;
    protected ImportJobEntity() {}
    public ImportJobEntity(String id, String fileName, String fileHash) { this.id=id; this.fileName=fileName; this.fileHash=fileHash; this.status="PREVIEW"; this.createdAt=Instant.now(); }
    public String getId() { return id; } public String getFileName() { return fileName; } public String getFileHash() { return fileHash; }
    public String getStatus() { return status; } public void setStatus(String v) { status=v; }
    public int getTotalRecords() { return totalRecords; } public void setTotalRecords(int v) { totalRecords=v; }
    public int getValidRecords() { return validRecords; } public void setValidRecords(int v) { validRecords=v; }
    public int getInvalidRecords() { return invalidRecords; } public void setInvalidRecords(int v) { invalidRecords=v; }
    public int getDuplicateRecords() { return duplicateRecords; } public void setDuplicateRecords(int v) { duplicateRecords=v; }
    public Instant getCreatedAt() { return createdAt; } public Instant getApprovedAt() { return approvedAt; } public void setApprovedAt(Instant v) { approvedAt=v; }
    public Instant getCancelledAt() { return cancelledAt; } public void setCancelledAt(Instant v) { cancelledAt=v; }
    public String getErrorMessage() { return errorMessage; } public void setErrorMessage(String v) { errorMessage=v; }
    public String getWorkbookWarningsJson() { return workbookWarningsJson; } public void setWorkbookWarningsJson(String v) { workbookWarningsJson=v; }
}
