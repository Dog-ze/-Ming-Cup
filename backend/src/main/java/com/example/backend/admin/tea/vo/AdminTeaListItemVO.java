package com.example.backend.admin.tea.vo;

import java.time.LocalDateTime;

public record AdminTeaListItemVO(
        Long id, String name, String category, String nature, String caffeine,
        String source, Integer enabled, LocalDateTime createdAt
) {
}
