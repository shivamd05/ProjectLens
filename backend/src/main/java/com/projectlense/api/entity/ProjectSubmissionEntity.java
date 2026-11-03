package com.projectlense.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "project_submissions", indexes =
        @Index(name = "ix_project_submissions_project_date", columnList = "project_id,submission_date"))
public class ProjectSubmissionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private Integer id;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @NotNull @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", nullable = false, columnDefinition = "ENUM('PDF','ZIP')")
    private SubmissionType submissionType;

    @NotBlank @Size(max = 255)
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @NotBlank @Size(max = 500)
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @NotNull @Enumerated(EnumType.STRING)
    @Column(name = "submission_status", nullable = false, columnDefinition = "ENUM('SUBMITTED','PROCESSING','COMPLETED','FAILED')")
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    @CreationTimestamp
    @Column(name = "submission_date", nullable = false, updatable = false)
    private Instant submissionDate;

    public ProjectSubmissionEntity() {}
    public Integer getId() { return id; }
    public ProjectEntity getProject() { return project; }
    public void setProject(ProjectEntity project) { this.project = project; }
    public SubmissionType getSubmissionType() { return submissionType; }
    public void setSubmissionType(SubmissionType submissionType) { this.submissionType = submissionType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }
    public Instant getSubmissionDate() { return submissionDate; }
}
