package com.projectlense.api.repository;

import com.projectlense.api.entity.ImportJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ImportJobRepository extends JpaRepository<ImportJobEntity, String> {}
