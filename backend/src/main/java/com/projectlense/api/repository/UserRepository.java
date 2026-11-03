package com.projectlense.api.repository;
import com.projectlense.api.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface UserRepository extends JpaRepository<UserEntity,String> {
 Optional<UserEntity> findByEmail(String email);
 List<UserEntity> findAllByRoleOrderByNameAsc(String role);
 Optional<UserEntity> findFirstByNameIgnoreCaseAndRole(String name, String role);
}
