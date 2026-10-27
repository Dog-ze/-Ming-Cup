package com.example.backend.admin.user.vo;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserDetailVO(Long id, String username, String phone, String email,
                                String avatar, String city, Integer status, LocalDateTime createdAt,
                                boolean questionnaireCompleted, Integer pointsBalance,
                                List<AdminTeaRecordSummaryVO> recentTeaRecords) {
}
