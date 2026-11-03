package com.projectlense.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_evaluations", indexes = {
        @Index(name = "ix_ai_evaluations_project_date", columnList = "project_id,evaluated_at"),
        @Index(name = "ix_ai_evaluations_submission_rubric", columnList = "submission_id,rubric_id")
})
public class AiEvaluationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_id")
    private Integer id;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private ProjectSubmissionEntity submission;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rubric_id", nullable = false)
    private EvaluationRubricEntity rubric;

    @NotNull @DecimalMin("0.00") @Digits(integer = 3, fraction = 2)
    @Column(name = "total_marks", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalMarks = BigDecimal.ZERO;

    @Column(name = "overall_feedback", columnDefinition = "TEXT")
    private String overallFeedback;

    @NotNull @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_status", nullable = false, columnDefinition = "ENUM('PENDING','COMPLETED','APPROVED','REJECTED')")
    private EvaluationStatus status = EvaluationStatus.PENDING;

    @CreationTimestamp
    @Column(name = "evaluated_at", nullable = false, updatable = false)
    private Instant evaluatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private UserEntity approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "decision_feedback", columnDefinition = "TEXT")
    private String decisionFeedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decision_by")
    private UserEntity decisionBy;

    @Column(name = "decision_at")
    private Instant decisionAt;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiEvaluationDetailEntity> details = new ArrayList<>();

    public AiEvaluationEntity() {}
    public void addDetail(AiEvaluationDetailEntity detail) { details.add(detail); detail.setEvaluation(this); }
    public Integer getId() { return id; }
    public ProjectEntity getProject() { return project; }
    public void setProject(ProjectEntity project) { this.project = project; }
    public ProjectSubmissionEntity getSubmission() { return submission; }
    public void setSubmission(ProjectSubmissionEntity submission) { this.submission = submission; }
    public EvaluationRubricEntity getRubric() { return rubric; }
    public void setRubric(EvaluationRubricEntity rubric) { this.rubric = rubric; }
    public BigDecimal getTotalMarks() { return totalMarks; }
    public void setTotalMarks(BigDecimal totalMarks) { this.totalMarks = totalMarks; }
    public String getOverallFeedback() { return overallFeedback; }
    public void setOverallFeedback(String overallFeedback) { this.overallFeedback = overallFeedback; }
    public EvaluationStatus getStatus() { return status; }
    public void setStatus(EvaluationStatus status) { this.status = status; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
    public UserEntity getApprovedBy() { return approvedBy; }
    public void setApprovedBy(UserEntity approvedBy) { this.approvedBy = approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public String getDecisionFeedback() { return decisionFeedback; }
    public void setDecisionFeedback(String decisionFeedback) { this.decisionFeedback = decisionFeedback; }
    public UserEntity getDecisionBy() { return decisionBy; }
    public void setDecisionBy(UserEntity decisionBy) { this.decisionBy = decisionBy; }
    public Instant getDecisionAt() { return decisionAt; }
    public void setDecisionAt(Instant decisionAt) { this.decisionAt = decisionAt; }
    public List<AiEvaluationDetailEntity> getDetails() { return details; }
}
