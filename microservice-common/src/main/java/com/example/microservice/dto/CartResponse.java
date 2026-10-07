package com.example.microservice.dto;

import com.example.microservice.model.Cart;
import com.example.microservice.model.CartItem;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record CartResponse(
        Long id,
        Long userId,
        LocalDateTime createdAt,
        List<CartItemResponse> items) {

    public static CartResponse from(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .sorted(Comparator.comparing(CartItem::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(CartItemResponse::from)
                .toList();
        return new CartResponse(cart.getId(), cart.getUserId(), cart.getCreatedAt(), items);
    }
}
