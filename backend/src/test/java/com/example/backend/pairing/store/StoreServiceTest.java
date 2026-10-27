package com.example.backend.pairing.store;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.backend.entity.PairingStore;
import com.example.backend.mapper.PairingStoreMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreServiceTest {

    @BeforeAll
    static void initializePairingStoreMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                PairingStore.class
        );
    }

    @Test
    void findStoresQueriesOnlyPromotionEnabledPartners() {
        PairingStoreMapper mapper = mock(PairingStoreMapper.class);
        when(mapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        StoreService service = new StoreService(mapper);

        service.findStores("绿茶", "清淡糕点", new BigDecimal("119.0077"), new BigDecimal("25.4541"));

        ArgumentCaptor<LambdaQueryWrapper<PairingStore>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("promotion_enabled");
    }

    @Test
    void findStoresDoesNotMarkPartnerWithMissingPromotionPeriodAsPromoted() {
        PairingStore partner = new PairingStore();
        partner.setId(8L);
        partner.setStoreName("茶香小馆");
        partner.setAddress("莆田市城厢区示例路 88 号");
        partner.setTeaCategories("[\"绿茶\"]");
        partner.setFoodTags("[\"清淡糕点\"]");
        partner.setStatus(1);
        partner.setPromotionEnabled(1);
        partner.setPromotionScore(20);

        PairingStoreMapper mapper = mock(PairingStoreMapper.class);
        when(mapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(partner));
        StoreService service = new StoreService(mapper);

        List<StoreRanker.Candidate> result = service.findStores("绿茶", "清淡糕点", null, null);

        assertThat(result).singleElement().satisfies(candidate -> {
            assertThat(candidate.store().promotion()).isFalse();
            assertThat(candidate.promotionScore()).isZero();
        });
    }
}
