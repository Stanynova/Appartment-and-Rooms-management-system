package com.arms.controller;

import com.arms.dto.AuditDtos.AuditLogListResponse;
import com.arms.dto.AuditDtos.AuditLogResponse;
import com.arms.entity.AuditEventType;
import com.arms.entity.AuditLog;
import com.arms.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Audit log reading (SRS FR-7.6): security-relevant events with the user,
 * the date and the time. Restricted to administrators.
 */
@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public AuditLogListResponse list(
            @RequestParam(required = false) AuditEventType eventType,
            @RequestParam(defaultValue = "100") int limit) {
        Page<AuditLog> page = auditService.list(eventType, limit);
        return new AuditLogListResponse(
                page.getTotalElements(),
                page.getContent().stream()
                        .map(AuditLogController::toResponse)
                        .toList());
    }

    private static AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getEventType().name(),
                log.getUsername(),
                log.getUserId(),
                log.getDetails(),
                log.getEventTime());
    }
}
