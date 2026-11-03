package com.projectlense.api.controller;

import com.projectlense.api.dto.ProjectWorkflowDtos;
import com.projectlense.api.service.ReviewStageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ReviewStagesController {
    private final ReviewStageService stages;
    public ReviewStagesController(ReviewStageService stages) { this.stages = stages; }

    @GetMapping("/api/review-stages") public List<Map<String, Object>> list() { return stages.list(); }

    @PutMapping("/api/admin/review-stages/{id}")
    public Map<String, Object> update(@PathVariable Integer id,
                                      @Valid @RequestBody ProjectWorkflowDtos.ReviewStageUpdateRequest request) {
        return stages.update(id, request);
    }
}
