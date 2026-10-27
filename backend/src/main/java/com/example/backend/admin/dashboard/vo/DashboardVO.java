package com.example.backend.admin.dashboard.vo;

/**
 * 后台管理系统首页数据统计返回对象。
 *
 * @param totalUserCount 总用户数
 * @param enabledUserCount 启用的用户数
 * @param teaCount 品茶记录总数
 * @param promotionPartnerStoreCount 推广合作门店总数
 * @param activePromotionStoreCount 活跃推广门店总数
 * @param todayTeaRecordCount 今日品茶记录总数
 * @param questionnaireCompletedCount 问卷完成总数
 * @param userGrowth 用户增长数据
 */
public record DashboardVO(
        long totalUserCount,
        long enabledUserCount,
        long teaCount,
        long promotionPartnerStoreCount,
        long activePromotionStoreCount,
        long todayTeaRecordCount,
        long questionnaireCompletedCount,
        UserGrowthVO userGrowth
) {
}