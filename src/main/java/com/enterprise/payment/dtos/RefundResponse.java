package com.enterprise.payment.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class RefundResponse {
    private String refundId;          // Stripe refund ID: re_xxx
    private String paymentId;         // Our internal payment UUID
    private BigDecimal amount;
    private String currency;
    private String status;            // "pending", "succeeded", "failed"
    private Instant createdAt;
}
