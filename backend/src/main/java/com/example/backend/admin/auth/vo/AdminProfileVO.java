package com.example.backend.admin.auth.vo;

import java.time.LocalDateTime;

public record AdminProfileVO(
        Long id,
        String username,
        String displayName,
        LocalDateTime expiresAt
) {
}