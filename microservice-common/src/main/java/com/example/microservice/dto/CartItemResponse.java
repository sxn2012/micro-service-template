package com.example.microservice.dto;

import com.example.microservice.model.CartItem;

public record CartItemResponse(Long id, Long productId, Integer quantity) {

    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(item.getId(), item.getProductId(), item.getQuantity());
    }
}
