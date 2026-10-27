package com.example.backend.pairing.amap;

import com.example.backend.common.BizException;
import com.example.backend.location.AmapProperties;
import com.example.backend.pairing.store.vo.StoreVO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Component
public class AmapPlaceSearchClient {
    private static final String FOOD_TYPE = "050000";
    private static final Logger LOGGER = Logger.getLogger(AmapPlaceSearchClient.class.getName());
    private final RestClient amapRestClient;
    private final AmapProperties properties;

    public AmapPlaceSearchClient(
            @Qualifier("amapRestClient") RestClient amapRestClient,
            AmapProperties properties
    ) {
        this.amapRestClient = amapRestClient;
        this.properties = properties;
    }

    /**
     * 搜索附近的店铺
     * @param foodName
     * @param longitude
     * @param latitude
     * @param radiusMeters
     * @return
     */
    public List<StoreVO> search(
            String foodName, BigDecimal longitude, BigDecimal latitude, int radiusMeters
    ) {
        if (!properties.isPlaceSearchEnabled()) return List.of();
        if (properties.getWebServiceKey() == null || properties.getWebServiceKey().isBlank()) {
            LOGGER.warning("Amap place search is unavailable because the Web service key is missing");
            throw new BizException(503, "高德地图服务暂时不可用");
        }

        Map<?, ?> response = amapRestClient.get()
                .uri(builder -> builder.path("/v5/place/around")
                        .queryParam("key", properties.getWebServiceKey())
                        .queryParam("location", longitude.setScale(6, RoundingMode.HALF_UP)
                                + "," + latitude.setScale(6, RoundingMode.HALF_UP))
                        .queryParam("radius", radiusMeters)
                        .queryParam("keywords", foodName)
                        .queryParam("types", FOOD_TYPE)
                        .queryParam("page_size", 20)
                        .build())
                .retrieve().body(Map.class);
        return parseStores(response, longitude, latitude);
    }

    /**
     * 解析高德地图附近店铺搜索结果
     * @param response 高德地图返回的响应
     * @param longitude 用户经度
     * @param latitude 用户纬度
     * @return 店铺列表
     */
    List<StoreVO> parseStores(Map<?, ?> response, BigDecimal longitude, BigDecimal latitude) {
        if (response == null || !"1".equals(String.valueOf(response.get("status")))) {
            LOGGER.warning("Amap place search returned an unsuccessful response");
            throw new BizException(502, "高德地图服务暂时不可用");
        }

        Object poisValue = response.get("pois");
        if (!(poisValue instanceof List<?> pois)) {
            return List.of();
        }

        List<StoreVO> stores = new ArrayList<>();
        for (Object poiValue : pois) {
            if (!(poiValue instanceof Map<?, ?> poi)) {
                continue;
            }

            StoreVO store = toStore(poi, longitude, latitude);
            if (store != null) {
                stores.add(store);
            }
        }
        return stores;
    }

    /**
     * 将高德地图 POI 数据转换为店铺 VO
     * @param poi 高德地图 POI 数据
     * @param userLongitude 用户经度
     * @param userLatitude 用户纬度
     * @return 店铺 VO，如果数据不完整则返回 null
     */
    private StoreVO toStore(Map<?, ?> poi, BigDecimal userLongitude, BigDecimal userLatitude) {
        String storeName = text(poi.get("name"));
        BigDecimal[] coordinate = parseCoordinate(text(poi.get("location")));
        if (storeName == null || coordinate == null) {
            return null;
        }

        String address = text(poi.get("address"));
        Double distanceMeters = parseDistance(text(poi.get("distance")));
        if (distanceMeters == null) {
            distanceMeters = calculateDistance(
                    userLongitude, userLatitude, coordinate[0], coordinate[1]
            );
        }

        return new StoreVO(
                null,
                storeName,
                null,
                address == null ? "地址信息暂缺" : address,
                coordinate[0],
                coordinate[1],
                text(poi.get("tel")),
                "来自高德地图附近店铺信息",
                distanceMeters,
                false
        );
    }

    /**
     * 将对象转换为字符串，如果对象为 null 或空字符串则返回 null
     * @param value 对象
     * @return 字符串或 null
     */
    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 解析坐标字符串为经纬度数组
     * @param location 坐标字符串，格式为 "longitude,latitude"
     * @return 经度和纬度的 BigDecimal 数组，如果解析失败则返回 null
     */
    private BigDecimal[] parseCoordinate(String location) {
        if (location == null) {
            return null;
        }
        String[] parts = location.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new BigDecimal[]{new BigDecimal(parts[0].trim()), new BigDecimal(parts[1].trim())};
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * 解析距离字符串为 Double
     * @param distance 距离字符串
     * @return 距离的 Double 值，如果解析失败则返回 null
     */
    private Double parseDistance(String distance) {
        if (distance == null) {
            return null;
        }
        try {
            return Double.parseDouble(distance);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * 计算两点之间的距离（单位：米）
     * @param longitude1 第一点经度
     * @param latitude1 第一点纬度
     * @param longitude2 第二点经度
     * @param latitude2 第二点纬度
     * @return 两点之间的距离（单位：米），如果任意一个坐标为 null 则返回 null
     */
    private Double calculateDistance(
            BigDecimal longitude1, BigDecimal latitude1,
            BigDecimal longitude2, BigDecimal latitude2
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
