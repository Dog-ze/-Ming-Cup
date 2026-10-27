package com.example.backend.enums;

public enum FlavorPreference {
    LIGHT_FRESH(0, "清淡鲜爽"), FLORAL_FRUITY(1, "花香果香"), MELLOW_SWEET(2, "醇厚回甘"),
    AGED_RICH(3, "陈香厚重"), SWEET_MELLOW(4, "甜润温和");

    private final int code;
    private final String label;

    FlavorPreference(int code, String label) { this.code = code; this.label = label; }
    public int getCode() { return code; }
    public String getLabel() { return label; }
    public static FlavorPreference fromCode(int code) {
        for (FlavorPreference value : values()) if (value.code == code) return value;
        throw new IllegalArgumentException("未知口感偏好编码：" + code);
    }
}
