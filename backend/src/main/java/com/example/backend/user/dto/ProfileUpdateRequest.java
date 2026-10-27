package com.example.backend.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProfileUpdateRequest(
        @Size(max = 255, message = "头像地址不能超过 255 个字符")
        String avatar,

        @NotBlank(message = "城市不能为空")
        @Size(max = 64, message = "城市不能超过 64 个字符")
        @Pattern(regexp = "^(?!\\s*当前位置\\s*$).*$", message = "城市必须填写详细地址")
        String city,

        @DecimalMin(value = "-180.0", message = "经度必须在 -180 到 180 之间")
        @DecimalMax(value = "180.0", message = "经度必须在 -180 到 180 之间")
        BigDecimal longitude,

        @DecimalMin(value = "-90.0", message = "纬度必须在 -90 到 90 之间")
        @DecimalMax(value = "90.0", message = "纬度必须在 -90 到 90 之间")
        BigDecimal latitude
) {
}
