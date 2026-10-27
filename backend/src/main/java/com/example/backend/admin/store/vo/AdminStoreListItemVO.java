package com.example.backend.admin.store.vo;

import java.time.LocalDateTime;
import java.util.List;

public record AdminStoreListItemVO(
        Long id,
        String storeName,
        String coverImage,
        String address,
        List<String> teaCategories,
        Integer status,
        Integer promotionEnabled,
        LocalDateTime promotionStartAt,
        LocalDateTime promotionEndAt,
        LocalDateTime updatedAt
) {
}
