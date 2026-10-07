package com.example.microservice.message;

public record ProductIndexEvent(Long productId, IndexAction action) {

    public enum IndexAction {
        INDEX, DELETE
    }
}
