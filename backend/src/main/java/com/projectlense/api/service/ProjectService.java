package com.projectlense.api.service;

import com.projectlense.api.dto.FacultyReviewRequest;
import com.projectlense.api.exception.ApiExceptionHandler.ApiProblem;
import com.projectlense.api.repository.ProjectJsonRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProjectService {
    private final ProjectJsonRepository repository;
    private final PdfProcessor pdfProcessor;
    private final ZipProcessor zipProcessor;
    private final EvaluationService evaluator;
    private final Path uploadDir;
    private final Path extractDir;

    public ProjectService(ProjectJsonRepository repository, PdfProcessor pdfProcessor, ZipProcessor zipProcessor,
                          EvaluationService evaluator,
                          @Value("${projectlense.upload-dir}") String uploadDir,
                          @Value("${projectlense.extract-dir}") String extractDir) {
        this.repository = repository; this.pdfProcessor = pdfProcessor; this.zipProcessor = zipProcessor;
        this.evaluator = evaluator; this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        this.extractDir = Path.of(extractDir).toAbsolutePath().normalize();
    }

    public Map<String, Object> upload(String title, String description, String department, String year,
                                      String facultyGuide, String studentName, String studentId, MultipartFile report) {
        String originalName = report.getOriginalFilename() == null ? "" : report.getOriginalFilename();
        String lower = originalName.toLowerCase();
        boolean pdf = lower.endsWith(".pdf"), zip = lower.endsWith(".zip");
        if (!pdf && !zip) throw new ApiProblem(HttpStatus.BAD_REQUEST, "Unsupported format. Please upload either a PDF document (.pdf) or a ZIP archive (.zip).");
        String projectId = "proj-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String safeName = Path.of(originalName.replace('\\', '/')).getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
        Path savedFile = uploadDir.resolve(projectId + "_" + safeName).normalize();
        try {
            Files.createDirectories(uploadDir); Files.createDirectories(extractDir);
            report.transferTo(savedFile);
            String extractedText;
            List<String> sections;
            Map<String, Object> stats = new LinkedHashMap<>();
            Map<String, Object> metrics = null;
            if (pdf) {
                Map<String, Object> data = pdfProcessor.extract(savedFile.toFile());
                extractedText = (String)data.get("text"); sections = (List<String>)data.get("detected_sections");
                copyStats(data, stats);
            } else {
                Map<String, Object> analysis = zipProcessor.analyze(savedFile, extractDir.resolve(projectId), pdfProcessor);
                metrics = new LinkedHashMap<>();
                for (String key : List.of("total_files", "total_loc", "detected_stack", "file_distribution", "sample_tree", "found_report_pdf")) metrics.put(key, analysis.get(key));
                @SuppressWarnings("unchecked") Map<String, Object> innerPdf = (Map<String, Object>)analysis.get("pdf_report_data");
                if (innerPdf != null) {
                    extractedText = (String)innerPdf.get("text"); sections = (List<String>)innerPdf.get("detected_sections"); copyStats(innerPdf, stats);
                } else {
                    extractedText = "Project: " + title + "\nDescription: " + description + "\nTechnologies: " + String.join(", ", (List<String>)analysis.get("detected_stack"))
                            + "\nLines of Code: " + analysis.get("total_loc") + "\nFiles: " + String.join(", ", (List<String>)analysis.get("sample_tree"));
                    sections = List.of("Codebase Architecture", "Implementation Modules", "Repository Documentation");
                    long loc = ((Number)analysis.get("total_loc")).longValue();
                    stats.put("page_count", 1); stats.put("word_count", loc / 4); stats.put("char_count", loc * 25);
                    stats.put("reading_time_min", Math.max(1, loc / 800));
                }
            }
            Map<String, Object> evaluation = evaluator.evaluate(extractedText, title, sections, stats);
            if (metrics != null) {
                evaluation.put("codebase_metrics", metrics);
                if (((Number)metrics.get("total_loc")).longValue() > 500) {
                    List<Map<String, Object>> scores = (List<Map<String, Object>>)evaluation.get("criterion_scores");
                    for (Map<String, Object> criterion : scores) if ("Implementation".equals(criterion.get("criterion"))) {
                        List<String> stack = (List<String>)metrics.get("detected_stack");
                        criterion.put("reasoning", "Verified codebase with " + String.format("%,d", metrics.get("total_loc")) + " lines of code across " + metrics.get("total_files") + " files (" + String.join(", ", stack.subList(0, Math.min(3, stack.size()))) + ").");
                    }
                }
            }
            Map<String, Object> project = new LinkedHashMap<>();
            project.put("id", projectId); project.put("title", title);
            project.put("description", description == null || description.isEmpty() ? extractedText.substring(0, Math.min(300, extractedText.length())) + "..." : description);
            project.put("department", department); project.put("academic_year", year); project.put("faculty_guide", facultyGuide);
            project.put("student_id", studentId); project.put("student_name", studentName); project.put("status", "evaluated");
            project.put("submitted_at", Instant.now().toString()); project.put("filename", originalName); project.put("file_type", zip ? "zip" : "pdf");
            project.put("document_stats", stats); project.put("detected_sections", sections); project.put("codebase_metrics", metrics);
            project.put("ai_evaluation", evaluation); project.put("faculty_evaluation", null);
            Map<String, Object> response = new LinkedHashMap<>(); response.put("success", true); response.put("project", repository.insert(project));
            return response;
        } catch (ApiProblem e) { throw e; }
        catch (Exception e) { throw new ApiProblem(HttpStatus.INTERNAL_SERVER_ERROR, "File evaluation failed: " + e.getMessage()); }
    }

    public List<Map<String, Object>> list(String studentId) {
        List<Map<String, Object>> all = repository.findAll();
        if (studentId == null) return all;
        return all.stream().filter(p -> studentId.equals(p.get("student_id"))).toList();
    }

    public Map<String, Object> get(String id) {
        return repository.findById(id).orElseThrow(() -> new ApiProblem(HttpStatus.NOT_FOUND, "Project not found"));
    }

    public Map<String, Object> review(String id, FacultyReviewRequest request, boolean approve) {
        get(id);
        Map<String, Object> faculty = new LinkedHashMap<>();
        faculty.put("final_score", request.final_score()); faculty.put("comment", request.comment());
        faculty.put("reviewed_by", request.resolvedFacultyName());
        faculty.put(approve ? "approved_at" : "reviewed_at", Instant.now().toString()); faculty.put("is_approved", approve);
        Map<String, Object> updates = new LinkedHashMap<>(); updates.put("status", approve ? "approved" : "under_review"); updates.put("faculty_evaluation", faculty);
        Map<String, Object> project = repository.update(id, updates).orElseThrow(() -> new ApiProblem(HttpStatus.NOT_FOUND, "Project not found"));
        return Map.of("success", true, "project", project);
    }

    private static void copyStats(Map<String, Object> source, Map<String, Object> target) {
        for (String key : List.of("page_count", "word_count", "char_count", "reading_time_min")) target.put(key, source.get(key));
    }
}
