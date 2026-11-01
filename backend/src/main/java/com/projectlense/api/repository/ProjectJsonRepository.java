package com.projectlense.api.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectlense.api.entity.ProjectEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Compatibility adapter for the pre-existing /api/projects JSON contract, backed by MySQL. */
@Repository
public class ProjectJsonRepository {
    private final ObjectMapper mapper;
    private final ProjectDataRepository projects;

    public ProjectJsonRepository(ObjectMapper mapper, ProjectDataRepository projects) {
        this.mapper = mapper;
        this.projects = projects;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> findAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ProjectEntity entity : projects.findAllByOrderByCreatedAtDesc()) result.add(asApiRecord(entity));
        return result;
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> findById(String id) {
        return projects.findById(id).map(this::asApiRecord);
    }

    @Transactional
    public Map<String, Object> insert(Map<String, Object> project) {
        ProjectEntity entity = new ProjectEntity();
        String id = string(project.get("id"));
        entity.setId(id);
        entity.setTitle(or(string(project.get("title")), "Untitled project"));
        entity.setNormalizedTitle(normalize(entity.getTitle()));
        entity.setDescription(string(project.get("description")));
        entity.setDepartment(string(project.get("department")));
        entity.setAcademicYear(or(string(project.get("academic_year")), "2024-25"));
        entity.setFacultyCoordinator(string(project.get("faculty_guide")));
        entity.setStatus(or(string(project.get("status")), "evaluated"));
        entity.setApiPayloadJson(write(project));
        entity.setCreatedAt(parseInstant(project.get("submitted_at")));
        return asApiRecord(projects.save(entity));
    }

    @Transactional
    public Optional<Map<String, Object>> update(String id, Map<String, Object> updates) {
        return projects.findById(id).map(entity -> {
            Map<String, Object> project = asApiRecord(entity);
            project.putAll(updates);
            entity.setStatus(or(string(project.get("status")), entity.getStatus()));
            entity.setApiPayloadJson(write(project));
            projects.save(entity);
            return project;
        });
    }

    private Map<String, Object> asApiRecord(ProjectEntity entity) {
        if (entity.getApiPayloadJson() != null && !entity.getApiPayloadJson().isBlank()) {
            try { return mapper.readValue(entity.getApiPayloadJson(), new TypeReference<>() {}); }
            catch (Exception ignored) { /* Fall back to the project columns below. */ }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", entity.getId()); out.put("title", entity.getTitle()); out.put("description", entity.getDescription());
        out.put("department", entity.getDepartment()); out.put("academic_year", entity.getAcademicYear());
        out.put("faculty_guide", entity.getFacultyCoordinator()); out.put("team_name", entity.getTeamName());
        out.put("team_leader", entity.getTeamLeader()); out.put("group_number", entity.getGroupNumber());
        out.put("status", entity.getStatus()); out.put("submitted_at", entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString());
        return out;
    }

    private String write(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalStateException("Could not serialize project data", e); }
    }
    public static String normalize(String value) { return value == null ? "" : value.strip().toLowerCase().replaceAll("\\s+", " "); }
    private static String string(Object value) { return value == null ? null : String.valueOf(value); }
    private static String or(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static Instant parseInstant(Object value) {
        try { return value == null ? Instant.now() : Instant.parse(String.valueOf(value)); }
        catch (Exception e) { return Instant.now(); }
    }
}
