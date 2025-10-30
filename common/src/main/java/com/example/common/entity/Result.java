package com.example.common.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用响应类
 */
@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }
}