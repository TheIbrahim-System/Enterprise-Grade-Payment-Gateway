package com.enterprise.payment.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidPaymentStateException extends RuntimeException {
    public InvalidPaymentStateException(String message) { super(message); }
}

