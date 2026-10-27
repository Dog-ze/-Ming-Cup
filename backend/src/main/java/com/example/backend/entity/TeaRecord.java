package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tea_record")
public class TeaRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("tea_name")
    private String teaName;
    private String category;
    private String brand;
    @TableField("acquisition_method")
    private String acquisitionMethod;
    private BigDecimal price;
    @TableField("brew_temp")
    private Integer brewTemp;
    private Integer rating;
    private String comment;
    private String images;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
