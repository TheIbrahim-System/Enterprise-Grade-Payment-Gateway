package com.enterprise.payment.repositories;

import com.enterprise.payment.entities.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment,Long> {

    /**
     * WHAT IT DOES: Finds payment by Stripe's own PaymentIntent ID.
     * WHAT HAPPENS: Used in WebhookService when Stripe sends an event.
     * The event contains the Stripe ID, not our internal UUID.
     * This query links the incoming webhook to our DB record.
     */
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    /**
     * WHAT IT DOES: Finds all payments for a specific user, paginated.
     * WHAT HAPPENS: Used in PaymentController GET /api/v1/payments.
     * Pageable allows clients to request page=0&size=20&sort=createdAt,desc.
     * Prevents loading thousands of records into memory at once.
     */
    Page<Payment> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * WHAT IT DOES: Finds payment by order ID for a specific user.
     * WHAT HAPPENS: Used to check if an order was already paid before
     * creating a new PaymentIntent — extra idempotency guard at DB level.
     */
    Optional<Payment> findByOrderIdAndUserId(String orderId, String userId);

    /**
     * WHAT IT DOES: Checks if an idempotency key already exists.
     * WHAT HAPPENS: Fast existence check before loading full entity.
     * More efficient than findByIdempotencyKey() when you only need
     * to know if it exists, not retrieve the full record.
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * WHAT IT DOES: Finds payment by idempotency key.
     * WHAT HAPPENS: DB-level deduplication fallback when Redis is unavailable.
     * IdempotencyService checks Redis first, falls back here.
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * WHAT IT DOES: Retrieves payments by status for admin reporting.
     * WHAT HAPPENS: Used for operations dashboards.
     * Example: find all FAILED payments today to investigate issues.
     */
    List<Payment> findByStatusAndCreatedAtBetween(
            Payment.PaymentStatus status, Instant from, Instant to);

    /**
     * WHAT IT DOES: Bulk status update using JPQL for efficiency.
     * WHAT HAPPENS: @Modifying + @Query bypasses entity loading.
     * Instead of: load entity → set status → save (3 DB ops per record),
     * this executes a single UPDATE statement for N records.
     * @Transactional is required — @Modifying queries must run in a txn.
     */
    @Modifying
    @Query("UPDATE Payment p SET p.status = :status WHERE p.id IN :ids")
    int bulkUpdateStatus(@Param("ids") List<String> ids,
                         @Param("status") Payment.PaymentStatus status);
}

