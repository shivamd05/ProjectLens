package com.projectlense.api.dto;

import java.util.List;

public record ImportPreviewRecord(
        String recordId,
        String worksheet,
        int sourceRow,
        String groupNumber,
        String title,
        String description,
        String problemStatement,
        String objectives,
        String technologies,
        String teamName,
        String teamLeader,
        List<ImportMemberDto> members,
        String facultyCoordinator,
        String department,
        String academicYear,
        List<String> validationErrors,
        List<String> duplicateWarnings,
        boolean valid,
        boolean duplicate) {
    public ImportPreviewRecord withReview(List<String> errors, List<String> warnings, boolean isValid, boolean isDuplicate) {
        return new ImportPreviewRecord(recordId, worksheet, sourceRow, groupNumber, title, description,
                problemStatement, objectives, technologies, teamName, teamLeader, members, facultyCoordinator,
                department, academicYear, errors, warnings, isValid, isDuplicate);
    }
    public ImportPreviewRecord withCorrection(ImportRecordCorrection c) {
        return new ImportPreviewRecord(recordId, worksheet, sourceRow,
                c.groupNumber() == null ? groupNumber : c.groupNumber(), c.title() == null ? title : c.title(),
                c.description() == null ? description : c.description(),
                c.problemStatement() == null ? problemStatement : c.problemStatement(),
                c.objectives() == null ? objectives : c.objectives(), c.technologies() == null ? technologies : c.technologies(),
                c.teamName() == null ? teamName : c.teamName(), c.teamLeader() == null ? teamLeader : c.teamLeader(),
                c.members() == null ? members : c.members(),
                c.facultyCoordinator() == null ? facultyCoordinator : c.facultyCoordinator(),
                c.department() == null ? department : c.department(), c.academicYear() == null ? academicYear : c.academicYear(),
                validationErrors, duplicateWarnings, valid, duplicate);
    }
}
