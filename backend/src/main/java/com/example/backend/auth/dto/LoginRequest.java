package com.example.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "登录方式不能为空")
        String loginType,

        @NotBlank(message = "手机号或邮箱不能为空")
        @Size(max = 128, message = "手机号或邮箱不能超过 128 个字符")
        String account,

        @NotBlank(message = "密码不能为空")
        @Size(max = 32, message = "密码不能超过 32 个字符")
        String password
) {
}
