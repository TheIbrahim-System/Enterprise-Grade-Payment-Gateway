package com.enterprise.payment.services;

import com.enterprise.payment.entities.Payment;

public interface NotificationService {

    public void sendPaymentConfirmation(Payment payment);
    public void sendPaymentFailedNotification(Payment payment);
    public void sendRefundConfirmation(Payment payment, String refundAmount);

}
