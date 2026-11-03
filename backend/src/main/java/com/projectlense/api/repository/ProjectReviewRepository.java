package com.projectlense.api.repository;

import com.projectlense.api.entity.ProjectReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectReviewRepository extends JpaRepository<ProjectReviewEntity, Integer> {
    List<ProjectReviewEntity> findByProject_Id(String projectId);
    Optional<ProjectReviewEntity> findByIdAndProject_Id(Integer id, String projectId);
    Optional<ProjectReviewEntity> findByProject_IdAndStage_Id(String projectId, Integer stageId);
    boolean existsByProject_IdAndStage_Id(String projectId, Integer stageId);
}
