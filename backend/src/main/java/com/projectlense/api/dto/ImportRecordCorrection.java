package com.projectlense.api.dto;

import java.util.List;

public record ImportRecordCorrection(String groupNumber, String title, String description,
        String problemStatement, String objectives, String technologies, String teamName, String teamLeader,
        List<ImportMemberDto> members, String facultyCoordinator, String department, String academicYear) {}
