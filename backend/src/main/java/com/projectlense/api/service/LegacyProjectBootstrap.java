package com.projectlense.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectlense.api.entity.ProjectEntity;
import com.projectlense.api.repository.ProjectDataRepository;
import com.projectlense.api.repository.ProjectJsonRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** One-time, idempotent migration of legacy projects_db.json rows into MySQL. */
@Component
public class LegacyProjectBootstrap implements ApplicationRunner {
    private final ProjectDataRepository projects;
    private final ObjectMapper mapper;
    private final Path legacyFile;

    public LegacyProjectBootstrap(ProjectDataRepository projects, ObjectMapper mapper,
                                  @Value("${projectlense.legacy-db-file:}") String legacyFile) {
        this.projects = projects; this.mapper = mapper;
        this.legacyFile = legacyFile == null || legacyFile.isBlank() ? null : Path.of(legacyFile).toAbsolutePath().normalize();
    }

    @Override @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (legacyFile == null || !Files.isRegularFile(legacyFile)) return;
        List<Map<String, Object>> records = mapper.readValue(legacyFile.toFile(), new TypeReference<>() {});
        for (Map<String, Object> record : records) {
            String id = text(record.get("id")), title = text(record.get("title"));
            String year = text(record.get("academic_year"));
            if (id == null || title == null || projects.existsById(id)) continue;
            String normalized = ProjectJsonRepository.normalize(title);
            if (projects.existsByNormalizedTitleAndAcademicYear(normalized, year == null ? "2024-25" : year)) continue;
            ProjectEntity entity = new ProjectEntity();
            entity.setId(id); entity.setTitle(title); entity.setNormalizedTitle(normalized);
            entity.setDescription(text(record.get("description"))); entity.setDepartment(text(record.get("department")));
            entity.setAcademicYear(year == null ? "2024-25" : year); entity.setFacultyCoordinator(text(record.get("faculty_guide")));
            entity.setStatus(text(record.get("status")) == null ? "evaluated" : text(record.get("status")));
            entity.setApiPayloadJson(mapper.writeValueAsString(record));
            entity.setCreatedAt(parseInstant(record.get("submitted_at")));
            projects.save(entity);
        }
    }

    private static String text(Object value) { return value == null ? null : String.valueOf(value); }
    private static Instant parseInstant(Object value) {
        try { return value == null ? Instant.now() : Instant.parse(String.valueOf(value)); }
        catch (Exception e) { return Instant.now(); }
    }
}
