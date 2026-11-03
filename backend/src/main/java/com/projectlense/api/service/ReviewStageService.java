package com.projectlense.api.service;

import com.projectlense.api.dto.ProjectWorkflowDtos;
import com.projectlense.api.entity.ProjectReviewStageEntity;
import com.projectlense.api.repository.ProjectReviewRepository;
import com.projectlense.api.repository.ProjectReviewStageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewStageService {
    private final ProjectReviewStageRepository stages;
    private final ProjectReviewRepository reviews;

    public ReviewStageService(ProjectReviewStageRepository stages, ProjectReviewRepository reviews) {
        this.stages = stages;
        this.reviews = reviews;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() { return stages.findAllByOrderByIdAsc().stream().map(ReviewStageService::toMap).toList(); }

    @Transactional
    public Map<String, Object> update(Integer id, ProjectWorkflowDtos.ReviewStageUpdateRequest request) {
        ProjectReviewStageEntity stage = stages.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review stage not found"));
        boolean wouldInvalidateExisting = reviews.findAll().stream()
                .filter(r -> r.getStage().getId().equals(id))
                .anyMatch(r -> r.getMarksObtained().compareTo(request.maxMarks()) > 0);
        if (wouldInvalidateExisting) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Maximum marks cannot be reduced below a mark already recorded for this stage");
        stage.setMaxMarks(request.maxMarks());
        return toMap(stages.save(stage));
    }

    private static Map<String, Object> toMap(ProjectReviewStageEntity stage) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", stage.getId()); out.put("name", stage.getName()); out.put("maxMarks", stage.getMaxMarks());
        return out;
    }
}
