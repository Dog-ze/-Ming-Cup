package com.example.backend.admin.userdata.mapper;

/** 一道问卷题目中某个选项的数量。 */
public record QuestionnaireStatisticRow(String questionKey, Integer answerCode, long answerCount) {
}
