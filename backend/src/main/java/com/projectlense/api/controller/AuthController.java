package com.projectlense.api.controller;
import com.projectlense.api.entity.UserEntity;
import com.projectlense.api.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final AuthService auth; public AuthController(AuthService auth){this.auth=auth;}
 public record RegisterRequest(@NotBlank @Size(max=255) String name,@NotBlank @Email @Size(max=320) String email,@NotBlank @Size(min=10,max=128) String password,@Size(max=255) String department){}
 public record LoginRequest(@NotBlank @Email String email,@NotBlank String password){}
 public record AdminLoginRequest(@NotBlank String token){}
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> register(@Valid @RequestBody RegisterRequest r){return auth.register(r.name(),r.email(),r.password(),r.department());}
 @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody LoginRequest r){return auth.login(r.email(),r.password());}
 @PostMapping("/admin-login") public Map<String,Object> adminLogin(@Valid @RequestBody AdminLoginRequest r){return auth.adminLogin(r.token());}
 @GetMapping("/me") public Map<String,Object> me(Authentication authentication){if(authentication==null||!(authentication.getPrincipal() instanceof UserEntity u))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return auth.userJson(u);}
 @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) public void logout(@RequestHeader(name="Authorization",required=false) String header){if(header!=null&&header.startsWith("Bearer "))auth.logout(header.substring(7));}
}
