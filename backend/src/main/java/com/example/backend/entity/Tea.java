package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tea")
public class Tea {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private String nature;
    private String caffeine;
    @TableField("flavor_tags")
    private String flavorTags;
    @TableField("effect_tags")
    private String effectTags;
    @TableField("suitable_strength")
    private String suitableStrength;
    @TableField("brew_temp")
    private Integer brewTemp;
    @TableField("brew_time")
    private Integer brewTime;
    private String source;
    private Integer enabled;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("image_url")
    private String imageUrl;

}
