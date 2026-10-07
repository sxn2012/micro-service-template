package com.example.microservice.userservice.client;

import com.example.microservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service")
public interface OrderClient {

    @DeleteMapping("/orders/user/{userId}")
    ApiResponse<Void> deleteUserOrders(@PathVariable("userId") Long userId);
}
