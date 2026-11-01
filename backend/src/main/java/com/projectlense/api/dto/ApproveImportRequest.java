package com.projectlense.api.dto;

import java.util.Map;

public record ApproveImportRequest(Map<String, ImportRecordCorrection> corrections) {}
