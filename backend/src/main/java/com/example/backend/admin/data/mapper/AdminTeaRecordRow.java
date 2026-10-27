package com.example.backend.admin.data.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminTeaRecordRow(
        Long id,
        Long userId,
        String username,
        String teaName,
        String category,
        String brand,
        // 采买方式
        String acquisitionMethod,
        BigDecimal price,
        Integer brewTemp,
        Integer rating,
        String comment,
        String images,
        LocalDateTime createdAt
) {
}