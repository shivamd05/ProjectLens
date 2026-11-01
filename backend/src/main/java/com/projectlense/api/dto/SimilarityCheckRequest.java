package com.projectlense.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SimilarityCheckRequest(@NotBlank @Size(max = 500) String title,
        @Size(max = 20000) String description,
        @Size(max = 10000) String objectives,
        @Size(max = 2000) String technologies,
        Double threshold,
        Integer limit) {}
