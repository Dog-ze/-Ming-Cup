package com.example.backend.admin.data.mapper;

public record QuestionnaireStatisticRow(
        String questionKey,
        Integer answerCode,
        long answerCount
) {
}