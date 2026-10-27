package com.example.backend.admin.userdata.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 品茶记录弹窗使用的只读数据。 */
public record AdminUserTeaRecordVO(
        Long id,
        String teaName,
        String category,
        String brand,
        String acquisitionMethod,
        BigDecimal price,
        Integer brewTemp,
        Integer rating,
        String comment,
        List<String> images,
        LocalDateTime createdAt
) {
}
