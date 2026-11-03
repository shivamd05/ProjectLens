package com.projectlense.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(name = "ai_evaluation_details", uniqueConstraints =
        @UniqueConstraint(name = "uq_ai_evaluation_criterion", columnNames = {"evaluation_id", "criterion_id"}))
public class AiEvaluationDetailEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Integer id;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private AiEvaluationEntity evaluation;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    private RubricCriterionEntity criterion;

    @NotNull @DecimalMin("0.00") @Digits(integer = 3, fraction = 2)
    @Column(name = "awarded_marks", nullable = false, precision = 5, scale = 2)
    private BigDecimal awardedMarks;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String justification;

    @Column(name = "improvement_suggestions", columnDefinition = "TEXT")
    private String improvementSuggestions;

    public AiEvaluationDetailEntity() {}
    public void setEvaluation(AiEvaluationEntity evaluation) { this.evaluation = evaluation; }
    public void setCriterion(RubricCriterionEntity criterion) { this.criterion = criterion; }
    public Integer getId() { return id; }
    public AiEvaluationEntity getEvaluation() { return evaluation; }
    public RubricCriterionEntity getCriterion() { return criterion; }
    public BigDecimal getAwardedMarks() { return awardedMarks; }
    public void setAwardedMarks(BigDecimal awardedMarks) { this.awardedMarks = awardedMarks; }
    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
    public String getImprovementSuggestions() { return improvementSuggestions; }
    public void setImprovementSuggestions(String improvementSuggestions) { this.improvementSuggestions = improvementSuggestions; }
}
