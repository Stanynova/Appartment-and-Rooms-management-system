package com.arms.repository;

import com.arms.entity.AuditEventType;
import com.arms.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByEventTypeOrderByEventTimeDesc(AuditEventType eventType, Pageable pageable);
}
