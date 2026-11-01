package com.projectlense.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.*;

@Service
public class EmbeddingService {
    private final RestClient client; private final ObjectMapper mapper; private final String key, model;
    public EmbeddingService(ObjectMapper mapper, @Value("${projectlense.embedding.api-key:}") String key,
                            @Value("${projectlense.embedding.model:text-embedding-3-small}") String model) {
        this.mapper=mapper; this.key=key; this.model=model;
        this.client=RestClient.builder().baseUrl("https://api.openai.com/v1").build();
    }
    public String model(){return model;}
    public boolean isConfigured(){return key!=null&&!key.isBlank();}
    public float[] embed(String input) {
        if(key==null||key.isBlank()) throw new IllegalStateException("OPENAI_API_KEY is not configured");
        Map<?,?> response=client.post().uri("/embeddings").contentType(MediaType.APPLICATION_JSON).headers(h->h.setBearerAuth(key))
                .body(Map.of("model",model,"input",input)).retrieve().body(Map.class);
        try { JsonNode data=mapper.valueToTree(response).path("data").path(0).path("embedding"); float[] v=new float[data.size()];
            for(int i=0;i<v.length;i++)v[i]=(float)data.get(i).asDouble(); if(v.length==0)throw new IllegalStateException("Embedding response was empty"); return v;
        } catch(Exception ex){throw new IllegalStateException("Could not parse embedding response",ex);}
    }
    public String json(float[] vector){try{return mapper.writeValueAsString(vector);}catch(Exception e){throw new IllegalStateException(e);}}
    public float[] parse(String json){try{return mapper.readValue(json,float[].class);}catch(Exception e){throw new IllegalStateException(e);}}
    public static double cosine(float[] a,float[] b){if(a.length!=b.length) return 0; double dot=0,x=0,y=0; for(int i=0;i<a.length;i++){dot+=a[i]*b[i];x+=a[i]*a[i];y+=b[i]*b[i];}return x==0||y==0?0:dot/(Math.sqrt(x)*Math.sqrt(y));}
}
