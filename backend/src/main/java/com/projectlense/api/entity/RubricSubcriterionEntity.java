package com.projectlense.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
@Table(name = "rubric_subcriteria", uniqueConstraints =
        @UniqueConstraint(name = "uq_rubric_subcriterion_name", columnNames = {"criterion_id", "subcriterion_name"}))
public class RubricSubcriterionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subcriterion_id")
    private Integer id;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    private RubricCriterionEntity criterion;

    @NotBlank @Size(max = 150)
    @Column(name = "subcriterion_name", nullable = false, length = 150)
    private String name;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull @DecimalMin("0.00") @Digits(integer = 3, fraction = 2)
    @Column(name = "max_marks", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxMarks;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public RubricSubcriterionEntity() {}
    public void setCriterion(RubricCriterionEntity criterion) { this.criterion = criterion; }
    public Integer getId() { return id; }
    public RubricCriterionEntity getCriterion() { return criterion; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getMaxMarks() { return maxMarks; }
    public void setMaxMarks(BigDecimal maxMarks) { this.maxMarks = maxMarks; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
