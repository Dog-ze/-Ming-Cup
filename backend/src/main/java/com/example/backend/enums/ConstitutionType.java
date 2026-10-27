package com.example.backend.enums;

public enum ConstitutionType {
    COLD_SENSITIVE(0, "怕冷"), HEAT_SENSITIVE(1, "怕热"), NEUTRAL(2, "都还好");

    private final int code;
    private final String label;

    ConstitutionType(int code, String label) { this.code = code; this.label = label; }
    public int getCode() { return code; }
    public String getLabel() { return label; }
    public static ConstitutionType fromCode(int code) {
        for (ConstitutionType value : values()) if (value.code == code) return value;
        throw new IllegalArgumentException("未知自述状态编码：" + code);
    }
}
