package com.example.backend.config;

import com.example.backend.weather.WeatherProperties;
import com.example.backend.weather.vo.WeatherResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@AllArgsConstructor
public class WeatherCacheConfig {

    private final WeatherProperties weatherProperties;

    /*
     * 创建天气缓存。
     * 缓存时间从 application.yml 的 cache-minutes 读取。
     */
    @Bean
    public Cache<String, WeatherResponse> weatherCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(
                        Duration.ofMinutes(weatherProperties.getCacheMinutes())
                )
                .maximumSize(200)
                .build();
    }

    @Bean
    public Cache<String, WeatherResponse> lastSuccessfulWeatherCache() {
        return Caffeine.newBuilder()
                .maximumSize(200)
                .build();
    }
}
