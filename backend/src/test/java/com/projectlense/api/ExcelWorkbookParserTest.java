package com.projectlense.api;

import com.projectlense.api.service.ExcelWorkbookParser;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ExcelWorkbookParserTest {
    private final ExcelWorkbookParser parser = new ExcelWorkbookParser();

    @Test
    void parsesMergedRegistrationRowsAndPreservesMissingDetails() throws Exception {
        byte[] workbook;
        try (XSSFWorkbook xlsx = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = xlsx.createSheet("Project Registration List");
            sheet.createRow(0).createCell(0).setCellValue("Department of Computer Engineering");
            sheet.createRow(1).createCell(0).setCellValue("Academic Year: 2026-2027");
            var header = sheet.createRow(3);
            String[] labels = {"Group No.", "Roll No.", "Student Name", "Guide Name", "Title of Project"};
            for (int i = 0; i < labels.length; i++) header.createCell(i).setCellValue(labels[i]);
            sheet.createRow(4).createCell(0).setCellValue("G01");
            sheet.getRow(4).createCell(1).setCellValue("001234");
            sheet.getRow(4).createCell(2).setCellValue("Student One");
            sheet.getRow(4).createCell(3).setCellValue("Faculty One");
            sheet.getRow(4).createCell(4).setCellValue("Project One");
            sheet.createRow(5).createCell(1).setCellValue("001235");
            sheet.getRow(5).createCell(2).setCellValue("Student Two");
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(4, 5, 0, 0));
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(4, 5, 3, 3));
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(4, 5, 4, 4));
            xlsx.write(out); workbook = out.toByteArray();
        }
        var result = parser.parse(new ByteArrayInputStream(workbook));
        assertEquals(1, result.records().size());
        var row = result.records().get(0);
        assertEquals("001234", row.members().get(0).studentId());
        assertEquals(2, row.members().size());
        assertEquals("2026-2027", row.academicYear());
        assertNull(row.description());
        assertNull(row.problemStatement());
    }

    @Test
    void parsesProvidedWorkbookOnceWithoutGuideCopiesWhenConfigured() throws Exception {
        String path = System.getenv("PROJECTLENSE_SAMPLE_WORKBOOK");
        if (path == null || path.isBlank()) return;
        var result = parser.parse(Files.newInputStream(Path.of(path)));
        assertEquals(54, result.records().size());
        assertEquals(211, result.records().stream().mapToInt(r -> r.members().size()).sum());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("duplicates projects")));
    }
}
