package com.example.backend.weather.client;

import com.example.backend.weather.dto.GeoLocation;
import com.example.backend.weather.vo.WeatherResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class OpenMeteoClient {
    private final RestClient.Builder restClientBuilder;

    public GeoLocation geocode(String city) {
        Map<?, ?> body = restClientBuilder.baseUrl("https://geocoding-api.open-meteo.com")
                .build()
                .get()
                .uri(builder -> builder.path("/v1/search")
                        .queryParam("name", city)
                        .queryParam("count", 1)
                        .queryParam("language", "zh")
                        .build())
                .retrieve()
                .body(Map.class);

        List<?> results = body == null ? null : (List<?>) body.get("results");
        if (results == null || results.isEmpty() || !(results.get(0) instanceof Map<?, ?> first)) {
            return null;
        }
        return new GeoLocation(
                String.valueOf(first.get("name")),
                new BigDecimal(String.valueOf(first.get("longitude"))),
                new BigDecimal(String.valueOf(first.get("latitude")))
        );
    }

    public WeatherResponse fetch(String city, BigDecimal longitude, BigDecimal latitude) {
        Map<?, ?> body = restClientBuilder.baseUrl("https://api.open-meteo.com")
                .build()
                .get()
                .uri(builder -> builder.path("/v1/forecast")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("current", "temperature_2m,relative_humidity_2m,weather_code")
                        .queryParam("timezone", "Asia/Shanghai")
                        .build())
                .retrieve()
                .body(Map.class);

        Map<?, ?> current = body == null ? null : (Map<?, ?>) body.get("current");
        if (current == null) throw new IllegalStateException("天气服务未返回实时数据");

        BigDecimal temperature = new BigDecimal(String.valueOf(current.get("temperature_2m")));
        Integer humidity = Integer.valueOf(String.valueOf(current.get("relative_humidity_2m")));
        Integer weatherCode = Integer.valueOf(String.valueOf(current.get("weather_code")));

        return new WeatherResponse(
                city, longitude, latitude, temperature, humidity,
                weatherText(weatherCode), climateTag(temperature, humidity),
                "open-meteo", OffsetDateTime.now(ZoneOffset.ofHours(8))
        );
    }

    private String weatherText(int code) {
        return switch (code) {
            case 0 -> "晴";
            case 1, 2, 3 -> "多云";
            case 45, 48 -> "雾";
            case 51, 53, 55, 56, 57 -> "毛毛雨";
            case 61, 63, 65, 66, 67, 80, 81, 82 -> "雨";
            case 71, 73, 75, 77, 85, 86 -> "雪";
            case 95, 96, 99 -> "雷雨";
            default -> "天气未知";
        };
    }

    private String climateTag(BigDecimal temperature, Integer humidity) {
        if (temperature.compareTo(new BigDecimal("30")) >= 0 && humidity >= 70) return "hot_humid";
        if (temperature.compareTo(new BigDecimal("30")) >= 0) return "hot";
        if (temperature.compareTo(new BigDecimal("10")) <= 0) return "cold";
        return "mild";
    }
}