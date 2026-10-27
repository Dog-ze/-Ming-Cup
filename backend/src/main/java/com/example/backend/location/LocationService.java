package com.example.backend.location;

import com.example.backend.common.BizException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@AllArgsConstructor
public class LocationService {
    private final ReverseGeocodeClient reverseGeocodeClient;

    public String reverseGeocode(BigDecimal longitude, BigDecimal latitude) {
        validateCoordinates(longitude, latitude);
        String city;
        try {
            city = reverseGeocodeClient.resolveCity(longitude, latitude);
        } catch (BizException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BizException(502, "城市识别服务暂不可用，请稍后再试");
        }
        if (city == null || city.isBlank()) {
            throw new BizException(502, "暂时无法识别所在城市，请稍后再试");
        }
        return city;
    }

    private void validateCoordinates(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null
                || longitude.compareTo(new BigDecimal("-180")) < 0
                || longitude.compareTo(new BigDecimal("180")) > 0
                || latitude.compareTo(new BigDecimal("-90")) < 0
                || latitude.compareTo(new BigDecimal("90")) > 0) {
            throw new BizException(400, "经纬度不正确");
        }
    }
}
