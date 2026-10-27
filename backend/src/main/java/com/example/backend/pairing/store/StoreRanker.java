package com.example.backend.pairing.store;

import com.example.backend.pairing.store.vo.StoreVO;

import java.util.Comparator;
import java.util.List;

/**
 * 统一排列本地店铺和 OpenStreetMap 店铺。
 *
 * <p>距离始终是主要因素。推广分数只会在用户距离店铺较近时产生有限加成，
 * 超出三公里后加成自动归零，避免远处推广店压过附近普通店。</p>
 */
public class StoreRanker {
    private static final double PROMOTION_EFFECTIVE_DISTANCE_METERS = 3_000D;
    private static final int MAX_PROMOTION_SCORE = 30;

    /**
     * 排序候选项。promotionScore 是内部数据，不会出现在接口响应中。
     */
    public record Candidate(StoreVO store, int promotionScore) {
    }

    /**
     * 将候选店铺按“距离基础分 + 有距离衰减的推广加分”从高到低排序。
     */
    public List<StoreVO> rank(List<Candidate> candidates) {
        return candidates.stream()
                .sorted(Comparator
                        .comparingDouble(this::rankScore)
                        .reversed()
                        .thenComparing(candidate -> candidate.store().distanceMeters(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(candidate -> candidate.store().storeName(),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(Candidate::store)
                .toList();
    }

    /**
     * 计算店铺的内部排序分数；该分数不返回给用户。
     */
    private double rankScore(Candidate candidate) {
        Double distance = candidate.store().distanceMeters();
        double distanceScore = distance == null
                ? 0D
                : Math.max(0D, 100D - distance / 50D);

        if (!candidate.store().promotion() || distance == null) {
            return distanceScore;
        }

        double distanceFactor = Math.max(0D, 1D - distance / PROMOTION_EFFECTIVE_DISTANCE_METERS);
        int effectivePromotionScore = Math.min(
                Math.max(candidate.promotionScore(), 0),
                MAX_PROMOTION_SCORE
        );
        return distanceScore + effectivePromotionScore * distanceFactor;
    }
}
