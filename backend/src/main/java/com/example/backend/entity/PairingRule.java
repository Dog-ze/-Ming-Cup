package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("pairing_rule")
public class PairingRule {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tea_category")
    private String teaCategory;

    private String foods;
    private String advice;

    @TableField("updated_at")
    private java.time.LocalDateTime updatedAt;
}