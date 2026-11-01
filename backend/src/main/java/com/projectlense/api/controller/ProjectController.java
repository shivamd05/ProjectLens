package com.projectlense.api.controller;

import com.projectlense.api.dto.FacultyReviewRequest;
import com.projectlense.api.service.ProjectService;
import jakarta.validation.Valid;
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
    private final String provider;
    public ProjectController(ProjectService projects, @Value("${LLM_PROVIDER:Auto (Gemini / OpenAI / Content-Aware Fallback)}") String provider) {
        this.projects = projects; this.provider = provider;
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
        return projects.upload(title, description, department, academicYear, facultyGuide, user.getName(), user.getId(), report);
    }

    @GetMapping("/api/projects")
    public List<Map<String, Object>> list(Authentication authentication) {
        UserEntity user=currentUser(authentication);
        return projects.list("student".equals(user.getRole()) ? user.getId() : null);
    }

    @GetMapping("/api/projects/{project_id}")
    public Map<String, Object> get(@PathVariable("project_id") String id, Authentication authentication) {
        UserEntity user=currentUser(authentication); Map<String,Object> project=projects.get(id);
        if("student".equals(user.getRole()) && !user.getId().equals(project.get("student_id"))) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Project not found");
        return project;
    }

    @PostMapping("/api/projects/{project_id}/faculty-review")
    public Map<String, Object> review(@PathVariable("project_id") String id, @Valid @RequestBody FacultyReviewRequest request) {
        return projects.review(id, request, false);
    }

    @PostMapping("/api/projects/{project_id}/approve")
    public Map<String, Object> approve(@PathVariable("project_id") String id, @Valid @RequestBody FacultyReviewRequest request) {
        return projects.review(id, request, true);
    }

    private static UserEntity currentUser(Authentication authentication) {
        if(authentication!=null && authentication.getPrincipal() instanceof UserEntity user) return user;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");
    }
}
