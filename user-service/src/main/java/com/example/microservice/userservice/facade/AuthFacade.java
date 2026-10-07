package com.example.microservice.userservice.facade;

import com.example.microservice.security.JwtUtil;
import com.example.microservice.dto.ApiStatus;
import com.example.microservice.dto.LoginRequest;
import com.example.microservice.dto.LoginResponse;
import com.example.microservice.dto.RegisterRequest;
import com.example.microservice.exception.BusinessException;
import com.example.microservice.exception.ResourceNotFoundException;
import com.example.microservice.model.User;
import com.example.microservice.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthFacade {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public LoginResponse register(RegisterRequest request) {
        if (userService.getUserByUsername(request.username()).isPresent()) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setAddress(request.address());
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        user = userService.save(user);

        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException e) {
            throw new BusinessException(ApiStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        User user = userService.getUserByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
