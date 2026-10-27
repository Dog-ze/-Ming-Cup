package com.example.backend.admin.auth.vo;

import java.time.LocalDateTime;

public record AdminLoginVO(
        Long id,
        String username,
        String displayName,
        String token,
        LocalDateTime expiresAt
) {
}