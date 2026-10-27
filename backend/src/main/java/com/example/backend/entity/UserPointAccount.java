package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_point_account")
public class UserPointAccount {
    @TableId("user_id") private Long userId;
    private Integer balance;
}
