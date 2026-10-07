package com.example.microservice.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Table
public class Cart {
    @Id
    private Long id;
    private Long userId;
    @MappedCollection(idColumn = "cart_id")
    private Set<CartItem> items = new HashSet<>();
    private LocalDateTime createdAt;
}
