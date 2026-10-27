package com.example.backend.weather.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record WeatherResponse(
        String city,
        BigDecimal longitude,
        BigDecimal latitude,
        BigDecimal temperature,
        Integer humidity,
        String weatherText,
        String climateTag,
        String source,
        OffsetDateTime updatedAt
) {
}