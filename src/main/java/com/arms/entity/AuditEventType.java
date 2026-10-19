package com.arms.entity;

/**
 * Security-relevant events recorded by the system (SRS FR-7.6).
 */
public enum AuditEventType {
    SIGN_IN,
    FAILED_SIGN_IN,
    USER_CREATED,
    USER_UPDATED,
    USER_DISABLED,
    USER_ENABLED,
    PASSWORD_CHANGED,
    PASSWORD_RESET,
    RENTAL_CANCELLED
}
