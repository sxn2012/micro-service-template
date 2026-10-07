package com.example.microservice.web;

import com.example.microservice.dto.ApiResponse;
import com.example.microservice.dto.ApiStatus;
import com.example.microservice.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        return ok(ApiResponse.error(e.getStatus(), e.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
        return ok(ApiResponse.error(ApiStatus.CONFLICT, "数据冲突或违反唯一性约束"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage() != null
                        ? fieldError.getDefaultMessage()
                        : fieldError.getField() + " 参数不合法")
                .orElse("请求参数格式错误");
        return ok(ApiResponse.error(ApiStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        return ok(ApiResponse.error(ApiStatus.BAD_REQUEST, "请求体格式错误"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception e) {
        log.error("Unhandled exception", e);
        return ok(ApiResponse.error(ApiStatus.INTERNAL_ERROR, "服务器内部错误"));
    }

    private ResponseEntity<ApiResponse<Void>> ok(ApiResponse<Void> body) {
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }
}
