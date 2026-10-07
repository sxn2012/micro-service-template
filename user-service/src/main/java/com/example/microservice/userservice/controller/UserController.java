package com.example.microservice.userservice.controller;

import com.example.microservice.dto.ApiResponse;
import com.example.microservice.dto.ApiStatus;
import com.example.microservice.dto.CreateUserRequest;
import com.example.microservice.dto.UserResponse;
import com.example.microservice.userservice.facade.UserFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserFacade userFacade;
    private final PasswordEncoder passwordEncoder;

    @PostMapping
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        CreateUserRequest encoded = new CreateUserRequest(
                request.username(), passwordEncoder.encode(request.password()),
                request.email(), request.phone(), request.address());
        return ApiResponse.success("User created", userFacade.createUser(encoded));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {
        return userFacade.getUserById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(ApiStatus.NOT_FOUND, "User not found"));
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.success(userFacade.getAllUsers());
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody CreateUserRequest request) {
        CreateUserRequest encoded = new CreateUserRequest(
                request.username(),
                request.password() != null && !request.password().isBlank()
                        ? passwordEncoder.encode(request.password()) : null,
                request.email(), request.phone(), request.address());
        return ApiResponse.success("User updated", userFacade.updateUser(id, encoded));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        try {
            userFacade.deleteUser(id);
            return ApiResponse.success("User deleted", null);
        } catch (DataIntegrityViolationException e) {
            return ApiResponse.error(ApiStatus.CONFLICT, "该用户存在关联数据（购物车/订单），无法删除");
        }
    }
}
