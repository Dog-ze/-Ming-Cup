package com.example.backend.admin.userdata.vo;

import java.util.List;

/** 问卷统计抽屉使用的完整数据。 */
public record AdminQuestionnaireStatisticsVO(
        long sampleCount,
        List<AdminAnswerOptionVO> purposes,
        List<AdminAnswerOptionVO> constitutionTypes,
        List<AdminAnswerOptionVO> insomniaStatuses,
        List<AdminAnswerOptionVO> flavorPreferences,
        List<AdminAnswerOptionVO> strengthPreferences
) {
}
