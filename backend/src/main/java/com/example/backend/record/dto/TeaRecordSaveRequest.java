package com.example.backend.record.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record TeaRecordSaveRequest(
        @NotBlank(message = "茶名不能为空")
        @Size(max = 64, message = "茶名不能超过 64 个字符")
        String teaName,

        @NotBlank(message = "请选择茶类")
        @Pattern(regexp = "绿茶|白茶|黄茶|青茶|红茶|黑茶|花草", message = "茶类不正确")
        String category,

        @Size(max = 64, message = "品牌不能超过 64 个字符")
        String brand,

        @NotBlank(message = "请选择获取方式")
        @Pattern(regexp = "线上购买|线下购买|朋友赠送|其他", message = "获取方式不正确")
        String acquisitionMethod,

        @DecimalMin(value = "0.00", message = "价格不能小于 0")
        BigDecimal price,

        @Min(value = 0, message = "水温必须在 0 到 100 之间")
        @Max(value = 100, message = "水温必须在 0 到 100 之间")
        Integer brewTemp,

        @Min(value = 1, message = "评分必须在 1 到 5 之间")
        @Max(value = 5, message = "评分必须在 1 到 5 之间")
        Integer rating,

        @Size(max = 500, message = "评价不能超过 500 个字符")
        String comment,

        @Size(max = 9, message = "最多上传 9 张图片")
        List<
                @Pattern(
                        regexp = "(?:/uploads/records/[0-9a-fA-F-]+\\.(?:jpg|png|webp)|https?://.+)",
                        message = "图片地址不正确"
                ) String
                > images
) {
}