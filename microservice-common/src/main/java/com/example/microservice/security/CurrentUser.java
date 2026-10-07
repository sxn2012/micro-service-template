package com.example.microservice.security;

public record CurrentUser(Long id, String username, String role) {
}
