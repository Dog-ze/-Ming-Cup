package com.example.backend.location;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private ReverseGeocodeClient reverseGeocodeClient;

    @Test
    void resolvesCoordinatesToTheDetailedCityName() {
        when(reverseGeocodeClient.resolveCity(
                new BigDecimal("119.0077"), new BigDecimal("25.4541")
        )).thenReturn("莆田市");

        LocationService service = new LocationService(reverseGeocodeClient);

        assertThat(service.reverseGeocode(
                new BigDecimal("119.0077"), new BigDecimal("25.4541")
        )).isEqualTo("莆田市");
    }
}
