package com.example.backend.admin.user.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(@NotNull(message = "用户状态不能为空") Integer status) {
}