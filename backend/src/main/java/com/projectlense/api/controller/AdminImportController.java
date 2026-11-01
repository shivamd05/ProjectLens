package com.projectlense.api.controller;
import com.projectlense.api.dto.ApproveImportRequest;import com.projectlense.api.service.ImportService;import jakarta.validation.Valid;import org.springframework.http.MediaType;import org.springframework.web.bind.annotation.*;import org.springframework.web.multipart.MultipartFile;import java.util.Map;
@RestController @RequestMapping("/api/v1/admin/excel-imports") public class AdminImportController{
 private final ImportService service;public AdminImportController(ImportService s){service=s;}
 @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Map<String,Object> upload(@RequestPart("file") MultipartFile file){return service.upload(file);}
 @GetMapping("/{jobId}") public Map<String,Object> get(@PathVariable String jobId){return service.get(jobId);}
 @GetMapping("/{jobId}/preview") public Map<String,Object> preview(@PathVariable String jobId){return service.preview(jobId);}
 @PostMapping("/{jobId}/approve") public Map<String,Object> approve(@PathVariable String jobId,@RequestBody(required=false) ApproveImportRequest request){return service.approve(jobId,request);}
 @PostMapping("/{jobId}/cancel") public Map<String,Object> cancel(@PathVariable String jobId){return service.cancel(jobId);}
}
