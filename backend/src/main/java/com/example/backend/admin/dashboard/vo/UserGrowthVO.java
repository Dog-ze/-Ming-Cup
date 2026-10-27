package com.example.backend.admin.dashboard.vo;

import java.util.List;

/**
 * 用户增长数据。
 *
 * @param days          统计的天数
 * @param totalNewUsers 新增用户总数
 * @param points        每天的新增用户数据点
 */
public record UserGrowthVO(int days, long totalNewUsers, List<UserGrowthPointVO> points) {
}