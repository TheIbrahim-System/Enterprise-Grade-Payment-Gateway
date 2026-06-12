package com.enterprise.payment.services.servicesImp;


import com.enterprise.payment.dtos.RefundRequest;
import com.enterprise.payment.dtos.RefundResponse;
import com.enterprise.payment.entities.AuditAction;
import com.enterprise.payment.entities.Payment;
import com.enterprise.payment.exceptions.*;
import com.enterprise.payment.repositories.PaymentRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final StripeClient stripeClient;
    private final PaymentRepository paymentRepository;
    private final AuditServicesImp auditService;

    /**
     * WHAT IT DOES: Initiates a full or partial refund via Stripe.
     * WHAT HAPPENS:
     *  1. Load payment from DB — verify it exists and belongs to this user
     *  2. Check payment is in SUCCEEDED status (can't refund FAILED payments)
     *  3. Validate refund amount <= original charge amount
     *  4. Call Stripe Refunds.create() with idempotency key
     *  5. Update payment status in DB
     *  6. Write audit log entry
     *
     * IDEMPOTENCY: If refund fails midway and is retried, the same
     * idempotency key prevents Stripe from creating a second refund.
     */
    @Transactional
    @Retry(name = "stripeRetry")
    public RefundResponse initiateRefund(RefundRequest request,
                                         String userId, String ipAddress) {

        // Load and validate payment
        Payment payment = paymentRepository.findById(Long.valueOf(request.getPaymentId()))
                .orElseThrow(() -> new PaymentNotFoundException(request.getPaymentId()));

        if (!payment.getUserId().equals(userId)) {
            throw new UnauthorizedException("Not your payment");
        }

        if (payment.getStatus() != Payment.PaymentStatus.SUCCEEDED) {
            throw new InvalidPaymentStateException(
                    "Can only refund succeeded payments. Current: " + payment.getStatus());
        }

        BigDecimal refundAmount = request.getAmount() != null
                ? request.getAmount() : payment.getAmount();

        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new InvalidRefundAmountException("Refund exceeds original charge");
        }

        // Build Stripe refund params
        RefundCreateParams params = RefundCreateParams.builder()
               // .setPaymentIntent(payment.getStripePaymentIntentId())
                .setAmount(refundAmount.movePointRight(2).longValueExact())
                .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                .putMetadata("initiated_by", userId)
                .build();

        String refundIdempotencyKey = "refund_" + payment.getId()
                + "_" + request.getIdempotencyNonce();

        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey(refundIdempotencyKey)
                .build();

        try {
            Refund refund = stripeClient.refunds().create(params, options);

            // Update payment status
            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            // Audit
            auditService.log(AuditAction.REFUND_INITIATED, String.valueOf(payment.getId()),
                    userId, ipAddress, "refund_id=" + refund.getId()
                            + " amount=" + refundAmount);

            return RefundResponse.builder()
                    .refundId(refund.getId())
                    .paymentId(String.valueOf(payment.getId()))
                    .amount(refundAmount)
                    .currency(payment.getCurrency())
                    .status(refund.getStatus())
                    .build();

        } catch (StripeException e) {
            log.error("Refund failed: {}", e.getMessage());
            throw new PaymentException(e.getCode(), "Refund failed: " + e.getMessage());
        }
    }
}
