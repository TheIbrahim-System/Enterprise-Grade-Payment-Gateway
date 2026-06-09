package com.enterprise.payment.repositories;

import com.enterprise.payment.entities.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * WHAT IT DOES: Retrieves all audit events for a specific entity (payment).
     * WHAT HAPPENS: Used by admin endpoints to show the full history of a
     * payment: CREATED → WEBHOOK_RECEIVED → SUCCEEDED → REFUND_INITIATED.
     * Essential for customer support and dispute resolution.
     */
    List<AuditLog> findByEntityIdOrderByCreatedAtAsc(String entityId);

    /**
     * WHAT IT DOES: Retrieves all actions performed by a specific user.
     * WHAT HAPPENS: Used for compliance audits — "show everything user X
     * did in the last 30 days". Paginated to handle large result sets.
     */
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(
            String userId, Pageable pageable);

    /**
     * WHAT IT DOES: Time-range audit query for reporting.
     * WHAT HAPPENS: Used for daily audit reports.
     * Example: all PAYMENT_CREATED events between 2024-01-01 and 2024-01-31.
     */
    List<AuditLog> findByActionAndCreatedAtBetween(
            String action, Instant from, Instant to);

    /**
     * WHAT IT DOES: Retrieves audit logs by correlation ID.
     * WHAT HAPPENS: A correlation ID is set per HTTP request in MDC.
     * This query retrieves every audit event that happened during
     * one specific request — useful for tracing a full request flow.
     */
    List<AuditLog> findByCorrelationId(String correlationId);
}

