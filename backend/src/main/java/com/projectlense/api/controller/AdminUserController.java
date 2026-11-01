package com.projectlense.api.controller;
import com.projectlense.api.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/v1/admin/users")
public class AdminUserController {
 private final AuthService auth;
 public AdminUserController(AuthService auth){this.auth=auth;}
 public record FacultyCreateRequest(@NotBlank @Size(max=255) String name,@NotBlank @Email @Size(max=320) String email,@NotBlank @Size(min=10,max=128) String password,@Size(max=255) String department){}
 @PostMapping("/faculty") @ResponseStatus(HttpStatus.CREATED)
 public Map<String,Object> createFaculty(@Valid @RequestBody FacultyCreateRequest request){return auth.createFaculty(request.name(),request.email(),request.password(),request.department());}
}
