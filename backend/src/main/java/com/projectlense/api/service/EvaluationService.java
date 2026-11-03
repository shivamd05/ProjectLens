package com.projectlense.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.net.http.HttpClient;
import java.time.Duration;

@Service
public class EvaluationService {
    private static final String SYSTEM_PROMPT = """
            You are an expert Academic Project Reviewer and University Examination Committee Member.
            Evaluate the following academic project report against the standardized 7-criteria rubric (Total 100 points).
            Standard Rubric:
            1. Problem Definition (Max 10): Clarity of research objective, motivation, and problem scope.
            2. Literature Review (Max 10): Survey of existing solutions, citation relevancy, and gap analysis.
            3. Methodology (Max 15): Soundness of proposed architecture, design flow, and theoretical models.
            4. Implementation (Max 20): Technical depth, algorithms, database schema, and execution quality.
            5. Innovation (Max 15): Novelty, unique features, and creative problem solving.
            6. Results (Max 15): Experimental verification, benchmarking, performance metrics, and validation.
            7. Documentation (Max 15): Academic formatting, diagrams, and language quality.
            Respond ONLY with a valid JSON object using this exact structure:
            {
              "overall_score": <number 0-100>, "similarity_score": <estimated percentage 5-25>,
              "criterion_scores": [
                {"criterion":"Problem Definition","score":<0-10>,"maximum_score":10,"reasoning":"specific evidence-based reasoning"},
                {"criterion":"Literature Review","score":<0-10>,"maximum_score":10,"reasoning":"specific evidence-based reasoning"},
                {"criterion":"Methodology","score":<0-15>,"maximum_score":15,"reasoning":"specific evidence-based reasoning"},
                {"criterion":"Implementation","score":<0-20>,"maximum_score":20,"reasoning":"specific evidence-based reasoning"},
                {"criterion":"Innovation","score":<0-15>,"maximum_score":15,"reasoning":"specific evidence-based reasoning"},
                {"criterion":"Results","score":<0-15>,"maximum_score":15,"reasoning":"specific evidence-based reasoning"},
                {"criterion":"Documentation","score":<0-15>,"maximum_score":15,"reasoning":"specific evidence-based reasoning"}
              ],
              "strengths":["strength 1","strength 2","strength 3"],
              "weaknesses":["weakness 1","weakness 2","weakness 3"],
              "suggestions":["suggestion 1","suggestion 2","suggestion 3"],
              "overall_feedback":"executive summary for faculty review"
            }
            """;
    private final RestClient http = RestClient.builder().requestFactory(requestFactory()).build();
    private final String geminiKey;
    private final String geminiModel;
    private final String openAiKey;
    private final String openAiModel;

    public EvaluationService(@Value("${GEMINI_API_KEY:}") String geminiKey,
                             @Value("${projectlense.evaluation.gemini-model:gemini-3.8-flash}") String geminiModel,
                             @Value("${OPENAI_API_KEY:}") String openAiKey,
                             @Value("${OPENAI_MODEL:gpt-6-luna}") String openAiModel) {
        this.geminiKey = geminiKey;
        this.geminiModel = geminiModel;
        this.openAiKey = openAiKey;
        this.openAiModel = openAiModel;
    }

