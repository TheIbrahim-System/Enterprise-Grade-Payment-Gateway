package com.enterprise.payment.dtos;


import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentRequest {

    @NotBlank(message = "Order ID is required")
    @Size(max = 100)
    private String orderId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.50", message = "Minimum charge is 0.50")
    @DecimalMax(value = "999999.99")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z]{3}$", message = "Invalid currency code")
    private String currency;

    @NotBlank(message = "Payment method is required")
    private String paymentMethodId;     // pm_xxxx from Stripe.js

    @Size(max = 500)
    private String description;

    // Idempotency nonce from client — stays fixed across retries
    @NotBlank
    private String idempotencyNonce;

    private Map<String, String> metadata;  // Custom key-value pairs
}

