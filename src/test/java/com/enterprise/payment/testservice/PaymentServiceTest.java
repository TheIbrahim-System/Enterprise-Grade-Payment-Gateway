package com.enterprise.payment.testservice;

import com.enterprise.payment.dtos.CreatePaymentRequest;
import com.enterprise.payment.dtos.PaymentResponse;
import com.enterprise.payment.services.PaymentService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Test
    void createPaymentShouldPersistPayment() {

        CreatePaymentRequest request =
                new CreatePaymentRequest();

        request.setOrderId("ORD-TEST-001");
        request.setAmount(new BigDecimal("10.00"));
        request.setCurrency("usd");
        request.setPaymentMethodId("REAL_PM_ID");
        request.setDescription("Test Payment");
        request.setIdempotencyNonce("nonce123");

        PaymentResponse response =
                paymentService.createPayment(
                        request,
                        "test-user",
                        "127.0.0.1"
                );

        assertNotNull(response);
    }
}
