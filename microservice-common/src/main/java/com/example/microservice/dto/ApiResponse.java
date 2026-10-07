package com.example.microservice.dto;

public record ApiResponse<T>(int code, String message, T data) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ApiStatus.SUCCESS.getCode(), "success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ApiStatus.SUCCESS.getCode(), message, data);
    }

    public static <T> ApiResponse<T> error(ApiStatus status, String message) {
        return new ApiResponse<>(status.getCode(), message, null);
    }
}
