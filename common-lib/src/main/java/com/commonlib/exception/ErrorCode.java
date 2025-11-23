package com.commonlib.exception;

public enum ErrorCode {

    INVALID_REQUEST("40001"),
    NOT_FOUND("40004"),
    CONFLICT("40901"),
    INTERNAL_ERROR("50001");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
