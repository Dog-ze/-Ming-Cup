package com.example.backend.pairing.store;

import com.example.backend.entity.PairingStore;
import com.example.backend.location.AmapProperties;
import com.example.backend.pairing.amap.AmapCoordinateConverter;
import com.example.backend.pairing.amap.AmapPlaceSearchClient;
import com.example.backend.pairing.store.vo.NearbyStoreResponse;
import com.example.backend.pairing.store.vo.StoreVO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * “附近茶食”页面的总服务。
 * 将后台合作店与高德地图附近店铺合并为一个统一列表。
 */
@Service
public class NearbyStoreService {
    private static final Logger LOGGER = Logger.getLogger(NearbyStoreService.class.getName());
    private final StoreService storeService;
    private final AmapPlaceSearchClient amapPlaceSearchClient;
    private final AmapProperties properties;
    private final AmapCoordinateConverter coordinateConverter;
    private final StoreRanker storeRanker = new StoreRanker();

    private static final double PARTNER_MATCH_DISTANCE_METERS = 100D;
    private static final int DEFAULT_MAP_PROMOTION_SCORE = 0;

    /**
     * 高德地图数据缓存 30 分钟，避免每次打开页面都请求第三方地图服务。
     */
    private final Cache<String, List<StoreVO>> amapCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .maximumSize(500)
            .build();
    private final Cache<String, Boolean> amapFailureCache;

