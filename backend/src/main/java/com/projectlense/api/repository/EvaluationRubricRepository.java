package com.projectlense.api.repository;

import com.projectlense.api.entity.EvaluationRubricEntity;
import com.projectlense.api.entity.SubmissionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRubricRepository extends JpaRepository<EvaluationRubricEntity, Integer> {
    List<EvaluationRubricEntity> findAllByOrderBySubmissionTypeAscVersionDesc();
    List<EvaluationRubricEntity> findBySubmissionTypeOrderByVersionDesc(SubmissionType submissionType);
    List<EvaluationRubricEntity> findBySubmissionTypeAndActiveTrueOrderByVersionDesc(SubmissionType submissionType);
    Optional<EvaluationRubricEntity> findFirstBySubmissionTypeOrderByVersionDesc(SubmissionType submissionType);
}
