package com.example.backend.admin.operationlog.mapper;

import lombok.Data;

import java.time.LocalDateTime;

// 临时装住“操作日志查询结果”的一行数据。
@Data
public class AdminOperationLogQueryRow {
    private Long id;
    private Long adminId;
    private String adminUsername;
    private String adminDisplayName;
    private String module;
    private String action;
    private Long targetId;
    private String detail;
    private LocalDateTime createdAt;
}