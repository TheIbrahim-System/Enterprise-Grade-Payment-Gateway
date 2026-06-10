package com.enterprise.payment.services.servicesImp;

import com.enterprise.payment.dtos.CreatePaymentRequest;
import com.enterprise.payment.dtos.PaymentResponse;
import com.enterprise.payment.entities.AuditAction;
import com.enterprise.payment.entities.Payment;
import com.enterprise.payment.exceptions.PaymentException;
import com.enterprise.payment.repositories.PaymentRepository;
import com.enterprise.payment.services.PaymentService;
import com.stripe.StripeClient;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentServiceImp implements PaymentService {

    private final StripeClient stripeClient;
    private final PaymentRepository paymentRepository;
    private final IdempotencyServicesImp idempotencyService;
    private final AuditServicesImp auditService;
    private final NotificationServiceImp notificationService;
    private final MeterRegistry meterRegistry;
    private final ModelMapper modelMapper;

    /**
     * WHAT IT DOES: End-to-end payment creation with full enterprise guarantees.
     *
     * WHAT HAPPENS (step by step):
     *  1. Build idempotency key from orderId + client nonce
     *  2. Check if this key was already processed → return cached response
     *  3. Convert amount to cents (Stripe requirement)
     *  4. Build PaymentIntentCreateParams with all required fields
     *  5. Call Stripe API with circuit breaker + retry protection
     *  6. Persist payment record to DB with status=PENDING
     *  7. Save idempotency response to Redis + DB
     *  8. Emit audit log entry
     *  9. Record Prometheus metric
     * 10. Return response DTO to controller
     *
     * @Retry handles transient Stripe errors (network blips, rate limits).
     * @CircuitBreaker opens if Stripe has 50%+ failures over 10 calls.
     */
    @Retry(name = "stripeRetry", fallbackMethod = "paymentFallback")
    @CircuitBreaker(name = "stripeCircuitBreaker",
            fallbackMethod = "paymentFallback")
    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request, String userId, String ipAddress) {
        // STEP 1: Build idempotency key
        String idempotencyKey = idempotencyService.buildKey(
                request.getOrderId(), request.getIdempotencyNonce());

        // STEP 2: Check for duplicate request
        Optional<PaymentResponse> existing = idempotencyService.getExistingResponse(idempotencyKey);
        if(existing.isPresent()) {
            log.warn("Duplicate payment request detected for idempotency key {}. Returning cached response.", idempotencyKey);
            return existing.get();
        }

        //STEP 3 : Converting Amount into Smaller Unit
        long amountInCents = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

        // STEP 4: Build Stripe Params
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(request.getCurrency())
                .setPaymentMethod(request.getPaymentMethodId())
                .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.MANUAL)
                .setConfirm(true)
                .setOffSession(false)
                .putMetadata("orderId", request.getOrderId())
                .putMetadata("userId", userId)
                .putMetadata("idempotencyKey", idempotencyKey)
                .setDescription(sanitize(request.getDescription()))
                .setReturnUrl("https://yourapp.com/payment/return")
                .build();
        // STEP 5: Call Stripe (protected by retry + circuit breaker)
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey(idempotencyKey)
                .build();
        Timer.Sample timer = Timer.start(meterRegistry);
        PaymentIntent intent;
        try{
            intent = stripeClient.paymentIntents().create(params, options);
        } catch (Exception ex) {
            recordFailureMetric(request.getCurrency(), "stripe_api_error");
            log.error("Stripe API error for idempotency key {}: {}",ex.getCause(), idempotencyKey, ex.getMessage());
            throw new PaymentException("Payment processing failed. Please try again.", request.getOrderId());
        }finally {
            timer.stop(meterRegistry.timer("payment.stripe_api_latency",
                    "currency", request.getCurrency()));
        }
        // STEP 6: Persist to DB
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .stripePaymentIntentId(intent.getId())
                .userId(userId)
                .currency(request.getCurrency())
                .amount(request.getAmount())
                .status(mapStripeStatus(intent.getStatus()))
                .idempotencyKey(idempotencyKey)
                .build();
       Payment paymentSaved = paymentRepository.save(payment);

        // STEP 7: Map response
        PaymentResponse response = modelMapper.map(paymentSaved, PaymentResponse.class);

        // STEP 8: Save idempotency record
        idempotencyService.saveResponse(idempotencyKey, response);

        // STEP 9: Audit log
        auditService.log(AuditAction.PAYMENT_CREATED, String.valueOf(paymentSaved.getId()),
                userId, ipAddress, "PaymentIntent ID: " + intent.getId() + ", Amount: " + request.getAmount() + " " + request.getCurrency());

        // STEP 10: Metrics
        recordSuccessMetric(request.getCurrency(), request.getAmount());

        log.info("Payment created: id={}, stripe={}, amount={} {}",
                payment.getId(), intent.getId(),
                request.getAmount(), request.getCurrency());

        return response;
    }

    /**
     * WHAT IT DOES: Fallback when Stripe is unreachable after all retries.
     * WHAT HAPPENS: Circuit breaker or retry exhaustion calls this method.
     * We record the failure metric, log the full exception, and throw
     * a user-friendly exception — never expose raw Stripe errors to clients.
     */
    @Override
    public PaymentResponse paymentFallback(CreatePaymentRequest req, Exception ex) {
        log.error("Payment fallback triggered after retries exhausted", ex);
        meterRegistry.counter("payment.fallback.triggered").increment();
        throw new PaymentException(req.getOrderId(),
                "Payment service temporarily unavailable. Please try again.");
    }

    /**
     * WHAT IT DOES: Sanitizes free-text input from users.
     * WHAT HAPPENS: Strips control characters (ASCII 0-31) that could
     * cause issues in Stripe API or enable log injection attacks.
     * Enforces 500-char max to respect Stripe field length limits.
     */
    private String sanitize(String input) {
        if (input == null) return null;
        String clean = input.replaceAll("[\\p{Cntrl}]", "").strip();
        return clean.substring(0, Math.min(clean.length(), 500));
    }

    private void recordSuccessMetric(String currency, BigDecimal amount) {
        meterRegistry.counter("payment.success",
                "currency", currency).increment();
        meterRegistry.summary("payment.amount",
                "currency", currency).record(amount.doubleValue());
    }

    private void recordFailureMetric(String currency, String code) {
        meterRegistry.counter("payment.failure",
                "currency", currency, "stripe_code", code).increment();
    }

    private Payment.PaymentStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "succeeded"          -> Payment.PaymentStatus.SUCCEEDED;
            case "processing"         -> Payment.PaymentStatus.PROCESSING;
            case "requires_action"    -> Payment.PaymentStatus.PENDING;
            case "canceled"           -> Payment.PaymentStatus.CANCELLED;
            default                   -> Payment.PaymentStatus.PENDING;
        };
    }

    public Page<PaymentResponse> getPaymentById(String id, String name, Pageable pageable) {

        Page<PaymentResponse> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(id, pageable)
                .map(payment -> modelMapper.map(payment, PaymentResponse.class));
        return payments;
    }
}





