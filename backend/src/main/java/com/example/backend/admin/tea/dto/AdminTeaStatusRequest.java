package com.example.backend.admin.tea.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminTeaStatusRequest(
        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态只能是0或1")
        @Max(value = 1, message = "状态只能是0或1")
        Integer status
) {
}