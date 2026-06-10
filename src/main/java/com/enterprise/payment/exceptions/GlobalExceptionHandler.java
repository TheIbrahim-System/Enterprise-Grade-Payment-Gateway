package com.enterprise.payment.exceptions;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * WHAT IT DOES: Catches validation failures on @Valid request bodies.
     * WHAT HAPPENS: Returns 400 with a map of field → error message.
     * Example: { "amount": "must be > 0.50", "currency": "required" }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null
                                ? fe.getDefaultMessage() : "Invalid value"));

        return Map.of(
                "status", 400,
                "error", "Validation Failed",
                "fields", fieldErrors,
                "timestamp", Instant.now()
        );
    }

    /**
     * WHAT IT DOES: Handles payment processing failures.
     * WHAT HAPPENS: Returns 402 Payment Required with sanitized message.
     * stripeCode is logged internally but NEVER sent to client.
     */
    @ExceptionHandler(PaymentException.class)
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public Map<String, Object> handlePaymentException(PaymentException ex) {
        log.error("Payment failed [stripeCode={}]: {}",
                ex.getStripeCode(), ex.getMessage());
        return Map.of(
                "status", 402,
                "error", "Payment Failed",
                "message", ex.getMessage(),
                "timestamp", Instant.now()
        );
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(PaymentNotFoundException ex) {
        return Map.of("status", 404, "error", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        // Never expose internals to client
        return Map.of("status", 500, "error", "Internal server error");
    }
}

