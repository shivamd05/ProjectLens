package com.projectlense.api.repository;

import com.projectlense.api.entity.ProjectReviewStageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectReviewStageRepository extends JpaRepository<ProjectReviewStageEntity, Integer> {
    List<ProjectReviewStageEntity> findAllByOrderByIdAsc();
}
