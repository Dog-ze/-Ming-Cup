package com.example.backend.point;

import java.time.LocalDate;


public final class PointPolicy {
    private PointPolicy() { }

    /**
     * 判断 AI 泡法该扣多少分
     * @param freeAlreadyUsed
     * @param configuredCost
     * @return
     */
    public static int brewGuideCost(boolean freeAlreadyUsed, int configuredCost) {
        return freeAlreadyUsed ? configuredCost : 0;
    }

    /**
     * 生成“当天只能做一次”的唯一标记
     * @param type
     * @param userId
     * @param date
     * @return
     */
    public static String dailyKey(String type, Long userId, LocalDate date) {
        return type + ":" + userId + ":" + date;
    }
}
