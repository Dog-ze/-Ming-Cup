package com.example.backend.admin.userdata.mapper;

import java.time.LocalDateTime;

/** 数据库中保存的问卷答案编码。 */
public record AdminUserQuestionnaireRow(
        Integer purpose,
        Integer constitutionType,
        Integer insomnia,
        Integer flavorPreference,
        Integer strengthPreference,
        LocalDateTime updatedAt
) {
}
