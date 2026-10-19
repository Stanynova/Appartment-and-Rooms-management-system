package com.arms.exception;

/** 404: a referenced entity does not exist. */
public class NotFoundException extends RuntimeException {

    private final String code;

    public NotFoundException(String message) {
        this(message, "NOT_FOUND");
    }

    public NotFoundException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
