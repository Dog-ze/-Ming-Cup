package com.example.backend.point.vo;

import java.time.LocalDateTime;

/** 面向用户的积分流水，不暴露业务幂等键和管理员内部信息。 */
public record PointTransactionVO(
        Long id,
        String type,
        Integer changeAmount,
        Integer balanceAfter,
        String status,
        String description,
        LocalDateTime createdAt
) {
}
