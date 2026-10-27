package com.example.backend.pairing.store;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.BizException;
import com.example.backend.entity.PairingStore;
import com.example.backend.mapper.PairingStoreMapper;
import com.example.backend.pairing.store.vo.StoreVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 负责查询本项目后台维护的店铺，并准备内部排序数据。
 */
@Service
@AllArgsConstructor
public class StoreService {
    private static final Set<String> TEA_CATEGORIES = Set.of(
            "绿茶", "白茶", "黄茶", "青茶", "红茶", "黑茶", "花草"
    );

    private final PairingStoreMapper pairingStoreMapper;

    /**
     * 查询符合茶类和食品标签、且已开启推广的合作店铺。
     *
     * 后续地图店铺合并流程会复用该方法；这里不按推广时间过滤，
     * 以便调用方根据当前时刻判断是否展示推广标识及给予推广分。
     *
     * @param teaCategory 当前茶类
     * @param foodName 当前推荐食品
     * @return 合作推广店铺实体列表
     */
    public List<PairingStore> findPromotionPartners(String teaCategory, String foodName) {
        validateTeaCategory(teaCategory);
        validateFoodName(foodName);

        return pairingStoreMapper.selectList(
                new LambdaQueryWrapper<PairingStore>()
                        .eq(PairingStore::getStatus, 1)
                        .eq(PairingStore::getPromotionEnabled, 1)
                        .apply("JSON_CONTAINS(tea_categories, JSON_QUOTE({0}))", teaCategory)
                        .apply("JSON_CONTAINS(food_tags, JSON_QUOTE({0}))", foodName)
                        .orderByDesc(PairingStore::getId)
        );
    }

    /**
     * 兼容当前附近店铺流程：将合作推广店转换为可排序候选项。
     *
     * @param teaCategory 当前茶类
     * @param foodName 当前推荐食品
     * @param longitude 用户经度，可为空
     * @param latitude 用户纬度，可为空
     * @return 用于统一排序的候选店铺；推广分数不会返回给前端
     */
    public List<StoreRanker.Candidate> findStores(
            String teaCategory,
            String foodName,
            BigDecimal longitude,
            BigDecimal latitude
    ) {
        LocalDateTime now = LocalDateTime.now();
        return findPromotionPartners(teaCategory, foodName).stream()
                .map(store -> toCandidate(store, longitude, latitude, now))
                .toList();
    }

    /**
     * 生成内部候选项：只在推广开关开启、且处于有效时间内时，才给予推广分数。
     */
    private StoreRanker.Candidate toCandidate(
            PairingStore store,
            BigDecimal longitude,
            BigDecimal latitude,
            LocalDateTime now
    ) {
        boolean promotion = isPromotionActive(store, now);
        StoreVO storeVO = StoreVO.from(
                store,
                calculateDistance(longitude, latitude, store),
                promotion
        );
        return new StoreRanker.Candidate(storeVO, promotion ? safeScore(store.getPromotionScore()) : 0);
    }

    /**
     * 判断推广是否在当前时刻生效。
     */
    public boolean isPromotionActive(PairingStore store, LocalDateTime now) {
        if (!Integer.valueOf(1).equals(store.getPromotionEnabled())) {
            return false;
        }
        if (store.getPromotionStartAt() == null || store.getPromotionEndAt() == null) {
            return false;
        }
        return !now.isBefore(store.getPromotionStartAt()) && !now.isAfter(store.getPromotionEndAt());
    }

    /**
     * 安全地获取推广分数：如果为 null，则返回 0。
     */
    private int safeScore(Integer score) {
        return score == null ? 0 : score;
    }

    /**
     * 验证茶类参数是否合法。
     *
     * @param teaCategory 茶类
     */
    private void validateTeaCategory(String teaCategory) {
        if (teaCategory == null || !TEA_CATEGORIES.contains(teaCategory.trim())) {
            throw new BizException(400, "请选择正确的茶类");
        }
    }

    /**
     * 验证推荐食品参数是否合法。
     * @param foodName
     */
    private void validateFoodName(String foodName) {
        if (foodName == null || foodName.isBlank() || foodName.trim().length() > 32) {
            throw new BizException(400, "请选择正确的推荐食品");
        }
    }

    /**
     * 没有用户位置或店铺位置时，不计算距离。
     */
    private Double calculateDistance(
            BigDecimal userLongitude,
            BigDecimal userLatitude,
            PairingStore store
    ) {
        if (userLongitude == null || userLatitude == null
                || store.getLongitude() == null || store.getLatitude() == null) {
            return null;
        }

        double earthRadius = 6_371_000D;

        double latitude1 = Math.toRadians(userLatitude.doubleValue());
        double latitude2 = Math.toRadians(store.getLatitude().doubleValue());
        double deltaLatitude = Math.toRadians(store.getLatitude().doubleValue() - userLatitude.doubleValue());
        double deltaLongitude = Math.toRadians(store.getLongitude().doubleValue() - userLongitude.doubleValue());

        double a = Math.sin(deltaLatitude / 2) * Math.sin(deltaLatitude / 2)
                + Math.cos(latitude1) * Math.cos(latitude2)
                * Math.sin(deltaLongitude / 2) * Math.sin(deltaLongitude / 2);

        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
