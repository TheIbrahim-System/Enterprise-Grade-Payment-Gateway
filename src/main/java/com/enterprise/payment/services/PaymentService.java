package com.enterprise.payment.services;

import com.enterprise.payment.dtos.CreatePaymentRequest;
import com.enterprise.payment.dtos.PaymentResponse;

public interface PaymentService {
    public PaymentResponse createPayment(CreatePaymentRequest request,
                                         String userId,String ipAddress);

    public PaymentResponse paymentFallback(
            CreatePaymentRequest req,
            String userId,
            String ipAddress,
            Exception ex);


}
