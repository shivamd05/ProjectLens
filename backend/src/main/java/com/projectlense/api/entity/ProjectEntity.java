package com.projectlense.api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
public class ProjectEntity {
    @Id @Column(length = 64) private String id;
    @Column(nullable = false, length = 500) private String title;
    @Column(name = "normalized_title", nullable = false, length = 500) private String normalizedTitle;
    @Lob private String description;
    @Column(name = "problem_statement", columnDefinition = "LONGTEXT") private String problemStatement;
    @Column(columnDefinition = "LONGTEXT") private String objectives;
    @Column(columnDefinition = "TEXT") private String technologies;
    @Column(name = "team_name") private String teamName;
    @Column(name = "team_leader") private String teamLeader;
    @Column(name = "group_number", length = 50) private String groupNumber;
    @Column(name = "faculty_coordinator") private String facultyCoordinator;
    private String department;
    @Column(name = "academic_year", nullable = false, length = 50) private String academicYear;
    @Column(nullable = false, length = 32) private String status;
    @Column(name = "source_file_name") private String sourceFileName;
    @Column(name = "source_file_hash", length = 64) private String sourceFileHash;
    @Column(name = "source_sheet", length = 100) private String sourceSheet;
    @Column(name = "source_row") private Integer sourceRow;
    @Column(name = "api_payload_json", columnDefinition = "LONGTEXT") private String apiPayloadJson;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMemberEntity> members = new ArrayList<>();

    public ProjectEntity() {}
    @PrePersist void prePersist() { if (createdAt == null) createdAt = Instant.now(); }
    public void addMember(ProjectMemberEntity member) { members.add(member); member.setProject(this); }
    public String getId() { return id; } public void setId(String v) { id = v; }
    public String getTitle() { return title; } public void setTitle(String v) { title = v; }
    public String getNormalizedTitle() { return normalizedTitle; } public void setNormalizedTitle(String v) { normalizedTitle = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public String getProblemStatement() { return problemStatement; } public void setProblemStatement(String v) { problemStatement = v; }
    public String getObjectives() { return objectives; } public void setObjectives(String v) { objectives = v; }
    public String getTechnologies() { return technologies; } public void setTechnologies(String v) { technologies = v; }
    public String getTeamName() { return teamName; } public void setTeamName(String v) { teamName = v; }
    public String getTeamLeader() { return teamLeader; } public void setTeamLeader(String v) { teamLeader = v; }
    public String getGroupNumber() { return groupNumber; } public void setGroupNumber(String v) { groupNumber = v; }
    public String getFacultyCoordinator() { return facultyCoordinator; } public void setFacultyCoordinator(String v) { facultyCoordinator = v; }
    public String getDepartment() { return department; } public void setDepartment(String v) { department = v; }
    public String getAcademicYear() { return academicYear; } public void setAcademicYear(String v) { academicYear = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public String getSourceFileName() { return sourceFileName; } public void setSourceFileName(String v) { sourceFileName = v; }
    public String getSourceFileHash() { return sourceFileHash; } public void setSourceFileHash(String v) { sourceFileHash = v; }
    public String getSourceSheet() { return sourceSheet; } public void setSourceSheet(String v) { sourceSheet = v; }
    public Integer getSourceRow() { return sourceRow; } public void setSourceRow(Integer v) { sourceRow = v; }
    public String getApiPayloadJson() { return apiPayloadJson; } public void setApiPayloadJson(String v) { apiPayloadJson = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
    public List<ProjectMemberEntity> getMembers() { return members; }
}
