package com.example.backend.recommend.vo;

import com.example.backend.weather.vo.WeatherResponse;

import java.util.List;

/**
 * 整个推荐接口的返回结果
 * @param weather
 * @param personalized
 * @param recommendations
 */
public record RecommendResponseVO(
        WeatherResponse weather,
        boolean personalized,
        List<RecommendationVO> recommendations
) {
}