package com.projectlense.api.service;

import com.projectlense.api.dto.RubricDtos;
import com.projectlense.api.entity.*;
import com.projectlense.api.repository.AiEvaluationRepository;
import com.projectlense.api.repository.EvaluationRubricRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
public class RubricService {
    private final EvaluationRubricRepository rubrics;
    private final AiEvaluationRepository evaluations;

    public RubricService(EvaluationRubricRepository rubrics, AiEvaluationRepository evaluations) {
        this.rubrics = rubrics;
        this.evaluations = evaluations;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return rubrics.findAllByOrderBySubmissionTypeAscVersionDesc().stream().map(RubricService::toMap).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Integer id) { return toMap(require(id)); }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> byType(SubmissionType type) {
        return rubrics.findBySubmissionTypeOrderByVersionDesc(type).stream().map(RubricService::toMap).toList();
    }

    @Transactional
    public Map<String, Object> create(RubricDtos.RubricRequest request) {
        validate(request);
        int nextVersion = rubrics.findFirstBySubmissionTypeOrderByVersionDesc(request.submissionType())
                .map(r -> r.getVersion() + 1).orElse(1);
        EvaluationRubricEntity rubric = new EvaluationRubricEntity();
        rubric.setName(request.name().trim());
        rubric.setSubmissionType(request.submissionType());
        rubric.setDescription(request.description().trim());
        rubric.setTotalMarks(request.totalMarks());
        rubric.setVersion(nextVersion);
        rubric.setActive(request.active() == null || request.active());
        replaceCriteria(rubric, request.criteria());
        if (rubric.isActive()) deactivateOtherActive(rubric.getSubmissionType(), null);
        return toMap(rubrics.saveAndFlush(rubric));
    }

    @Transactional
    public Map<String, Object> update(Integer id, RubricDtos.RubricRequest request) {
        validate(request);
        EvaluationRubricEntity rubric = require(id);
        if (request.submissionType() != rubric.getSubmissionType()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Create a new rubric version to change submission type");
        }
        boolean hasEvaluations = evaluations.existsByRubric_IdAndStatusIn(id, List.of(EvaluationStatus.PENDING,
                EvaluationStatus.COMPLETED, EvaluationStatus.APPROVED, EvaluationStatus.REJECTED));
        if (hasEvaluations && !sameContent(rubric, request)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This rubric version has been used by an evaluation; create a new version to change its content");
        }
        if (!hasEvaluations) {
            rubric.setName(request.name().trim());
            rubric.setDescription(request.description().trim());
            rubric.setTotalMarks(request.totalMarks());
            rubric.clearCriteria();
            rubrics.flush();
            replaceCriteria(rubric, request.criteria());
        }
        if (request.active() != null) rubric.setActive(request.active());
        if (rubric.isActive()) deactivateOtherActive(rubric.getSubmissionType(), rubric.getId());
        return toMap(rubrics.saveAndFlush(rubric));
    }

    @Transactional(readOnly = true)
    public EvaluationRubricEntity activeFor(SubmissionType type) {
        EvaluationRubricEntity rubric = rubrics.findBySubmissionTypeAndActiveTrueOrderByVersionDesc(type).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No active rubric is configured for " + type));
        rubric.getCriteria().forEach(criterion -> criterion.getSubcriteria().size());
        return rubric;
    }

    private EvaluationRubricEntity require(Integer id) {
        return rubrics.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rubric not found"));
    }

    private void deactivateOtherActive(SubmissionType type, Integer keepId) {
        for (EvaluationRubricEntity other : rubrics.findBySubmissionTypeAndActiveTrueOrderByVersionDesc(type)) {
            if (!Objects.equals(other.getId(), keepId)) other.setActive(false);
        }
    }

