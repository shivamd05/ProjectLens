package com.projectlense.api.controller;

import com.projectlense.api.service.ProjectService;
import com.projectlense.api.service.ProjectLifecycleService;
import com.projectlense.api.service.ProjectSubmissionService;
import com.projectlense.api.service.ProjectAccessService;
import com.projectlense.api.dto.ProjectWorkflowDtos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.projectlense.api.entity.UserEntity;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ProjectController {
    private final ProjectService projects;
    private final ProjectLifecycleService lifecycle;
    private final ProjectSubmissionService submissions;
    private final ProjectAccessService access;
    private final String provider;
    public ProjectController(ProjectService projects, ProjectLifecycleService lifecycle,
                             ProjectSubmissionService submissions, ProjectAccessService access,
                             @Value("${LLM_PROVIDER:Auto (Gemini / OpenAI / Content-Aware Fallback)}") String provider) {
        this.projects = projects; this.lifecycle = lifecycle; this.submissions = submissions; this.access = access; this.provider = provider;
    }

    @GetMapping("/")
    public Map<String, Object> root() {
        Map<String, Object> out = new LinkedHashMap<>(); out.put("service", "ProjectLense AI Evaluation API"); out.put("status", "online");
        out.put("supported_uploads", List.of("PDF (.pdf)", "ZIP Project Archive (.zip)")); out.put("llm_provider", provider); return out;
    }

    @PostMapping(value = "/api/projects/upload", consumes = "multipart/form-data")
    public Map<String, Object> upload(@RequestParam String title,
            @RequestParam(defaultValue = "") String description,
            @RequestParam(defaultValue = "Computer Science") String department,
            @RequestParam(name = "academic_year") String academicYear,
            @RequestParam(name = "faculty_guide", defaultValue = "") String facultyGuide,
            @RequestPart MultipartFile report, Authentication authentication) {
        UserEntity user = currentUser(authentication);
        if (report == null || report.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a non-empty PDF or ZIP file");
        var request = new ProjectWorkflowDtos.ProjectCreateRequest(title, description, department, academicYear, facultyGuide);
        Map<String, Object> project = lifecycle.create(request, user);
        Map<String, Object> submission = submissions.submit(String.valueOf(project.get("id")), report, user);
        project.put("submission", submission);
        project.put("file_type", String.valueOf(submission.get("submissionType")).toLowerCase());
        return Map.of("success", true, "project", project);
    }

    @GetMapping("/api/projects")
    public List<Map<String, Object>> list(Authentication authentication) {
        UserEntity user=currentUser(authentication);
        List<Map<String, Object>> records = projects.list(null);
        return access.visibleProjects(records, user);
    }

    @GetMapping("/api/projects/{project_id}")
    public Map<String, Object> get(@PathVariable("project_id") String id, Authentication authentication) {
        UserEntity user=currentUser(authentication); access.requireView(id, user);
        return projects.get(id);
    }

    @Deprecated
    @PostMapping("/api/projects/{project_id}/faculty-review")
    public Map<String, Object> review(@PathVariable("project_id") String id, Authentication authentication) {
        access.requireFacultyGuideOrAdmin(id, currentUser(authentication));
        throw new ResponseStatusException(HttpStatus.GONE, "Use the three staged project review endpoints instead");
    }

    @Deprecated
    @PostMapping("/api/projects/{project_id}/approve")
    public Map<String, Object> approve(@PathVariable("project_id") String id, Authentication authentication) {
        access.requireFacultyGuideOrAdmin(id, currentUser(authentication));
        throw new ResponseStatusException(HttpStatus.GONE, "Approve or reject the AI recommendation through its evaluation endpoint");
    }

    private static UserEntity currentUser(Authentication authentication) {
        if(authentication!=null && authentication.getPrincipal() instanceof UserEntity user) return user;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");
    }
}
