package com.example.backend.pairing.osm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenStreetMap / Overpass 服务配置。
 */
@Data
@ConfigurationProperties(prefix = "app.osm")
public class OpenStreetMapProperties {

    /**
     * 是否启用 OSM 附近店铺查询。
     * 开发初期可先设为 false。
     */
    private boolean enabled = false;

    /**
     * Overpass API 的基础地址。
     */
    private String baseUrl = "https://overpass-api.de";

    /**
     * 默认搜索半径，单位：米。
     */
    private int defaultRadiusMeters = 3000;

    /**
     * 最大搜索半径，单位：米。
     */
    private int maxRadiusMeters = 5000;
}