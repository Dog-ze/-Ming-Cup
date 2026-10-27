package com.example.backend.location;

import java.math.BigDecimal;

public interface ReverseGeocodeClient {
    String resolveCity(BigDecimal longitude, BigDecimal latitude);
}
