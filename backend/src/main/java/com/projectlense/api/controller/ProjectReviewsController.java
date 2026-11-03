package com.projectlense.api.controller;

import com.projectlense.api.dto.ProjectWorkflowDtos;
import com.projectlense.api.entity.UserEntity;
import com.projectlense.api.service.ProjectReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/reviews")
public class ProjectReviewsController {
    private final ProjectReviewService reviews;
    public ProjectReviewsController(ProjectReviewService reviews) { this.reviews = reviews; }

    @GetMapping public List<Map<String, Object>> list(@PathVariable String projectId, Authentication authentication) {
        return reviews.list(projectId, currentUser(authentication));
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@PathVariable String projectId,
                                      @Valid @RequestBody ProjectWorkflowDtos.ReviewMarkRequest request,
                                      Authentication authentication) {
        return reviews.create(projectId, request, currentUser(authentication));
    }

    @PutMapping("/{reviewId}")
    public Map<String, Object> update(@PathVariable String projectId, @PathVariable Integer reviewId,
                                      @Valid @RequestBody ProjectWorkflowDtos.ReviewMarkRequest request,
                                      Authentication authentication) {
        return reviews.update(projectId, reviewId, request, currentUser(authentication));
    }

    private static UserEntity currentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserEntity user) return user;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}