    private static void validate(RubricDtos.RubricRequest request) {
        Set<String> names = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (RubricDtos.CriterionRequest criterion : request.criteria()) {
            if (criterion.maxMarks().signum() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Criterion maximum marks must be positive");
            if (!names.add(criterion.name().trim().toLowerCase(Locale.ROOT))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Criterion names must be unique within a rubric");
            }
            total = total.add(criterion.maxMarks());
            if (criterion.subcriteria() != null && !criterion.subcriteria().isEmpty()) {
                Set<String> subNames = new HashSet<>();
                BigDecimal subTotal = BigDecimal.ZERO;
                for (RubricDtos.SubcriterionRequest sub : criterion.subcriteria()) {
                    if (sub.maxMarks().signum() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subcriterion maximum marks must be positive");
                    if (!subNames.add(sub.name().trim().toLowerCase(Locale.ROOT))) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subcriterion names must be unique within a criterion");
                    }
                    subTotal = subTotal.add(sub.maxMarks());
                }
                if (subTotal.compareTo(criterion.maxMarks()) != 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subcriterion maximum marks must sum to the parent criterion maximum");
                }
            }
        }
        if (total.compareTo(request.totalMarks()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Criterion maximum marks must sum to the rubric total");
        }
    }

    private static void replaceCriteria(EvaluationRubricEntity rubric, List<RubricDtos.CriterionRequest> requests) {
        rubric.clearCriteria();
        for (int i = 0; i < requests.size(); i++) {
            RubricDtos.CriterionRequest input = requests.get(i);
            RubricCriterionEntity criterion = new RubricCriterionEntity();
            criterion.setName(input.name().trim());
            criterion.setDescription(input.description().trim());
            criterion.setMaxMarks(input.maxMarks());
            criterion.setDisplayOrder(i + 1);
            rubric.addCriterion(criterion);
            if (input.subcriteria() != null) {
                for (int j = 0; j < input.subcriteria().size(); j++) {
                    RubricDtos.SubcriterionRequest subInput = input.subcriteria().get(j);
                    RubricSubcriterionEntity sub = new RubricSubcriterionEntity();
                    sub.setName(subInput.name().trim());
                    sub.setDescription(subInput.description().trim());
                    sub.setMaxMarks(subInput.maxMarks());
                    sub.setDisplayOrder(j + 1);
                    criterion.addSubcriterion(sub);
                }
            }
        }
    }

    private static boolean sameContent(EvaluationRubricEntity rubric, RubricDtos.RubricRequest request) {
        if (!Objects.equals(rubric.getName(), request.name().trim())
                || !Objects.equals(rubric.getDescription(), request.description().trim())
                || rubric.getTotalMarks().compareTo(request.totalMarks()) != 0
                || rubric.getCriteria().size() != request.criteria().size()) return false;
        for (int i = 0; i < request.criteria().size(); i++) {
            RubricCriterionEntity saved = rubric.getCriteria().get(i);
            RubricDtos.CriterionRequest wanted = request.criteria().get(i);
            if (!Objects.equals(saved.getName(), wanted.name().trim())
                    || !Objects.equals(saved.getDescription(), wanted.description().trim())
                    || saved.getMaxMarks().compareTo(wanted.maxMarks()) != 0
                    || saved.getSubcriteria().size() != (wanted.subcriteria() == null ? 0 : wanted.subcriteria().size())) return false;
            for (int j = 0; j < saved.getSubcriteria().size(); j++) {
                RubricSubcriterionEntity savedSub = saved.getSubcriteria().get(j);
                RubricDtos.SubcriterionRequest wantedSub = wanted.subcriteria().get(j);
                if (!Objects.equals(savedSub.getName(), wantedSub.name().trim())
                        || !Objects.equals(savedSub.getDescription(), wantedSub.description().trim())
                        || savedSub.getMaxMarks().compareTo(wantedSub.maxMarks()) != 0) return false;
            }
        }
        return true;
    }

    private static Map<String, Object> toMap(EvaluationRubricEntity rubric) {
        List<Map<String, Object>> criteria = rubric.getCriteria().stream().map(c -> {
            List<Map<String, Object>> subs = c.getSubcriteria().stream().map(s -> Map.<String, Object>of(
                    "id", s.getId(), "name", s.getName(), "description", s.getDescription(), "maxMarks", s.getMaxMarks())).toList();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", c.getId()); row.put("name", c.getName()); row.put("description", c.getDescription());
            row.put("maxMarks", c.getMaxMarks()); row.put("displayOrder", c.getDisplayOrder()); row.put("subcriteria", subs);
            return row;
        }).toList();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", rubric.getId()); out.put("name", rubric.getName());
        out.put("submissionType", rubric.getSubmissionType()); out.put("description", rubric.getDescription());
        out.put("totalMarks", rubric.getTotalMarks()); out.put("version", rubric.getVersion());
        out.put("active", rubric.isActive()); out.put("createdAt", rubric.getCreatedAt());
        out.put("updatedAt", rubric.getUpdatedAt()); out.put("criteria", criteria);
        return out;
    }
}
