package com.example.microservice.productservice.mapper;

import com.example.microservice.model.Category;
import org.springframework.data.repository.CrudRepository;

public interface CategoryRepository extends CrudRepository<Category, Long> {
}
