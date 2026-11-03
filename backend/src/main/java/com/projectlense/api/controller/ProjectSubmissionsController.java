package com.projectlense.api.controller;

import com.projectlense.api.entity.UserEntity;
import com.projectlense.api.service.ProjectSubmissionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/submissions")
public class ProjectSubmissionsController {
    private final ProjectSubmissionService submissions;
    public ProjectSubmissionsController(ProjectSubmissionService submissions) { this.submissions = submissions; }

    @PostMapping(consumes = "multipart/form-data") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> submit(@PathVariable String projectId, @RequestPart("file") MultipartFile file,
                                      Authentication authentication) {
        return submissions.submit(projectId, file, currentUser(authentication));
    }

    @GetMapping public List<Map<String, Object>> list(@PathVariable String projectId, Authentication authentication) {
        return submissions.list(projectId, currentUser(authentication));
    }

    private static UserEntity currentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserEntity user) return user;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}
