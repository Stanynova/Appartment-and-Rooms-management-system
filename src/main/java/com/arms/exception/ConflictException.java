package com.arms.exception;

/**
 * 409: the request conflicts with a business rule or the current state of the
 * system (duplicate names, double booking, blocked operations, ...).
 */
public class ConflictException extends RuntimeException {

    private final String code;

    /** True when the caller may retry with an explicit confirmation flag (FR-1.5). */
    private final boolean confirmationRequired;

    public ConflictException(String message, String code) {
        this(message, code, false);
    }

    public ConflictException(String message, String code, boolean confirmationRequired) {
        super(message);
        this.code = code;
        this.confirmationRequired = confirmationRequired;
    }

    /** Signals FR-1.5: warn the user and require explicit confirmation. */
    public static ConflictException confirmRequired(String message, String code) {
        return new ConflictException(message, code, true);
    }

    public String getCode() {
        return code;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }
}
