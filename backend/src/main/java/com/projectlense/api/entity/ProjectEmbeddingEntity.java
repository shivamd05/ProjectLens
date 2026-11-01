package com.projectlense.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="project_embeddings")
public class ProjectEmbeddingEntity {
    @Id @Column(name="project_id", length=64) private String projectId;
    @Column(name="text_hash", nullable=false, length=64) private String textHash;
    @Column(name="model_id", nullable=false, length=128) private String modelId;
    @Column(name="vector_json", columnDefinition="LONGTEXT") private String vectorJson;
    @Column(nullable=false, length=24) private String status;
    @Column(nullable=false) private int attempts;
    @Column(name="last_error", columnDefinition="TEXT") private String lastError;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    protected ProjectEmbeddingEntity() {}
    public ProjectEmbeddingEntity(String id,String hash,String model){projectId=id;textHash=hash;modelId=model;status="PENDING";updatedAt=Instant.now();}
    public String getProjectId(){return projectId;} public String getTextHash(){return textHash;} public String getModelId(){return modelId;}
    public String getVectorJson(){return vectorJson;} public void setVectorJson(String v){vectorJson=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public int getAttempts(){return attempts;} public void setAttempts(int v){attempts=v;}
    public String getLastError(){return lastError;} public void setLastError(String v){lastError=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
    public void setTextHash(String v){textHash=v;} public void setModelId(String v){modelId=v;}
}
