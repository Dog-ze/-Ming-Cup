package com.example.backend.pairing.amap;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AmapCoordinateConverterTest {

    @Test
    void toGcj02ConvertsWgs84CoordinateInsideChina() {
        AmapCoordinateConverter converter = new AmapCoordinateConverter();

        AmapCoordinateConverter.Coordinate converted = converter.toGcj02(
                new BigDecimal("116.397128"), new BigDecimal("39.916527"), "wgs84"
        );

        assertThat(converted.longitude()).isGreaterThan(new BigDecimal("116.397128"));
        assertThat(converted.latitude()).isGreaterThan(new BigDecimal("39.916527"));
    }

    @Test
    void toGcj02KeepsGcj02CoordinateUnchanged() {
        AmapCoordinateConverter converter = new AmapCoordinateConverter();

        AmapCoordinateConverter.Coordinate converted = converter.toGcj02(
                new BigDecimal("116.403372"), new BigDecimal("39.917000"), "gcj02"
        );

        assertThat(converted.longitude()).isEqualByComparingTo("116.403372");
        assertThat(converted.latitude()).isEqualByComparingTo("39.917000");
    }
}
