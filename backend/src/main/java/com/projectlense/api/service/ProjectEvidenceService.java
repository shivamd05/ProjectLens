package com.projectlense.api.service;

import com.projectlense.api.entity.ProjectEntity;
import com.projectlense.api.entity.ProjectSubmissionEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class ProjectEvidenceService {
    private static final int MAX_EVIDENCE_CHARS = 50_000;
    private static final int MAX_SOURCE_FILE_CHARS = 7_500;
    private static final int MAX_SOURCE_FILES = 30;
    private static final Set<String> SOURCE_EXTENSIONS = Set.of(".md", ".txt", ".java", ".py", ".js", ".jsx",
            ".ts", ".tsx", ".html", ".css", ".sql", ".json", ".yml", ".yaml", ".xml", ".sh", ".cs",
            ".go", ".rs", ".c", ".cpp", ".h", ".hpp", ".kt", ".swift", ".php", ".rb", ".properties");
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile("(?i)(\\b(?:api[_-]?key|client[_-]?secret|private[_-]?key|secret|password|passwd|access[_-]?token|refresh[_-]?token|token)\\b\\s*[:=]\\s*)(\\\"(?:\\\\.|[^\\\"\\\\])*\\\"|'(?:\\\\.|[^'\\\\])*'|[^\\s,;]+)");
    private static final Pattern AUTHORIZATION_HEADER = Pattern.compile("(?im)(\\bauthorization\\s*:\\s*)(?:bearer\\s+)?[^\\r\\n]+");
    private static final Pattern ABSTRACT = Pattern.compile("(?is)\\babstract\\b\\s*[:\\-]?\\s*(.{40,5000}?)(?=\\n\\s*(?:1\\.?\\s*)?(?:introduction|keywords|problem statement|chapter\\s+1)\\b|\\z)");

    private final PdfProcessor pdfProcessor;
    private final ZipProcessor zipProcessor;
    private final ProjectSubmissionService submissions;
    private final Path extractDir;

    public ProjectEvidenceService(PdfProcessor pdfProcessor, ZipProcessor zipProcessor,
                                  ProjectSubmissionService submissions,
                                  @Value("${projectlense.extract-dir}") String extractDir) {
        this.pdfProcessor = pdfProcessor;
        this.zipProcessor = zipProcessor;
        this.submissions = submissions;
        this.extractDir = Path.of(extractDir).toAbsolutePath().normalize();
    }

    public ProjectEvidence extract(ProjectEntity project, ProjectSubmissionEntity submission) throws Exception {
        Path storedFile = submissions.resolveStoredFile(submission);
        String abstractText = "";
        String documentText = "";
        Map<String, Object> analysis;
        if (submission.getSubmissionType().name().equals("PDF")) {
            Map<String, Object> parsed = pdfProcessor.extract(storedFile.toFile());
            documentText = safeText(parsed.get("text"));
            abstractText = abstractFrom(documentText);
            analysis = new LinkedHashMap<>();
            for (String key : List.of("page_count", "word_count", "char_count", "reading_time_min", "detected_sections")) {
                analysis.put(key, parsed.get(key));
            }
            analysis.put("document_type", "PDF report");
        } else {
            Path target = extractDir.resolve("evaluation-submissions").resolve(String.valueOf(submission.getId())).normalize();
            if (!target.startsWith(extractDir)) throw new IllegalArgumentException("Invalid extraction target");
            analysis = zipProcessor.analyze(storedFile, target, pdfProcessor);
            @SuppressWarnings("unchecked") Map<String, Object> pdf = (Map<String, Object>) analysis.get("pdf_report_data");
            if (pdf != null) {
                documentText = safeText(pdf.get("text"));
                abstractText = abstractFrom(documentText);
            }
            String codeEvidence = collectSourceEvidence(target);
            if (!codeEvidence.isBlank()) analysis.put("source_evidence", codeEvidence);
            analysis.put("document_type", "ZIP source archive");
        }

        StringBuilder content = new StringBuilder();
        append(content, "Project title", project.getTitle(), 1000);
        append(content, "Project description", project.getDescription(), 5000);
        append(content, "Abstract extracted from submission", abstractText, 4000);
        append(content, "Extracted report text and detected structure", documentText, 27000);
        Object sourceEvidence = analysis.get("source_evidence");
        if (sourceEvidence != null) append(content, "Selected source code and documentation excerpts", String.valueOf(sourceEvidence), 12000);
        append(content, "Static archive/document analysis", analysis.toString(), 6000);
        return new ProjectEvidence(abstractText, content.toString(), analysis);
    }

    private static String collectSourceEvidence(Path root) throws IOException {
        if (!Files.exists(root)) return "";
        StringBuilder output = new StringBuilder();
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().limit(500).toList()) {
                if (output.length() >= 12000 || countIncludedFiles(output) >= MAX_SOURCE_FILES) break;
                String name = path.getFileName().toString();
                if (isSecretFile(name) || Files.size(path) > 256 * 1024) continue;
                String lower = name.toLowerCase(Locale.ROOT);
                int dot = lower.lastIndexOf('.');
                if (dot < 0 || !SOURCE_EXTENSIONS.contains(lower.substring(dot))) continue;
                String text;
                try { text = Files.readString(path, StandardCharsets.UTF_8); }
                catch (Exception ignored) { continue; }
                if (text.indexOf('\u0000') >= 0) continue;
                text = redactSecrets(text);
                if (text.isBlank()) continue;
                if (text.length() > MAX_SOURCE_FILE_CHARS) text = text.substring(0, MAX_SOURCE_FILE_CHARS) + "\n[excerpt truncated]";
                String relative = root.relativize(path).toString().replace('\\', '/').replaceAll("[\\r\\n]", " ");
                output.append("\n--- ").append(relative).append(" ---\n").append(text).append('\n');
            }
        }
        return output.length() > 12000 ? output.substring(0, 12000) : output.toString();
    }

    private static int countIncludedFiles(StringBuilder output) {
        int count = 0, index = 0;
        while ((index = output.indexOf("\n--- ", index)) >= 0) { count++; index += 5; }
        return count;
    }

    private static boolean isSecretFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.equals(".env") || lower.startsWith(".env.") || lower.contains("secret") || lower.contains("credential")
                || lower.endsWith(".pem") || lower.endsWith(".key") || lower.endsWith(".p12") || lower.endsWith(".jks")
                || lower.equals("id_rsa") || lower.equals("id_ed25519");
    }

    static String redactSecrets(String value) {
        Matcher matcher = SECRET_ASSIGNMENT.matcher(value);
        String redacted = matcher.replaceAll("$1[REDACTED]");
        redacted = AUTHORIZATION_HEADER.matcher(redacted).replaceAll("$1[REDACTED]");
        return redacted.replaceAll("AIza[0-9A-Za-z_-]{30,}", "[REDACTED_API_KEY]");
    }

    private static String abstractFrom(String text) {
        Matcher matcher = ABSTRACT.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1).strip() : "";
    }

    private static void append(StringBuilder output, String label, String value, int limit) {
        if (value == null || value.isBlank() || output.length() >= MAX_EVIDENCE_CHARS) return;
        String cleaned = redactSecrets(value).replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ").strip();
        int remaining = Math.min(limit, MAX_EVIDENCE_CHARS - output.length());
        if (remaining <= 0) return;
        output.append("\n\n## ").append(label).append("\n");
        output.append(cleaned, 0, Math.min(cleaned.length(), Math.max(0, remaining - label.length() - 8)));
        if (cleaned.length() > remaining) output.append("\n[content truncated]");
    }

    private static String safeText(Object value) { return value == null ? "" : String.valueOf(value); }

    public record ProjectEvidence(String abstractText, String content, Map<String, Object> analysis) {}
}
