package com.example.microservice.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table
public class User {
    @Id
    private Long id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private String address;
    private String role;
    private LocalDateTime createdAt;
}
