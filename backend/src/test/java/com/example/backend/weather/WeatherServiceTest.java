package com.example.backend.weather;

import com.example.backend.common.BizException;
import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import com.example.backend.security.CurrentUser;
import com.example.backend.weather.client.OpenMeteoClient;
import com.example.backend.weather.dto.GeoLocation;
import com.example.backend.weather.vo.WeatherResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private OpenMeteoClient openMeteoClient;

    @Mock
    private UserMapper userMapper;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        WeatherProperties properties = new WeatherProperties();
        Cache<String, WeatherResponse> weatherCache = Caffeine.newBuilder().build();
        Cache<String, WeatherResponse> lastSuccessfulWeatherCache = Caffeine.newBuilder().build();
        weatherService = new WeatherService(
                openMeteoClient,
                properties,
                userMapper,
                weatherCache,
                lastSuccessfulWeatherCache
        );
    }

    @Test
    void cityLookupFailureUsesDefaultCityInsteadOfThrowingException() {
        when(openMeteoClient.geocode("杭州市"))
                .thenThrow(new RuntimeException("天气网站连接失败"));
        when(openMeteoClient.fetch(
                "北京市",
                new BigDecimal("116.4074"),
                new BigDecimal("39.9042")
        )).thenReturn(weather("北京市"));

        WeatherResponse result = weatherService.getWeather("杭州市", null, null);

        assertThat(result.city()).isEqualTo("北京市");
    }

    @Test
    void cityNameEndingWithShiRetriesWithoutSuffix() {
        GeoLocation wuxi = new GeoLocation(
                "无锡",
                new BigDecimal("120.29"),
                new BigDecimal("31.57")
        );
        when(openMeteoClient.geocode("无锡市")).thenReturn(null);
        when(openMeteoClient.geocode("无锡")).thenReturn(wuxi);
        when(openMeteoClient.fetch(
                "无锡市",
                new BigDecimal("120.29"),
                new BigDecimal("31.57")
        )).thenReturn(weather("无锡市"));

        WeatherResponse result = weatherService.getWeather("无锡市", null, null);

        assertThat(result.city()).isEqualTo("无锡市");
        verify(openMeteoClient).geocode("无锡");
    }

    @Test
    void sameLocationUsesCache() {
        GeoLocation hangzhou = new GeoLocation(
                "杭州市",
                new BigDecimal("120.1551"),
                new BigDecimal("30.2741")
        );
        when(openMeteoClient.geocode("杭州市")).thenReturn(hangzhou);
        when(openMeteoClient.fetch(
                "杭州市",
                new BigDecimal("120.1551"),
                new BigDecimal("30.2741")
        )).thenReturn(weather("杭州市"));

        weatherService.getWeather("杭州市", null, null);
        weatherService.getWeather("杭州市", null, null);

        verify(openMeteoClient, times(1)).fetch(
                "杭州市",
                new BigDecimal("120.1551"),
                new BigDecimal("30.2741")
        );
    }

    @Test
    void loggedInUserUsesSavedCoordinatesWhenRequestHasNoLocation() {
        CurrentUser.set(1L);
        User user = new User();
        user.setCity("杭州市");
        user.setLongitude(new BigDecimal("120.1551"));
        user.setLatitude(new BigDecimal("30.2741"));

        when(userMapper.selectById(1L)).thenReturn(user);
        when(openMeteoClient.fetch(
                "杭州市",
                new BigDecimal("120.1551"),
                new BigDecimal("30.2741")
        )).thenReturn(weather("杭州市"));

        WeatherResponse result = weatherService.getWeather(null, null, null);

        assertThat(result.city()).isEqualTo("杭州市");
    }

    @Test
    void incompleteCoordinatesAreRejected() {
        assertThatThrownBy(() -> weatherService.getWeather(
                null,
                new BigDecimal("120.1551"),
                null
        ))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(400));
    }

    @Test
    void externalWeatherFailureUsesRecentSuccessfulWeather() {
        BigDecimal longitude = new BigDecimal("120.1551");
        BigDecimal latitude = new BigDecimal("30.2741");
        WeatherResponse successfulWeather = weather("杭州市");
        Cache<String, WeatherResponse> lastSuccessfulWeatherCache = Caffeine.newBuilder().build();
        lastSuccessfulWeatherCache.put("120.1551,30.2741", successfulWeather);

        WeatherProperties properties = new WeatherProperties();
        weatherService = new WeatherService(
                openMeteoClient,
                properties,
                userMapper,
                Caffeine.<String, WeatherResponse>newBuilder().build(),
                lastSuccessfulWeatherCache
        );
        when(openMeteoClient.fetch("杭州市", longitude, latitude))
                .thenThrow(new RuntimeException("天气网站连接失败"));

        WeatherResponse result = weatherService.getWeather("杭州市", longitude, latitude);

        assertThat(result.source()).isEqualTo("stale_cache");
        assertThat(result.updatedAt()).isEqualTo(successfulWeather.updatedAt());
    }

    @Test
    void expiredSuccessfulWeatherFallsBackToDefaultWeather() {
        BigDecimal longitude = new BigDecimal("120.1551");
        BigDecimal latitude = new BigDecimal("30.2741");
        WeatherResponse expiredWeather = new WeatherResponse(
                "杭州市", longitude, latitude, new BigDecimal("26.8"), 72,
                "多云", "mild", "open-meteo",
                OffsetDateTime.now(ZoneOffset.ofHours(8)).minusMinutes(361)
        );
        Cache<String, WeatherResponse> lastSuccessfulWeatherCache = Caffeine.newBuilder().build();
        lastSuccessfulWeatherCache.put("120.1551,30.2741", expiredWeather);
        weatherService = new WeatherService(
                openMeteoClient,
                new WeatherProperties(),
                userMapper,
                Caffeine.<String, WeatherResponse>newBuilder().build(),
                lastSuccessfulWeatherCache
        );
        when(openMeteoClient.fetch("杭州市", longitude, latitude))
                .thenThrow(new RuntimeException("天气网站连接失败"));

        WeatherResponse result = weatherService.getWeather("杭州市", longitude, latitude);

        assertThat(result.source()).isEqualTo("fallback");
        assertThat(result.temperature()).isEqualByComparingTo("22.0");
    }

    @AfterEach
    void clearCurrentUser() {
        CurrentUser.clear();
    }

    private WeatherResponse weather(String city) {
        return new WeatherResponse(
                city,
                new BigDecimal("120.1551"),
                new BigDecimal("30.2741"),
                new BigDecimal("26.8"),
                72,
                "多云",
                "mild",
                "mock",
                OffsetDateTime.now(ZoneOffset.ofHours(8))
        );
    }
}
