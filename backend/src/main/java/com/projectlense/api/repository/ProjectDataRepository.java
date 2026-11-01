package com.projectlense.api.repository;

import com.projectlense.api.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectDataRepository extends JpaRepository<ProjectEntity, String> {
    List<ProjectEntity> findAllByOrderByCreatedAtDesc();
    Optional<ProjectEntity> findByNormalizedTitleAndAcademicYear(String normalizedTitle, String academicYear);
    boolean existsByNormalizedTitleAndAcademicYear(String normalizedTitle, String academicYear);
}
