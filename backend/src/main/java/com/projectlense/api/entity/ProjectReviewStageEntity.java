package com.projectlense.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
@Table(name = "project_review_stages", uniqueConstraints =
        @UniqueConstraint(name = "uq_project_review_stage_name", columnNames = "review_name"))
public class ProjectReviewStageEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Integer id;

    @NotBlank @Size(max = 50)
    @Column(name = "review_name", nullable = false, length = 50)
    private String name;

    @NotNull @DecimalMin("0.01") @Digits(integer = 3, fraction = 2)
    @Column(name = "max_marks", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxMarks;

    protected ProjectReviewStageEntity() {}
    public Integer getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getMaxMarks() { return maxMarks; }
    public void setMaxMarks(BigDecimal maxMarks) { this.maxMarks = maxMarks; }
}
