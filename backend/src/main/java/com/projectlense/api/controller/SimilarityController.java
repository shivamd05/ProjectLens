package com.projectlense.api.controller;
import com.projectlense.api.dto.SimilarityCheckRequest;import com.projectlense.api.service.SimilarityService;import jakarta.validation.Valid;import org.springframework.web.bind.annotation.*;import java.util.Map;
@RestController @RequestMapping("/api/v1/projects/similarity-checks") public class SimilarityController{
 private final SimilarityService service;public SimilarityController(SimilarityService s){service=s;}
 @PostMapping public Map<String,Object> check(@Valid @RequestBody SimilarityCheckRequest request){return service.check(request);}
 @GetMapping("/{checkId}") public Map<String,Object> get(@PathVariable String checkId){return service.get(checkId);}
}
