package com.projectlense.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.Matchers.is;
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

    @Test
    void rootAndUnknownProjectContracts() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(jsonPath("$.service", is("ProjectLense AI Evaluation API")));
        mvc.perform(get("/api/projects/missing")).andExpect(status().isNotFound()).andExpect(jsonPath("$.detail", is("Project not found")));
    }

    @Test
    void adminImportApiRequiresConfiguredToken() throws Exception {
        mvc.perform(get("/api/v1/admin/excel-imports/missing"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/excel-imports/missing").header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void excelImportCanBePreviewedAndApproved() throws Exception {
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
        mvc.perform(get("/api/projects")).andExpect(status().isOk()).andExpect(jsonPath("$[0].title", is("Imported Test Project")));
    }

    @Test
    void pdfUploadListDetailAndFacultyReviewFlow() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("report", "sample.pdf", "application/pdf", samplePdf());
        String body = mvc.perform(multipart("/api/projects/upload").file(pdf)
                        .param("title", "Test project").param("description", "A test description").param("student_id", "student-test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.project.file_type", is("pdf"))).andReturn().getResponse().getContentAsString();
        JsonNode project = mapper.readTree(body).path("project");
        String id = project.path("id").asText();
        mvc.perform(get("/api/projects").param("student_id", "student-test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id", is(id)));
        mvc.perform(get("/api/projects/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.title", is("Test project")));
        String review = "{\"final_score\":83,\"comment\":\"Good work\"}";
        mvc.perform(post("/api/projects/" + id + "/faculty-review").contentType(MediaType.APPLICATION_JSON).content(review))
                .andExpect(status().isOk()).andExpect(jsonPath("$.project.status", is("under_review")))
                .andExpect(jsonPath("$.project.faculty_evaluation.final_score", is(83)));
        mvc.perform(post("/api/projects/" + id + "/approve").contentType(MediaType.APPLICATION_JSON).content(review))
                .andExpect(status().isOk()).andExpect(jsonPath("$.project.status", is("approved")))
                .andExpect(jsonPath("$.project.faculty_evaluation.is_approved", is(true)));
    }

    @Test
    void unsupportedUploadAndMissingRequiredFieldsKeepErrorShapes() throws Exception {
        MockMultipartFile bad = new MockMultipartFile("report", "sample.txt", "text/plain", "x".getBytes());
        mvc.perform(multipart("/api/projects/upload").file(bad).param("title", "Test"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").exists());
        mvc.perform(multipart("/api/projects/upload"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.detail").isArray());
        mvc.perform(post("/api/projects/example/faculty-review").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"final_score\":\"not-a-number\",\"comment\":\"x\"}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.detail").isArray());
    }

    @Test
    void zipUploadReturnsCodebaseMetrics() throws Exception {
        MockMultipartFile zip = new MockMultipartFile("report", "project.zip", "application/zip", sampleZip());
        mvc.perform(multipart("/api/projects/upload").file(zip).param("title", "ZIP test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.project.file_type", is("zip")))
                .andExpect(jsonPath("$.project.codebase_metrics.total_files", is(2)))
                .andExpect(jsonPath("$.project.codebase_metrics.total_loc", is(3)))
                .andExpect(jsonPath("$.project.ai_evaluation.codebase_metrics.total_loc", is(3)));
    }

    private byte[] sampleZip() throws Exception {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("src/Main.java")); zip.write("class Main {\n}\n".getBytes()); zip.closeEntry();
            zip.putNextEntry(new ZipEntry("README.md")); zip.write("# Demo\n".getBytes()); zip.closeEntry();
            zip.finish(); return bytes.toByteArray();
        }
    }

    private byte[] sampleWorkbook() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
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
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(); document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText(); content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700); content.showText("Abstract Problem Definition Methodology Implementation Results Literature Review"); content.endText();
            }
            document.save(output); return output.toByteArray();
        }
    }
}
