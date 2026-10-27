package com.example.backend.enums;

public enum StrengthPreference {
    LIGHT(0, "清淡"), MEDIUM(1, "适中"), STRONG(2, "浓厚");

    private final int code;
    private final String label;

    StrengthPreference(int code, String label) { this.code = code; this.label = label; }
    public int getCode() { return code; }
    public String getLabel() { return label; }
    public static StrengthPreference fromCode(int code) {
        for (StrengthPreference value : values()) if (value.code == code) return value;
        throw new IllegalArgumentException("未知浓度偏好编码：" + code);
    }
}
