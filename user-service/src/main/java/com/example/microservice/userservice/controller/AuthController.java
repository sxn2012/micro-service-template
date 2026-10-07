package com.example.microservice.userservice.controller;

import com.example.microservice.dto.ApiResponse;
import com.example.microservice.dto.LoginRequest;
import com.example.microservice.dto.LoginResponse;
import com.example.microservice.dto.RegisterRequest;
import com.example.microservice.security.ApiSecurityUtil;
import com.example.microservice.security.JwtAuthFilter;
import com.example.microservice.security.JwtUtil;
import com.example.microservice.security.TokenBlacklist;
import com.example.microservice.userservice.facade.AuthFacade;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthFacade authFacade;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        LoginResponse login = authFacade.register(request);
        setAuthCookie(response, login.token());
        return ApiResponse.success("注册成功", login);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResponse login = authFacade.login(request);
        setAuthCookie(response, login.token());
        return ApiResponse.success("登录成功", login);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = ApiSecurityUtil.resolveToken(request);
        if (token != null && jwtUtil.validate(token)) {
            Claims claims = jwtUtil.parse(token);
            tokenBlacklist.revoke(token, claims.getExpiration());
        }
        ResponseCookie cookie = ResponseCookie.from(JwtAuthFilter.TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ApiResponse.success("退出成功", null);
    }

    private void setAuthCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(JwtAuthFilter.TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofMillis(expiration))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
