package com.enterprise.payment.exceptions;

import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// PaymentException.java
// WHAT IT DOES: Wraps Stripe errors into our domain exception.
// Carries stripeCode for logging without leaking to clients.
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
@Data
public class PaymentException extends RuntimeException {
    private final String stripeCode;
    public PaymentException(String stripeCode, String message) {
        super(message);
        this.stripeCode = stripeCode;
    }


}

