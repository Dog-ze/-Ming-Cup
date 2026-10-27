package com.example.backend.enums;

public enum InsomniaStatus {
    EASY(0, "容易"), NOT_EASY(1, "不容易"), UNCERTAIN(2, "不确定");

    private final int code;
    private final String label;

    InsomniaStatus(int code, String label) { this.code = code; this.label = label; }
    public int getCode() { return code; }
    public String getLabel() { return label; }
    public static InsomniaStatus fromCode(int code) {
        for (InsomniaStatus value : values()) if (value.code == code) return value;
        throw new IllegalArgumentException("未知失眠状态编码：" + code);
    }
}
