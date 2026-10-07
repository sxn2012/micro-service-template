package com.example.microservice.productservice.controller;

import com.example.microservice.dto.ApiResponse;
import com.example.microservice.dto.ApiStatus;
import com.example.microservice.dto.CreateProductRequest;
import com.example.microservice.dto.ProductResponse;
import com.example.microservice.productservice.facade.ProductFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductFacade productFacade;

    @PostMapping
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.success("Product created", productFacade.createProduct(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Long id) {
        return productFacade.getProductById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(ApiStatus.NOT_FOUND, "Product not found"));
    }

    @GetMapping
    public ApiResponse<Page<ProductResponse>> getAllProducts(Pageable pageable) {
        return ApiResponse.success(productFacade.getAllProducts(pageable));
    }

    @GetMapping("/category/{categoryId}")
    public ApiResponse<List<ProductResponse>> getProductsByCategory(@PathVariable Long categoryId) {
        return ApiResponse.success(productFacade.getProductsByCategory(categoryId));
    }

    @GetMapping("/search")
    public ApiResponse<List<ProductResponse>> searchProducts(@RequestParam String keyword) {
        return ApiResponse.success(productFacade.searchProducts(keyword));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable Long id, @Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.success("Product updated", productFacade.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable Long id) {
        productFacade.deleteProduct(id);
        return ApiResponse.success("Product deleted", null);
    }

    @PostMapping("/{id}/stock/decrease")
    public ApiResponse<Void> decreaseStock(@PathVariable Long id, @RequestParam int quantity) {
        productFacade.decreaseStock(id, quantity);
        return ApiResponse.success("Stock decreased", null);
    }

    @PostMapping("/{id}/stock/increase")
    public ApiResponse<Void> increaseStock(@PathVariable Long id, @RequestParam int quantity) {
        productFacade.increaseStock(id, quantity);
        return ApiResponse.success("Stock increased", null);
    }
}
