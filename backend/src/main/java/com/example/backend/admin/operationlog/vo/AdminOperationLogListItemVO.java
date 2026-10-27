package com.example.backend.admin.operationlog.vo;

import java.time.LocalDateTime;

public record AdminOperationLogListItemVO(
        Long id,
        Long adminId,
        String adminUsername,
        String adminDisplayName,
        String module,
        String action,
        Long targetId,
        String detail,
        LocalDateTime createdAt
) {
}