package com.projectlense.api.repository;

import com.projectlense.api.entity.ProjectSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectSubmissionRepository extends JpaRepository<ProjectSubmissionEntity, Integer> {
    List<ProjectSubmissionEntity> findByProject_IdOrderBySubmissionDateDesc(String projectId);
    Optional<ProjectSubmissionEntity> findFirstByProject_IdOrderBySubmissionDateDesc(String projectId);
    Optional<ProjectSubmissionEntity> findByIdAndProject_Id(Integer id, String projectId);
}
