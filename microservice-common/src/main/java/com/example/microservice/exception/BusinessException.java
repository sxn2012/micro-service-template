package com.example.microservice.exception;

import com.example.microservice.dto.ApiStatus;

public class BusinessException extends RuntimeException {

    private final ApiStatus status;

    public BusinessException(String message) {
        this(ApiStatus.BAD_REQUEST, message);
    }

    public BusinessException(ApiStatus status, String message) {
        super(message);
        this.status = status;
    }

    public ApiStatus getStatus() {
        return status;
    }
}
