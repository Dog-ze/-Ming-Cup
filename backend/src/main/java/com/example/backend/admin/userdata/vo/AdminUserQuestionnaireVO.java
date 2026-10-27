package com.example.backend.admin.userdata.vo;

import java.time.LocalDateTime;

/** 单个用户的问卷；未填写时 completed 为 false，其余答案为空。 */
public record AdminUserQuestionnaireVO(
        Long userId,
        String username,
        boolean completed,
        String purpose,
        String constitutionType,
        String insomnia,
        String flavorPreference,
        String strengthPreference,
        LocalDateTime updatedAt
) {
}
