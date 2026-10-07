package com.example.microservice.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table
public class Category {
    @Id
    private Long id;
    private String name;
    private String description;
}
