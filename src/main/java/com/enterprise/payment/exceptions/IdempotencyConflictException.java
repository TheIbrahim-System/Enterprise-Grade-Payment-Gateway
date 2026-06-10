package com.enterprise.payment.exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * WHAT IT DOES: Signals that a concurrent duplicate payment request
 * was detected — the first request is still processing.
 *
 * WHAT HAPPENS (race condition scenario):
 *   T=0ms  Request A arrives with key "pay_ORD001_abc" — starts processing
 *   T=5ms  Request B arrives with key "pay_ORD001_abc"
 *          Redis check: key NOT yet in Redis (A hasn't saved yet)
 *          DB check: key NOT yet in DB (A hasn't saved yet)
 *          IdempotencyService detects in-flight lock → throws this exception
 *
 * WHY 409 (Conflict) not 429 (Too Many Requests):
 *   - 409 signals: "same resource being created concurrently"
 *   - Client should wait briefly and poll for the first request's result
 *   - 429 would imply rate limiting, which is a different concept
 *
 * HOW in-flight detection works:
 *   IdempotencyService uses a Redis SETNX (set-if-not-exists) with a
 *   short TTL as a distributed lock before processing starts.
 *   If SETNX returns false, the key is in-flight → throw this exception.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class IdempotencyConflictException extends RuntimeException {

    private final String idempotencyKey;

    public IdempotencyConflictException(String idempotencyKey) {
        super("Request with idempotency key is already being processed: "
                + idempotencyKey);
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
