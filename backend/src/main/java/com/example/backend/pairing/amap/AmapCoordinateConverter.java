package com.example.backend.pairing.amap;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// 把用户定位坐标转换成高德地图能正确使用的坐标。
@Component
public class AmapCoordinateConverter {
    private static final double EARTH_SEMI_MAJOR_AXIS = 6_378_245D;
    private static final double ECCENTRICITY_SQUARED = 0.00669342162296594323D;

    public record Coordinate(BigDecimal longitude, BigDecimal latitude) {
    }

    /**
     * 将 WGS84 坐标系的经纬度转换为 GCJ02 坐标系的经纬度。
     *
     * @param longitude        经度
     * @param latitude         纬度
     * @param coordinateSystem 坐标系类型，必须为 "wgs84" 才会进行转换，否则直接返回原始坐标
     * @return 转换后的 GCJ02 坐标，如果输入坐标在中国境外，则返回原始坐标
     */
    public Coordinate toGcj02(BigDecimal longitude, BigDecimal latitude, String coordinateSystem) {
        if (longitude == null || latitude == null || !"wgs84".equalsIgnoreCase(coordinateSystem)
                || outsideChina(longitude.doubleValue(), latitude.doubleValue())) {
            return new Coordinate(longitude, latitude);
        }

        double longitudeValue = longitude.doubleValue();
        double latitudeValue = latitude.doubleValue();
        double latitudeOffset = transformLatitude(longitudeValue - 105D, latitudeValue - 35D);
        double longitudeOffset = transformLongitude(longitudeValue - 105D, latitudeValue - 35D);
        double latitudeRadians = latitudeValue / 180D * Math.PI;
        double magic = 1D - ECCENTRICITY_SQUARED * Math.sin(latitudeRadians) * Math.sin(latitudeRadians);
        double squareRootMagic = Math.sqrt(magic);

        latitudeOffset = latitudeOffset * 180D / (
                (EARTH_SEMI_MAJOR_AXIS * (1D - ECCENTRICITY_SQUARED)) / (magic * squareRootMagic) * Math.PI
        );
        longitudeOffset = longitudeOffset * 180D / (
                EARTH_SEMI_MAJOR_AXIS / squareRootMagic * Math.cos(latitudeRadians) * Math.PI
        );

        return new Coordinate(
                BigDecimal.valueOf(longitudeValue + longitudeOffset),
                BigDecimal.valueOf(latitudeValue + latitudeOffset)
        );
    }

    /**
     * 判断给定的经纬度是否在中国境外。
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 如果在中国境外，返回 true；否则返回 false
     */
    private boolean outsideChina(double longitude, double latitude) {
        return longitude < 72.004D || longitude > 137.8347D || latitude < 0.8293D || latitude > 55.8271D;
    }

    /**
     * 转换纬度。
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 转换后的纬度偏移量
     */
    private double transformLatitude(double longitude, double latitude) {
        double result = -100D + 2D * longitude + 3D * latitude + 0.2D * latitude * latitude
                + 0.1D * longitude * latitude + 0.2D * Math.sqrt(Math.abs(longitude));
        result += (20D * Math.sin(6D * longitude * Math.PI) + 20D * Math.sin(2D * longitude * Math.PI)) * 2D / 3D;
        result += (20D * Math.sin(latitude * Math.PI) + 40D * Math.sin(latitude / 3D * Math.PI)) * 2D / 3D;
        result += (160D * Math.sin(latitude / 12D * Math.PI) + 320D * Math.sin(latitude * Math.PI / 30D)) * 2D / 3D;
        return result;
    }

    /**
     * 转换经度。
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 转换后的经度偏移量
     */
    private double transformLongitude(double longitude, double latitude) {
        double result = 300D + longitude + 2D * latitude + 0.1D * longitude * longitude
                + 0.1D * longitude * latitude + 0.1D * Math.sqrt(Math.abs(longitude));
        result += (20D * Math.sin(6D * longitude * Math.PI) + 20D * Math.sin(2D * longitude * Math.PI)) * 2D / 3D;
        result += (20D * Math.sin(longitude * Math.PI) + 40D * Math.sin(longitude / 3D * Math.PI)) * 2D / 3D;
        result += (150D * Math.sin(longitude / 12D * Math.PI) + 300D * Math.sin(longitude / 30D * Math.PI)) * 2D / 3D;
        return result;
    }
}
