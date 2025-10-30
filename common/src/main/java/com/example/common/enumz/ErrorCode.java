package com.example.common.enumz;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {
    SUCCESS(200, "success"),
    SERVER_ERROR(500, "服务器错误"),
    PARAM_ERROR(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    LOCK_FAIL(1001, "获取分布式锁失败"),
    LOCK_TIMEOUT(1002, "分布式锁超时"),
    SERVICE_UNAVAILABLE(2001, "服务不可用");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}