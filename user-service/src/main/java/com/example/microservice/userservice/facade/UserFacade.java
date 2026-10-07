package com.example.microservice.userservice.facade;

import com.example.microservice.dto.CreateUserRequest;
import com.example.microservice.dto.UserResponse;
import com.example.microservice.userservice.client.CartClient;
import com.example.microservice.userservice.client.OrderClient;
import com.example.microservice.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;
    private final CartClient cartClient;
    private final OrderClient orderClient;

    public UserResponse createUser(CreateUserRequest request) {
        return UserResponse.from(userService.createUser(request));
    }

    public Optional<UserResponse> getUserById(Long id) {
        return userService.getUserById(id).map(UserResponse::from);
    }

    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers().stream().map(UserResponse::from).toList();
    }

    public UserResponse updateUser(Long id, CreateUserRequest request) {
        return UserResponse.from(userService.updateUser(id, request));
    }

    public void deleteUser(Long id) {
        cartClient.deleteUserCart(id);
        orderClient.deleteUserOrders(id);
        userService.deleteById(id);
    }
}
