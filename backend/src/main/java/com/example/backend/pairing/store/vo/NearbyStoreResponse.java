package com.example.backend.pairing.store.vo;

import java.util.List;

/**
 * “附近茶食”页面需要的完整数据。
 */
public record NearbyStoreResponse(
        String teaCategory,
        String foodName,
        List<StoreVO> stores,
        boolean locationAvailable
) {
}
