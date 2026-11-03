package com.projectlense.api.service;

import com.projectlense.api.dto.ProjectWorkflowDtos;
import com.projectlense.api.entity.ProjectEntity;
import com.projectlense.api.entity.ProjectReviewEntity;
import com.projectlense.api.entity.ProjectReviewStageEntity;
import com.projectlense.api.entity.UserEntity;
import com.projectlense.api.repository.ProjectReviewRepository;
import com.projectlense.api.repository.ProjectReviewStageRepository;
import com.projectlense.api.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@Service
public class ProjectReviewService {
    private static final Set<String> REQUIRED_STAGES = Set.of("Project Review 1", "Project Review 2", "Project Review 3");
    private final ProjectReviewRepository reviews;
    private final ProjectReviewStageRepository stages;
    private final ProjectAccessService access;
    private final UserRepository users;

    public ProjectReviewService(ProjectReviewRepository reviews, ProjectReviewStageRepository stages,
                                ProjectAccessService access, UserRepository users) {
        this.reviews = reviews;
        this.stages = stages;
        this.access = access;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String projectId, UserEntity user) {
        access.requireView(projectId, user);
        Map<Integer, ProjectReviewEntity> existing = new HashMap<>();
        for (ProjectReviewEntity review : reviews.findByProject_Id(projectId)) existing.put(review.getStage().getId(), review);
        return stages.findAllByOrderByIdAsc().stream().map(stage -> {
            ProjectReviewEntity review = existing.get(stage.getId());
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", stage.getId()); out.put("name", stage.getName()); out.put("maxMarks", stage.getMaxMarks());
            out.put("completed", review != null);
            out.put("recordId", review == null ? null : review.getId());
            out.put("marksObtained", review == null ? null : review.getMarksObtained());
            out.put("feedback", review == null ? null : review.getFeedback());
            out.put("reviewDate", review == null ? null : review.getReviewDate());
            out.put("facultyId", review == null ? null : review.getFaculty().getId());
            out.put("facultyName", review == null ? null : review.getFaculty().getName());
            return out;
        }).toList();
    }

    @Transactional
    public Map<String, Object> create(String projectId, ProjectWorkflowDtos.ReviewMarkRequest request, UserEntity user) {
        ProjectEntity project = access.requireFacultyGuideOrAdmin(projectId, user);
        ProjectReviewStageEntity stage = requireStage(request.reviewId());
        validateMarks(request.marksObtained(), stage);
        if (reviews.existsByProject_IdAndStage_Id(projectId, stage.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A review has already been recorded for this project stage");
        }
        ProjectReviewEntity review = new ProjectReviewEntity();
        review.setProject(project); review.setStage(stage); review.setFaculty(requireUser(user.getId()));
        review.setMarksObtained(request.marksObtained()); review.setFeedback(trimToNull(request.feedback()));
        review.setReviewDate(request.reviewDate() == null ? LocalDate.now() : request.reviewDate());
        try { return toMap(reviews.saveAndFlush(review)); }
        catch (DataIntegrityViolationException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, "A review has already been recorded for this project stage"); }
    }

    @Transactional
    public Map<String, Object> update(String projectId, Integer recordId,
                                     ProjectWorkflowDtos.ReviewMarkRequest request, UserEntity user) {
        access.requireFacultyGuideOrAdmin(projectId, user);
        ProjectReviewEntity review = reviews.findByIdAndProject_Id(recordId, projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project review not found"));
        if (!ProjectAccessService.isAdmin(user) && !review.getFaculty().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the faculty member who entered this review or an administrator can modify it");
        }
        if (!review.getStage().getId().equals(request.reviewId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A review cannot be moved to another stage");
        }
        validateMarks(request.marksObtained(), review.getStage());
        review.setMarksObtained(request.marksObtained()); review.setFeedback(trimToNull(request.feedback()));
        if (request.reviewDate() != null) review.setReviewDate(request.reviewDate());
        return toMap(reviews.save(review));
    }

    @Transactional(readOnly = true)
    public boolean requiredStagesComplete(String projectId) {
        Set<String> done = new HashSet<>();
        for (ProjectReviewEntity review : reviews.findByProject_Id(projectId)) done.add(review.getStage().getName());
        return done.containsAll(REQUIRED_STAGES);
    }

    private ProjectReviewStageEntity requireStage(Integer id) {
        return stages.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown project review stage"));
    }

    private UserEntity requireUser(String id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user account no longer exists"));
    }

    private static void validateMarks(java.math.BigDecimal marks, ProjectReviewStageEntity stage) {
        if (marks.signum() < 0 || marks.compareTo(stage.getMaxMarks()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Marks must be between 0 and " + stage.getMaxMarks());
        }
    }

    private static Map<String, Object> toMap(ProjectReviewEntity review) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("recordId", review.getId()); out.put("projectId", review.getProject().getId());
        out.put("reviewId", review.getStage().getId()); out.put("reviewName", review.getStage().getName());
        out.put("maxMarks", review.getStage().getMaxMarks()); out.put("facultyId", review.getFaculty().getId());
        out.put("facultyName", review.getFaculty().getName()); out.put("marksObtained", review.getMarksObtained());
        out.put("feedback", review.getFeedback()); out.put("reviewDate", review.getReviewDate());
        out.put("createdAt", review.getCreatedAt()); out.put("updatedAt", review.getUpdatedAt());
        return out;
    }

    private static String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
