package com.enterprise.payment.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// WebhookProcessingException.java
// Thrown when webhook event payload cannot be deserialized
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class WebhookProcessingException extends RuntimeException {
    public WebhookProcessingException(String message) { super(message); }
}
