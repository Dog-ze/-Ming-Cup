package com.example.backend.pairing.osm;

import com.example.backend.pairing.store.vo.StoreVO;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 只负责调用 OpenStreetMap 的 Overpass API，
 * 不负责店铺排序、缓存和前端响应组合。
 */
@Component
@AllArgsConstructor
public class OpenStreetMapClient {
    private final RestClient.Builder restClientBuilder;
    private final OpenStreetMapProperties properties;

    /**
     * 根据食品名称和用户坐标，查询附近公开店铺。
     *
     * @param foodName 食品名称，例如“清淡糕点”
     * @param longitude 用户经度
     * @param latitude 用户纬度
     * @param radiusMeters 搜索半径，单位米
     * @return OSM 查询到的店铺列表
     */
    public List<StoreVO> search(
            String foodName,
            BigDecimal longitude,
            BigDecimal latitude,
            int radiusMeters
    ) {
        if (!properties.isEnabled()) {
            return List.of();
        }

        String query = buildQuery(foodName, longitude, latitude, radiusMeters);

        Map<?, ?> body = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build()
                .post()
                .uri("/api/interpreter")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("data=" + URLEncoder.encode(query, StandardCharsets.UTF_8))
                .retrieve()
                .body(Map.class);

        return parseStores(body, longitude, latitude);
    }

    /**
     * 根据食物类型生成 Overpass 查询语句。
     */
    private String buildQuery(
            String foodName,
            BigDecimal longitude,
            BigDecimal latitude,
            int radiusMeters
    ) {
        String tags = switch (foodName) {
            case "清淡糕点", "低糖饼干", "米糕", "广式茶点", "全麦面包" ->
                    "[\"shop\"~\"bakery|confectionery\"]";
            case "水果" ->
                    "[\"shop\"~\"greengrocer|supermarket\"]";
            case "坚果" ->
                    "[\"shop\"~\"convenience|supermarket\"]";
            case "蒸点心" ->
                    "[\"amenity\"~\"restaurant|cafe\"]";
            case "牛肉干", "凤梨酥", "牛奶饼干" ->
                    "[\"shop\"~\"convenience|bakery\"]";
            default ->
                    "[\"shop\"~\"bakery|confectionery|convenience\"]";
        };

        return """
                [out:json][timeout:5];
                (
                  node(around:%d,%s,%s)%s;
                  way(around:%d,%s,%s)%s;
                );
                out center tags 20;
                """.formatted(
                radiusMeters, latitude, longitude, tags,
                radiusMeters, latitude, longitude, tags
        );
    }

    /**
     * 将 Overpass 返回的复杂 Map 数据整理成 StoreVO 列表。
     */
    private List<StoreVO> parseStores(
            Map<?, ?> body,
            BigDecimal userLongitude,
            BigDecimal userLatitude
    ) {
        if (body == null || !(body.get("elements") instanceof List<?> elements)) {
            return List.of();
        }

        List<StoreVO> stores = new ArrayList<>();

        for (Object item : elements) {
            if (!(item instanceof Map<?, ?> element)) {
                continue;
            }

            if (!(element.get("tags") instanceof Map<?, ?> tags)) {
                continue;
            }

            String storeName = stringValue(tags.get("name"));
            if (storeName.isBlank()) {
                continue;
            }

            BigDecimal longitude = readLongitude(element);
            BigDecimal latitude = readLatitude(element);

            if (longitude == null || latitude == null) {
                continue;
            }

            String address = buildAddress(tags);
            double distance = calculateDistanceMeters(
                    userLongitude,
                    userLatitude,
                    longitude,
                    latitude
            );

            stores.add(StoreVO.fromOpenStreetMap(
                    storeName,
                    address,
                    longitude,
                    latitude,
                    distance
            ));
        }

        return stores;
    }

    /**
     * 读取节点店铺或区域店铺的经度。
     */
    private BigDecimal readLongitude(Map<?, ?> element) {
        Object longitude = element.get("lon");

        if (longitude == null && element.get("center") instanceof Map<?, ?> center) {
            longitude = center.get("lon");
        }

        return toBigDecimal(longitude);
    }

    /**
     * 读取节点店铺或区域店铺的纬度。
     */
    private BigDecimal readLatitude(Map<?, ?> element) {
        Object latitude = element.get("lat");

        if (latitude == null && element.get("center") instanceof Map<?, ?> center) {
            latitude = center.get("lat");
        }

        return toBigDecimal(latitude);
    }

    /**
     * 将 OSM 地址标签拼成可展示的地址。
     */
    private String buildAddress(Map<?, ?> tags) {
        String fullAddress = stringValue(tags.get("addr:full"));
        if (!fullAddress.isBlank()) {
            return fullAddress;
        }

        String street = stringValue(tags.get("addr:street"));
        String houseNumber = stringValue(tags.get("addr:housenumber"));
        String address = (street + " " + houseNumber).trim();

        return address.isBlank() ? "地址信息暂缺" : address;
    }

    /**
     * 计算两个坐标的直线距离，单位：米。
     */
    private double calculateDistanceMeters(
            BigDecimal longitude1,
            BigDecimal latitude1,
            BigDecimal longitude2,
            BigDecimal latitude2
    ) {
        double earthRadius = 6_371_000D;

        double lat1 = Math.toRadians(latitude1.doubleValue());
        double lat2 = Math.toRadians(latitude2.doubleValue());
        double deltaLat = Math.toRadians(latitude2.doubleValue() - latitude1.doubleValue());
        double deltaLon = Math.toRadians(longitude2.doubleValue() - longitude1.doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}