package com.projectlense.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectlense.api.dto.*;
import com.projectlense.api.entity.*;
import com.projectlense.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;

@Service
public class ImportService {
 private final ExcelWorkbookParser parser; private final ImportJobRepository jobs; private final ImportRecordRepository records; private final ProjectDataRepository projects; private final ProjectEmbeddingRepository embeddings; private final ObjectMapper mapper; private final EmbeddingService embedder;
 public ImportService(ExcelWorkbookParser p,ImportJobRepository j,ImportRecordRepository r,ProjectDataRepository pr,ProjectEmbeddingRepository er,ObjectMapper m,EmbeddingService e){parser=p;jobs=j;records=r;projects=pr;embeddings=er;mapper=m;embedder=e;}
 @Transactional public Map<String,Object> upload(MultipartFile file){try{if(file.isEmpty())throw new IllegalArgumentException("Workbook is empty"); String name=Objects.requireNonNullElse(file.getOriginalFilename(),"workbook.xlsx"); if(!name.toLowerCase(Locale.ROOT).matches(".*\\.(xlsx|xls)$"))throw new IllegalArgumentException("Upload an .xlsx or .xls workbook"); byte[] bytes=file.getBytes(); String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
   var parsed=parser.parse(new java.io.ByteArrayInputStream(bytes)); ImportJobEntity job=new ImportJobEntity(UUID.randomUUID().toString(),name,hash); job.setWorkbookWarningsJson(mapper.writeValueAsString(parsed.warnings())); jobs.save(job); int valid=0,invalid=0,dupes=0;
   for(var item:parsed.records()){List<String>w=new ArrayList<>(item.duplicateWarnings()); boolean duplicate=item.duplicate(); if(item.title()!=null&&item.academicYear()!=null&&projects.existsByNormalizedTitleAndAcademicYear(norm(item.title()),item.academicYear())){w.add("Project title and academic year already exist in the database.");duplicate=true;} var review=item.withReview(item.validationErrors(),w,item.valid(),duplicate); if(review.valid())valid++;else invalid++;if(duplicate)dupes++;
    records.save(new ImportRecordEntity(item.recordId(),job,item.worksheet(),item.sourceRow(),mapper.writeValueAsString(review),mapper.writeValueAsString(review.validationErrors()),mapper.writeValueAsString(review.duplicateWarnings()),review.valid()?"PREVIEW":"INVALID")); }
   job.setTotalRecords(parsed.records().size());job.setValidRecords(valid);job.setInvalidRecords(invalid);job.setDuplicateRecords(dupes);jobs.save(job);return jobView(job,parsed.warnings());
  }catch(Exception e){throw new IllegalArgumentException("Unable to preview workbook: "+e.getMessage(),e);}}
 @Transactional(readOnly=true) public Map<String,Object> get(String id){return jobView(job(id),List.of());}
 @Transactional(readOnly=true) public Map<String,Object> preview(String id){ImportJobEntity job=job(id);return Map.of("job",jobView(job,List.of()),"records",records.findAllByJob_IdOrderBySourceRowAsc(id).stream().map(this::recordView).toList());}
 @Transactional public Map<String,Object> cancel(String id){ImportJobEntity j=job(id); if(!j.getStatus().equals("PREVIEW"))throw new IllegalArgumentException("Only a preview import can be cancelled");j.setStatus("CANCELLED");j.setCancelledAt(Instant.now());jobs.save(j);return jobView(j,List.of());}
 @Transactional public Map<String,Object> approve(String id,ApproveImportRequest request){ImportJobEntity j=job(id);if(!j.getStatus().equals("PREVIEW"))throw new IllegalArgumentException("Import is not awaiting approval");Map<String,ImportRecordCorrection> corrections=request==null||request.corrections()==null?Map.of():request.corrections();int imported=0,skipped=0;
   for(ImportRecordEntity rec:records.findAllByJob_IdOrderBySourceRowAsc(id)){ImportPreviewRecord data=read(rec.getRecordJson());var correction=corrections.get(rec.getId());if(correction!=null)data=data.withCorrection(correction);List<String> errors=validate(data);if(!errors.isEmpty()){rec.setStatus("INVALID");rec.setValidationJson(write(errors));skipped++;continue;}String normalized=norm(data.title()); if(projects.existsByNormalizedTitleAndAcademicYear(normalized,data.academicYear())){rec.setStatus("DUPLICATE");skipped++;continue;}
    ProjectEntity p=new ProjectEntity();p.setId(UUID.randomUUID().toString());p.setTitle(data.title().strip());p.setNormalizedTitle(normalized);p.setDescription(data.description());p.setProblemStatement(data.problemStatement());p.setObjectives(data.objectives());p.setTechnologies(data.technologies());p.setTeamName(data.teamName());p.setTeamLeader(data.teamLeader());p.setGroupNumber(data.groupNumber());p.setFacultyCoordinator(data.facultyCoordinator());p.setDepartment(data.department());p.setAcademicYear(data.academicYear());p.setStatus("IMPORTED");p.setSourceFileName(j.getFileName());p.setSourceFileHash(j.getFileHash());p.setSourceSheet(rec.getSheetName());p.setSourceRow(rec.getSourceRow());Set<String> seen=new HashSet<>();for(var m:data.members())if(seen.add(norm(m.studentId())))p.addMember(new ProjectMemberEntity(m.studentId().strip(),m.studentName().strip(),m.sourceRow()));projects.save(p);rec.setImportedProject(p);rec.setStatus("IMPORTED");embeddings.save(new ProjectEmbeddingEntity(p.getId(),hash(embeddingText(p)),embedder.model()));imported++;}
   j.setStatus("APPROVED");j.setApprovedAt(Instant.now());jobs.save(j);return Map.of("job",jobView(j,List.of()),"imported",imported,"skipped",skipped);}
 private List<String> validate(ImportPreviewRecord d){List<String> e=new ArrayList<>();if(d.title()==null||d.title().isBlank())e.add("Project title is required");if(d.academicYear()==null||d.academicYear().isBlank())e.add("Academic year is required");if(d.members()==null||d.members().isEmpty())e.add("At least one member is required");if(d.members()!=null)for(var m:d.members())if(m.studentId()==null||m.studentId().isBlank()||m.studentName()==null||m.studentName().isBlank())e.add("Every member needs an ID and name");return e;}
 public static String embeddingText(ProjectEntity p){return String.join(" ",Arrays.asList(p.getTitle(),p.getDescription(),p.getProblemStatement(),p.getObjectives(),p.getTechnologies()).stream().filter(Objects::nonNull).toList());}
 private ImportJobEntity job(String id){return jobs.findById(id).orElseThrow(()->new NoSuchElementException("Import job not found"));}
 private Map<String,Object> jobView(ImportJobEntity j,List<String>w){if(w.isEmpty()&&j.getWorkbookWarningsJson()!=null)try{w=mapper.readValue(j.getWorkbookWarningsJson(),new TypeReference<List<String>>(){});}catch(Exception ignored){}Map<String,Object>m=new LinkedHashMap<>();m.put("jobId",j.getId());m.put("fileName",j.getFileName());m.put("status",j.getStatus());m.put("totalRecords",j.getTotalRecords());m.put("validRecords",j.getValidRecords());m.put("invalidRecords",j.getInvalidRecords());m.put("duplicateRecords",j.getDuplicateRecords());m.put("createdAt",j.getCreatedAt());m.put("warnings",w);return m;}
 private Map<String,Object> recordView(ImportRecordEntity r){try{return mapper.readValue(r.getRecordJson(),new TypeReference<>(){});}catch(Exception e){throw new IllegalStateException(e);}}
 private ImportPreviewRecord read(String s){try{return mapper.readValue(s,ImportPreviewRecord.class);}catch(Exception e){throw new IllegalStateException(e);}}
 private String write(Object o){try{return mapper.writeValueAsString(o);}catch(Exception e){throw new IllegalStateException(e);}}
 private static String norm(String s){return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").strip();}
 private static String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
