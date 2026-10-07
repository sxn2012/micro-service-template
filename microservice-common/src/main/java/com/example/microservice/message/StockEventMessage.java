package com.example.microservice.message;

import java.time.LocalDateTime;

public record StockEventMessage(Long productId, String eventType, int quantity, LocalDateTime timestamp) {

    public static StockEventMessage decreased(Long productId, int quantity) {
        return new StockEventMessage(productId, "DECREASE", quantity, LocalDateTime.now());
    }

    public static StockEventMessage increased(Long productId, int quantity) {
        return new StockEventMessage(productId, "INCREASE", quantity, LocalDateTime.now());
    }
}
