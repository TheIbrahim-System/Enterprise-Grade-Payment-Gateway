package com.enterprise.payment.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// InvalidRefundAmountException.java
// Thrown when refund amount exceeds original charge
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidRefundAmountException extends RuntimeException {
    public InvalidRefundAmountException(String message) { super(message); }
}
