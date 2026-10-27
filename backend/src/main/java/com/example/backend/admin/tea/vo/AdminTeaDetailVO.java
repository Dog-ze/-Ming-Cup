package com.example.backend.admin.tea.vo;

import java.time.LocalDateTime;

public record AdminTeaDetailVO(
        Long id, String name, String category, String nature, String caffeine,
        String flavorTags, String effectTags, String suitableStrength,
        Integer brewTemp, Integer brewTime, String source, String imageUrl, Integer enabled,
        LocalDateTime createdAt
) {
}