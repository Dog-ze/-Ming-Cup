package com.example.backend.pairing.store.vo;

import com.example.backend.entity.PairingStore;

import java.math.BigDecimal;

/**
 * 返回给前端的一家店铺信息。
 */
public record StoreVO(
        Long id,
        String storeName,
        String coverImage,
        String address,
        BigDecimal longitude,
        BigDecimal latitude,
        String phone,
        String description,
        Double distanceMeters,
        boolean promotion
) {
    /**
     * 将本项目数据库中的店铺转换为前端对象。
     *
     * @param entity 店铺实体
     * @param distanceMeters 用户到店铺的距离；没有定位时为 null
     * @param promotion 是否为当前生效的推广店铺
     * @return 前端可用的店铺对象
     */
    public static StoreVO from(PairingStore entity, Double distanceMeters, boolean promotion) {
        return new StoreVO(
                entity.getId(),
                entity.getStoreName(),
                entity.getCoverImage(),
                entity.getAddress(),
                entity.getLongitude(),
                entity.getLatitude(),
                entity.getPhone(),
                entity.getDescription(),
                distanceMeters,
                promotion
        );
    }

    /**
     * 将地图店铺与后台合作店合并为一条前端结果。
     * 地图坐标与距离更接近用户当前位置，后台资料则用于展示推广店的封面和介绍。
     */
    public static StoreVO fromPartnerMatch(
            StoreVO mapStore,
            PairingStore partner,
            boolean promotion
    ) {
        return new StoreVO(
                partner.getId(),
                partner.getStoreName(),
                partner.getCoverImage(),
                partner.getAddress() == null || partner.getAddress().isBlank()
                        ? mapStore.address() : partner.getAddress(),
                mapStore.longitude(),
                mapStore.latitude(),
                partner.getPhone(),
                partner.getDescription() == null || partner.getDescription().isBlank()
                        ? mapStore.description() : partner.getDescription(),
                mapStore.distanceMeters(),
                promotion
        );
    }

    /**
     * 创建从 OpenStreetMap 查询到的附近店铺对象。
     *
     * @param storeName 店铺名称
     * @param address 地址
     * @param longitude 经度
     * @param latitude 纬度
     * @param distanceMeters 距离
     * @return 前端可用的店铺对象
     */
    public static StoreVO fromOpenStreetMap(
            String storeName,
            String address,
            BigDecimal longitude,
            BigDecimal latitude,
            Double distanceMeters
    ) {
        return new StoreVO(
                null,
                storeName,
                null,
                address,
                longitude,
                latitude,
                null,
                "来自附近公开地图店铺信息",
                distanceMeters,
                false
        );
    }

}
