package com.projectlense.api.service;

import com.projectlense.api.dto.ProjectWorkflowDtos;
import com.projectlense.api.entity.ProjectEntity;
import com.projectlense.api.entity.UserEntity;
import com.projectlense.api.repository.ProjectDataRepository;
import com.projectlense.api.repository.ProjectJsonRepository;
import com.projectlense.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProjectLifecycleService {
    private final ProjectJsonRepository projects;
    private final ProjectDataRepository projectData;
    private final UserRepository users;

    public ProjectLifecycleService(ProjectJsonRepository projects, ProjectDataRepository projectData, UserRepository users) {
        this.projects = projects;
        this.projectData = projectData;
        this.users = users;
    }

    @Transactional
    public Map<String, Object> create(ProjectWorkflowDtos.ProjectCreateRequest request, UserEntity owner) {
        if (request == null || request.title() == null || request.title().isBlank() || request.title().trim().length() > 500
                || request.academicYear() == null || request.academicYear().isBlank() || request.academicYear().trim().length() > 50
                || request.facultyGuide() == null || request.facultyGuide().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project title, academic year, and assigned faculty guide are required");
        }
        if (request.description() != null && request.description().length() > 20000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project description is too long");
        }
        String title = request.title().trim();
        String year = request.academicYear().trim();
        String normalized = ProjectJsonRepository.normalize(title);
        if (projectData.existsByNormalizedTitleAndAcademicYear(normalized, year)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A project with this title already exists for the academic year");
        }
        UserEntity faculty = findFaculty(request.facultyGuide());
        String id = "proj-" + UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", id); record.put("title", title); record.put("description", blankToNull(request.description()));
        record.put("department", blankToNull(request.department())); record.put("academic_year", year);
        record.put("faculty_guide", faculty.getName()); record.put("faculty_id", faculty.getId());
        record.put("student_id", owner.getId()); record.put("student_name", owner.getName());
        record.put("status", "pending_reviews"); record.put("submitted_at", Instant.now().toString());
        return projects.insert(record);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> facultyGuides() {
        return users.findAllByRoleOrderByNameAsc("faculty").stream().map(user -> Map.<String, Object>of(
                "id", user.getId(), "name", user.getName(), "email", user.getEmail(),
                "department", user.getDepartment() == null ? "" : user.getDepartment())).toList();
    }

    private UserEntity findFaculty(String identity) {
        String value = identity.trim();
        UserEntity user = users.findByEmail(value.toLowerCase()).orElseGet(() -> users.findById(value)
                .or(() -> users.findFirstByNameIgnoreCaseAndRole(value, "faculty"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Select a faculty guide account that has already been provisioned")));
        if (!"faculty".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The selected guide must have a faculty account");
        }
        return user;
    }

    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
