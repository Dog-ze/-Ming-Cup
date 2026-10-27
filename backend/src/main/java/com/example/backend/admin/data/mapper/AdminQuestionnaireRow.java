package com.example.backend.admin.data.mapper;

import java.time.LocalDateTime;

public record AdminQuestionnaireRow(
        Long id,
        Long userId,
        String username,
        // 喝茶的目的
        Integer purpose,
        // 体质类型
        Integer constitutionType,
        // 睡眠情况
        Integer insomnia,
        // 口味偏好
        Integer flavorPreference,
        // 浓度偏好
        Integer strengthPreference,
        LocalDateTime updatedAt
) {
}