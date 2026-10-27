package com.example.backend.admin.userdata.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 数据库查询出的单条品茶记录。 */
public record AdminUserTeaRecordRow(
        Long id,
        String teaName,
        String category,
        String brand,
        String acquisitionMethod,
        BigDecimal price,
        Integer brewTemp,
        Integer rating,
        String comment,
        String images,
        LocalDateTime createdAt
) {
}
