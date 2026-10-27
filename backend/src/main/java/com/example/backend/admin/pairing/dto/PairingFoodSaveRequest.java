package com.example.backend.admin.pairing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PairingFoodSaveRequest(
        @NotBlank(message = "食品名称不能为空")
        @Size(max = 30, message = "食品名称不能超过30个字符")
        String name,

        @Size(max = 500, message = "食品图片地址不能超过500个字符")
        String imageUrl
) {
}