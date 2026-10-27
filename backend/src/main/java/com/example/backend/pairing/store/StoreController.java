package com.example.backend.pairing.store;

import com.example.backend.common.Result;
import com.example.backend.pairing.store.vo.NearbyStoreResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 提供给 App“附近茶食”页面的接口。
 */
@RestController
@RequestMapping("/api/pairing/stores")
@AllArgsConstructor
public class StoreController {
    private final NearbyStoreService nearbyStoreService;

    /**
     * 获取按距离与推广规则统一排序的附近店铺。
     */
    @GetMapping("/nearby")
    public Result<NearbyStoreResponse> getNearbyStores(
            @RequestParam String teaCategory,
            @RequestParam String foodName,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false, defaultValue = "gcj02") String coordinateSystem,
            @RequestParam(required = false) Integer radius
    ) {
        return Result.ok(nearbyStoreService.getNearbyStores(
                teaCategory, foodName, longitude, latitude, coordinateSystem, radius
        ));
    }
}
