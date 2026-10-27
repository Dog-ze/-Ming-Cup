package com.example.backend.pairing.amap;

import com.example.backend.common.BizException;
import com.example.backend.location.AmapProperties;
import com.example.backend.pairing.store.vo.StoreVO;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AmapPlaceSearchClientTest {

    @Test
    void parseStoresMapsUsableAmapPoiToNonPromotedStore() {
        AmapPlaceSearchClient client = new AmapPlaceSearchClient(
                RestClient.builder().build(), new AmapProperties()
        );
        Map<String, Object> response = Map.of(
                "status", "1",
                "pois", List.of(Map.of(
                        "name", "莆田茶点铺",
                        "address", "城厢区示例路 8 号",
                        "location", "119.007700,25.454100",
                        "tel", "0594-1234567",
                        "distance", "120"
                ))
        );

        List<StoreVO> stores = client.parseStores(
                response, new BigDecimal("119.0077"), new BigDecimal("25.4541")
        );

        assertThat(stores).singleElement().satisfies(store -> {
            assertThat(store.id()).isNull();
            assertThat(store.storeName()).isEqualTo("莆田茶点铺");
            assertThat(store.address()).isEqualTo("城厢区示例路 8 号");
            assertThat(store.longitude()).isEqualByComparingTo("119.007700");
            assertThat(store.latitude()).isEqualByComparingTo("25.454100");
            assertThat(store.phone()).isEqualTo("0594-1234567");
            assertThat(store.distanceMeters()).isEqualTo(120D);
            assertThat(store.promotion()).isFalse();
        });
    }

    @Test
    void parseStoresRejectsAmapFailureResponse() {
        AmapPlaceSearchClient client = new AmapPlaceSearchClient(
                RestClient.builder().build(), new AmapProperties()
        );

        assertThatThrownBy(() -> client.parseStores(
                Map.of("status", "0", "info", "INVALID_USER_KEY"),
                new BigDecimal("119.0077"), new BigDecimal("25.4541")
        )).isInstanceOf(BizException.class)
                .hasMessage("高德地图服务暂时不可用");
    }
}
