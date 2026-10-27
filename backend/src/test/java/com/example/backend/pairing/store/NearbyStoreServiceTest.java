package com.example.backend.pairing.store;

import com.example.backend.entity.PairingStore;
import com.example.backend.location.AmapProperties;
import com.example.backend.pairing.amap.AmapCoordinateConverter;
import com.example.backend.pairing.amap.AmapPlaceSearchClient;
import com.example.backend.pairing.store.vo.NearbyStoreResponse;
import com.example.backend.pairing.store.vo.StoreVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NearbyStoreServiceTest {

    @Test
    void nearbyStoresMergesMatchingMapStoreWithActivePromotionPartner() {
        StoreVO mapStore = new StoreVO(
                null, "茶香小馆", null, "地图地址",
                new BigDecimal("119.007700"), new BigDecimal("25.454100"),
                null, "来自地图", 60D, false
        );
        PairingStore partner = partner(
                new BigDecimal("119.007900"), new BigDecimal("25.454100")
        );

        StoreService storeService = mock(StoreService.class);
        AmapPlaceSearchClient mapClient = mock(AmapPlaceSearchClient.class);
        AmapProperties properties = new AmapProperties();
        when(storeService.findPromotionPartners("绿茶", "清淡糕点")).thenReturn(List.of(partner));
        when(storeService.isPromotionActive(any(PairingStore.class), any(LocalDateTime.class))).thenReturn(true);
        when(mapClient.search(any(), any(), any(), any(Integer.class))).thenReturn(List.of(mapStore));

        NearbyStoreService service = new NearbyStoreService(
                storeService, mapClient, properties, new AmapCoordinateConverter()
        );

        NearbyStoreResponse response = service.getNearbyStores(
                "绿茶", "清淡糕点",
                new BigDecimal("119.007700"), new BigDecimal("25.454100"), 3000
        );

        assertThat(response.stores()).singleElement().satisfies(store -> {
            assertThat(store.storeName()).isEqualTo("茶香小馆");
            assertThat(store.coverImage()).isEqualTo("/uploads/stores/tea-house.webp");
            assertThat(store.address()).isEqualTo("后台详细地址");
            assertThat(store.longitude()).isEqualByComparingTo("119.007700");
            assertThat(store.distanceMeters()).isEqualTo(60D);
            assertThat(store.promotion()).isTrue();
        });
    }

    @Test
    void nearbyStoresMergesPartnerWhenAddressesMatchButMapNameContainsBranchSuffix() {
        StoreVO mapStore = new StoreVO(
                null, "鲜果时光(新华北路店)", null, "西湖街与新华北路交叉口西南80米",
                new BigDecimal("114.480000"), new BigDecimal("38.060000"),
                null, "来自地图", 1_100D, false
        );
        PairingStore partner = partner(
                "鲜果时光", "新华北路685号",
                new BigDecimal("114.480500"), new BigDecimal("38.060000")
        );

        StoreService storeService = mock(StoreService.class);
        AmapPlaceSearchClient mapClient = mock(AmapPlaceSearchClient.class);
        AmapProperties properties = new AmapProperties();
        when(storeService.findPromotionPartners("绿茶", "水果")).thenReturn(List.of(partner));
        when(storeService.isPromotionActive(any(PairingStore.class), any(LocalDateTime.class))).thenReturn(true);
        when(mapClient.search(any(), any(), any(), any(Integer.class))).thenReturn(List.of(mapStore));

        NearbyStoreService service = new NearbyStoreService(
                storeService, mapClient, properties, new AmapCoordinateConverter()
        );

        NearbyStoreResponse response = service.getNearbyStores(
                "绿茶", "水果",
                new BigDecimal("114.480000"), new BigDecimal("38.060000"), 3000
        );

        assertThat(response.stores()).singleElement().satisfies(store -> {
            assertThat(store.storeName()).isEqualTo("鲜果时光");
            assertThat(store.promotion()).isTrue();
        });
    }

    @Test
    void nearbyStoresMergesPartnerWhenCoordinatesAreMissingButDetailedAddressesMatch() {
        StoreVO mapStore = new StoreVO(
                null, "鲜果时光(新华北路店)", null, "西湖街与新华北路交叉口西南80米",
                new BigDecimal("114.480000"), new BigDecimal("38.060000"),
                null, "来自地图", 1_100D, false
        );
        PairingStore partner = partner(
                "鲜果时光(新华北路店)", "丰泽区西湖街与新华北路交叉口西南80米",
                null, null
        );

        StoreService storeService = mock(StoreService.class);
        AmapPlaceSearchClient mapClient = mock(AmapPlaceSearchClient.class);
        AmapProperties properties = new AmapProperties();
        when(storeService.findPromotionPartners("绿茶", "水果")).thenReturn(List.of(partner));
        when(storeService.isPromotionActive(any(PairingStore.class), any(LocalDateTime.class))).thenReturn(true);
        when(mapClient.search(any(), any(), any(), any(Integer.class))).thenReturn(List.of(mapStore));

        NearbyStoreService service = new NearbyStoreService(
                storeService, mapClient, properties, new AmapCoordinateConverter()
        );

        NearbyStoreResponse response = service.getNearbyStores(
                "绿茶", "水果",
                new BigDecimal("114.480000"), new BigDecimal("38.060000"), 3000
        );

        assertThat(response.stores()).singleElement().satisfies(store -> {
            assertThat(store.storeName()).isEqualTo("鲜果时光(新华北路店)");
            assertThat(store.promotion()).isTrue();
        });
    }

    @Test
    void nearbyStoresConvertsWgs84BeforeCallingAmap() {
        StoreService storeService = mock(StoreService.class);
        AmapPlaceSearchClient mapClient = mock(AmapPlaceSearchClient.class);
        AmapProperties properties = new AmapProperties();
        when(storeService.findPromotionPartners("绿茶", "清淡糕点")).thenReturn(List.of());
        when(mapClient.search(any(), any(), any(), any(Integer.class))).thenReturn(List.of());

        NearbyStoreService service = new NearbyStoreService(
                storeService, mapClient, properties, new AmapCoordinateConverter()
        );
        service.getNearbyStores(
                "绿茶", "清淡糕点",
                new BigDecimal("116.397128"), new BigDecimal("39.916527"),
                "wgs84", 3000
        );

        verify(mapClient).search(
                eq("清淡糕点"),
                argThat(value -> value.compareTo(new BigDecimal("116.397128")) > 0),
                argThat(value -> value.compareTo(new BigDecimal("39.916527")) > 0),
                eq(3000)
        );
    }

    @Test
    void nearbyStoresKeepsPartnersAndBrieflyStopsRetryingAfterAmapFails() {
        StoreService storeService = mock(StoreService.class);
        AmapPlaceSearchClient mapClient = mock(AmapPlaceSearchClient.class);
        AmapProperties properties = new AmapProperties();
        PairingStore partner = partner(
                new BigDecimal("119.007900"), new BigDecimal("25.454100")
        );
        when(storeService.findPromotionPartners("绿茶", "水果")).thenReturn(List.of(partner));
        when(storeService.isPromotionActive(any(PairingStore.class), any(LocalDateTime.class))).thenReturn(true);
        when(mapClient.search(any(), any(), any(), any(Integer.class)))
                .thenThrow(new IllegalStateException("third party unavailable"));

        NearbyStoreService service = new NearbyStoreService(
                storeService, mapClient, properties, new AmapCoordinateConverter()
        );

        NearbyStoreResponse firstResponse = service.getNearbyStores(
                "绿茶", "水果", new BigDecimal("119.007700"), new BigDecimal("25.454100"), 3000
        );
        NearbyStoreResponse secondResponse = service.getNearbyStores(
                "绿茶", "水果", new BigDecimal("119.007700"), new BigDecimal("25.454100"), 3000
        );

        assertThat(firstResponse.stores()).singleElement()
                .extracting(store -> store.storeName())
                .isEqualTo("茶香小馆");
        assertThat(secondResponse.stores()).singleElement()
                .extracting(store -> store.storeName())
                .isEqualTo("茶香小馆");
        verify(mapClient, times(1)).search(any(), any(), any(), any(Integer.class));
    }

    private PairingStore partner(BigDecimal longitude, BigDecimal latitude) {
        return partner("茶香小馆", "后台详细地址", longitude, latitude);
    }

    private PairingStore partner(
            String storeName, String address, BigDecimal longitude, BigDecimal latitude
    ) {
        PairingStore partner = new PairingStore();
        partner.setId(8L);
        partner.setStoreName(storeName);
        partner.setCoverImage("/uploads/stores/tea-house.webp");
        partner.setAddress(address);
        partner.setLongitude(longitude);
        partner.setLatitude(latitude);
        partner.setPhone("0594-1234567");
        partner.setDescription("后台维护的店铺说明");
        partner.setPromotionEnabled(1);
        partner.setPromotionScore(20);
        partner.setPromotionStartAt(LocalDateTime.of(2026, 10, 1, 0, 0));
        partner.setPromotionEndAt(LocalDateTime.of(2026, 10, 31, 23, 59));
        partner.setStatus(1);
        return partner;
    }
}
