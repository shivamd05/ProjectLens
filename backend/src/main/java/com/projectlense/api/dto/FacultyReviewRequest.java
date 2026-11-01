package com.projectlense.api.dto;

import jakarta.validation.constraints.NotNull;

public record FacultyReviewRequest(@NotNull Integer final_score,
                                   @NotNull String comment,
                                   String faculty_name) {
    public String resolvedFacultyName() {
        return faculty_name == null ? "Dr. Priya Nair" : faculty_name;
    }
}
