package com.enterprise.payment.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// WebhookSignatureException.java
// Thrown when Stripe webhook signature verification fails
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class WebhookSignatureException extends RuntimeException {
    public WebhookSignatureException(String message) { super(message); }
}
