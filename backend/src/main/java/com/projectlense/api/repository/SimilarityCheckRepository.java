package com.projectlense.api.repository;

import com.projectlense.api.entity.SimilarityCheckEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SimilarityCheckRepository extends JpaRepository<SimilarityCheckEntity, String> {}