    public NearbyStoreService(
            StoreService storeService,
            AmapPlaceSearchClient amapPlaceSearchClient,
            AmapProperties properties,
            AmapCoordinateConverter coordinateConverter
    ) {
        this.storeService = storeService;
        this.amapPlaceSearchClient = amapPlaceSearchClient;
        this.properties = properties;
        this.coordinateConverter = coordinateConverter;
        this.amapFailureCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(Math.max(1, properties.getFailureCooldownSeconds())))
                .maximumSize(500)
                .build();
    }

    /**
     * 获取附近茶食页面所需的数据。
     */
    public NearbyStoreResponse getNearbyStores(
            String teaCategory,
            String foodName,
            BigDecimal longitude,
            BigDecimal latitude,
            Integer radius
    ) {
        return getNearbyStores(teaCategory, foodName, longitude, latitude, "gcj02", radius);
    }

    /**
     * 获取附近茶食页面所需的数据，并将浏览器 WGS84 坐标转换为高德使用的 GCJ-02 坐标。
     */
    public NearbyStoreResponse getNearbyStores(
            String teaCategory,
            String foodName,
            BigDecimal longitude,
            BigDecimal latitude,
            String coordinateSystem,
            Integer radius
    ) {
        List<PairingStore> partners = storeService.findPromotionPartners(teaCategory, foodName);
        boolean locationAvailable = longitude != null && latitude != null;
        AmapCoordinateConverter.Coordinate amapCoordinate = coordinateConverter.toGcj02(
                longitude, latitude, coordinateSystem
        );

        if (!locationAvailable) {
            return new NearbyStoreResponse(
                    teaCategory,
                    foodName,
                    storeRanker.rank(mergeMapStoresAndPartners(
                            List.of(), partners, longitude, latitude, LocalDateTime.now()
                    )),
                    false
            );
        }

        int radiusMeters = normalizeRadius(radius);
        String cacheKey = buildCacheKey(
                foodName, amapCoordinate.longitude(), amapCoordinate.latitude(), radiusMeters
        );

        List<StoreVO> nearbyStores;

        if (amapFailureCache.getIfPresent(cacheKey) != null) {
            nearbyStores = List.of();
        } else {
            try {
                nearbyStores = amapCache.get(
                        cacheKey,
                        ignored -> amapPlaceSearchClient.search(
                                foodName,
                                amapCoordinate.longitude(),
                                amapCoordinate.latitude(),
                                radiusMeters
                        )
                );
            } catch (RuntimeException exception) {
                // 地图服务失败时，不影响本地店铺显示；短暂冷却避免反复请求已故障的服务。
                LOGGER.log(Level.WARNING, "Amap nearby place search failed: {0}",
                        exception.getClass().getSimpleName());
                amapFailureCache.put(cacheKey, Boolean.TRUE);
                nearbyStores = List.of();
            }
        }

        List<StoreRanker.Candidate> candidates = mergeMapStoresAndPartners(
                nearbyStores,
                partners,
                amapCoordinate.longitude(),
                amapCoordinate.latitude(),
                LocalDateTime.now()
        );

        return new NearbyStoreResponse(
                teaCategory,
                foodName,
                storeRanker.rank(candidates),
                true
        );
    }


    /**
     * 将地图店铺与后台合作店合并，确保同一家店只展示一次。
     */
    private List<StoreRanker.Candidate> mergeMapStoresAndPartners(
            List<StoreVO> mapStores,
            List<PairingStore> partners,
            BigDecimal userLongitude,
            BigDecimal userLatitude,
            LocalDateTime now
    ) {
        List<StoreRanker.Candidate> candidates = new ArrayList<>();
        Set<Long> matchedPartnerIds = new HashSet<>();

        // 先匹配地图店铺与后台合作店，避免重复展示。
        for (StoreVO mapStore : mapStores) {
            // 先尝试在后台合作店中找到匹配的店铺。
            PairingStore partner = partners.stream()
                    .filter(item -> !matchedPartnerIds.contains(item.getId()))
                    .filter(item -> isSameStore(mapStore, item))
                    .findFirst()
                    .orElse(null);

            // 如果没有匹配的后台合作店，则直接使用地图店铺信息。
            if (partner == null) {
                candidates.add(new StoreRanker.Candidate(mapStore, DEFAULT_MAP_PROMOTION_SCORE));
                continue;
            }

            // 如果匹配到了后台合作店，则使用后台合作店信息，并标记为已匹配。
            matchedPartnerIds.add(partner.getId());
            // 检查后台合作店的促销是否仍然有效。
            boolean promotionActive = storeService.isPromotionActive(partner, now);
            // 将匹配的店铺加入候选列表，使用后台合作店信息，并根据促销状态设置分数。
            candidates.add(new StoreRanker.Candidate(
                    StoreVO.fromPartnerMatch(mapStore, partner, promotionActive),
                    promotionActive ? safePromotionScore(partner.getPromotionScore()) : 0
            ));
        }

        // 将未匹配的后台合作店也加入候选列表。
        for (PairingStore partner : partners) {
            if (matchedPartnerIds.contains(partner.getId())) {
                continue;
            }
            // 检查后台合作店的促销是否仍然有效。
            boolean promotionActive = storeService.isPromotionActive(partner, now);

            // 将未匹配的后台合作店加入候选列表，使用后台合作店信息，并根据促销状态设置分数。
            candidates.add(new StoreRanker.Candidate(
                    StoreVO.from(
                            partner,
                            calculateDistance(userLongitude, userLatitude,
                                    partner.getLongitude(), partner.getLatitude()),
                            promotionActive
                    ),
                    promotionActive ? safePromotionScore(partner.getPromotionScore()) : 0
            ));
        }

        return candidates;
    }

    /**
     * 规范搜索半径。
     */
    private int normalizeRadius(Integer radius) {
        if (radius == null || radius <= 0) {
            return properties.getDefaultRadiusMeters();
        }

        return Math.min(radius, properties.getMaxRadiusMeters());
    }

    /**
     * 将相近坐标归为同一个缓存区域，避免用户轻微移动就重复请求高德地图服务。
     */
    private String buildCacheKey(
            String foodName,
            BigDecimal longitude,
            BigDecimal latitude,
            int radiusMeters
    ) {
        double longitudeGrid = Math.round(longitude.doubleValue() * 1000D) / 1000D;
        double latitudeGrid = Math.round(latitude.doubleValue() * 1000D) / 1000D;

        return foodName + ":" + longitudeGrid + ":" + latitudeGrid + ":" + radiusMeters;
    }

    /**
     * 判断地图店铺与后台合作店是否为同一家店。
     * 先检查两者的坐标距离是否在 100 米以内，再检查地址或名称是否匹配。
     */
    private boolean isSameStore(StoreVO mapStore, PairingStore partner) {
        Double distanceMeters = calculateDistance(
                mapStore.longitude(), mapStore.latitude(),
                partner.getLongitude(), partner.getLatitude()
        );
        String normalizedMapAddress = normalizeAddress(mapStore.address());
        String normalizedPartnerAddress = normalizeAddress(partner.getAddress());
        boolean detailedAddressesMatch = detailedAddressesMatch(
                normalizedMapAddress, normalizedPartnerAddress
        );

        // 后台尚未填写坐标时，只允许完整详细地址对应的店铺合并，避免误合并同一路段店铺。
        if (distanceMeters == null) {
            return detailedAddressesMatch;
        }
        if (distanceMeters > PARTNER_MATCH_DISTANCE_METERS) {
            return false;
        }

        return detailedAddressesMatch
                || sharesRoadName(normalizedMapAddress, normalizedPartnerAddress)
                || normalizeName(mapStore.storeName()).equals(normalizeName(partner.getStoreName()));
    }

    /**
     * 名称需要在清除空格后完全相等，避免“茶食店”和“茶食店（北京）”被误认为同一家店。
     */
    private String normalizeName(String name) {
        return name == null ? "" : name.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private boolean detailedAddressesMatch(String normalizedMapAddress, String normalizedPartnerAddress) {
        if (normalizedMapAddress.length() < 6 || normalizedPartnerAddress.length() < 6
                || "地址信息暂缺".equals(normalizedMapAddress)
                || "地址信息暂缺".equals(normalizedPartnerAddress)) {
            return false;
        }
        return normalizedMapAddress.contains(normalizedPartnerAddress)
                || normalizedPartnerAddress.contains(normalizedMapAddress);
    }

    /**
     * 规范化地址，去除空格、标点符号和中文标点，并转换为小写。
     */
    private String normalizeAddress(String address) {
        return address == null ? "" : address
                .replaceAll("[\\s\\p{Punct}，。；：！？、】【（）【】《》]", "")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * 高德可能把门牌号转换成“某路交叉口附近”。两段地址只要共享包含“路、街、巷、道”的
     * 三个及以上连续字符，就视为地址相符；只在坐标距离已通过校验时使用。
     */
    private boolean sharesRoadName(String firstAddress, String secondAddress) {
        int[] previousRow = new int[secondAddress.length() + 1];
        String longestCommonText = "";

        for (int firstIndex = 0; firstIndex < firstAddress.length(); firstIndex++) {
            int[] currentRow = new int[secondAddress.length() + 1];
            for (int secondIndex = 0; secondIndex < secondAddress.length(); secondIndex++) {
                if (firstAddress.charAt(firstIndex) != secondAddress.charAt(secondIndex)) {
                    continue;
                }
                currentRow[secondIndex + 1] = previousRow[secondIndex] + 1;
                if (currentRow[secondIndex + 1] > longestCommonText.length()) {
                    longestCommonText = firstAddress.substring(
                            firstIndex - currentRow[secondIndex + 1] + 1, firstIndex + 1
                    );
                }
            }
            previousRow = currentRow;
        }

        return longestCommonText.length() >= 3
                && (longestCommonText.contains("路") || longestCommonText.contains("街")
                || longestCommonText.contains("巷") || longestCommonText.contains("道"));
    }

    /**
     * 安全地获取后台合作店的促销分数，如果为 null 则返回 0。
     */
    private int safePromotionScore(Integer score) {
        return score == null ? 0 : score;
    }

    /**
     * 计算两点之间的距离，单位为米。
     * 使用 Haversine 公式计算球面距离。
     */
    private Double calculateDistance(
            BigDecimal longitude1,
            BigDecimal latitude1,
            BigDecimal longitude2,
            BigDecimal latitude2
    ) {
        if (longitude1 == null || latitude1 == null || longitude2 == null || latitude2 == null) {
            return null;
        }

        double latitudeRadians1 = Math.toRadians(latitude1.doubleValue());
        double latitudeRadians2 = Math.toRadians(latitude2.doubleValue());
        double deltaLatitude = Math.toRadians(latitude2.doubleValue() - latitude1.doubleValue());
        double deltaLongitude = Math.toRadians(longitude2.doubleValue() - longitude1.doubleValue());
        double a = Math.sin(deltaLatitude / 2) * Math.sin(deltaLatitude / 2)
                + Math.cos(latitudeRadians1) * Math.cos(latitudeRadians2)
                * Math.sin(deltaLongitude / 2) * Math.sin(deltaLongitude / 2);
        return 6_371_000D * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
