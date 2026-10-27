package com.example.backend.admin.data.vo;

// 答案选项数据传输对象
public record AdminAnswerOptionVO(
        Integer code,
        String label,
        long count,
        double percent
) {
}