package com.projectlense.api.service;

import com.projectlense.api.entity.*;
import com.projectlense.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class AiEvaluationPersistenceService {
    private final AiEvaluationRepository evaluations;
    private final ProjectDataRepository projects;
    private final ProjectSubmissionRepository submissions;
    private final EvaluationRubricRepository rubrics;
    private final RubricCriterionRepository criteria;
    private final UserRepository users;

    public AiEvaluationPersistenceService(AiEvaluationRepository evaluations, ProjectDataRepository projects,
                                          ProjectSubmissionRepository submissions, EvaluationRubricRepository rubrics,
                                          RubricCriterionRepository criteria, UserRepository users) {
        this.evaluations = evaluations;
        this.projects = projects;
        this.submissions = submissions;
        this.rubrics = rubrics;
        this.criteria = criteria;
        this.users = users;
    }

    @Transactional
    public Integer pendingId(String projectId, Integer submissionId, Integer rubricId) {
        Optional<AiEvaluationEntity> latest = evaluations.findFirstBySubmission_IdAndRubric_IdOrderByEvaluatedAtDesc(submissionId, rubricId);
        if (latest.isPresent() && latest.get().getStatus() != EvaluationStatus.REJECTED) return latest.get().getId();
        AiEvaluationEntity entity = new AiEvaluationEntity();
        entity.setProject(projects.getReferenceById(projectId));
        entity.setSubmission(submissions.getReferenceById(submissionId));
        entity.setRubric(rubrics.getReferenceById(rubricId));
        entity.setStatus(EvaluationStatus.PENDING);
        entity.setTotalMarks(BigDecimal.ZERO);
        return evaluations.saveAndFlush(entity).getId();
    }

    @Transactional
    public Map<String, Object> complete(Integer evaluationId, Integer submissionId, BigDecimal total,
                                        String overallFeedback, List<CriterionResult> results) {
        AiEvaluationEntity evaluation = evaluations.findById(evaluationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI evaluation not found"));
        if (evaluation.getStatus() != EvaluationStatus.PENDING) return toMap(evaluation);
        for (CriterionResult result : results) {
            AiEvaluationDetailEntity detail = new AiEvaluationDetailEntity();
            detail.setCriterion(criteria.getReferenceById(result.criterionId()));
            detail.setAwardedMarks(result.awardedMarks());
            detail.setJustification(result.justification());
            detail.setImprovementSuggestions(result.improvementSuggestions());
            evaluation.addDetail(detail);
        }
        evaluation.setTotalMarks(total);
        evaluation.setOverallFeedback(overallFeedback);
        evaluation.setStatus(EvaluationStatus.COMPLETED);
        evaluation.getProject().setStatus("evaluated");
        ProjectSubmissionEntity submission = submissions.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project submission not found"));
        submission.setStatus(SubmissionStatus.COMPLETED);
        submissions.save(submission);
        return toMap(evaluations.saveAndFlush(evaluation));
    }

    @Transactional
    public void markSubmission(Integer submissionId, SubmissionStatus status) {
        ProjectSubmissionEntity submission = submissions.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project submission not found"));
        submission.setStatus(status);
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> latestForProject(String projectId) {
        return evaluations.findFirstByProject_IdOrderByEvaluatedAtDesc(projectId).map(AiEvaluationPersistenceService::toMap);
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> latestForSubmission(Integer submissionId, Integer rubricId) {
        return evaluations.findFirstBySubmission_IdAndRubric_IdOrderByEvaluatedAtDesc(submissionId, rubricId)
                .map(AiEvaluationPersistenceService::toMap);
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> get(Integer evaluationId) {
        return evaluations.findById(evaluationId).map(AiEvaluationPersistenceService::toMap);
    }

    @Transactional
    public Map<String, Object> decide(Integer evaluationId, String userId, boolean approve, String feedback) {
        AiEvaluationEntity evaluation = evaluations.findById(evaluationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI evaluation not found"));
        if (evaluation.getStatus() != EvaluationStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a completed AI recommendation can be approved or rejected");
        }
        UserEntity user = users.getReferenceById(userId);
        evaluation.setStatus(approve ? EvaluationStatus.APPROVED : EvaluationStatus.REJECTED);
        evaluation.getProject().setStatus(approve ? "approved" : "under_review");
        evaluation.setDecisionBy(user); evaluation.setDecisionAt(Instant.now());
        evaluation.setDecisionFeedback(feedback == null || feedback.isBlank() ? null : feedback.trim());
        if (approve) { evaluation.setApprovedBy(user); evaluation.setApprovedAt(Instant.now()); }
        return toMap(evaluations.saveAndFlush(evaluation));
    }

    private static Map<String, Object> toMap(AiEvaluationEntity evaluation) {
        List<Map<String, Object>> details = evaluation.getDetails().stream().map(detail -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("detailId", detail.getId()); row.put("criterionId", detail.getCriterion().getId());
            row.put("criterionName", detail.getCriterion().getName()); row.put("maximumMarks", detail.getCriterion().getMaxMarks());
            row.put("awardedMarks", detail.getAwardedMarks()); row.put("justification", detail.getJustification());
            row.put("improvementSuggestions", detail.getImprovementSuggestions());
            return row;
        }).toList();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("evaluationId", evaluation.getId()); out.put("projectId", evaluation.getProject().getId());
        out.put("submissionId", evaluation.getSubmission().getId()); out.put("submissionType", evaluation.getSubmission().getSubmissionType());
        out.put("rubricId", evaluation.getRubric().getId()); out.put("rubricName", evaluation.getRubric().getName());
        out.put("totalMarks", evaluation.getTotalMarks()); out.put("maximumMarks", evaluation.getRubric().getTotalMarks());
        out.put("overallFeedback", evaluation.getOverallFeedback()); out.put("status", evaluation.getStatus());
        out.put("evaluatedAt", evaluation.getEvaluatedAt());
        out.put("approvedBy", evaluation.getApprovedBy() == null ? null : Map.of("id", evaluation.getApprovedBy().getId(), "name", evaluation.getApprovedBy().getName()));
        out.put("approvedAt", evaluation.getApprovedAt());
        out.put("decisionBy", evaluation.getDecisionBy() == null ? null : Map.of("id", evaluation.getDecisionBy().getId(), "name", evaluation.getDecisionBy().getName()));
        out.put("decisionAt", evaluation.getDecisionAt()); out.put("decisionFeedback", evaluation.getDecisionFeedback());
        out.put("details", details);
        return out;
    }

    public record CriterionResult(Integer criterionId, BigDecimal awardedMarks, String justification, String improvementSuggestions) {}
}
