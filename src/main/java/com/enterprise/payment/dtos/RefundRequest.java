package com.enterprise.payment.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    /**
     * WHAT IT DOES: References which payment to refund.
     * WHAT HAPPENS: RefundService loads the Payment entity by this ID,
     * verifies it belongs to the requesting user, and checks it is in
     * SUCCEEDED status before calling Stripe Refunds API.
     */
    @NotBlank(message = "Payment ID is required")
    private String paymentId;

    /**
     * WHAT IT DOES: Specifies refund amount — null means full refund.
     *
     * WHAT HAPPENS:
     *   - If null   → RefundService refunds the full original amount
     *   - If provided → RefundService validates amount <= original charge,
     *                   then issues partial refund to Stripe.
     *
     * @DecimalMin("0.01") prevents zero-amount refunds which would
     * be accepted by Stripe but are logically meaningless.
     *
     * BigDecimal is used (not double/float) because floating-point
     * arithmetic is imprecise for money. 0.1 + 0.2 != 0.3 in float.
     */
    @DecimalMin(value = "0.01", message = "Refund amount must be at least 0.01")
    @Digits(integer = 8, fraction = 2,
            message = "Amount must have max 2 decimal places")
    private BigDecimal amount;      // null = full refund

    /**
     * WHAT IT DOES: Prevents duplicate refunds on retry.
     * WHAT HAPPENS: Client generates this nonce once and keeps it
     * fixed across retries. RefundService builds idempotency key:
     * "refund_{paymentId}_{idempotencyNonce}".
     * Stripe uses this to ensure only one refund is created even
     * if the client retries due to a network timeout.
     */
    @NotBlank(message = "Idempotency nonce is required")
    @Size(min = 8, max = 36)
    private String idempotencyNonce;

    /**
     * WHAT IT DOES: Optional reason for the refund.
     * WHAT HAPPENS: Stored in audit log and passed to Stripe metadata.
     * Useful for operations teams to understand why refunds are happening.
     * Examples: "customer_request", "duplicate_charge", "product_defect"
     */
    @Size(max = 200)
    private String reason;
}

