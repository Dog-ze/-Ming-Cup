package com.example.backend.point.vo;

import java.util.List;

public record PointTransactionPageVO(List<PointTransactionVO> records, long page, long pageSize, long total) {
}
