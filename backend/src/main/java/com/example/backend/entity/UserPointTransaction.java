package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_point_transaction")
public class UserPointTransaction {
    @TableId(type = IdType.AUTO) private Long id;
    @TableField("user_id") private Long userId;
    private String type;
    @TableField("change_amount") private Integer changeAmount;
    @TableField("balance_after") private Integer balanceAfter;
    @TableField("business_key") private String businessKey;
    @TableField("related_record_id") private Long relatedRecordId;
    private String status;
    private String description;
    @TableField("admin_id") private Long adminId;
    @TableField("created_at") private LocalDateTime createdAt;
}
