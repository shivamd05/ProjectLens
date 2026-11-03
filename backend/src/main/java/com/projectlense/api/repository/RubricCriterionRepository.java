package com.projectlense.api.repository;

import com.projectlense.api.entity.RubricCriterionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RubricCriterionRepository extends JpaRepository<RubricCriterionEntity, Integer> {
    List<RubricCriterionEntity> findByRubric_IdOrderByDisplayOrderAscIdAsc(Integer rubricId);
}
