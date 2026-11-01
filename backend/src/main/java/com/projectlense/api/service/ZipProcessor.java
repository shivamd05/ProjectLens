package com.projectlense.api.service;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ZipProcessor {
    private static final Map<String, String> EXTENSIONS = Map.ofEntries(
            Map.entry(".py", "Python"), Map.entry(".js", "JavaScript"), Map.entry(".jsx", "React (JSX)"),
            Map.entry(".ts", "TypeScript"), Map.entry(".tsx", "React (TSX)"), Map.entry(".java", "Java"),
            Map.entry(".cpp", "C++"), Map.entry(".c", "C"), Map.entry(".html", "HTML"), Map.entry(".css", "CSS"),
            Map.entry(".sql", "SQL"), Map.entry(".json", "JSON Configuration"), Map.entry(".md", "Markdown Documentation"));
    private static final Set<String> IGNORED_DIRS = Set.of(".git", "node_modules", "__pycache__", "dist", "build", ".venv", "env");
    private static final long MAX_EXTRACTED_BYTES = 250L * 1024 * 1024;

    public Map<String, Object> analyze(Path zipPath, Path target, PdfProcessor pdfProcessor) throws Exception {
        Files.createDirectories(target);
        int files = 0;
        long loc = 0, expanded = 0;
        Map<String, Integer> languageCounts = new LinkedHashMap<>();
        List<Path> foundPdfs = new ArrayList<>();
        List<String> tree = new ArrayList<>();
        try (InputStream raw = Files.newInputStream(zipPath); ZipInputStream zin = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName().replace('\\', '/');
                Path relative = Path.of(name).normalize();
                if (relative.isAbsolute() || relative.startsWith("..")) throw new IllegalArgumentException("Invalid ZIP entry path");
                boolean ignored = false;
                for (Path segment : relative) if (IGNORED_DIRS.contains(segment.toString())) ignored = true;
                if (ignored) continue;
                Path out = target.resolve(relative).normalize();
                if (!out.startsWith(target.normalize())) throw new IllegalArgumentException("Invalid ZIP entry path");
                Files.createDirectories(out.getParent());
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = zin.read(buffer)) != -1) {
                    expanded += n;
                    if (expanded > MAX_EXTRACTED_BYTES) throw new IllegalArgumentException("ZIP expands beyond the 250 MB extraction limit");
                    bytes.write(buffer, 0, n);
                }
                Files.write(out, bytes.toByteArray());
                files++;
                String rel = relative.toString().replace('\\', '/');
                if (rel.toLowerCase().endsWith(".pdf")) foundPdfs.add(out);
                String filename = relative.getFileName().toString();
                int dot = filename.lastIndexOf('.');
                String ext = dot >= 0 ? filename.substring(dot).toLowerCase() : "";
                if (EXTENSIONS.containsKey(ext)) {
                    String lang = EXTENSIONS.get(ext);
                    languageCounts.merge(lang, 1, Integer::sum);
                    String source = Files.readString(out, java.nio.charset.StandardCharsets.UTF_8);
                    if (!source.isEmpty()) {
                        String[] lines = source.split("\\R", -1);
                        loc += lines.length - (source.endsWith("\n") || source.endsWith("\r") ? 1 : 0);
                    }
                }
                if (tree.size() < 25) tree.add(rel);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("is_zip", true); result.put("total_files", files); result.put("total_loc", loc);
        result.put("detected_stack", languageCounts.isEmpty() ? List.of("Standard Project Archive") : new ArrayList<>(languageCounts.keySet()));
        result.put("file_distribution", languageCounts); result.put("sample_tree", tree);
        result.put("found_report_pdf", foundPdfs.isEmpty() ? null : foundPdfs.get(0).getFileName().toString());
        Map<String, Object> pdfData = null;
        if (!foundPdfs.isEmpty()) {
            try { pdfData = pdfProcessor.extract(foundPdfs.get(0).toFile()); }
            catch (Exception e) { System.err.println("[ZIP Processor] Error parsing inner PDF: " + e.getMessage()); }
        }
        result.put("pdf_report_data", pdfData);
        return result;
    }
}
