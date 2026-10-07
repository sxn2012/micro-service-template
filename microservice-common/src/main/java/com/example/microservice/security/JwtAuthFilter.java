package com.example.microservice.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String TOKEN_COOKIE = "token";
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        String token = ApiSecurityUtil.resolveToken(request);
        if (token != null && jwtUtil.validate(token) && !tokenBlacklist.isRevoked(token)) {
            Claims claims = jwtUtil.parse(token);
            Number userIdClaim = (Number) claims.get("userId");
            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            if (userIdClaim != null && role != null) {
                CurrentUser principal = new CurrentUser(userIdClaim.longValue(), username, role);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
