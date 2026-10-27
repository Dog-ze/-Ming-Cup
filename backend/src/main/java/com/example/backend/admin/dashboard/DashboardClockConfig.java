package com.example.backend.admin.dashboard;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * DashboardClockConfig 用于配置仪表盘的时钟。
 * 该时钟使用北京时间（Asia/Shanghai）。
 */
@Configuration
public class DashboardClockConfig {
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    @Bean
    public Clock dashboardClock() {
        return Clock.system(BUSINESS_ZONE);
    }
}