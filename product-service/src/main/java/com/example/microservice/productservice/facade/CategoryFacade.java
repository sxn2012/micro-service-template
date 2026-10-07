package com.example.microservice.productservice.facade;

import com.example.microservice.dto.CategoryResponse;
import com.example.microservice.dto.CreateCategoryRequest;
import com.example.microservice.productservice.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryFacade {

    private final CategoryService categoryService;

    public CategoryResponse createCategory(CreateCategoryRequest request) {
        return CategoryResponse.from(categoryService.createCategory(request));
    }

    public Optional<CategoryResponse> getCategoryById(Long id) {
        return categoryService.getCategoryById(id).map(CategoryResponse::from);
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryService.getAllCategories().stream().map(CategoryResponse::from).toList();
    }

    public CategoryResponse updateCategory(Long id, CreateCategoryRequest request) {
        return CategoryResponse.from(categoryService.updateCategory(id, request));
    }

    public void deleteCategory(Long id) {
        categoryService.deleteCategory(id);
    }
}
