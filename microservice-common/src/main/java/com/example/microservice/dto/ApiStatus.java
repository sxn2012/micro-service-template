package com.example.microservice.dto;

public enum ApiStatus {
    SUCCESS(200),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409),
    INTERNAL_ERROR(500);

    private final int code;

    ApiStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
