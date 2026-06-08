package com.enterprise.payment.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String action;          // "PAYMENT_CREATED", "REFUND_INITIATED"

    @Column(name = "entity_id")
    private String entityId;        // Payment ID or order ID

    @Column(name = "user_id")
    private String userId;          // Who performed the action

    @Column(name = "ip_address")
    private String ipAddress;       // For fraud/security auditing

    @Column(columnDefinition = "TEXT")
    private String details;         // JSON: before/after state, amounts

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "correlation_id")
    private String correlationId;   // Traces one request across microservices
}
