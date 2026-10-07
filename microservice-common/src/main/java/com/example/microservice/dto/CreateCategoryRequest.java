package com.example.microservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 100, message = "分类名称过长")
        String name,
        @Size(max = 1000, message = "分类描述过长")
        String description) {
}
