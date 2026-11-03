package com.projectlense.api.repository;

import com.projectlense.api.entity.RubricSubcriterionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RubricSubcriterionRepository extends JpaRepository<RubricSubcriterionEntity, Integer> {
    List<RubricSubcriterionEntity> findByCriterion_IdOrderByDisplayOrderAscIdAsc(Integer criterionId);
}
