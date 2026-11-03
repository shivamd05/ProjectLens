package com.projectlense.api.controller;

import com.projectlense.api.dto.ProjectWorkflowDtos;
import com.projectlense.api.entity.UserEntity;
import com.projectlense.api.service.ProjectLifecycleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
public class ProjectWorkflowController {
    private final ProjectLifecycleService lifecycle;
    public ProjectWorkflowController(ProjectLifecycleService lifecycle) { this.lifecycle = lifecycle; }

    @PostMapping("/api/projects") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody ProjectWorkflowDtos.ProjectCreateRequest request,
                                      Authentication authentication) {
        return Map.of("project", lifecycle.create(request, currentUser(authentication)));
    }

    @GetMapping("/api/faculty-guides")
    public List<Map<String, Object>> facultyGuides() { return lifecycle.facultyGuides(); }

    private static UserEntity currentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserEntity user) return user;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}
