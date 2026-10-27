package com.example.backend.weather.dto;

import java.math.BigDecimal;

public record GeoLocation(String city, BigDecimal longitude, BigDecimal latitude) {
}