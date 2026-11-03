package com.projectlense.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectlense.api.entity.ProjectEntity;
import com.projectlense.api.entity.UserEntity;
import com.projectlense.api.repository.ProjectDataRepository;
import com.projectlense.api.repository.ProjectMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ProjectAccessService {
    private final ProjectDataRepository projects;
    private final ProjectMemberRepository members;
    private final ObjectMapper mapper;

    public ProjectAccessService(ProjectDataRepository projects, ProjectMemberRepository members, ObjectMapper mapper) {
        this.projects = projects;
        this.members = members;
        this.mapper = mapper;
    }

    public ProjectEntity requireProject(String projectId) {
        return projects.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    public ProjectEntity requireView(String projectId, UserEntity user) {
        ProjectEntity project = requireProject(projectId);
        if (!canView(project, user)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        return project;
    }

    public ProjectEntity requireFacultyGuideOrAdmin(String projectId, UserEntity user) {
        ProjectEntity project = requireProject(projectId);
        if (!isAdmin(user) && (!isFaculty(user) || !isAssignedFaculty(project, user))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assigned faculty guide or an administrator can perform this action");
        }
        return project;
    }

    public ProjectEntity requireStudentOwnerOrAdmin(String projectId, UserEntity user) {
        ProjectEntity project = requireProject(projectId);
        if (!isAdmin(user) && (!isStudent(user) || !isStudentOwner(project, user))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a project student or administrator can submit files");
        }
        return project;
    }

    public boolean canView(ProjectEntity project, UserEntity user) {
        if (isAdmin(user)) return true;
        if (isFaculty(user)) return isAssignedFaculty(project, user);
        return isStudent(user) && isStudentOwner(project, user);
    }

    public List<Map<String, Object>> visibleProjects(List<Map<String, Object>> source, UserEntity user) {
        if (isAdmin(user)) return source;
        return source.stream().filter(item -> {
            Object rawId = item.get("id");
            if (rawId == null) return false;
            return projects.findById(String.valueOf(rawId)).map(project -> canView(project, user)).orElse(false);
        }).toList();
    }

    public boolean isStudentOwner(ProjectEntity project, UserEntity user) {
        JsonNode payload = payload(project);
        if (matches(payload.path("student_id").asText(null), user.getId())
                || matches(payload.path("studentId").asText(null), user.getId())) return true;
        return members.existsByProject_IdAndStudentId(project.getId(), user.getId());
    }

    public boolean isAssignedFaculty(ProjectEntity project, UserEntity user) {
        JsonNode payload = payload(project);
        if (matches(payload.path("faculty_id").asText(null), user.getId())) return true;
        String assigned = project.getFacultyCoordinator();
        return matches(assigned, user.getId()) || matches(assigned, user.getName()) || matches(assigned, user.getEmail());
    }

    public static boolean isAdmin(UserEntity user) { return user != null && "admin".equalsIgnoreCase(user.getRole()); }
    public static boolean isFaculty(UserEntity user) { return user != null && "faculty".equalsIgnoreCase(user.getRole()); }
    public static boolean isStudent(UserEntity user) { return user != null && "student".equalsIgnoreCase(user.getRole()); }

    private JsonNode payload(ProjectEntity project) {
        if (project.getApiPayloadJson() == null || project.getApiPayloadJson().isBlank()) return mapper.createObjectNode();
        try { return mapper.readTree(project.getApiPayloadJson()); }
        catch (Exception ignored) { return mapper.createObjectNode(); }
    }

    private static boolean matches(String left, String right) {
        return left != null && right != null && left.trim().toLowerCase(Locale.ROOT).equals(right.trim().toLowerCase(Locale.ROOT));
    }
}
