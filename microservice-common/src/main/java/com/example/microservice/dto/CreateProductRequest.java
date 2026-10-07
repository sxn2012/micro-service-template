package com.example.microservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "商品名称不能为空")
        @Size(max = 200, message = "商品名称过长")
        String name,
        @Size(max = 2000, message = "商品描述过长")
        String description,
        @NotNull(message = "价格不能为空")
        @DecimalMin(value = "0.01", message = "价格必须大于 0")
        BigDecimal price,
        @NotNull(message = "库存不能为空")
        @Min(value = 0, message = "库存不能为负数")
        Integer stock,
        @Size(max = 500, message = "图片 URL 过长")
        String imageUrl,
        Long categoryId) {
}
