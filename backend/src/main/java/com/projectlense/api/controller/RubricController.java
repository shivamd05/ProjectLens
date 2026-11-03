package com.projectlense.api.controller;

import com.projectlense.api.dto.RubricDtos;
import com.projectlense.api.entity.SubmissionType;
import com.projectlense.api.service.RubricService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/rubrics")
public class RubricController {
    private final RubricService rubrics;
    public RubricController(RubricService rubrics) { this.rubrics = rubrics; }

    @GetMapping public List<Map<String, Object>> list() { return rubrics.list(); }
    @GetMapping("/{id}") public Map<String, Object> get(@PathVariable Integer id) { return rubrics.get(id); }

    @GetMapping("/type/{submissionType}")
    public List<Map<String, Object>> byType(@PathVariable String submissionType) {
        try { return rubrics.byType(SubmissionType.valueOf(submissionType.toUpperCase(Locale.ROOT))); }
        catch (IllegalArgumentException e) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Submission type must be PDF or ZIP");
        }
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody RubricDtos.RubricRequest request) { return rubrics.create(request); }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Integer id, @Valid @RequestBody RubricDtos.RubricRequest request) {
        return rubrics.update(id, request);
    }
}
