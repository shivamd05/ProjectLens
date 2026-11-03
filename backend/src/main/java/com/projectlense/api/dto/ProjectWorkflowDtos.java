package com.projectlense.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ProjectWorkflowDtos {
    private ProjectWorkflowDtos() {}

    public record ProjectCreateRequest(
            @NotBlank @Size(max = 500) String title,
            @Size(max = 20000) String description,
            @Size(max = 255) String department,
            @NotBlank @Size(max = 50) String academicYear,
            @NotBlank @Size(max = 320) String facultyGuide) {}

    public record ReviewMarkRequest(
            @NotNull Integer reviewId,
            @NotNull @DecimalMin("0.00") @Digits(integer = 3, fraction = 2) BigDecimal marksObtained,
            @Size(max = 10000) String feedback,
            LocalDate reviewDate) {}

    public record ReviewStageUpdateRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 3, fraction = 2) BigDecimal maxMarks) {}

    public record DecisionRequest(@Size(max = 10000) String feedback) {}
}
