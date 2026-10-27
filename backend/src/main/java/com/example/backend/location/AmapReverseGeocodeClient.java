package com.example.backend.location;

import com.example.backend.common.BizException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
class AmapReverseGeocodeClient implements ReverseGeocodeClient {
    private static final Logger LOGGER = Logger.getLogger(AmapReverseGeocodeClient.class.getName());
    private static final String FRIENDLY_ERROR_MESSAGE = "暂时无法识别所在城市，请手动选择城市";
    private final RestClient amapRestClient;
    private final AmapProperties properties;

    AmapReverseGeocodeClient(
            @Qualifier("amapRestClient") RestClient amapRestClient,
            AmapProperties properties
    ) {
        this.amapRestClient = amapRestClient;
        this.properties = properties;
    }

    @Override
    public String resolveCity(BigDecimal longitude, BigDecimal latitude) {
        if (properties.getWebServiceKey() == null || properties.getWebServiceKey().isBlank()) {
            LOGGER.warning("Amap reverse geocoding is unavailable because the Web service key is missing");
            throw new BizException(503, FRIENDLY_ERROR_MESSAGE);
        }

        Map<?, ?> response;
        try {
            response = amapRestClient.get()
                    .uri(builder -> builder.path("/v3/geocode/regeo")
                            .queryParam("key", properties.getWebServiceKey())
                            .queryParam("location", longitude + "," + latitude)
                            .queryParam("extensions", "base")
                            .queryParam("output", "JSON")
                            .build())
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException exception) {
            LOGGER.log(Level.WARNING, "Amap reverse geocoding request failed: {0}",
                    exception.getClass().getSimpleName());
            throw new BizException(502, FRIENDLY_ERROR_MESSAGE);
        }

        return extractCity(response);
    }

    static String extractCity(Map<?, ?> response) {
        if (response == null) {
            LOGGER.warning("Amap reverse geocoding returned an empty response");
            throw new BizException(502, FRIENDLY_ERROR_MESSAGE);
        }
        if (!"1".equals(String.valueOf(response.get("status")))) {
            LOGGER.log(Level.WARNING, "Amap reverse geocoding failed: infocode={0}",
                    safeDiagnosticCode(response.get("infocode")));
            throw new BizException(502, FRIENDLY_ERROR_MESSAGE);
        }
        if (!(response.get("regeocode") instanceof Map<?, ?> regeocode)
                || !(regeocode.get("addressComponent") instanceof Map<?, ?> address)) {
            return null;
        }

        String city = stringValue(address.get("city"));
        return city.isBlank() ? stringValue(address.get("province")) : city;
    }

    private static String stringValue(Object value) {
        if (value == null || value instanceof List<?>) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private static String safeDiagnosticCode(Object value) {
        String code = stringValue(value);
        if (code.isBlank()) {
            return "unknown";
        }
        String safeCode = code.replaceAll("[^A-Za-z0-9_-]", "");
        return safeCode.isBlank() ? "unknown" : safeCode.substring(0, Math.min(safeCode.length(), 32));
    }
}
