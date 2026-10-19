package com.arms.exception;

/** 400: the request is well-formed but semantically invalid. */
public class BadRequestException extends RuntimeException {

    private final String code;

    public BadRequestException(String message) {
        this(message, "BAD_REQUEST");
    }

    public BadRequestException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
