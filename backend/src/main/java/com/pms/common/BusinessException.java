package com.pms.common;

/**
 * 业务异常 — 用于Service层抛出可预见的业务逻辑错误
 * 替代直接抛出 RuntimeException，符合阿里巴巴异常处理规约
 *
 * @author ROY
 * @date 2026/05/27
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
