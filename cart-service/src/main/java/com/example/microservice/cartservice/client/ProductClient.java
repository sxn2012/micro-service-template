package com.example.microservice.cartservice.client;

import com.example.microservice.dto.ApiResponse;
import com.example.microservice.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/products/{id}")
    ApiResponse<ProductResponse> getProduct(@PathVariable("id") Long id);
}
