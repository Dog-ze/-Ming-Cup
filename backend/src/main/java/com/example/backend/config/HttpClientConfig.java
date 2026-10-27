package com.example.backend.config;

import com.example.backend.location.AmapProperties;
import com.example.backend.ai.AiProperties;
import com.example.backend.weather.WeatherProperties;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@AllArgsConstructor
public class HttpClientConfig {
    private final WeatherProperties weatherProperties;

    @Bean
    public RestClient.Builder restClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(weatherProperties.getConnectTimeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(weatherProperties.getReadTimeoutSeconds()));
        return RestClient.builder().requestFactory(factory);
    }

    /**
     * 高德地图服务使用独立的超时配置，避免修改天气接口的等待时间时影响地图服务。
     */
    @Bean("amapRestClient")
    public RestClient amapRestClient(AmapProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    /** AI 服务使用独立的较长读取超时，不能沿用天气接口的 3 秒超时。 */
    @Bean("deepSeekRestClient")
    public RestClient deepSeekRestClient(AiProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));
        return RestClient.builder().requestFactory(factory).build();
    }
}
