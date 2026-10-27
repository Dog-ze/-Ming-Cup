package com.example.backend.admin.user.vo;

import java.time.LocalDateTime;

public record AdminUserListItemVO(Long id, String username, String phone, String email,
                                  String city, Integer status, Integer pointsBalance, LocalDateTime createdAt) {
}
