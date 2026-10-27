package com.example.backend.admin.store.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminStoreDetailVO(
        Long id,
        String storeName,
        String coverImage,
        String address,
        BigDecimal longitude,
        BigDecimal latitude,
        String phone,
        List<String> teaCategories,
        List<String> foodTags,
        String description,
        Integer promotionEnabled,
        Integer promotionScore,
        LocalDateTime promotionStartAt,
        LocalDateTime promotionEndAt,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
