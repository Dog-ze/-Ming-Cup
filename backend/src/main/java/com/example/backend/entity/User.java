package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class User {
    @TableId(type = IdType.AUTO) private Long id;
    private String username;
    private String phone;
    private String email;
    private String avatar;
    @TableField("password_hash") private String passwordHash;
    private String city;
    private BigDecimal longitude;
    private BigDecimal latitude;
    /** 0 停用，1 启用。 */
    private Integer status;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}