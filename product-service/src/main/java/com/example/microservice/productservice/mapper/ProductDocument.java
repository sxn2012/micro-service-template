package com.example.microservice.productservice.mapper;

import lombok.Data;

@Data
public class ProductDocument {

    private Long id;

    private String name;

    private String description;

    private Double price;

    private Long categoryId;
}
