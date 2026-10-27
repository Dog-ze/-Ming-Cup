package com.example.backend.point;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointType {
    DAILY_CLAIM("每日领取积分"), QUESTIONNAIRE_FIRST("首次完成问卷奖励"), TEA_RECORD_DAILY("每日首条茶记奖励"),
    AI_BREW_FREE("AI 泡法每日免费"), AI_BREW_DEBIT("AI 泡法扣分"), AI_BREW_REFUND("AI 泡法退款"),
    AI_CHAT_DEBIT("AI 问茶扣分"), AI_CHAT_REFUND("AI 问茶退款"), ADMIN_GRANT("管理员发放积分");
    private final String label;
}
