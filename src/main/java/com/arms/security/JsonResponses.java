package com.arms.security;

import com.arms.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Writes uniform JSON error documents for the security filter chain, so that
 * 401 and 403 responses have the same shape as controller errors
 * (SRS 3.1: no unhandled technical error reaches a user).
 */
public final class JsonResponses {

    private JsonResponses() {
    }

    public static void write(HttpServletResponse response,
                             ObjectMapper objectMapper,
                             HttpStatus status,
                             String code,
                             String message,
                             String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiError error = ApiError.of(status.value(), status.getReasonPhrase(), code, message, path);
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
