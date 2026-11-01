package com.projectlense.api.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class PdfProcessor {
    private static final Map<String, Pattern> SECTION_PATTERNS = new LinkedHashMap<>();
    static {
        SECTION_PATTERNS.put("Abstract", Pattern.compile("\\b(abstract|executive\\s+summary)\\b", Pattern.CASE_INSENSITIVE));
        SECTION_PATTERNS.put("Problem Definition", Pattern.compile("\\b(problem\\s+definition|problem\\s+statement|motivation|objectives?|introduction)\\b", Pattern.CASE_INSENSITIVE));
        SECTION_PATTERNS.put("Literature Review", Pattern.compile("\\b(literature\\s+review|related\\s+work|prior\\s+work|background\\s+study|existing\\s+systems?)\\b", Pattern.CASE_INSENSITIVE));
        SECTION_PATTERNS.put("Methodology", Pattern.compile("\\b(methodology|proposed\\s+system|system\\s+architecture|design\\s+methodology|workflow)\\b", Pattern.CASE_INSENSITIVE));
        SECTION_PATTERNS.put("Implementation", Pattern.compile("\\b(implementation|system\\s+development|algorithms?|modules?|code\\s+structure|database\\s+design|tech\\s+stack)\\b", Pattern.CASE_INSENSITIVE));
        SECTION_PATTERNS.put("Results & Discussion", Pattern.compile("\\b(results?|performance\\s+analysis|experimental\\s+results?|discussion|findings?|testing)\\b", Pattern.CASE_INSENSITIVE));
        SECTION_PATTERNS.put("Conclusion & References", Pattern.compile("\\b(conclusion|future\\s+scope|future\\s+work|references|bibliography)\\b", Pattern.CASE_INSENSITIVE));
    }

    public Map<String, Object> extract(File file) throws Exception {
        String text;
        int pages;
        try (PDDocument doc = Loader.loadPDF(file)) {
            pages = doc.getNumberOfPages();
            text = new PDFTextStripper().getText(doc).strip();
        }
        int words = (int)java.util.regex.Pattern.compile("(?U)\\b\\w+\\b").matcher(text).results().count();
        List<String> sections = new ArrayList<>();
        String extractedText = text;
        for (Map.Entry<String, Pattern> entry : SECTION_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(extractedText).find()) sections.add(entry.getKey());
        }
        if (sections.isEmpty()) sections = List.of("General Content", "Technical Documentation");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("text", text);
        result.put("page_count", pages);
        result.put("word_count", words);
        result.put("char_count", text.length());
        result.put("reading_time_min", Math.max(1, Math.round(words / 200.0f)));
        result.put("detected_sections", sections);
        result.put("preview_snippet", text.substring(0, Math.min(1200, text.length())));
        return result;
    }
}
