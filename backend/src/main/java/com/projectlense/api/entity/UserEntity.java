package com.projectlense.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uq_users_email", columnNames = "email"))
public class UserEntity {
    @Id @Column(name = "user_id", length = 36) private String id;
    @Column(name = "full_name", nullable = false, length = 255) private String name;
    @Column(nullable = false, length = 320) private String email;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Column(nullable = false, length = 16) private String role;
    @Column(length = 255) private String department;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected UserEntity() {}
    public UserEntity(String id, String name, String email, String passwordHash, String role, String department) {
        this.id=id; this.name=name; this.email=email; this.passwordHash=passwordHash; this.role=role; this.department=department; this.createdAt=Instant.now();
    }
    public String getId(){return id;} public String getName(){return name;} public String getEmail(){return email;}
    public String getPasswordHash(){return passwordHash;} public String getRole(){return role;} public String getDepartment(){return department;}
}
