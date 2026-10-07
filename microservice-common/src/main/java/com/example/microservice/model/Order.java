package com.example.microservice.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Table
public class Order {
    @Id
    private Long id;
    private Long userId;
    private BigDecimal totalAmount;
    private String status;
    @MappedCollection(idColumn = "order_id")
    private Set<OrderItem> items = new HashSet<>();
    private LocalDateTime createdAt;
}
