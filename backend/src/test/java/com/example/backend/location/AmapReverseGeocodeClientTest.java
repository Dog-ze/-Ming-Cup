package com.example.backend.location;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AmapReverseGeocodeClientTest {

    @Test
    void usesProvinceAsCityForMunicipalitiesWhenAmapReturnsAnEmptyCity() {
        Map<String, Object> response = Map.of(
                "status", "1",
                "regeocode", Map.of(
                        "addressComponent", Map.of(
                                "city", java.util.List.of(),
                                "province", "北京市"
                        )
                )
        );

        assertThat(AmapReverseGeocodeClient.extractCity(response)).isEqualTo("北京市");
    }

    @Test
    void hidesAmapInternalErrorWhenReverseGeocodingFails() {
        Map<String, Object> response = Map.of(
                "status", "0",
                "info", "USERKEY_PLAT_NOMATCH",
                "infocode", "10001"
        );

        assertThatThrownBy(() -> AmapReverseGeocodeClient.extractCity(response))
                .hasMessage("暂时无法识别所在城市，请手动选择城市")
                .hasMessageNotContaining("USERKEY_PLAT_NOMATCH")
                .hasMessageNotContaining("10001");
    }
}
