package com.projectlense.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="similarity_checks")
public class SimilarityCheckEntity {
    @Id @Column(name="check_id", length=36) private String id;
    @Column(nullable=false, length=500) private String title;
    @Column(columnDefinition="LONGTEXT") private String description;
    @Column(columnDefinition="LONGTEXT") private String objectives;
    @Column(columnDefinition="TEXT") private String technologies;
    @Column(nullable=false, length=24) private String status;
    @Column(name="threshold_value", nullable=false) private double threshold;
    @Column(name="model_id", nullable=false, length=128) private String modelId;
    @Column(name="results_json", columnDefinition="LONGTEXT") private String resultsJson;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    protected SimilarityCheckEntity() {}
    public SimilarityCheckEntity(String id,String title,String description,String objectives,String technologies,double threshold,String model) {
        this.id=id;this.title=title;this.description=description;this.objectives=objectives;this.technologies=technologies;
        this.threshold=threshold;this.modelId=model;this.status="PROCESSING";this.createdAt=Instant.now();
    }
    public String getId(){return id;} public String getTitle(){return title;} public String getDescription(){return description;}
    public String getObjectives(){return objectives;} public String getTechnologies(){return technologies;} public String getStatus(){return status;}
    public void setStatus(String v){status=v;} public double getThreshold(){return threshold;} public String getModelId(){return modelId;}
    public String getResultsJson(){return resultsJson;} public void setResultsJson(String v){resultsJson=v;}
    public Instant getCreatedAt(){return createdAt;}
}
