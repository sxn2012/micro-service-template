package com.example.microservice.productservice.service;

import com.example.microservice.dto.CreateCategoryRequest;
import com.example.microservice.model.Category;
import com.example.microservice.productservice.mapper.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @CacheEvict(value = "categories_cache", allEntries = true)
    public Category createCategory(CreateCategoryRequest request) {
        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        return categoryRepository.save(category);
    }

    @Cacheable(value = "categories_cache", key = "'id-' + #id")
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    @Cacheable(value = "categories_cache", key = "'all'")
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        categoryRepository.findAll().forEach(categories::add);
        return categories;
    }

    @CacheEvict(value = "categories_cache", allEntries = true)
    public Category updateCategory(Long id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
        category.setName(request.name());
        category.setDescription(request.description());
        return categoryRepository.save(category);
    }

    @CacheEvict(value = "categories_cache", allEntries = true)
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
