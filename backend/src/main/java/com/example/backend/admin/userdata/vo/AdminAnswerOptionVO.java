package com.example.backend.admin.userdata.vo;

/** 问卷单项答案的统计值。 */
public record AdminAnswerOptionVO(Integer code, String label, long count, double percent) {
}
