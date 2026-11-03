package com.projectlense.api.repository;

import com.projectlense.api.entity.AiEvaluationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiEvaluationRepository extends JpaRepository<AiEvaluationEntity, Integer> {
    Optional<AiEvaluationEntity> findFirstByProject_IdOrderByEvaluatedAtDesc(String projectId);
    Optional<AiEvaluationEntity> findFirstBySubmission_IdAndRubric_IdOrderByEvaluatedAtDesc(Integer submissionId, Integer rubricId);
    Optional<AiEvaluationEntity> findByIdAndProject_Id(Integer id, String projectId);
    List<AiEvaluationEntity> findByRubric_IdAndStatusIn(Integer rubricId, List<com.projectlense.api.entity.EvaluationStatus> statuses);
    boolean existsByRubric_IdAndStatusIn(Integer rubricId, List<com.projectlense.api.entity.EvaluationStatus> statuses);
}
