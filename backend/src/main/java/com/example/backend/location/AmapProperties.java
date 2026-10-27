package com.example.backend.location;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.amap")
public class AmapProperties {
    private String webServiceKey = "";
    private boolean placeSearchEnabled = true;
    private String baseUrl = "https://restapi.amap.com";
    private int connectTimeoutSeconds = 3;
    private int readTimeoutSeconds = 3;
    private int failureCooldownSeconds = 30;
    private int defaultRadiusMeters = 3000;
    private int maxRadiusMeters = 5000;
}
