package com.example.backend.pairing.store;

import com.example.backend.pairing.store.vo.StoreVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StoreRankerTest {

    @Test
    void promotedStoreCanRankAheadWhenItIsOnlySlightlyFartherAway() {
        StoreVO nearby = store("近处普通店", 500D, false);
        StoreVO promoted = store("推广店", 650D, true);

        List<StoreVO> result = new StoreRanker().rank(List.of(
                new StoreRanker.Candidate(nearby, 0),
                new StoreRanker.Candidate(promoted, 30)
        ));

        assertThat(result).extracting(StoreVO::storeName)
                .containsExactly("推广店", "近处普通店");
    }

    @Test
    void farAwayPromotedStoreCannotRankAheadOfNearbyStore() {
        StoreVO nearby = store("近处普通店", 300D, false);
        StoreVO promoted = store("远处推广店", 4_000D, true);

        List<StoreVO> result = new StoreRanker().rank(List.of(
                new StoreRanker.Candidate(promoted, 30),
                new StoreRanker.Candidate(nearby, 0)
        ));

        assertThat(result).extracting(StoreVO::storeName)
                .containsExactly("近处普通店", "远处推广店");
    }

    private StoreVO store(String name, Double distanceMeters, boolean promotion) {
        return new StoreVO(
                null, name, null, "测试地址", null, null,
                null, "测试说明", distanceMeters, promotion
        );
    }
}
