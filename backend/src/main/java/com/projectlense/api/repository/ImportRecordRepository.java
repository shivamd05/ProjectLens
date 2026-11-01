package com.projectlense.api.repository;

import com.projectlense.api.entity.ImportRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ImportRecordRepository extends JpaRepository<ImportRecordEntity, String> {
    List<ImportRecordEntity> findAllByJob_IdOrderBySourceRowAsc(String jobId);
    long countByJob_IdAndStatus(String jobId, String status);
}
