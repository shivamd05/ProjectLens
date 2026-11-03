package com.projectlense.api.repository;

import com.projectlense.api.entity.ProjectMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMemberEntity, Long> {
    boolean existsByProject_IdAndStudentId(String projectId, String studentId);
}
