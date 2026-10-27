package com.example.backend.weather;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.math.BigDecimal;

@Data
@ConfigurationProperties(prefix = "app.weather")
public class WeatherProperties {
    private int connectTimeoutSeconds = 3;
    private int readTimeoutSeconds = 3;
    private int cacheMinutes = 30;
    private int staleCacheMinutes = 360;
    private String defaultCity = "北京市";
    private BigDecimal defaultLongitude = new BigDecimal("116.4074");
    private BigDecimal defaultLatitude = new BigDecimal("39.9042");
}
