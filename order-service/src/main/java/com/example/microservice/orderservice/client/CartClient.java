package com.example.microservice.orderservice.client;

import com.example.microservice.dto.ApiResponse;
import com.example.microservice.dto.CartResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cart-service")
public interface CartClient {

    @GetMapping("/cart/{userId}")
    ApiResponse<CartResponse> getCart(@PathVariable("userId") Long userId);

    @DeleteMapping("/cart/{userId}")
    ApiResponse<Void> clearCart(@PathVariable("userId") Long userId);
}
