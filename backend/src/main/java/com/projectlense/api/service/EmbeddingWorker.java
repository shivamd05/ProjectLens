package com.projectlense.api.service;
import com.projectlense.api.entity.ProjectEmbeddingEntity;import com.projectlense.api.repository.*;import org.springframework.scheduling.annotation.Scheduled;import org.springframework.stereotype.Component;import org.springframework.transaction.annotation.Transactional;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.util.*;
@Component public class EmbeddingWorker{
 private final ProjectEmbeddingRepository embeddings;private final ProjectDataRepository projects;private final EmbeddingService service;
 public EmbeddingWorker(ProjectEmbeddingRepository e,ProjectDataRepository p,EmbeddingService s){embeddings=e;projects=p;service=s;}
 @Scheduled(fixedDelayString="${projectlense.embedding.retry-delay-ms:60000}") @Transactional public void retryPending(){
  if(!service.isConfigured())return;
  for(var p:projects.findAll()) if(!embeddings.existsById(p.getId())) embeddings.save(new ProjectEmbeddingEntity(p.getId(),hash(ImportService.embeddingText(p)),service.model()));
  for(ProjectEmbeddingEntity e:embeddings.findAllByStatusIn(List.of("PENDING","FAILED"))){var p=projects.findById(e.getProjectId()).orElse(null);if(p==null)continue;try{e.setStatus("PROCESSING");e.setAttempts(e.getAttempts()+1);e.setModelId(service.model());e.setTextHash(hash(ImportService.embeddingText(p)));e.setVectorJson(service.json(service.embed(ImportService.embeddingText(p))));e.setStatus("READY");e.setLastError(null);}catch(Exception ex){e.setStatus("FAILED");e.setLastError(ex.getMessage());}e.setUpdatedAt(java.time.Instant.now());embeddings.save(e);}}
 private static String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
