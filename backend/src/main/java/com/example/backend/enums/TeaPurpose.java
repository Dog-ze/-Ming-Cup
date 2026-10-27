package com.example.backend.enums;

public enum TeaPurpose {
    DIGEST(0, "解腻"), REFRESH(1, "提神"), WARM_STOMACH(2, "暖胃"),
    REDUCE_HEAT(3, "降火"), RELAX(4, "放松"), CASUAL(5, "随便");

    private final int code;
    private final String label;

    TeaPurpose(int code, String label) { this.code = code; this.label = label; }
    public int getCode() { return code; }
    public String getLabel() { return label; }
    public static TeaPurpose fromCode(int code) {
        for (TeaPurpose value : values()) if (value.code == code) return value;
        throw new IllegalArgumentException("未知喝茶目的编码：" + code);
    }
}
