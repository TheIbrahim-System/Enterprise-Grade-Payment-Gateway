package com.enterprise.payment.entities;

import aj.org.objectweb.asm.commons.Remapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
@Entity
@Table(name = "idempotency_keys",indexes = {
        @Index(name = "idx_expires_at", columnList = "expiresAt")
})
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdempotencyKey {

    /**
     * WHAT IT DOES: Primary key — the idempotency key string itself.
     * WHAT HAPPENS: Key format is "pay_{orderId}_{clientNonce}".
     * Using the key as PK ensures uniqueness at DB level — even if
     * two concurrent requests race, only one INSERT will succeed.
     * The second will throw a DataIntegrityViolationException which
     * IdempotencyService catches and treats as a duplicate detection.
     */
    @Id
    @Column(name = "key_value", length = 200)
    private String keyValue;

    /**
     * WHAT IT DOES: Stores the serialized PaymentResponse JSON.
     * WHAT HAPPENS: After a successful payment, the response is
     * serialized to JSON and stored here. On duplicate requests,
     * this JSON is deserialized and returned — Stripe is never called again.
     * TEXT type supports up to 65535 chars — more than enough for any response.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String response;

    /**
     * WHAT IT DOES: Records when this key was first used.
     * WHAT HAPPENS: Stored for audit purposes — when was the original request?
     * updatable = false ensures this field is set once and never changed.
     */
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    /**
     * WHAT IT DOES: Defines when this key expires.
     * WHAT HAPPENS: Set to createdAt + 24 hours.
     * After expiry, duplicate detection no longer applies.
     * Nightly cleanup job deletes rows where expiresAt < now.
     * DB index on this column makes cleanup query fast even on large tables.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
