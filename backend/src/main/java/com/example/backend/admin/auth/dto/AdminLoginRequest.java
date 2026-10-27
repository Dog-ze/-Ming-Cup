package com.example.backend.admin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLoginRequest(
        @NotBlank(message = "管理员账号不能为空")
        @Size(max = 32, message = "管理员账号不能超过 32 个字符")
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 72, message = "密码长度应为 6 至 72 个字符")
        String password
) {
}