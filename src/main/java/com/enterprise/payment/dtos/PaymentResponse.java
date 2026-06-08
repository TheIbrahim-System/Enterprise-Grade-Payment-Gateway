package com.enterprise.payment.dtos;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class PaymentResponse {
    private String paymentId;           // Your internal UUID
    private String clientSecret;        // Stripe client_secret for frontend confirm
    private String stripePaymentIntentId;
    private String status;              // "PENDING", "SUCCEEDED", etc.
    private BigDecimal amount;
    private String currency;
    private String orderId;
    private Instant createdAt;
    private String message;             // Human-readable status message
}
