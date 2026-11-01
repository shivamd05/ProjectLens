package com.projectlense.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "project_members", uniqueConstraints = @UniqueConstraint(name = "uq_project_member", columnNames = {"project_id", "student_id"}))
public class ProjectMemberEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private ProjectEntity project;
    @Column(name = "student_id", nullable = false, length = 100) private String studentId;
    @Column(name = "student_name", nullable = false, length = 255) private String studentName;
    @Column(name = "source_row") private Integer sourceRow;
    protected ProjectMemberEntity() {}
    public ProjectMemberEntity(String studentId, String studentName, Integer sourceRow) { this.studentId = studentId; this.studentName = studentName; this.sourceRow = sourceRow; }
    public Long getId() { return id; } public ProjectEntity getProject() { return project; }
    public void setProject(ProjectEntity project) { this.project = project; }
    public String getStudentId() { return studentId; } public String getStudentName() { return studentName; }
    public Integer getSourceRow() { return sourceRow; }
}
