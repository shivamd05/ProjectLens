package com.projectlense.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import com.projectlense.api.entity.UserEntity;
import com.projectlense.api.service.AuthService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class AdminTokenFilter extends OncePerRequestFilter {
    private final String configuredToken;
    private final AuthService authService;
    public AdminTokenFilter(@Value("${projectlense.admin-token:}") String configuredToken, AuthService authService) { this.configuredToken=configuredToken; this.authService=authService; }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) { return request.getRequestURI().equals("/api/auth/login") || request.getRequestURI().equals("/api/auth/register") || request.getRequestURI().equals("/api/auth/admin-login") || request.getRequestURI().equals("/"); }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String supplied=request.getHeader("X-Admin-Token");
        boolean adminRoute=request.getRequestURI().startsWith("/api/v1/admin/");
        if (adminRoute && configuredToken!=null&&!configuredToken.isBlank()&&supplied!=null&&MessageDigest.isEqual(
                configuredToken.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8))) {
            var auth=new UsernamePasswordAuthenticationToken("configured-admin",null,List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else if (!adminRoute) {
            String header=request.getHeader("Authorization");
            if (header!=null && header.startsWith("Bearer ")) {
                UserEntity user=authService.authenticate(header.substring(7));
                if(user!=null) SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        user,null,List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole().toUpperCase()))));
            }
        }
        chain.doFilter(request,response);
    }
}
