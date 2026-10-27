package com.example.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[\\p{IsHan}A-Za-z0-9_]{1,10}$", message = "用户名长度为 1～10 个字符，只能包含中文、字母、数字或下划线")
        String username,

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的 11 位中国大陆手机号")
        String phone,

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "请输入正确的邮箱地址")
        @Size(max = 128, message = "邮箱不能超过 128 个字符")
        String email,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度应为 6～32 位")
        String password
) {
}
