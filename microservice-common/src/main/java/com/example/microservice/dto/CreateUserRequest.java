package com.example.microservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 50, message = "用户名过长")
        String username,
        @Size(min = 6, message = "密码至少 6 位")
        String password,
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱过长")
        String email,
        @Size(max = 20, message = "电话过长")
        String phone,
        @Size(max = 200, message = "地址过长")
        String address) {
}
