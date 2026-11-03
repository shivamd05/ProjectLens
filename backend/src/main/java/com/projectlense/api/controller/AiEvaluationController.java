package com.projectlense.api.controller;

import com.projectlense.api.dto.ProjectWorkflowDtos;
import com.projectlense.api.entity.UserEntity;
import com.projectlense.api.service.AiEvaluationWorkflowService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
public class AiEvaluationController {
    private final AiEvaluationWorkflowService evaluations;
    public AiEvaluationController(AiEvaluationWorkflowService evaluations) { this.evaluations = evaluations; }

    @PostMapping("/api/projects/{projectId}/evaluate")
    public Map<String, Object> evaluate(@PathVariable String projectId, Authentication authentication) {
        return evaluations.evaluate(projectId, currentUser(authentication));
    }

    @GetMapping("/api/projects/{projectId}/evaluation")
    public Map<String, Object> forProject(@PathVariable String projectId, Authentication authentication) {
        return evaluations.forProject(projectId, currentUser(authentication));
    }

    @GetMapping("/api/evaluations/{evaluationId}")
    public Map<String, Object> get(@PathVariable Integer evaluationId, Authentication authentication) {
        return evaluations.byId(evaluationId, currentUser(authentication));
    }

    @PostMapping("/api/evaluations/{evaluationId}/approve")
    public Map<String, Object> approve(@PathVariable Integer evaluationId,
                                       @Valid @RequestBody(required = false) ProjectWorkflowDtos.DecisionRequest request,
                                       Authentication authentication) {
        return evaluations.approve(evaluationId, request == null ? null : request.feedback(), currentUser(authentication));
    }

    @PostMapping("/api/evaluations/{evaluationId}/reject")
    public Map<String, Object> reject(@PathVariable Integer evaluationId,
                                      @Valid @RequestBody(required = false) ProjectWorkflowDtos.DecisionRequest request,
                                      Authentication authentication) {
        return evaluations.reject(evaluationId, request == null ? null : request.feedback(), currentUser(authentication));
    }

    private static UserEntity currentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserEntity user) return user;
        throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}
