package com.example.microservice.message;

import com.example.microservice.model.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderEventMessage(Long orderId, Long userId, String eventType, String status,
                                BigDecimal totalAmount, LocalDateTime timestamp) {

    public static OrderEventMessage created(Order order) {
        return new OrderEventMessage(order.getId(), order.getUserId(), "CREATED", order.getStatus(),
                order.getTotalAmount(), order.getCreatedAt());
    }

    public static OrderEventMessage statusChanged(Order order) {
        return new OrderEventMessage(order.getId(), order.getUserId(), "STATUS_CHANGED", order.getStatus(),
                order.getTotalAmount(), order.getCreatedAt());
    }
}
