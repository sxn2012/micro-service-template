package com.example.microservice.exception;

import com.example.microservice.dto.ApiStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(ApiStatus.NOT_FOUND, message);
    }
}
