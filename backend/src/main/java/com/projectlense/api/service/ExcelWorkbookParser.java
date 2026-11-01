package com.projectlense.api.service;

import com.projectlense.api.dto.ImportMemberDto;
import com.projectlense.api.dto.ImportPreviewRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExcelWorkbookParser {
    private static final Pattern YEAR = Pattern.compile("(?i)academic\\s+year\\s*[:\\-]?\\s*((?:19|20)\\d{2}\\s*[-/]\\s*(?:19|20)\\d{2})");
    private static final Pattern DEPARTMENT = Pattern.compile("(?i)department\\s+of\\s+(.+?)(?=\\s+academic\\s+year|[()|]|$)");

    public record ParsedWorkbook(List<ImportPreviewRecord> records, List<String> warnings) {}
    private record Header(int row, Map<String, Integer> columns, List<String> errors) {}
    private record SheetData(String name, List<ImportPreviewRecord> records, List<String> warnings) {}

    public ParsedWorkbook parse(InputStream stream) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(stream)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            String department = null, academicYear = null;
            List<SheetData> candidates = new ArrayList<>();
            for (Sheet sheet : workbook) {
                String meta = firstRowsText(sheet, formatter, evaluator, 8);
                if (department == null) department = extract(DEPARTMENT, meta);
                if (academicYear == null) academicYear = normalizeYear(extract(YEAR, meta));
                Header header = findHeader(sheet, formatter, evaluator);
                if (header != null) candidates.add(parseSheet(sheet, header, formatter, evaluator, department, academicYear));
            }
            if (candidates.isEmpty()) throw new IllegalArgumentException("No worksheet has the required group, student identifier, student name, and project title columns.");

            candidates.sort(Comparator.comparingInt((SheetData s) -> s.records().size()).reversed());
            SheetData primary = candidates.get(0);
            List<ImportPreviewRecord> combined = new ArrayList<>(primary.records());
            Set<String> primarySignatures = new HashSet<>();
            for (ImportPreviewRecord r : primary.records()) primarySignatures.add(signature(r));
            List<String> warnings = new ArrayList<>(primary.warnings());
            for (int i = 1; i < candidates.size(); i++) {
                SheetData candidate = candidates.get(i);
                boolean copy = !candidate.records().isEmpty() && candidate.records().stream().allMatch(r -> primarySignatures.contains(signature(r)));
                if (copy) warnings.add("Worksheet '" + candidate.name() + "' duplicates projects already present in '" + primary.name() + "' and was skipped.");
                else combined.addAll(candidate.records());
                warnings.addAll(candidate.warnings());
            }

            String defaultDepartment = department == null || department.isBlank() ? null : clean(department);
            String defaultYear = academicYear;
            List<ImportPreviewRecord> finalized = new ArrayList<>();
            for (ImportPreviewRecord r : combined) {
                String guide = blank(r.facultyCoordinator()) ? guideFromSheet(r.worksheet()) : r.facultyCoordinator();
                List<String> errors = new ArrayList<>();
                List<String> itemWarnings = new ArrayList<>();
                if (blank(r.title())) errors.add("Project title is missing.");
                if (r.members().isEmpty()) errors.add("No student members were found for this group.");
                for (ImportMemberDto m : r.members()) {
                    if (blank(m.studentId())) errors.add("A team member is missing a student identifier (source row " + m.sourceRow() + ").");
                    if (blank(m.studentName())) errors.add("A team member is missing a student name (source row " + m.sourceRow() + ").");
                }
                if (blank(guide)) itemWarnings.add("Faculty coordinator could not be read from the worksheet.");
                if (blank(defaultDepartment)) itemWarnings.add("Department was not present in workbook headings.");
                if (blank(defaultYear)) itemWarnings.add("Academic year was not present in workbook headings.");
                finalized.add(new ImportPreviewRecord(UUID.randomUUID().toString(), r.worksheet(), r.sourceRow(), r.groupNumber(),
                        clean(r.title()), r.description(), r.problemStatement(), r.objectives(), r.technologies(),
                        blank(r.teamName()) && !blank(r.groupNumber()) ? "Group " + r.groupNumber() : r.teamName(), r.teamLeader(),
                        r.members(), guide, blank(r.department()) ? defaultDepartment : r.department(),
                        blank(r.academicYear()) ? defaultYear : r.academicYear(), errors, itemWarnings, errors.isEmpty(), false));
            }
            addWorkbookDuplicateWarnings(finalized);
            return new ParsedWorkbook(finalized, warnings);
        }
    }

    private SheetData parseSheet(Sheet sheet, Header header, DataFormatter formatter, FormulaEvaluator evaluator,
                                 String department, String year) {
        List<ImportPreviewRecord> records = new ArrayList<>();
        List<String> warnings = new ArrayList<>(header.errors());
        Map<CellKey, CellKey> merged = new HashMap<>();
        for (CellRangeAddress range : sheet.getMergedRegions()) {
            CellKey anchor = new CellKey(range.getFirstRow(), range.getFirstColumn());
            for (int r = range.getFirstRow(); r <= range.getLastRow(); r++)
                for (int c = range.getFirstColumn(); c <= range.getLastColumn(); c++) merged.put(new CellKey(r,c), anchor);
        }
        MutableRecord current = null;
        int lastRow = sheet.getLastRowNum();
        for (int rowIndex = header.row() + 1; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            String group = value(sheet,rowIndex,header.columns().get("group"),merged,formatter,evaluator);
            String title = value(sheet,rowIndex,header.columns().get("title"),merged,formatter,evaluator);
            String studentId = value(sheet,rowIndex,header.columns().get("student"),merged,formatter,evaluator);
            String studentName = value(sheet,rowIndex,header.columns().get("name"),merged,formatter,evaluator);
            String faculty = value(sheet,rowIndex,header.columns().get("guide"),merged,formatter,evaluator);
            String description = value(sheet,rowIndex,header.columns().get("description"),merged,formatter,evaluator);
            String problem = value(sheet,rowIndex,header.columns().get("problem"),merged,formatter,evaluator);
            String objectives = value(sheet,rowIndex,header.columns().get("objectives"),merged,formatter,evaluator);
            String technologies = value(sheet,rowIndex,header.columns().get("technologies"),merged,formatter,evaluator);
            String team = value(sheet,rowIndex,header.columns().get("team"),merged,formatter,evaluator);
            String leader = value(sheet,rowIndex,header.columns().get("leader"),merged,formatter,evaluator);
            String rowDepartment = value(sheet,rowIndex,header.columns().get("department"),merged,formatter,evaluator);
            String rowYear = value(sheet,rowIndex,header.columns().get("year"),merged,formatter,evaluator);
            if (blank(group) && current != null && blank(title)) group = current.groupNumber;
            String grouping = !blank(group) ? "g:" + normalize(group) : (!blank(title) ? "t:" + normalize(title) : null);
            if (grouping == null && blank(studentId) && blank(studentName)) {
                if (current != null) { records.add(current.finish()); current = null; }
                continue;
            }
            if (current != null && grouping != null && !current.groupKey.equals(grouping)) {
                records.add(current.finish()); current = null;
            }
            if (current == null) current = new MutableRecord(sheet.getSheetName(), rowIndex + 1, group, title, faculty);
            if (blank(current.title) && !blank(title)) current.title = title;
            if (blank(current.faculty) && !blank(faculty)) current.faculty = faculty;
            if (blank(current.description) && !blank(description)) current.description=description;
            if (blank(current.problemStatement) && !blank(problem)) current.problemStatement=problem;
            if (blank(current.objectives) && !blank(objectives)) current.objectives=objectives;
            if (blank(current.technologies) && !blank(technologies)) current.technologies=technologies;
            if (blank(current.teamName) && !blank(team)) current.teamName=team;
            if (blank(current.teamLeader) && !blank(leader)) current.teamLeader=leader;
            if (blank(current.department) && !blank(rowDepartment)) current.department=rowDepartment;
            if (blank(current.academicYear) && !blank(rowYear)) current.academicYear=normalizeYear(rowYear);
            if (!blank(studentId) || !blank(studentName)) current.members.add(new ImportMemberDto(studentId, studentName, rowIndex + 1));
            if (blank(current.groupNumber) && !blank(group)) current.groupNumber = group;
            current.groupKey = grouping == null ? current.groupKey : grouping;
        }
        if (current != null) records.add(current.finish());
        records.removeIf(r -> blank(r.title()) && r.members().isEmpty());
        if (!records.isEmpty()) warnings.add("Worksheet '" + sheet.getSheetName() + "' was parsed as a project registration table.");
        return new SheetData(sheet.getSheetName(), records, warnings);
    }

    private Header findHeader(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        int max = Math.min(sheet.getLastRowNum(), 15);
        for (int r = 0; r <= max; r++) {
            Row row = sheet.getRow(r); if (row == null) continue;
            Map<String,Integer> columns = new HashMap<>(); List<String> duplicates = new ArrayList<>();
            for (int c = 0; c < Math.min(row.getLastCellNum(), 40); c++) {
                String raw = formatter.formatCellValue(row.getCell(c), evaluator).strip();
                if (raw.isBlank()) continue;
                String key = canonicalHeader(raw);
                String field = switch (key) {
                    case "groupno", "groupnumber", "teamno", "group" -> "group";
                    case "rollno", "studentid", "rollnumber", "studentrollno", "enrollmentno", "enrollmentnumber" -> "student";
                    case "studentname", "nameofstudent", "membername" -> "name";
                    case "titleofproject", "beprojecttopic", "projecttitle", "projecttopic", "title" -> "title";
                    case "guidename", "facultycoordinator", "projectguide", "guide" -> "guide";
                    case "abstract", "projectidea", "overview", "description" -> "description";
                    case "problemstatement", "problemdefinition" -> "problem";
                    case "objectives", "projectobjectives" -> "objectives";
                    case "technologies", "technology", "techstack", "proposedtechnologies" -> "technologies";
                    case "teamname" -> "team";
                    case "teamleader", "teamleadername" -> "leader";
                    case "department" -> "department";
                    case "academicyear", "year" -> "year";
                    default -> null;
                };
                if (field != null && columns.putIfAbsent(field, c) != null) duplicates.add("Duplicate header for '" + field + "' in column " + (c + 1) + ".");
            }
            if (columns.containsKey("group") && columns.containsKey("student") && columns.containsKey("name") && columns.containsKey("title"))
                return new Header(r, columns, duplicates);
        }
        return null;
    }

    private static String firstRowsText(Sheet sheet, DataFormatter f, FormulaEvaluator e, int count) {
        StringBuilder b = new StringBuilder();
        for (int r=0;r<Math.min(count,sheet.getLastRowNum()+1);r++) {
            Row row=sheet.getRow(r); if(row==null) continue;
            for(Cell cell:row) { String value=f.formatCellValue(cell,e); if(!value.isBlank()) b.append(value).append(' '); }
        }
        return b.toString();
    }
    private static String value(Sheet sheet,int row,Integer col,Map<CellKey,CellKey> merged,DataFormatter f,FormulaEvaluator e) {
        if(col==null) return null;
        CellKey key=merged.getOrDefault(new CellKey(row,col),new CellKey(row,col));
        Row source=sheet.getRow(key.row); if(source==null) return null;
        Cell cell=source.getCell(key.col); if(cell==null) return null;
        try { String value=f.formatCellValue(cell,e).strip(); return value.isEmpty()?null:value; }
        catch(Exception ex) { return null; }
    }
    private static String canonicalHeader(String value) { return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]",""); }
    private static String normalize(String value) { return value==null?"":value.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+"," "); }
    private static String clean(String value) { return value==null?null:value.strip().replaceAll("\\s+"," "); }
    private static boolean blank(String value) { return value==null||value.isBlank(); }
    private static String extract(Pattern p,String value) { Matcher m=p.matcher(value); return m.find()?m.group(1).strip():null; }
    private static String normalizeYear(String year) { return year==null?null:year.replaceAll("\\s+","").replace('/','-'); }
    private static String guideFromSheet(String sheet) { return sheet.equalsIgnoreCase("Project Registration List")?null:sheet; }
    private static String signature(ImportPreviewRecord r) { return normalize(r.title())+"|"+r.members().stream().map(ImportMemberDto::studentId).filter(Objects::nonNull).map(ExcelWorkbookParser::normalize).sorted().reduce((a,b)->a+","+b).orElse(""); }

    private static void addWorkbookDuplicateWarnings(List<ImportPreviewRecord> records) {
        Map<String,Integer> titles=new HashMap<>(); Map<String,Set<String>> memberRows=new HashMap<>();
        for(int i=0;i<records.size();i++) {
            ImportPreviewRecord r=records.get(i); String title=normalize(r.title());
            if(!title.isBlank()) titles.merge(title,1,Integer::sum);
            for(ImportMemberDto m:r.members()) if(!blank(m.studentId())) memberRows.computeIfAbsent(normalize(m.studentId()),k->new HashSet<>()).add(r.recordId());
        }
        for(int i=0;i<records.size();i++) {
            ImportPreviewRecord r=records.get(i); List<String> warnings=new ArrayList<>(r.duplicateWarnings());
            if(titles.getOrDefault(normalize(r.title()),0)>1) warnings.add("The project title is duplicated within this workbook.");
            for(ImportMemberDto m:r.members()) if(!blank(m.studentId())&&memberRows.getOrDefault(normalize(m.studentId()),Set.of()).size()>1) {
                warnings.add("Student identifier '"+m.studentId()+"' appears in more than one project in this workbook."); break;
            }
            boolean duplicate=warnings.stream().anyMatch(w->w.toLowerCase(Locale.ROOT).contains("duplicate")||w.toLowerCase(Locale.ROOT).contains("appears in more than one"));
            records.set(i,r.withReview(r.validationErrors(),warnings,r.valid(),duplicate));
        }
    }
    private record CellKey(int row,int col) {}
    private static class MutableRecord {
        final String worksheet; final int sourceRow; String groupKey;
        String groupNumber,title,faculty,description,problemStatement,objectives,technologies,teamName,teamLeader,department,academicYear;
        final List<ImportMemberDto> members=new ArrayList<>();
        MutableRecord(String sheet,int row,String group,String title,String faculty) {
            worksheet=sheet;sourceRow=row;groupNumber=group;this.title=title;this.faculty=faculty;
            groupKey=!blank(group)?"g:"+normalize(group):(!blank(title)?"t:"+normalize(title):"row:"+row);
        }
        ImportPreviewRecord finish() {
            return new ImportPreviewRecord(UUID.randomUUID().toString(),worksheet,sourceRow,groupNumber,title,description,problemStatement,objectives,technologies,
                    blank(teamName)&&!blank(groupNumber)?"Group "+groupNumber:teamName,teamLeader,members,faculty,department,academicYear,List.of(),List.of(),true,false);
        }
    }
}
