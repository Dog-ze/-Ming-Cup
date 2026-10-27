package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("questionnaire")
public class Questionnaire {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    private Integer purpose;
    @TableField("constitution_type")
    private Integer constitutionType;
    private Integer insomnia;
    @TableField("flavor_preference")
    private Integer flavorPreference;
    @TableField("strength_preference")
    private Integer strengthPreference;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