    private static JdkClientHttpRequestFactory requestFactory() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(30));
        return factory;
    }

    public Map<String, Object> evaluate(String text, String title, List<String> sections, Map<String, Object> stats) {
        if (!geminiKey.isBlank()) {
            try { return callGemini(text, title); }
            catch (Exception e) { System.err.println("[LLM Evaluator] Gemini API error: " + e.getMessage() + ", using fallback"); }
        }
        if (!openAiKey.isBlank()) {
            try { return callOpenAi(text, title); }
            catch (Exception e) { System.err.println("[LLM Evaluator] OpenAI API error: " + e.getMessage() + ", using fallback"); }
        }
        return fallback(text, title, sections, stats);
    }

    /** Uses the configured provider for a caller-supplied rubric prompt without demo-score fallback. */
    public String evaluateRubricPrompt(String evaluationPrompt) throws Exception {
        List<Exception> failures = new ArrayList<>();
        if (!geminiKey.isBlank()) {
            try { return callGeminiText(evaluationPrompt); }
            catch (Exception e) { failures.add(e); System.err.println("[Rubric Evaluator] Gemini request failed; trying configured fallback provider"); }
        }
        if (!openAiKey.isBlank()) {
            try { return callOpenAiText(evaluationPrompt); }
            catch (Exception e) { failures.add(e); System.err.println("[Rubric Evaluator] OpenAI request failed"); }
        }
        IllegalStateException unavailable = new IllegalStateException("No configured LLM provider returned a valid response");
        failures.forEach(unavailable::addSuppressed);
        throw unavailable;
    }

    @SuppressWarnings("unchecked")
    private String callGeminiText(String prompt) {
        Map<String, Object> payload = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.2, "responseMimeType", "application/json"));
        Map<String, Object> data = http.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={key}", geminiModel, geminiKey)
                .body(payload).retrieve().body(Map.class);
        if (data == null) throw new IllegalStateException("Gemini returned an empty response");
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) data.get("candidates");
        if (candidates == null || candidates.isEmpty()) throw new IllegalStateException("Gemini returned no evaluation candidate");
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = content == null ? null : (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty() || parts.get(0).get("text") == null) throw new IllegalStateException("Gemini returned no evaluation text");
        return String.valueOf(parts.get(0).get("text"));
    }

    @SuppressWarnings("unchecked")
    private String callOpenAiText(String prompt) {
        Map<String, Object> payload = Map.of("model", openAiModel, "reasoning_effort", "none", "temperature", 0.2,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(Map.of("role", "system", "content", "Return only the JSON object requested by the evaluation instructions."),
                        Map.of("role", "user", "content", prompt)));
        Map<String, Object> data = http.post().uri("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + openAiKey).body(payload).retrieve().body(Map.class);
        if (data == null) throw new IllegalStateException("OpenAI returned an empty response");
        List<Map<String, Object>> choices = (List<Map<String, Object>>) data.get("choices");
        if (choices == null || choices.isEmpty()) throw new IllegalStateException("OpenAI returned no evaluation choice");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null || message.get("content") == null) throw new IllegalStateException("OpenAI returned no evaluation text");
        return String.valueOf(message.get("content"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callGemini(String text, String title) throws Exception {
        Map<String, Object> payload = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt(text, title))))),
                "generationConfig", Map.of("temperature", 0.2, "responseMimeType", "application/json"));
        Map<String, Object> data = http.post().uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={key}", geminiModel, geminiKey)
                .body(payload).retrieve().body(Map.class);
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) data.get("candidates");
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        Map<String, Object> parsed = new com.fasterxml.jackson.databind.ObjectMapper().readValue((String) parts.get(0).get("text"), Map.class);
        parsed.put("is_demo_mode", false); parsed.put("model_provider", "Google Gemini " + geminiModel);
        return parsed;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callOpenAi(String text, String title) throws Exception {
        Map<String, Object> payload = Map.of("model", openAiModel, "reasoning_effort", "none", "temperature", 0.2,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", prompt(text, title))));
        Map<String, Object> data = http.post().uri("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + openAiKey).body(payload).retrieve().body(Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) data.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        Map<String, Object> parsed = new com.fasterxml.jackson.databind.ObjectMapper().readValue((String) message.get("content"), Map.class);
        parsed.put("is_demo_mode", false); parsed.put("model_provider", "OpenAI GPT-4o-mini");
        return parsed;
    }

    private String prompt(String text, String title) {
        return SYSTEM_PROMPT + "\n\nProject Title: " + title + "\n\nProject Extracted Content:\n" + text.substring(0, Math.min(15000, text.length()));
    }

    private Map<String, Object> fallback(String text, String title, List<String> sections, Map<String, Object> stats) {
        int words = number(stats.get("word_count"), 0), pages = number(stats.get("page_count"), 1);
        String lower = text.toLowerCase();
        boolean problem = sections.contains("Problem Definition") || sections.contains("Abstract");
        boolean lit = sections.contains("Literature Review"), method = sections.contains("Methodology");
        boolean impl = sections.contains("Implementation"), results = sections.contains("Results & Discussion");
        String[] tech = {"database", "api", "model", "algorithm", "module", "system", "react", "python", "sql", "network", "hardware", "controller"};
        List<String> found = new ArrayList<>(); for (String keyword : tech) if (lower.contains(keyword)) found.add(keyword);
        int problemScore = problem && words > 300 ? 9 : (problem ? 7 : 5);
        int litScore = lit ? 8 : 6;
        int methodScore = method && words > 600 ? 13 : (method ? 11 : 8);
        int implScore = impl ? Math.min(19, 14 + found.size()) : 12;
        int innovation = found.size() >= 3 ? 12 : 10;
        int resultScore = results ? 12 : 9;
        int docScore = Math.min(14, Math.max(10, 8 + pages));
        List<Map<String, Object>> criteria = List.of(
                criterion("Problem Definition", problemScore, 10, problem ? "Problem statement clearly established in report." : "Problem definition could be more explicitly stated."),
                criterion("Literature Review", litScore, 10, lit ? "Comprehensive survey of related work included." : "Literature review section is brief; consider surveying additional recent peer-reviewed sources."),
                criterion("Methodology", methodScore, 15, method ? "Solid architectural formulation and design flow provided." : "System design methodology requires more step-by-step mathematical or architectural clarity."),
                criterion("Implementation", implScore, 20, "Practical implementation details presented with key modules (" + (found.isEmpty() ? "core components" : String.join(", ", found.subList(0, Math.min(4, found.size())))) + ")."),
                criterion("Innovation", innovation, 15, "Demonstrates good engineering application with practical utility."),
                criterion("Results", resultScore, 15, results ? "Results and experimental observations presented." : "Results section lacks quantitative comparative benchmark tables."),
                criterion("Documentation", docScore, 15, "Well-structured document with " + pages + " pages and " + words + " words adhering to university submission format."));
        int total = criteria.stream().mapToInt(c -> (Integer)c.get("score")).sum();
        List<String> strengths = new ArrayList<>();
        if (impl) strengths.add("Strong practical implementation utilizing modern technical stack (" + (found.isEmpty() ? "frameworks" : String.join(", ", found.subList(0, Math.min(3, found.size())))) + ").");
        if (method) strengths.add("Clear methodology and systematic architectural workflow.");
        strengths.add("Comprehensive document containing " + words + " words across " + pages + " pages.");
        List<String> weaknesses = new ArrayList<>();
        if (!results) weaknesses.add("Results section needs more empirical benchmarking and error rate measurements.");
        if (!lit) weaknesses.add("Literature review lacks recent (2023–2024) IEEE/ACM references.");
        weaknesses.add("Discussion on scalability constraints and hardware edge-cases could be expanded.");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("overall_score", total); out.put("similarity_score", 12); out.put("criterion_scores", criteria);
        out.put("strengths", strengths); out.put("weaknesses", weaknesses);
        out.put("suggestions", List.of("Include a comparative performance table against at least 2 existing benchmark systems.", "Add quantitative evaluation graphs (e.g. latency, accuracy, or resource utilization curves).", "Ensure all citations follow standard IEEE/APA reference formatting."));
        out.put("overall_feedback", "The project '" + title + "' demonstrates good engineering execution and clear documentation. The technical architecture is well articulated with room for improvement in comparative benchmarking and citation breadth.");
        out.put("is_demo_mode", true); out.put("model_provider", "Content-Aware Academic Evaluation Engine (Demo Mode)");
        return out;
    }

    private static int number(Object value, int defaultValue) { return value instanceof Number n ? n.intValue() : defaultValue; }
    private static Map<String, Object> criterion(String name, int score, int max, String reasoning) {
        return Map.of("criterion", name, "score", score, "maximum_score", max, "reasoning", reasoning);
    }
}
