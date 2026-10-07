package com.example.microservice.orderservice.client;

import com.example.microservice.dto.ApiResponse;
import com.example.microservice.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/products/{id}")
    ApiResponse<ProductResponse> getProduct(@PathVariable("id") Long id);

    @PostMapping("/products/{id}/stock/decrease")
    ApiResponse<Void> decreaseStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);

    @PostMapping("/products/{id}/stock/increase")
    ApiResponse<Void> increaseStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);
}
