package com.projectlense.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auth_sessions")
public class AuthSessionEntity {
    @Id @Column(name="token_hash", length=64, columnDefinition="CHAR(64)") private String tokenHash;
    @ManyToOne(optional=false, fetch=FetchType.EAGER) @JoinColumn(name="user_id", nullable=false) private UserEntity user;
    @Column(name="expires_at", nullable=false) private Instant expiresAt;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    protected AuthSessionEntity() {}
    public AuthSessionEntity(String hash, UserEntity user, Instant expiresAt) { this.tokenHash=hash; this.user=user; this.expiresAt=expiresAt; this.createdAt=Instant.now(); }
    public String getTokenHash(){return tokenHash;} public UserEntity getUser(){return user;} public Instant getExpiresAt(){return expiresAt;}
}
