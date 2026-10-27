package com.example.backend.admin.user.vo;

import java.time.LocalDateTime;

public record AdminTeaRecordSummaryVO(Long id, String teaName, String category,
                                      Integer rating, LocalDateTime createdAt) {
}