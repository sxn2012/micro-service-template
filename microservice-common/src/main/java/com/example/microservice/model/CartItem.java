package com.example.microservice.model;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class CartItem {
    @Id
    private Long id;
    private Long productId;
    private Integer quantity;
}
