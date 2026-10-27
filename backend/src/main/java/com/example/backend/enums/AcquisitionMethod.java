package com.example.backend.enums;

public enum AcquisitionMethod {
    ONLINE("线上购买"), OFFLINE("线下购买"), GIFT("朋友赠送"), OTHER("其他");

    private final String value;

    AcquisitionMethod(String value) { this.value = value; }
    public String getValue() { return value; }
    public static AcquisitionMethod fromValue(String value) {
        for (AcquisitionMethod method : values()) if (method.value.equals(value)) return method;
        throw new IllegalArgumentException("未知获取方式：" + value);
    }
}
