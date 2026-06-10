package com.enterprise.payment.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// PaymentServiceUnavailableException.java
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class PaymentServiceUnavailableException extends RuntimeException {
    public PaymentServiceUnavailableException(String msg) { super(msg); }
}
