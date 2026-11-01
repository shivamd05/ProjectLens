package com.projectlense.api.repository;
import com.projectlense.api.entity.AuthSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Optional;
public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity,String> {
 Optional<AuthSessionEntity> findByTokenHashAndExpiresAtAfter(String hash, Instant now);
 void deleteByTokenHash(String hash);
}
