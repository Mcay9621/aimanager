package com.example.aimanager.common;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ErrorCode.SUCCESS.code);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(ErrorCode.SUCCESS.code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(ErrorCode.INTERNAL_ERROR.code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> badRequest(String message) {
        return error(ErrorCode.BAD_REQUEST.code, message);
    }

    public static <T> Result<T> unauthorized(String message) {
        return error(ErrorCode.UNAUTHORIZED.code, message);
    }

    public static <T> Result<T> forbidden(String message) {
        return error(ErrorCode.FORBIDDEN.code, message);
    }

    public static <T> Result<T> notFound(String message) {
        return error(ErrorCode.NOT_FOUND.code, message);
    }
}
