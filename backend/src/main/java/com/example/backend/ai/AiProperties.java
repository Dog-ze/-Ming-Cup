package com.example.backend.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.ai.deepseek")
public class AiProperties {
    private String baseUrl;
    private String apiKey;
    private String model;
    /** 单个用户在一分钟内最多生成的泡茶建议次数，避免接口被频繁调用。 */
    private int brewGuideRequestsPerMinute = 5;
    /** 单个用户在一分钟内最多发送的问茶次数。 */
    private int chatRequestsPerMinute = 5;
    private int brewGuideMaxTokens = 500;
    private int chatMaxTokens = 800;
    private int connectTimeoutSeconds = 5;
    private int readTimeoutSeconds = 30;
}
