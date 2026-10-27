package com.example.backend.admin.dashboard.vo;

import java.time.LocalDate;

/**
 * 用户增长数据点
 *
 * @param date 日期
 * @param newUserCount 新增用户数
 */
public record UserGrowthPointVO(LocalDate date, long newUserCount) {
}