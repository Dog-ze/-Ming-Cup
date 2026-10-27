package com.example.backend.admin.data.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminTeaRecordListItemVO(
        Long id,
        Long userId,
        String username,
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