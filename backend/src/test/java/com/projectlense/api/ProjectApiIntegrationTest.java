package com.projectlense.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectlense.api.entity.SubmissionType;
import com.projectlense.api.entity.UserEntity;
import com.projectlense.api.entity.AuthSessionEntity;
import com.projectlense.api.repository.AuthSessionRepository;
import com.projectlense.api.repository.EvaluationRubricRepository;
import com.projectlense.api.repository.RubricCriterionRepository;
import com.projectlense.api.repository.UserRepository;
import com.projectlense.api.service.EvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectApiIntegrationTest {
    private static final String TEST_DB;
    static {
        try { TEST_DB = Files.createTempDirectory("projectlense-test-").resolve("projects.json").toString(); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:projectlense;MODE=MySQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("projectlense.legacy-db-file", () -> "");
        registry.add("projectlense.admin-token", () -> "test-admin-token");
        registry.add("projectlense.upload-dir", () -> Path.of(TEST_DB).getParent().resolve("uploads").toString());
        registry.add("projectlense.extract-dir", () -> Path.of(TEST_DB).getParent().resolve("extracted").toString());
        registry.add("GEMINI_API_KEY", () -> "");
        registry.add("OPENAI_API_KEY", () -> "");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired AuthSessionRepository sessions;
    @Autowired EvaluationRubricRepository rubricRepository;
    @Autowired RubricCriterionRepository criterionRepository;
    @MockitoBean EvaluationService evaluator;

    private UserEntity student;
    private UserEntity faculty;
    private UserEntity otherFaculty;
    private UserEntity admin;

    @BeforeEach
    void prepareUsersAndSeedRubrics() {
        String unique = UUID.randomUUID().toString();
        student = users.save(new UserEntity(UUID.randomUUID().toString(), "Test Student", "student-" + unique + "@example.test", "unused", "student", "Engineering"));
        faculty = users.save(new UserEntity(UUID.randomUUID().toString(), "Test Faculty", "faculty-" + unique + "@example.test", "unused", "faculty", "Engineering"));
        otherFaculty = users.save(new UserEntity(UUID.randomUUID().toString(), "Other Faculty", "other-" + unique + "@example.test", "unused", "faculty", "Engineering"));
        admin = users.save(new UserEntity(UUID.randomUUID().toString(), "Test Admin", "admin-" + unique + "@example.test", "unused", "admin", "Administration"));
        createSession(student); createSession(faculty); createSession(otherFaculty); createSession(admin);

        if (jdbc.queryForObject("SELECT COUNT(*) FROM project_review_stages", Integer.class) == 0) {
            jdbc.update("INSERT INTO project_review_stages (review_name, max_marks) VALUES ('Project Review 1', 20.00), ('Project Review 2', 30.00), ('Project Review 3', 50.00)");
        }
        seedRubric(SubmissionType.PDF, "PDF Test Rubric");
        seedRubric(SubmissionType.ZIP, "ZIP Test Rubric");
    }

    @Test
    void routesRequireAuthenticationAndUnknownProjectIsHidden() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(jsonPath("$.service", is("ProjectLense AI Evaluation API")));
        mvc.perform(get("/api/projects/missing")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/projects/missing").with(as(admin)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail", is("Project not found")));
    }

    @Test
    void adminImportApiRequiresConfiguredTokenAndExistingImportStillWorks() throws Exception {
        mvc.perform(get("/api/v1/admin/excel-imports/missing")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/excel-imports/missing").header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isNotFound());

        MockMultipartFile workbook = new MockMultipartFile("file", "registration.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", sampleWorkbook());
        String response = mvc.perform(multipart("/api/v1/admin/excel-imports").file(workbook)
                        .header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRecords", is(1)))
                .andExpect(jsonPath("$.validRecords", is(1))).andReturn().getResponse().getContentAsString();
        String jobId = mapper.readTree(response).path("jobId").asText();
        mvc.perform(get("/api/v1/admin/excel-imports/" + jobId + "/preview").header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.records[0].title", is("Imported Test Project")));
        mvc.perform(post("/api/v1/admin/excel-imports/" + jobId + "/approve").header("X-Admin-Token", "test-admin-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"corrections\":{}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.imported", is(1)));
        mvc.perform(get("/api/projects").with(as(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].title", is("Imported Test Project")));
    }

    @Test
    void pdfWorkflowIsGatedByThreeReviewsAndRequiresFacultyApproval() throws Exception {
        String id = createProject("PDF workflow " + UUID.randomUUID());
        upload(id, new MockMultipartFile("file", "sample.pdf", "application/pdf", samplePdf()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.submissionType", is("PDF")));
        mvc.perform(get("/api/projects/" + id + "/submissions").with(as(student)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].submissionType", is("PDF")));

        JsonNode stages = mapper.readTree(mvc.perform(get("/api/projects/" + id + "/reviews").with(as(faculty)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/api/projects/" + id + "/evaluate").with(as(faculty)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("all be completed")));
        int firstStageId = stages.get(0).path("id").asInt();
        mvc.perform(post("/api/projects/" + id + "/reviews").with(as(faculty)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewId\":" + firstStageId + ",\"marksObtained\":21,\"feedback\":\"too high\"}"))
                .andExpect(status().isBadRequest());
        for (JsonNode stage : stages) {
            mvc.perform(post("/api/projects/" + id + "/reviews").with(as(faculty)).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reviewId\":" + stage.path("id").asInt() + ",\"marksObtained\":1,\"feedback\":\"Progress recorded\"}"))
                    .andExpect(status().isCreated());
        }
        mvc.perform(post("/api/projects/" + id + "/reviews").with(as(faculty)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewId\":" + firstStageId + ",\"marksObtained\":1}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/admin/review-stages/" + firstStageId).with(as(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxMarks\":0.50}"))
                .andExpect(status().isConflict());

        mockValidLlm(SubmissionType.PDF, 88);
        String evaluationResponse = mvc.perform(post("/api/projects/" + id + "/evaluate").with(as(faculty)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.submissionType", is("PDF")))
                .andExpect(jsonPath("$.rubricName", is("PDF Test Rubric")))
                .andExpect(jsonPath("$.totalMarks", is(88.0))).andReturn().getResponse().getContentAsString();
        int evaluationId = mapper.readTree(evaluationResponse).path("evaluationId").asInt();
        mvc.perform(get("/api/evaluations/" + evaluationId).with(as(student))).andExpect(status().isNotFound());
        mvc.perform(post("/api/evaluations/" + evaluationId + "/approve").with(as(otherFaculty)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/evaluations/" + evaluationId + "/approve").with(as(faculty))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"feedback\":\"Approved after review\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("APPROVED")));
        mvc.perform(get("/api/projects/" + id + "/evaluation").with(as(student)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.details[0].awardedMarks", is(88.0)));
        upload(id, new MockMultipartFile("file", "updated.pdf", "application/pdf", samplePdf()))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/projects/" + id + "/evaluation").with(as(student))).andExpect(status().isNotFound());

        var rubric = rubricRepository.findBySubmissionTypeOrderByVersionDesc(SubmissionType.PDF).get(0);
        String update = "{\"name\":\"Changed PDF rubric\",\"submissionType\":\"PDF\",\"description\":\"Updated\",\"totalMarks\":100,\"active\":true,\"criteria\":[{\"name\":\"Overall quality\",\"description\":\"Changed content\",\"maxMarks\":100,\"subcriteria\":[]}]}";
        mvc.perform(put("/api/rubrics/" + rubric.getId()).with(as(admin)).contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isConflict());
    }

    @Test
    void zipSubmissionSelectsZipRubricAndMalformedLlmResponseIsRejected() throws Exception {
        String id = createProject("ZIP workflow " + UUID.randomUUID());
        upload(id, new MockMultipartFile("file", "project.zip", "application/zip", sampleZip()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.submissionType", is("ZIP")));
        JsonNode stages = mapper.readTree(mvc.perform(get("/api/projects/" + id + "/reviews").with(as(faculty)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        for (JsonNode stage : stages) {
            mvc.perform(post("/api/projects/" + id + "/reviews").with(as(faculty)).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reviewId\":" + stage.path("id").asInt() + ",\"marksObtained\":2}"))
                    .andExpect(status().isCreated());
        }

        doAnswer(invocation -> "not valid JSON")
                .when(evaluator).evaluateRubricPrompt(anyString());
        mvc.perform(post("/api/projects/" + id + "/evaluate").with(as(faculty)))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.detail", org.hamcrest.Matchers.containsString("valid rubric evaluation")));

        mockValidLlm(SubmissionType.ZIP, 91);
        mvc.perform(post("/api/projects/" + id + "/evaluate").with(as(faculty)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.submissionType", is("ZIP")))
                .andExpect(jsonPath("$.rubricName", is("ZIP Test Rubric"))).andExpect(jsonPath("$.totalMarks", is(91.0)));
    }

    @Test
    void unassignedFacultyCannotEnterReviewsAndInvalidProjectFieldsAreRejected() throws Exception {
        String id = createProject("Access workflow " + UUID.randomUUID());
        mvc.perform(post("/api/projects/" + id + "/reviews").with(as(otherFaculty)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewId\":1,\"marksObtained\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/projects").with(as(student)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.detail").isArray());
        mvc.perform(get("/api/rubrics").with(as(student))).andExpect(status().isForbidden());
        mvc.perform(get("/api/rubrics").with(as(admin))).andExpect(status().isOk());
        mvc.perform(post("/api/projects/" + id + "/approve").with(as(faculty)))
                .andExpect(status().isGone());
    }

    @Test
    void administratorCanCreateAndEditAnUnusedRubricVersion() throws Exception {
        String create = "{\"name\":\"Draft PDF rubric\",\"submissionType\":\"PDF\",\"description\":\"Draft rubric\",\"totalMarks\":100,\"active\":false,\"criteria\":[{\"name\":\"Overall quality\",\"description\":\"Evidence-supported quality\",\"maxMarks\":100,\"subcriteria\":[]}]}";
        String created = mvc.perform(post("/api/rubrics").with(as(admin)).contentType(MediaType.APPLICATION_JSON).content(create))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.version", is(2))).andReturn().getResponse().getContentAsString();
        int rubricId = mapper.readTree(created).path("id").asInt();
        String update = "{\"name\":\"Draft PDF rubric\",\"submissionType\":\"PDF\",\"description\":\"Updated draft description\",\"totalMarks\":100,\"active\":false,\"criteria\":[{\"name\":\"Overall quality\",\"description\":\"Updated criterion description\",\"maxMarks\":100,\"subcriteria\":[]}]}";
        mvc.perform(put("/api/rubrics/" + rubricId).with(as(admin)).contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk()).andExpect(jsonPath("$.description", is("Updated draft description")))
                .andExpect(jsonPath("$.criteria[0].description", is("Updated criterion description")));
    }

    @Test
    void legacyUploadRejectsUnsupportedFileTypesWithoutBypassingSubmissionRules() throws Exception {
        MockMultipartFile bad = new MockMultipartFile("report", "sample.txt", "text/plain", "x".getBytes());
        mvc.perform(multipart("/api/projects/upload").file(bad).with(as(student))
                        .param("title", "Invalid format " + UUID.randomUUID()).param("description", "Test")
                        .param("academic_year", "2026-27").param("faculty_guide", faculty.getId()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").exists());
        mvc.perform(multipart("/api/projects/upload").with(as(student)))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.detail").isArray());
    }

    private String createProject(String title) throws Exception {
        String body = mapper.writeValueAsString(Map.of("title", title, "description", "An academic project test.",
                "department", "Engineering", "academicYear", "2026-27", "facultyGuide", faculty.getId()));
        String response = mvc.perform(post("/api/projects").with(as(student)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).path("project").path("id").asText();
    }

    private org.springframework.test.web.servlet.ResultActions upload(String projectId, MockMultipartFile file) throws Exception {
        return mvc.perform(multipart("/api/projects/" + projectId + "/submissions").file(file).with(as(student)));
    }

    private RequestPostProcessor as(UserEntity user) {
        return request -> { request.addHeader("Authorization", "Bearer " + tokenFor(user)); return request; };
    }

    private final java.util.Map<String, String> tokens = new java.util.HashMap<>();
    private void createSession(UserEntity user) {
        String token = UUID.randomUUID().toString();
        tokens.put(user.getId(), token);
        sessions.save(new AuthSessionEntity(sha256(token), user, java.time.Instant.now().plusSeconds(3600)));
    }
    private String tokenFor(UserEntity user) { return tokens.get(user.getId()); }
    private static String sha256(String value) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    private void seedRubric(SubmissionType type, String name) {
        Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM evaluation_rubrics WHERE submission_type = ?", Integer.class, type.name());
        if (existing != null && existing > 0) return;
        jdbc.update("INSERT INTO evaluation_rubrics (rubric_name, submission_type, description, total_marks, `version`, is_active, created_at, updated_at) VALUES (?, ?, ?, 100.00, 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", name, type.name(), "Integration test rubric");
        Integer rubricId = jdbc.queryForObject("SELECT rubric_id FROM evaluation_rubrics WHERE submission_type = ? AND `version` = 1", Integer.class, type.name());
        jdbc.update("INSERT INTO rubric_criteria (rubric_id, criterion_name, description, max_marks, display_order) VALUES (?, ?, ?, 100.00, 1)", rubricId, "Overall quality", "Measures the quality supported by submission evidence.");
    }

    private void mockValidLlm(SubmissionType type, int awardedMarks) throws Exception {
        doAnswer(invocation -> {
            var rubric = rubricRepository.findBySubmissionTypeAndActiveTrueOrderByVersionDesc(type).get(0);
            var criteria = criterionRepository.findByRubric_IdOrderByDisplayOrderAscIdAsc(rubric.getId());
            List<Map<String, Object>> rows = new ArrayList<>();
            for (var criterion : criteria) rows.add(Map.of("criterionId", criterion.getId(), "awardedMarks", awardedMarks,
                    "justification", "The submitted evidence supports this mark.", "improvementSuggestions", "Add more measured validation."));
            return mapper.writeValueAsString(Map.of("evaluation", rows, "overallFeedback", "The project has evidence-backed strengths and clear next steps."));
        }).when(evaluator).evaluateRubricPrompt(anyString());
    }

    private byte[] sampleZip() throws Exception {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("src/Main.java")); zip.write("class Main {\n}\n".getBytes()); zip.closeEntry();
            zip.putNextEntry(new ZipEntry("README.md")); zip.write("# Demo\n".getBytes()); zip.closeEntry();
            zip.finish(); return bytes.toByteArray();
        }
    }

    private byte[] sampleWorkbook() throws Exception {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Project Registration List");
            sheet.createRow(0).createCell(0).setCellValue("Department of Computer Engineering");
            sheet.createRow(1).createCell(0).setCellValue("Academic Year: 2026-2027");
            var header = sheet.createRow(3);
            String[] columns = {"Group No.", "Roll No.", "Student Name", "Guide Name", "Title of Project"};
            for (int i = 0; i < columns.length; i++) header.createCell(i).setCellValue(columns[i]);
            var row = sheet.createRow(4);
            row.createCell(0).setCellValue("G-TEST"); row.createCell(1).setCellValue("TEST-001");
            row.createCell(2).setCellValue("Test Student"); row.createCell(3).setCellValue("Test Guide");
            row.createCell(4).setCellValue("Imported Test Project");
            workbook.write(output); return output.toByteArray();
        }
    }

    private byte[] samplePdf() throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument document = new org.apache.pdfbox.pdmodel.PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var page = new org.apache.pdfbox.pdmodel.PDPage(); document.addPage(page);
            try (var content = new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page)) {
                content.beginText(); content.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700); content.showText("Abstract Problem Definition Methodology Implementation Results Literature Review"); content.endText();
            }
            document.save(output); return output.toByteArray();
        }
    }
}
