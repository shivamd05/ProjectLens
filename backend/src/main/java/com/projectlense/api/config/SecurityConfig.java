package com.projectlense.api.config;

import com.projectlense.api.security.AdminTokenFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AdminTokenFilter adminTokenFilter) throws Exception {
        return http.csrf(csrf->csrf.disable())
                .cors(cors->{})
                .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors->errors.authenticationEntryPoint((request,response,exception)->{
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); response.setContentType("application/json");
                    response.getOutputStream().write("{\"detail\":\"Authentication required\"}".getBytes(StandardCharsets.UTF_8));
                }))
                .authorizeHttpRequests(auth->auth.requestMatchers("/", "/api/auth/login", "/api/auth/register", "/api/auth/admin-login").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/projects/*/faculty-review", "/api/projects/*/approve").hasRole("FACULTY")
                        .requestMatchers("/api/projects/upload").hasRole("STUDENT")
                        .anyRequest().authenticated())
                .addFilterBefore(adminTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
