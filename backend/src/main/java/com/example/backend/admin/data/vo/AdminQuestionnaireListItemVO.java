package com.example.backend.admin.data.vo;

import java.time.LocalDateTime;

// 管理员端问卷列表项数据。
public record AdminQuestionnaireListItemVO(
        Long id,
        Long userId,
        String username,
        String purpose,
        String constitutionType,
        String insomnia,
        String flavorPreference,
        String strengthPreference,
        LocalDateTime updatedAt
) {
}