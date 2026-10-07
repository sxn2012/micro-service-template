package com.example.microservice.productservice.service;

import com.example.microservice.dto.CreateProductRequest;
import com.example.microservice.exception.BusinessException;
import com.example.microservice.exception.ResourceNotFoundException;
import com.example.microservice.message.MqEvent;
import com.example.microservice.message.ProductIndexEvent;
import com.example.microservice.message.RabbitTopics;
import com.example.microservice.message.StockEventMessage;
import com.example.microservice.model.Product;
import com.example.microservice.productservice.mapper.ProductDocument;
import com.example.microservice.productservice.mapper.ProductRepository;
import com.example.microservice.productservice.mapper.ProductSearchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductSearchMapper productSearchMapper;
    private final ApplicationEventPublisher eventPublisher;

    @CacheEvict(value = "products_cache", allEntries = true)
    public Product createProduct(CreateProductRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setImageUrl(request.imageUrl());
        product.setCategoryId(request.categoryId());
        product.setCreatedAt(LocalDateTime.now());
        Product saved = productRepository.save(product);
        publishProductIndex(saved.getId(), ProductIndexEvent.IndexAction.INDEX);
        return saved;
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Cacheable(value = "products_cache", key = "'category-' + #categoryId")
    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    public List<Product> searchProducts(String keyword) {
        List<Long> ids = productSearchMapper.search(keyword).stream()
                .map(ProductDocument::getId)
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Product> byId = new HashMap<>();
        productRepository.findAllById(ids).forEach(product -> byId.put(product.getId(), product));
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @CacheEvict(value = "products_cache", allEntries = true)
    public Product updateProduct(Long id, CreateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setImageUrl(request.imageUrl());
        product.setCategoryId(request.categoryId());
        Product saved = productRepository.save(product);
        publishProductIndex(saved.getId(), ProductIndexEvent.IndexAction.INDEX);
        return saved;
    }

    @CacheEvict(value = "products_cache", allEntries = true)
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
        publishProductIndex(id, ProductIndexEvent.IndexAction.DELETE);
    }

    @CacheEvict(value = "products_cache", allEntries = true)
    public void decreaseStock(Long id, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("扣减数量必须大于 0");
        }
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        int updated = productRepository.decrementStock(id, quantity);
        if (updated == 0) {
            throw new BusinessException("Insufficient stock for product: " + product.getName());
        }
        publishProductIndex(id, ProductIndexEvent.IndexAction.INDEX);
        eventPublisher.publishEvent(new MqEvent(RabbitTopics.STOCK_DECREASE, StockEventMessage.decreased(id, quantity)));
    }

    @CacheEvict(value = "products_cache", allEntries = true)
    public void increaseStock(Long id, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("回补数量必须大于 0");
        }
        productRepository.incrementStock(id, quantity);
        publishProductIndex(id, ProductIndexEvent.IndexAction.INDEX);
        eventPublisher.publishEvent(new MqEvent(RabbitTopics.STOCK_INCREASE, StockEventMessage.increased(id, quantity)));
    }

    private void publishProductIndex(Long id, ProductIndexEvent.IndexAction action) {
        String routingKey = action == ProductIndexEvent.IndexAction.DELETE
                ? RabbitTopics.PRODUCT_DELETE
                : RabbitTopics.PRODUCT_INDEX;
        eventPublisher.publishEvent(new MqEvent(routingKey, new ProductIndexEvent(id, action)));
    }
}
