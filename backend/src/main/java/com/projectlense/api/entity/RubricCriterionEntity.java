package com.projectlense.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rubric_criteria", uniqueConstraints =
        @UniqueConstraint(name = "uq_rubric_criterion_name", columnNames = {"rubric_id", "criterion_name"}))
public class RubricCriterionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "criterion_id")
    private Integer id;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rubric_id", nullable = false)
    private EvaluationRubricEntity rubric;

    @NotBlank @Size(max = 150)
    @Column(name = "criterion_name", nullable = false, length = 150)
    private String name;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull @DecimalMin("0.00") @Digits(integer = 3, fraction = 2)
    @Column(name = "max_marks", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxMarks;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @OneToMany(mappedBy = "criterion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<RubricSubcriterionEntity> subcriteria = new ArrayList<>();

    public RubricCriterionEntity() {}

    public void setRubric(EvaluationRubricEntity rubric) { this.rubric = rubric; }
    public void addSubcriterion(RubricSubcriterionEntity item) { subcriteria.add(item); item.setCriterion(this); }
    public void clearSubcriteria() { subcriteria.clear(); }
    public Integer getId() { return id; }
    public EvaluationRubricEntity getRubric() { return rubric; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getMaxMarks() { return maxMarks; }
    public void setMaxMarks(BigDecimal maxMarks) { this.maxMarks = maxMarks; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public List<RubricSubcriterionEntity> getSubcriteria() { return subcriteria; }
}
