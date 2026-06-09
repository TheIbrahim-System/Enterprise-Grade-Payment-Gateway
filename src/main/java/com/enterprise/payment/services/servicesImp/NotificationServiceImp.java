package com.enterprise.payment.services.servicesImp;

import com.enterprise.payment.entities.Payment;
import com.enterprise.payment.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImp implements NotificationService {


    private final JavaMailSender mailSender;
    /**
     * WHAT IT DOES: Sends a payment confirmation email to the customer.
     *
     * WHAT HAPPENS:
     *   1. @Async runs this in a Spring-managed background thread
     *      — the HTTP response is already returned to the client by
     *        the time this method starts executing.
     *   2. Builds a SimpleMailMessage with payment details
     *   3. Sends via JavaMailSender (configured in application.yml)
     *   4. Any exception is caught and logged — it must NEVER propagate
     *      back to the payment flow (a failed email must not fail a payment).
     *
     * In production, replace SimpleMailMessage with MimeMessage
     * for HTML email templates (Thymeleaf + inline CSS).
     */
    @Async
    @Override
    public void sendPaymentConfirmation(Payment payment) {
        try
        {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(resolveUserEmail(payment.getUserId()));
            mailMessage.setSubject("Payment Confirmation for Order " + payment.getOrderId());
            mailMessage.setText(buildConfirmationText(payment));
            mailMessage.setFrom("payment@yourdomain.com");
            mailSender.send(mailMessage);
            log.info("Payment confirmation sent to your email");

        }
        catch (Exception e)
        {
            log.error("Failed to send payment confirmation email: {}", e.getMessage());
        }

    }
    /**
     * WHAT IT DOES: Notifies the customer that their payment failed.
     *
     * WHAT HAPPENS: Called from WebhookService.handlePaymentFailed().
     * Uses the failureCode from Payment entity to give a user-friendly
     * reason (e.g. "Your card was declined" not raw Stripe codes).
     * The email suggests what the user can do next:
     *   - Try a different card
     *   - Check card details
     *   - Contact their bank
     */
    @Async
    @Override
    public void sendPaymentFailedNotification(Payment payment) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(resolveUserEmail(payment.getUserId()));
            mailMessage.setSubject("Payment Failed Notification for Order " + payment.getOrderId());
            mailMessage.setText(buildFailureText(payment));
            mailMessage.setFrom("payment@yourdomain.com");
            mailSender.send(mailMessage);
            log.info("Payment failed notification sent to your email");
        }
        catch (Exception e) {
            log.error("Failed to send payment failed notification email: {}", e.getMessage());
        }

    }
    /**
     * WHAT IT DOES: Sends refund confirmation email.
     * WHAT HAPPENS: Called after Stripe confirms refund via webhook.
     * Includes refund amount, currency, and expected processing time
     * (typically 5-10 business days for card refunds).
     */
    @Async
    @Override
    public void sendRefundConfirmation(Payment payment, String refundAmount) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(resolveUserEmail(payment.getUserId()));
            mailMessage.setSubject("Refund Confirmation for Order " + payment.getOrderId());
            mailMessage.setText("Your refund of " + refundAmount + " " + payment.getCurrency()
                    + " has been processed. Please allow 5-10 business days.");
            mailMessage.setFrom("payment@yourdomain.com");
            mailSender.send(mailMessage);
            log.info("Refund confirmation sent to your email");
        }
        catch (Exception e) {
            log.error("Failed to send refund confirmation email: {}", e.getMessage());
        }
    }
    private String buildConfirmationText(Payment payment) {
        return String.format(
                "Thank you! Your payment of %s %s for order %s has been confirmed.%n"
                        + "Payment ID: %s%nDate: %s",
                payment.getAmount(), payment.getCurrency(),
                payment.getOrderId(), payment.getId(),
                payment.getCreatedAt());
    }

    private String buildFailureText(Payment payment) {
        String reason = mapFailureCode(payment.getFailureCode());
        return String.format(
                "Your payment for order %s could not be processed.%n"
                        + "Reason: %s%nPlease try again with a different payment method.",
                payment.getOrderId(), reason);
    }

    private String mapFailureCode(String code) {
        if (code == null) return "Payment declined";
        return switch (code) {
            case "card_declined"         -> "Your card was declined";
            case "insufficient_funds"    -> "Insufficient funds";
            case "expired_card"          -> "Card has expired";
            case "incorrect_cvc"         -> "Incorrect CVC code";
            default                      -> "Payment could not be processed";
        };
    }

    // In production: inject UserRepository and fetch email by userId
    private String resolveUserEmail(String userId) {
        return "user-" + userId + "@yourdomain.com"; // placeholder
    }
}

