package com.example.backend.weather;

import com.example.backend.common.Result;
import com.example.backend.weather.vo.WeatherResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/weather")
@AllArgsConstructor
public class WeatherController {
    private final WeatherService weatherService;

    @GetMapping
    public Result<WeatherResponse> getWeather(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) BigDecimal latitude
    ) {
        return Result.ok(weatherService.getWeather(city, longitude, latitude));
    }
}