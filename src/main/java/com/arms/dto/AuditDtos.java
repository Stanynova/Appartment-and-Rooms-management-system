package com.arms.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Audit log endpoints (SRS FR-7.6). */
public final class AuditDtos {

    private AuditDtos() {
    }

    public record AuditLogResponse(
            Long id,
            String eventType,
            String username,
            Long userId,
            String details,
            LocalDateTime eventTime) {
    }

    public record AuditLogListResponse(
            long totalElements,
            List<AuditLogResponse> entries) {
    }
}
