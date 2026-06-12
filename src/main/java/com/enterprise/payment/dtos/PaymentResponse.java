package com.enterprise.payment.dtos;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
