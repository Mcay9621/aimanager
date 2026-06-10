package com.example.aimanager.common;

public enum ErrorCode {
    SUCCESS(200),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    INTERNAL_ERROR(500);

    public final int code;

    ErrorCode(int code) {
        this.code = code;
    }
}
