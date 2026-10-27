package com.example.backend.admin.tea.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminTeaSaveRequest(
        @NotBlank(message = "茶叶名称不能为空") @Size(max = 64, message = "茶叶名称不能超过64个字符") String name,
        @NotBlank(message = "茶叶分类不能为空") @Size(max = 16, message = "茶叶分类不能超过16个字符") String category,
        @NotBlank(message = "茶性不能为空") @Size(max = 8, message = "茶性不能超过8个字符") String nature,
        @NotBlank(message = "咖啡因等级不能为空") @Size(max = 8, message = "咖啡因等级不能超过8个字符") String caffeine,
        @NotBlank(message = "口感标签不能为空") String flavorTags,
        @NotBlank(message = "饮用场景标签不能为空") String effectTags,
        @NotBlank(message = "推荐浓度不能为空") @Size(max = 10, message = "推荐浓度不能超过10个字符") String suitableStrength,
        @Min(value = 1, message = "冲泡水温必须大于0") @Max(value = 100, message = "冲泡水温不能超过100") Integer brewTemp,
        @Min(value = 1, message = "冲泡时间必须大于0") @Max(value = 3600, message = "冲泡时间不能超过3600秒") Integer brewTime,
        @NotBlank(message = "数据来源不能为空") @Size(max = 16, message = "数据来源不能超过16个字符") String source,
        @Size(max = 500, message = "茶叶图片地址不能超过500个字符") String imageUrl,
        @NotNull(message = "启用状态不能为空") @Min(value = 0, message = "启用状态只能是0或1") @Max(value = 1, message = "启用状态只能是0或1") Integer enabled
) {
}