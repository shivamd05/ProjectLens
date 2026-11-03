package com.projectlense.api.dto;

import com.projectlense.api.entity.SubmissionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class RubricDtos {
    private RubricDtos() {}

    public record RubricRequest(
            @NotBlank @Size(max = 150) String name,
            @NotNull SubmissionType submissionType,
            @NotBlank @Size(max = 10000) String description,
            @NotNull @DecimalMin("0.01") @Digits(integer = 3, fraction = 2) BigDecimal totalMarks,
            Boolean active,
            @NotEmpty List<@Valid CriterionRequest> criteria) {}

    public record CriterionRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 10000) String description,
            @NotNull @DecimalMin("0.00") @Digits(integer = 3, fraction = 2) BigDecimal maxMarks,
            List<@Valid SubcriterionRequest> subcriteria) {
        public CriterionRequest {
            if (subcriteria == null) subcriteria = List.of();
        }
    }

    public record SubcriterionRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 10000) String description,
            @NotNull @DecimalMin("0.00") @Digits(integer = 3, fraction = 2) BigDecimal maxMarks) {}
}
