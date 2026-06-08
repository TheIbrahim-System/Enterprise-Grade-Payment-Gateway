package com.enterprise.payment.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_order_id", columnList = "order_id"),
        @Index(name = "idx_stripe_id", columnList = "stripe_payment_intent_id"),
        @Index(name = "idx_user_id", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false,unique = true)
    private String userId;

    @Column(name = "stripe_payment_intent_id", unique = true)
    private String stripePaymentIntentId;  // Stripe's reference to the payment intent

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;               // internal order reference

    @Column(name = "currency", nullable = false,length = 3)
    private String currency;         // ISO 4217: "usd", "eur"

    @Column(name = "amount", nullable = false,precision = 19,scale = 4)
    private BigDecimal amount;   // Always store exact decimals for money

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;             // PENDING, SUCCEEDED, FAILED

    @Column(name = "failure_code")
    private String failureCode;               // Stripe error code if failed

    @Column(name = "failure_message")
    private String failureMessage;

    @Version
    private Long version;                     // Optimistic locking — prevents
    // concurrent update conflicts

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;            // Stored to prevent duplicates

    public enum PaymentStatus {
        PENDING, PROCESSING, SUCCEEDED, FAILED, CANCELLED, REFUNDED
    }
}


