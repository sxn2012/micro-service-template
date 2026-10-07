package com.example.microservice.exception;

import com.example.microservice.dto.ApiStatus;

public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(ApiStatus.FORBIDDEN, message);
    }
}
