package com.enterprise.payment.services;

import com.enterprise.payment.dtos.PaymentResponse;

import java.util.Optional;

public interface IdempotencyService {
    public Optional<PaymentResponse> getExistingResponse(String idempotencyKey);
    public void saveResponse(String idempotencyKey, PaymentResponse response);

}
