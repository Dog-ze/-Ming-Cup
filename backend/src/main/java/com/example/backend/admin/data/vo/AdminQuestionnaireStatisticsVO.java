package com.example.backend.admin.data.vo;

import java.util.List;

// 问卷调查统计数据
public record AdminQuestionnaireStatisticsVO(
        long sampleCount,
        List<AdminAnswerOptionVO> purposes,
        List<AdminAnswerOptionVO> constitutionTypes,
        List<AdminAnswerOptionVO> insomniaStatuses,
        List<AdminAnswerOptionVO> flavorPreferences,
        List<AdminAnswerOptionVO> strengthPreferences
) {
}