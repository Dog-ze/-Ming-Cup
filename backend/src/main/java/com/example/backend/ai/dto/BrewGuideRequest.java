package com.example.backend.ai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** 前端请求 AI 泡法时只提交茶叶编号与当前地点。 */
public record BrewGuideRequest(
        @NotNull(message = "茶叶编号不能为空") @Positive(message = "茶叶编号不正确") Long teaId,
        @Size(max = 32, message = "城市名称不能超过 32 个字符")
        @Pattern(regexp = "^[\\p{IsHan}A-Za-z]*$", message = "城市名称格式不正确") String city,
        @DecimalMin(value = "-180.0", message = "经度不正确")
        @DecimalMax(value = "180.0", message = "经度不正确") BigDecimal longitude,
        @DecimalMin(value = "-90.0", message = "纬度不正确")
        @DecimalMax(value = "90.0", message = "纬度不正确") BigDecimal latitude,
        @NotBlank(message = "请求标识不能为空")
        @Size(max = 64, message = "请求标识不能超过 64 个字符")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "请求标识格式不正确") String requestId
) {
    /** 仅用于保留既有单元测试的构造方式；HTTP 请求仍必须提供 requestId。 */
    public BrewGuideRequest(Long teaId, String city, BigDecimal longitude, BigDecimal latitude) {
        this(teaId, city, longitude, latitude, "test-request");
    }
}
