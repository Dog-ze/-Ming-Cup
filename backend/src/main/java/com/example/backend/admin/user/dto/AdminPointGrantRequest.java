package com.example.backend.admin.user.dto;
import jakarta.validation.constraints.*;
public record AdminPointGrantRequest(@Min(value=1,message="发放积分必须大于 0") @Max(value=10000,message="单次发放不能超过 10000 积分") int amount, @NotBlank(message="请填写发放原因") @Size(max=100,message="发放原因不能超过 100 个字符") String reason) { }
