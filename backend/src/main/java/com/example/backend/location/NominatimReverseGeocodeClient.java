package com.example.backend.location;

import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@AllArgsConstructor
class NominatimReverseGeocodeClient implements ReverseGeocodeClient {
    private static final String BASE_URL = "https://nominatim.openstreetmap.org";
    private final RestClient.Builder restClientBuilder;

    @Override
    public String resolveCity(BigDecimal longitude, BigDecimal latitude) {
        Map<?, ?> body = restClientBuilder.baseUrl(BASE_URL)
                .defaultHeader("User-Agent", "YiZhanMing/1.0")
                .build()
                .get()
                .uri(builder -> builder.path("/reverse")
                        .queryParam("lat", latitude)
                        .queryParam("lon", longitude)
                        .queryParam("format", "jsonv2")
                        .queryParam("accept-language", "zh-CN")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);

        if (body == null || !(body.get("address") instanceof Map<?, ?> address)) {
            return null;
        }

        return firstText(address, "city", "municipality", "town", "county");
    }

    private String firstText(Map<?, ?> address, String... keys) {
        for (String key : keys) {
            Object value = address.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                String city = String.valueOf(value).trim();
                return city.endsWith("市") ? city : city + "市";
            }
        }
        return null;
    }
}
