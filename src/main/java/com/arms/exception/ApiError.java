package com.arms.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Uniform error document returned by the API (SRS 3.1: no unhandled
 * technical error is ever presented to a user).
 *
 * @param timestamp           when the error occurred
 * @param status              HTTP status code
 * @param error               HTTP status phrase
 * @param code                stable machine-readable error code
 * @param message             human-readable, user-facing message
 * @param path                request path that produced the error
 * @param confirmationRequired present and true when the caller may retry with
 *                            an explicit confirmation (SRS FR-1.5)
 * @param fieldErrors         field name to message, present on validation failures
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        Boolean confirmationRequired,
        Map<String, String> fieldErrors) {

    public static ApiError of(int status, String error, String code, String message, String path) {
        return new ApiError(LocalDateTime.now(), status, error, code, message, path, null, null);
    }
}
