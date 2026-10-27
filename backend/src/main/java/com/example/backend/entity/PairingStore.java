package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 茶食店铺表 pairing_store 对应的实体类。
 */
@Data
@TableName("pairing_store")
public class PairingStore {

    /**
     * 店铺编号。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 店铺名称。
     */
    @TableField("store_name")
    private String storeName;

    /**
     * 店铺封面图片地址。
     */
    @TableField("cover_image")
    private String coverImage;

    /**
     * 店铺详细地址。
     */
    private String address;

    /**
     * 店铺经度。
     */
    private BigDecimal longitude;

    /**
     * 店铺纬度。
     */
    private BigDecimal latitude;

    /**
     * 店铺联系电话。
     */
    private String phone;

    /**
     * 适配茶类 JSON，例如：[\"绿茶\", \"白茶\"]。
     */
    @TableField("tea_categories")
    private String teaCategories;

    /**
     * 推荐食物标签 JSON，例如：[\"清淡糕点\", \"水果\"]。
     */
    @TableField("food_tags")
    private String foodTags;

    /**
     * 店铺推荐说明。
     */
    private String description;

    /**
     * 推广排序分数。仅供后台排序使用，绝不返回给 App。
     */
    @TableField("promotion_score")
    private Integer promotionScore;

    /**
     * 是否启用推广：0 否；1 是。
     */
    @TableField("promotion_enabled")
    private Integer promotionEnabled;

    /**
     * 推广开始时间；为空时表示立即生效。
     */
    @TableField("promotion_start_at")
    private LocalDateTime promotionStartAt;

    /**
     * 推广结束时间；为空时表示不设置结束时间。
     */
    @TableField("promotion_end_at")
    private LocalDateTime promotionEndAt;

    /**
     * 0：下架；1：上架。
     */
    private Integer status;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 最后修改时间。
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
