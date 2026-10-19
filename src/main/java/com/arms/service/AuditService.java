package com.arms.service;

import com.arms.entity.AuditEventType;
import com.arms.entity.AuditLog;
import com.arms.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Records security-relevant events (SRS FR-7.6): sign-in, failed sign-in,
 * user account changes and rental cancellations, each with the user, the date
 * and the time.
 *
 * <p>{@link #record} deliberately carries no transaction annotation: when it
 * is called from inside a service transaction it joins that transaction and
 * therefore stays consistent with the operation it describes (NFR-SF-2); when
 * it is called from a controller the repository save opens its own
 * transaction.</p>
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogs;

    public AuditService(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    public void record(AuditEventType eventType, String username, Long userId, String details) {
        AuditLog log = new AuditLog();
        log.setEventType(eventType);
        log.setUsername(truncate(username, 50));
        log.setUserId(userId);
        log.setDetails(truncate(details, 500));
        log.setEventTime(LocalDateTime.now());
        auditLogs.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> list(AuditEventType eventType, int limit) {
        int size = Math.max(1, Math.min(limit, 500));
        PageRequest pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "eventTime"));
        if (eventType != null) {
            return auditLogs.findByEventTypeOrderByEventTimeDesc(eventType, pageable);
        }
        return auditLogs.findAll(pageable);
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
