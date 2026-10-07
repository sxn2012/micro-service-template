package com.example.microservice.productservice.facade;

import com.example.microservice.dto.CreateProductRequest;
import com.example.microservice.dto.ProductResponse;
import com.example.microservice.model.Product;
import com.example.microservice.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductFacade {

    private final ProductService productService;

    public ProductResponse createProduct(CreateProductRequest request) {
        return ProductResponse.from(productService.createProduct(request));
    }

    public Optional<ProductResponse> getProductById(Long id) {
        return productService.getProductById(id).map(ProductResponse::from);
    }

    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productService.getAllProducts(pageable).map(ProductResponse::from);
    }

    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        return productService.getProductsByCategory(categoryId).stream().map(ProductResponse::from).toList();
    }

    public List<ProductResponse> searchProducts(String keyword) {
        return productService.searchProducts(keyword).stream().map(ProductResponse::from).toList();
    }

    public ProductResponse updateProduct(Long id, CreateProductRequest request) {
        return ProductResponse.from(productService.updateProduct(id, request));
    }

    public void deleteProduct(Long id) {
        productService.deleteProduct(id);
    }

    public void decreaseStock(Long id, int quantity) {
        productService.decreaseStock(id, quantity);
    }

    public void increaseStock(Long id, int quantity) {
        productService.increaseStock(id, quantity);
    }
}
