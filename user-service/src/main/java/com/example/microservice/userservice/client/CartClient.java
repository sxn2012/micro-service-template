package com.example.microservice.userservice.client;

import com.example.microservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cart-service")
public interface CartClient {

    @DeleteMapping("/cart/user/{userId}")
    ApiResponse<Void> deleteUserCart(@PathVariable("userId") Long userId);
}
