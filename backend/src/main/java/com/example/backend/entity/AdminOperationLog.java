package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("admin_operation_log")
public class AdminOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 管理员ID
    @TableField("admin_id")
    private Long adminId;

    // 模块
    private String module;

    // 操作
    private String action;

    // 目标ID
    @TableField("target_id")
    private Long targetId;

    // 详情
    private String detail;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime createdAt;
}