package com.example.backend.common;

/**
 * 用于向客户端返回预期的业务错误。
 */
public class BizException extends RuntimeException {
    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
