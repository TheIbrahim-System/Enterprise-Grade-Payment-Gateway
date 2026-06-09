package com.enterprise.payment.services.servicesImp;

import com.enterprise.payment.entities.AuditAction;
import com.enterprise.payment.entities.AuditLog;
import com.enterprise.payment.repositories.AuditLogRepository;
import com.enterprise.payment.services.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditServicesImp implements AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * WHAT IT DOES: Writes an immutable audit record.
     * WHAT HAPPENS: Every call creates a NEW row in audit_logs — rows
     * are never updated or deleted. This provides a tamper-evident trail.
     * correlationId links this entry to the current HTTP request trace.
     * @Async ensures audit writing never slows down the payment response.
     *
     * In regulated industries (PCI-DSS, SOX), audit logs must be:
     *   - Append-only (no updates/deletes)
     *   - Timestamped with UTC
     *   - Correlated with user + IP
     */
    @Async
    public void log(AuditAction action, String entityId,
                    String userId, String ipAddress, String details) {
        try {
            AuditLog entry = AuditLog.builder()
                    .action(action.name())
                    .entityId(entityId)
                    .userId(userId)
                    .ipAddress(ipAddress)
                    .details(details)
                    .correlationId(MDC.get("correlationId"))
                    .createdAt(Instant.now())
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Audit failure must NEVER break the main payment flow
            log.error("Audit log write failed: {}", e.getMessage());
        }
    }
}

