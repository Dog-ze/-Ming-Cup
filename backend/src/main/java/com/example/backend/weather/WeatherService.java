package com.example.backend.weather;

import com.example.backend.common.BizException;
import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import com.example.backend.security.CurrentUser;
import com.example.backend.weather.client.OpenMeteoClient;
import com.example.backend.weather.dto.GeoLocation;
import com.example.backend.weather.vo.WeatherResponse;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@AllArgsConstructor
public class WeatherService {

    private final OpenMeteoClient openMeteoClient;
    private final WeatherProperties properties;
    private final UserMapper userMapper;
    @Qualifier("weatherCache")
    private final Cache<String, WeatherResponse> weatherCache;
    @Qualifier("lastSuccessfulWeatherCache")
    private final Cache<String, WeatherResponse> lastSuccessfulWeatherCache;

    /*
     * 查询天气的总入口。
     */
    public WeatherResponse getWeather(String city, BigDecimal longitude, BigDecimal latitude) {
        // 先检查经纬度是否正确。
        validateCoordinates(longitude, latitude);
        GeoLocation location = resolveLocation(city, longitude, latitude);
        String cacheKey = location.longitude() + "," + location.latitude();

        WeatherResponse fresh = weatherCache.getIfPresent(cacheKey);
        if (fresh != null) {
            return fresh;
        }

        try {
            WeatherResponse latest = openMeteoClient.fetch(
                    location.city(),
                    location.longitude(),
                    location.latitude()
            );
            weatherCache.put(cacheKey, latest);
            lastSuccessfulWeatherCache.put(cacheKey, latest);
            return latest;
        } catch (Exception ignored) {
            WeatherResponse stale = lastSuccessfulWeatherCache.getIfPresent(cacheKey);
            if (stale != null && !isStaleExpired(stale)) {
                return new WeatherResponse(
                        stale.city(), stale.longitude(), stale.latitude(), stale.temperature(),
                        stale.humidity(), stale.weatherText(), stale.climateTag(),
                        "stale_cache", stale.updatedAt()
                );
            }
            return fallback(location);
        }
    }

    /*
     * 决定使用哪个城市和坐标。
     *
     * 顺序：
     * 1. 前端传了经纬度。
     * 2. 前端传了城市。
     * 3. 登录用户资料中保存的经纬度。
     * 4. 登录用户资料中保存的城市。
     * 5. 默认北京市。
     */
    private GeoLocation resolveLocation(String city, BigDecimal longitude, BigDecimal latitude) {
        if (longitude != null) {
            return new GeoLocation(
                    city == null || city.isBlank() ? "当前位置" : city.trim(),
                    longitude,
                    latitude
            );
        }
        GeoLocation requestCityLocation = geocode(city);
        if (requestCityLocation != null) {
            return requestCityLocation;
        }
        GeoLocation profileLocation = getProfileLocation();
        if (profileLocation != null) {
            return profileLocation;
        }
        return defaultLocation();
    }

    /*
     * 通过城市名称查询经纬度。
     * 查询失败时返回 null，不让接口直接报错。
     */
    private GeoLocation geocode(String city) {
        if (city == null || city.isBlank()) {
            return null;
        }

        String requestedCity = city.trim();
        GeoLocation location = tryGeocode(requestedCity);
        if (location != null) {
            return location;
        }

        // Open-Meteo 对“无锡”“苏州”识别正常，但部分情况下无法识别“无锡市”“苏州市”。
        // 去掉“市”后重试，同时仍保留用户原本选择的完整城市名称用于页面展示。
        if (requestedCity.endsWith("市") && requestedCity.length() > 1) {
            GeoLocation simplifiedLocation = tryGeocode(
                    requestedCity.substring(0, requestedCity.length() - 1)
            );
            if (simplifiedLocation != null) {
                return new GeoLocation(
                        requestedCity,
                        simplifiedLocation.longitude(),
                        simplifiedLocation.latitude()
                );
            }
        }
        return null;
    }

    /**
     * 调用外部地理编码服务；异常时返回 null，交由上层继续尝试或使用默认城市。
     */
    private GeoLocation tryGeocode(String city) {
        try {
            return openMeteoClient.geocode(city);
        } catch (Exception ignored) {
            return null;
        }
    }

    /*
     * 读取当前登录用户资料中的地点。
     * 游客访问时 CurrentUser.idOrNull() 会返回 null。
     */
    private GeoLocation getProfileLocation() {
        Long userId = CurrentUser.idOrNull();
        if (userId == null) {
            return null;
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        if (user.getLongitude() != null && user.getLatitude() != null) {
            return new GeoLocation(
                    user.getCity(),
                    user.getLongitude(),
                    user.getLatitude()
            );
        }
        return geocode(user.getCity());
    }

    /*
     * 检查前端传来的坐标。
     */
    private void validateCoordinates(BigDecimal longitude, BigDecimal latitude) {
        if ((longitude == null) != (latitude == null)) {
            throw new BizException(400, "经度和纬度必须同时传入");
        }
        if (longitude == null) {
            return;
        }

        if (
                longitude.compareTo(new BigDecimal("-180")) < 0
                        || longitude.compareTo(new BigDecimal("180")) > 0
        ) {
            throw new BizException(400, "经度必须在 -180 到 180 之间");
        }

        if (
                latitude.compareTo(new BigDecimal("-90")) < 0
                        || latitude.compareTo(new BigDecimal("90")) > 0
        ) {
            throw new BizException(400, "纬度必须在 -90 到 90 之间");
        }
    }

    /*
     * 北京默认地点。
     */
    private GeoLocation defaultLocation() {
        return new GeoLocation(
                properties.getDefaultCity(),
                properties.getDefaultLongitude(),
                properties.getDefaultLatitude()
        );
    }

    /*
     * 天气网站出错时，返回备用天气。
     */
    private WeatherResponse fallback(GeoLocation location) {
        return new WeatherResponse(
                location.city(),
                location.longitude(),
                location.latitude(),
                new BigDecimal("22.0"),
                60,
                "天气服务暂不可用",
                "mild",
                "fallback",
                OffsetDateTime.now(ZoneOffset.ofHours(8))
        );
    }

    private boolean isStaleExpired(WeatherResponse weather) {
        return weather.updatedAt().isBefore(
                OffsetDateTime.now(ZoneOffset.ofHours(8))
                        .minusMinutes(properties.getStaleCacheMinutes())
        );
    }
}
