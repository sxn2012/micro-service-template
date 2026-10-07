package com.example.microservice.productservice.controller;

import com.example.microservice.dto.ApiResponse;
import com.example.microservice.dto.ApiStatus;
import com.example.microservice.dto.CategoryResponse;
import com.example.microservice.dto.CreateCategoryRequest;
import com.example.microservice.productservice.facade.CategoryFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryFacade categoryFacade;

    @PostMapping
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.success("Category created", categoryFacade.createCategory(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return categoryFacade.getCategoryById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(ApiStatus.NOT_FOUND, "Category not found"));
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAllCategories() {
        return ApiResponse.success(categoryFacade.getAllCategories());
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable Long id, @Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.success("Category updated", categoryFacade.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        categoryFacade.deleteCategory(id);
        return ApiResponse.success("Category deleted", null);
    }
}
