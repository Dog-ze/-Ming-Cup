package com.example.backend.auth.vo;

import java.time.LocalDateTime;

public record AuthResponse(
        Long id,
        String username,
        String token,
        LocalDateTime expiresAt
) {
}
