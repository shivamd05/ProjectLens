package com.projectlense.api.repository;

import com.projectlense.api.entity.ProjectEmbeddingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectEmbeddingRepository extends JpaRepository<ProjectEmbeddingEntity, String> {
    List<ProjectEmbeddingEntity> findAllByStatusIn(List<String> statuses);
}
