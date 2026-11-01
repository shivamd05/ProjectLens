package com.projectlense.api.service;

import com.projectlense.api.entity.AuthSessionEntity;
import com.projectlense.api.entity.UserEntity;
import com.projectlense.api.repository.AuthSessionRepository;
import com.projectlense.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

@Service
public class AuthService {
 private final UserRepository users; private final AuthSessionRepository sessions; private final PasswordEncoder passwords;
 private final String configuredAdminToken;
 private final SecureRandom random = new SecureRandom();
 public AuthService(UserRepository users, AuthSessionRepository sessions, PasswordEncoder passwords,
                    @Value("${projectlense.admin-token:}") String configuredAdminToken){this.users=users;this.sessions=sessions;this.passwords=passwords;this.configuredAdminToken=configuredAdminToken;}
 @Transactional public Map<String,Object> register(String name,String email,String password,String department){
  String normalized=email.trim().toLowerCase();
  if(users.findByEmail(normalized).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email is already registered");
  UserEntity user=users.save(new UserEntity(UUID.randomUUID().toString(),name.trim(),normalized,passwords.encode(password),"student",department==null?null:department.trim()));
  return sessionResponse(user);
 }
 @Transactional public Map<String,Object> createFaculty(String name,String email,String password,String department){
  String normalized=email.trim().toLowerCase();
  if(users.findByEmail(normalized).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email is already registered");
  UserEntity user=users.save(new UserEntity(UUID.randomUUID().toString(),name.trim(),normalized,passwords.encode(password),"faculty",department==null?null:department.trim()));
  return userJson(user);
 }
 @Transactional public Map<String,Object> adminLogin(String token){
  if(configuredAdminToken==null||configuredAdminToken.isBlank()||token==null||!MessageDigest.isEqual(configuredAdminToken.getBytes(StandardCharsets.UTF_8),token.getBytes(StandardCharsets.UTF_8)))
   throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid administrator token");
  String email="configured-admin@projectlense.local";
  UserEntity admin=users.findByEmail(email).orElseGet(()->users.save(new UserEntity(UUID.randomUUID().toString(),"ProjectLense Administrator",email,passwords.encode(UUID.randomUUID().toString()),"admin","Administration")));
  return sessionResponse(admin);
 }
 @Transactional public Map<String,Object> login(String email,String password){
  UserEntity user=users.findByEmail(email.trim().toLowerCase()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password"));
  if(!passwords.matches(password,user.getPasswordHash())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password");
  return sessionResponse(user);
 }
 private Map<String,Object> sessionResponse(UserEntity user){
  byte[] bytes=new byte[32];random.nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  sessions.save(new AuthSessionEntity(hash(token),user,Instant.now().plus(30,ChronoUnit.DAYS)));
  Map<String,Object> out=new LinkedHashMap<>();out.put("token",token);out.put("user",userJson(user));return out;
 }
 @Transactional(readOnly=true) public UserEntity authenticate(String token){
  return sessions.findByTokenHashAndExpiresAtAfter(hash(token),Instant.now()).map(AuthSessionEntity::getUser).orElse(null);
 }
 @Transactional public void logout(String token){sessions.deleteByTokenHash(hash(token));}
 public Map<String,Object> userJson(UserEntity u){Map<String,Object> out=new LinkedHashMap<>();out.put("id",u.getId());out.put("name",u.getName());out.put("email",u.getEmail());out.put("role",u.getRole());out.put("department",u.getDepartment());return out;}
 private static String hash(String value){try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
